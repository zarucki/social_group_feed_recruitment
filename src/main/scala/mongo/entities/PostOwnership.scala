package mongo.entities
import mongo.entities.MongoKeyNames.OwnerId
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.annotations.BsonProperty

object PostOwnership {
  val collection: String = "post_ownerships"
}

case class PostOwnership(
    @BsonProperty("oid") ownerId: OwnerId,
    @BsonProperty("pid") postId: ObjectId
)
