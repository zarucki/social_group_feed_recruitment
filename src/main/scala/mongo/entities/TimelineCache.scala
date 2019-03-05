package mongo.entities
import java.time.Instant
import java.util.concurrent.{TimeUnit => JTimeUnit}

import mongo.entities.MongoKeyNames.{OwnerId, _}
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.annotations.BsonProperty
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes._

case class TTLIndexSettings(expireAfter: Long, timeUnit: JTimeUnit)

object TimelineCache {
  val collection = "timeline_cache"

  implicit val timelineCacheStoredInCollection = new StoredInCollection[PostOwnership] {
    override def collectionName: String = TimelineCache.collection
  }

  def ttlIndex(ttlIndexSettings: TTLIndexSettings): IndexThatShouldBePresent = {
    IndexThatShouldBePresent(
      s"index_ttl_${lastUpdatedKey}_1",
      (ascending(lastUpdatedKey), IndexOptions().expireAfter(ttlIndexSettings.expireAfter, ttlIndexSettings.timeUnit))
    )
  }
}

// cachedPosts should be ordered
case class TimelineCache(
    @BsonProperty("oid") ownerId: OwnerId,
    @BsonProperty("tp") cachedPosts: List[ObjectId],
    @BsonProperty("lu") lastUpdated: Instant
)
