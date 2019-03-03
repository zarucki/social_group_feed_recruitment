package entities
import java.time.LocalDateTime

import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.annotations.BsonProperty

object MongoEntities {
  val userIdKey: String = "uid"
  val groupIdKey: String = "gid"

}

object GroupPost {
  val collection: String = "group_posts"
}

case class GroupPost(
    _id: ObjectId,
    @BsonProperty("c") dateCreated: LocalDateTime,
    @BsonProperty("p") post: String,
    @BsonProperty("uid") userId: String,
    @BsonProperty("gid") groupId: String
)

object UserGroups {
  val collection: String = "user_groups"
}

case class UserGroups(
    _id: ObjectId,
    @BsonProperty("uid") userId: String,
    @BsonProperty("gid") groupId: String
)

object GroupUserMembers {
  val collection: String = "group_user_members"
}

case class GroupUserMembers(
    _id: ObjectId,
    @BsonProperty("gid") groupId: String,
    @BsonProperty("uid") userId: String
)

case class Author(_id: ObjectId, userId: String, userDisplayName: String)
