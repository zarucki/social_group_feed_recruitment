package persistance
import mongo.entities.Post
import persistance.entities._

import scala.concurrent.Future

trait PersistenceClient[F[_]] {
  def init(): F[PersistenceClient[F]]
  def getUserGroups(userId: UserId): F[Seq[String]]
  def addUserToGroup(userId: UserId, groupId: GroupId): F[Unit]
  def addPostToGroup(
      userId: UserId,
      groupId: GroupId,
      content: String,
      userName: String
  ): Future[Either[Throwable, String]]

  def getGroupFeed(groupId: Long): Future[Seq[Post]]
}
