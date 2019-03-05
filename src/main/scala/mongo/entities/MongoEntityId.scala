package mongo.entities

sealed trait MongoEntityId {
  def id: String
}

sealed trait MongoContentOwnerId extends MongoEntityId

object UserId {
  val USER_ID_PREFIX = "u_"
}

case class UserId(idWithoutPrefix: String) extends MongoContentOwnerId {
  override def id: String = UserId.USER_ID_PREFIX + idWithoutPrefix
}

object GroupId {
  val GROUP_ID_PREFIX = "g_"
}

case class GroupId(idWithoutPrefix: String) extends MongoContentOwnerId {
  override def id: String = GroupId.GROUP_ID_PREFIX + idWithoutPrefix
}
