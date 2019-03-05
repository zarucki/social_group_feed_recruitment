package rest

import config.AppConfig
import org.apache.logging.log4j.scala.Logging
import persistance.PersistenceClient
import persistance.entities._
import rest.entities.{ExistingGroupPost, NewGroupPost, UserGroup}

import scala.concurrent.{ExecutionContext, Future}

class RequestHandler(appConfig: AppConfig, persistenceClient: PersistenceClient[Future])(
    implicit executionContext: ExecutionContext
) extends Logging {
  def getUserGroups(userId: Long): Future[List[UserGroup]] = {
    persistenceClient.getUserGroups(UserId(userId)).map(_.map(UserGroup(_)).toList)
  }

  def addUserToGroup(userId: Long, groupId: Long): Future[Unit] = {
    persistenceClient.addUserToGroup(UserId(userId), GroupId(groupId))
  }

  def addPostToGroup(groupId: Long, groupPost: NewGroupPost): Future[Either[Throwable, String]] = {
    persistenceClient.addPostToGroup(
      UserId(groupPost.userId),
      GroupId(groupId),
      content = groupPost.content,
      userName = groupPost.userName
    )
  }

  def getGroupFeed(groupId: Long): Future[List[ExistingGroupPost]] = {
    persistenceClient
      .getGroupFeed(groupId)
      .map(_.map { post =>
        ExistingGroupPost(
          postId = post._id.toString,
          createdAt = post._id.getDate.toInstant,
          content = post.content,
          userId = post.userId,
          userName = post.userName.getOrElse(""),
          groupId = post.groupId
        )
      }.toList)
  }
}
