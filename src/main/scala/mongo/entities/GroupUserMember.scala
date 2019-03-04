package mongo.entities
import mongo.entities.MongoKeyNames.OwnerId
import org.mongodb.scala.bson.annotations.BsonProperty

object GroupUserMember {
  val collection: String = "group_user_members"
}

case class GroupUserMember(
    @BsonProperty("gid") groupId: OwnerId,
    @BsonProperty("uid") userId: OwnerId
)
