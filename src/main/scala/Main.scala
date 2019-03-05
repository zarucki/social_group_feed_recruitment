import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, RouteResult}
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LogEntry}
import akka.stream.ActorMaterializer
import config.AppConfig
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import org.apache.logging.log4j.scala.Logging
import persistance.MongoPersistenceClient
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

  val persistenceClient = new MongoPersistenceClient(appConfig)
  val resourceHandler = new RequestHandler(appConfig, persistenceClient)

  // TODO: logging http requests?

  val route: Route =
    get {
      pathPrefix("user") {
        pathPrefix(LongNumber) { userId =>
          path("groups") {
            logger.info(s"user $userId groups")
            val userGroups = resourceHandler.getUserGroups(userId)
            onSuccess(userGroups) {
              case Nil => complete(StatusCodes.OK)
              case l   => complete(l)
            }
          } ~
            path("add-to-group" / LongNumber) { groupId =>
              onSuccess(resourceHandler.addUserToGroup(userId, groupId)) {
                logger.info(s"user $userId successfully added to group $groupId")
                complete(StatusCodes.OK)
              }
            }
        }
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", appConfig.httpRestApiPort)

  println(s"Server online at http://localhost:${appConfig.httpRestApiPort}/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
