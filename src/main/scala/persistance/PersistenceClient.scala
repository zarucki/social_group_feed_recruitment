package persistance
import persistance.entities._

trait PersistenceClient[F[_]] {
  def init(): F[PersistenceClient[F]]
  def getUserGroups(userId: UserId): F[Seq[UserGroup]]
  def addUserToGroup(userId: UserId, groupId: GroupId): F[Unit]
}
