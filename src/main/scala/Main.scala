import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive, RejectionHandler}
import akka.stream.ActorMaterializer
import config.AppConfig
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import mongo.MongoPersistenceClient
import org.apache.logging.log4j.scala.Logging
import rest.RequestHandler

import scala.io.StdIn

object Main extends App with Logging {
  implicit val system = ActorSystem("feed-system")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

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

  def routing(requestHandler: RequestHandler) = {
    logDuration {
      get {
        pathPrefix("user") {
          pathPrefix(LongNumber) { userId =>
            path("groups") {
              val userGroups = requestHandler.getUserGroups(userId)
              onSuccess(userGroups) { case l => complete(l) }
            } ~
              path("add-to-group" / LongNumber) { groupId =>
                onSuccess(requestHandler.addUserToGroup(userId, groupId)) {
                  logger.info(s"user $userId successfully added to group $groupId")
                  complete(StatusCodes.OK)
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
}
