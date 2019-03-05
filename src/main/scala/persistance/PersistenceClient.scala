package persistance
import persistance.entities._
import rest.entities.UserGroup

import scala.concurrent.Future

trait PersistenceClient[F[_]] {
  def init(): F[PersistenceClient[F]]
  def getUserGroups(userId: UserId): F[Seq[UserGroup]]
  def addUserToGroup(userId: UserId, groupId: GroupId): F[Unit]
  def addPostToGroup(
      userId: UserId,
      groupId: GroupId,
      content: String,
      userName: String
  ): Future[Either[Throwable, String]]
}
