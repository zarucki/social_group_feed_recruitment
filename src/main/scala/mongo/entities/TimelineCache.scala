package mongo.entities
import java.time.Instant
import java.util.concurrent.{TimeUnit => JTimeUnit}

import mongo.StoredInCollection
import mongo.entities.MongoKeyNames.{OwnerId, _}
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.annotations.BsonProperty
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes._

case class TTLIndexSettings(expireAfter: Long, timeUnit: JTimeUnit)

object TimelineCache {
  val collection = "timeline_cache"

  implicit val timelineCacheStoredInCollection = new StoredInCollection[TimelineCache] {
    override def collectionName: String = TimelineCache.collection
  }

  // TODO: index on cachedPosts?

  def indexes(ttlIndexSettings: TTLIndexSettings): Seq[IndexThatShouldBePresent] = {
    Seq(
      IndexThatShouldBePresent(
        s"index_${ownerIdKey}_1",
        (ascending(ownerIdKey), IndexOptions().unique(true))
      ),
      IndexThatShouldBePresent(
        s"index_${TimelineCacheNames.topPostsKey}_-1",
        (descending(TimelineCacheNames.topPostsKey), IndexOptions().unique(true)) // TODO: will this unique be a problem?
      ),
      IndexThatShouldBePresent(
        s"index_ttl_${TimelineCacheNames.lastUpdatedKey}_1",
        (
          ascending(TimelineCacheNames.lastUpdatedKey),
          IndexOptions().expireAfter(ttlIndexSettings.expireAfter, ttlIndexSettings.timeUnit)
        )
      )
    )
  }
}

// TODO: cachedPosts should be ordered?
case class TimelineCache(
    @BsonProperty("oid") ownerId: OwnerId,
    @BsonProperty("tp") topPostIds: List[ObjectId],
    @BsonProperty("lu") lastUpdated: Instant
)
