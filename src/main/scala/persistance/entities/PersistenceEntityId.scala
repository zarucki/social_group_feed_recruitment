package persistance.entities

sealed trait PersistenceEntityId {
  def id: String
}

sealed trait PersistenceContentOwnerId extends PersistenceEntityId

object UserId {
  val USER_ID_PREFIX = "u_"

  def apply(long: Long): UserId = apply(idWithoutPrefix = long.toString)
  def apply(int: Int): UserId = apply(idWithoutPrefix = int.toString)
}

case class UserId(idWithoutPrefix: String) extends PersistenceContentOwnerId {
  override def id: String = UserId.USER_ID_PREFIX + idWithoutPrefix
}

object GroupId {
  val GROUP_ID_PREFIX = "g_"

  def apply(long: Long): GroupId = apply(idWithoutPrefix = long.toString)
  def apply(int: Int): GroupId = apply(idWithoutPrefix = int.toString)
}

case class GroupId(idWithoutPrefix: String) extends PersistenceContentOwnerId {
  override def id: String = GroupId.GROUP_ID_PREFIX + idWithoutPrefix
}
