package rest

import config.AppConfig
import org.apache.logging.log4j.scala.Logging
import persistance.PersistenceClient
import persistance.entities._
import rest.entities.{GroupPost, UserGroup}

import scala.concurrent.{ExecutionContext, Future}

class RequestHandler(appConfig: AppConfig, persistenceClient: PersistenceClient[Future])(
    implicit executionContext: ExecutionContext
) extends Logging {
  def getUserGroups(userId: Long): Future[List[UserGroup]] = {
    persistenceClient.getUserGroups(UserId(userId)).map(_.toList)
  }

  def addUserToGroup(userId: Long, groupId: Long): Future[Unit] = {
    persistenceClient.addUserToGroup(UserId(userId), GroupId(groupId))
  }

  def addPostToGroup(groupId: Long, groupPost: GroupPost): Future[Either[Throwable, String]] = {
    persistenceClient.addPostToGroup(
      UserId(groupPost.userId),
      GroupId(groupId),
      content = groupPost.content,
      userName = groupPost.userName
    )
  }
}
