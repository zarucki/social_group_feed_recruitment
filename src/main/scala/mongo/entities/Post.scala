package mongo.entities
import java.time.Instant

import mongo.entities.MongoKeyNames.OwnerId
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.annotations.BsonProperty

object Post {
  val collection: String = "posts"

  implicit val postsStoredInCollection = new StoredInCollection[Post] {
    override def collectionName: String = Post.collection
  }
}

case class Post(
    _id: ObjectId, // date created is contained in the id - seconds from epoch
    @BsonProperty("i") insertedAt: Instant,
    @BsonProperty("p") content: String,
    @BsonProperty("uid") userId: OwnerId,
    @BsonProperty("gid") groupId: OwnerId // this could be None for user wall posts
)
