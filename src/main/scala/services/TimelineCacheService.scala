package services
import java.time.{Clock, ZonedDateTime}

import com.mongodb.client.model.{PushOptions, UpdateOptions}
import mongo.repository.SimpleMongoEntityRepository.TimelineCacheRepo
import mongo.entities.MongoKeyNames._
import mongo.entities.TimelineCache
import org.apache.logging.log4j.scala.Logging
import org.mongodb.scala.{Completed, MongoDatabase, Observable, SingleObservable}
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Sorts._
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.result.UpdateResult

class TimelineCacheService(mongoDatabase: MongoDatabase, numberOfPostsToKeepInCache: Int)(implicit clock: Clock)
    extends Logging {
  private val timelineCacheRepo = new TimelineCacheRepo(mongoDatabase)

  def updateExistingCachedTimelinesWithNewPost(
      ownerIds: Seq[String],
      postId: ObjectId
  ): SingleObservable[UpdateResult] = {

    val now = ZonedDateTime.now(clock).toInstant
    logger.debug(s"Updating timeline cache of $ownerIds with $postId and marking as lastUpdated: $now")

    timelineCacheRepo
      .updateMany(
        in(ownerIdKey, ownerIds: _*),
        combine(
          set(TimelineCacheNames.lastUpdatedKey, now),
          pushEach(
            TimelineCacheNames.topPostsKey,
            new PushOptions()
              .slice(numberOfPostsToKeepInCache)
              .sort(-1),
            postId
          ),
        ),
        new UpdateOptions().upsert(false)
      )
      .map { updateResult =>
        logger.debug(
          s"Updated timeline cache of $ownerIds with $postId. Matched: ${updateResult.getMatchedCount} modifiedCount: ${updateResult.getModifiedCount}"
        )
        updateResult
      }
  }

  def getCachedTimelineForOwner(ownerId: OwnerId): Observable[ObjectId] = {
    getTimelineCacheObjectForOwner(ownerId)
      .flatMap(tc => Observable(tc.topPostIds))
  }

  def getTimelineCacheObjectForOwner(ownerId: OwnerId): Observable[TimelineCache] = {
    timelineCacheRepo
      .findByCondition[TimelineCache](equal(ownerIdKey, ownerId))
      .sort(descending(TimelineCacheNames.topPostsKey))
  }

  def cacheTimelineForOwner(ownerId: OwnerId, postIds: Seq[ObjectId]): Observable[Completed] = {
    timelineCacheRepo
      .put(TimelineCache(ownerId, postIds.take(numberOfPostsToKeepInCache).toList, ZonedDateTime.now(clock).toInstant))
  }
}
