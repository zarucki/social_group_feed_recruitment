package mongo.entities

import java.time.Instant

import mongo.entities.MongoEntities.OwnerId
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.annotations.BsonProperty

object MongoEntities {
  val _id: String = "_id"
  val userIdKey: String = "uid"
  val groupIdKey: String = "gid"
  val ownerIdKey: String = "oid"
  val postIdKey: String = "pid"

  type OwnerId = String
}

object Post {
  val collection: String = "posts"
}

case class Post(
    _id: ObjectId, // date created is contained in the id - seconds from epoch
    @BsonProperty("i") insertedAt: Instant,
    @BsonProperty("p") content: String,
    @BsonProperty("uid") userId: OwnerId,
    @BsonProperty("gid") groupId: OwnerId // this could be None for user wall posts
)

object PostOwnership {
  val collection: String = "post_ownerships"
}

case class PostOwnership(
    @BsonProperty("oid") ownerId: OwnerId,
    @BsonProperty("pid") postId: ObjectId
)

object UserGroup {
  val collection: String = "user_groups"
}

case class UserGroup(
    @BsonProperty("uid") userId: OwnerId,
    @BsonProperty("gid") groupId: OwnerId
)

object GroupUserMember {
  val collection: String = "group_user_members"
}

case class GroupUserMember(
    @BsonProperty("gid") groupId: OwnerId,
    @BsonProperty("uid") userId: OwnerId
)

case class Author(_id: ObjectId, userId: String, userDisplayName: String)
