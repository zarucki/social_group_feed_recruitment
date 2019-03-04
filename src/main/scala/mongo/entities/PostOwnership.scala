package mongo.entities
import mongo.entities.MongoKeyNames.{OwnerId, _}
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.annotations.BsonProperty
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes._

object PostOwnership {
  val collection: String = "post_ownerships"

  val indexes = Seq(
    IndexThatShouldBePresent(
      s"index_${ownerIdKey}_1_${postIdKey}_-1",
      (compoundIndex(ascending(ownerIdKey), descending(postIdKey)), IndexOptions().unique(true))
    )
  )

  implicit val postOwnershipsStoredInCollection = new StoredInCollection[PostOwnership] {
    override def collectionName: String = PostOwnership.collection
  }
}

case class PostOwnership(
    @BsonProperty("oid") ownerId: OwnerId,
    @BsonProperty("pid") postId: ObjectId
)
