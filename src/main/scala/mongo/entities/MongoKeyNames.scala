package mongo.entities

object MongoKeyNames {
  val _id: String = "_id"
  val userIdKey: String = "uid"
  val groupIdKey: String = "gid"
  val ownerIdKey: String = "oid"
  val postIdKey: String = "pid"

  object TimelineCacheNames {
    val lastUpdatedKey: String = "lu"
    val topPostsKey: String = "tp"
  }

  type OwnerId = String
}
