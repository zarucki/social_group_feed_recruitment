package mongo.entities
import mongo.entities.MongoKeyNames.OwnerId
import org.mongodb.scala.bson.annotations.BsonProperty

object UserGroup {
  val collection: String = "user_groups"
}

case class UserGroup(
    @BsonProperty("uid") userId: OwnerId,
    @BsonProperty("gid") groupId: OwnerId
)
