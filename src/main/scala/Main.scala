import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive, ExceptionHandler, RejectionHandler, Route}
import akka.stream.ActorMaterializer
import config.AppConfig
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import mongo.MongoPersistenceClient
import org.apache.logging.log4j.scala.Logging
import persistance.{PersistenceException, UserPostedToNotHisGroupException}
import rest.RequestHandler
import rest.entities.NewGroupPost

import scala.io.StdIn
import scala.util.control.NonFatal

object Main extends App with Logging {
  implicit val system = ActorSystem("feed-system")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val clock = java.time.Clock.systemUTC()

  val appConfig: AppConfig = AppConfig(
    connectionString = "mongodb://root:example@localhost:27017",
    dbName = "my-db",
    httpRestApiPort = 8080
  )

  val bindingFuture = for {
    requestHandler <- MongoPersistenceClient(appConfig).map(new RequestHandler(appConfig, _))
    serverBinding  <- Http().bindAndHandle(routing(requestHandler), "localhost", appConfig.httpRestApiPort)
  } yield serverBinding

  println(s"Server online at http://localhost:${appConfig.httpRestApiPort}/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

  def routing(requestHandler: RequestHandler): Route = {
    handleExceptions(exceptionHandler) {
      logDuration {
        get {
          pathPrefix("user" / LongNumber) { userId =>
            path("groups") {
              onSuccess(requestHandler.getUserGroups(userId)) { case l => complete(l) }
            } ~
              path("all-groups-feed") {
                onSuccess(requestHandler.getAllGroupsFeedForUser(userId)) { case l => complete(l) }
              }
          } ~
            pathPrefix("group" / LongNumber) { groupId =>
              path("feed") {
                onSuccess(requestHandler.getGroupFeed(groupId)) {
                  case l => complete(l)
                }
              }
            }
        } ~
          post {
            pathPrefix("user" / LongNumber) { userId =>
              path("add-to-group" / LongNumber) { groupId =>
                onSuccess(requestHandler.addUserToGroup(userId, groupId)) {
                  logger.info(s"user $userId successfully added to group $groupId")
                  complete(StatusCodes.OK)
                }
              }
            } ~
              path("group" / LongNumber) { groupId =>
                entity(as[NewGroupPost]) { groupPost =>
                  onSuccess(requestHandler.addPostToGroup(groupId, groupPost)) {
                    case Right(postId) => complete(postId)
                    case Left(t: UserPostedToNotHisGroupException) =>
                      complete(
                        StatusCodes.Forbidden -> s"User ${t.userId} has not enough permissions to post on ${t.groupId}."
                      )
                    case Left(t: Throwable) =>
                      logger.error(s"Got error while adding post $groupPost to $groupId.", t)
                      complete(StatusCodes.BadRequest)
                  }
                }
              }
          }
      }
    }
  }

  def logDuration: Directive[Unit] = {
    val rejectionHandler = RejectionHandler.default

    extractRequestContext.flatMap { ctx =>
      val start = System.currentTimeMillis()
      // handling rejections here so that we get proper status codes
      mapResponse { resp =>
        val d = System.currentTimeMillis() - start
        logger.info(s"[${resp.status.intValue()}] ${ctx.request.method.name} ${ctx.request.uri} took: ${d}ms")
        resp
      } & handleRejections(rejectionHandler)
    }
  }

  def exceptionHandler = {
    ExceptionHandler {
      case ex: PersistenceException =>
        logger.error("Persistence threw error.", ex)
        complete(StatusCodes.Forbidden -> "Illegal operation.")
      case NonFatal(ex: Exception) =>
        logger.error("I crashed hard.", ex)
        complete(StatusCodes.BadRequest)
    }
  }
}
