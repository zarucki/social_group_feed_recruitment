package rest

import config.AppConfig
import persistance.PersistenceClient
import persistance.entities._

import scala.concurrent.{ExecutionContext, Future}

class RequestHandler(appConfig: AppConfig, persistenceClient: PersistenceClient[Future])(
    implicit executionContext: ExecutionContext
) {
  def getUserGroups(userId: Long): Future[List[UserGroup]] = {
    persistenceClient.getUserGroups(UserId(userId)).map(_.toList)
  }

  def addUserToGroup(userId: Long, groupId: Long): Future[Unit] = {
    persistenceClient.addUserToGroup(UserId(userId), GroupId(groupId))
  }
}
