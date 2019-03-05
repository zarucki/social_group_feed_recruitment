package services
import java.time.{Clock, ZonedDateTime}

import com.mongodb.client.model.{PushOptions, UpdateOptions}
import mongo.repository.SimpleMongoEntityRepository.TimelineCacheRepo
import mongo.entities.MongoKeyNames._
import mongo.entities.TimelineCache
import org.apache.logging.log4j.scala.Logging
import org.mongodb.scala.{MongoDatabase, Observable, SingleObservable}
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Sorts._
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.result.UpdateResult

class TimelineCacheService(mongoDatabase: MongoDatabase, numberOfPostsToKeepInCache: Int) extends Logging {
  private val timelineCacheRepo = new TimelineCacheRepo(mongoDatabase)

  def updateAllOwnersCachedTimelinesWithNewPost(
      ownerIds: Seq[String],
      postId: ObjectId
  ): SingleObservable[UpdateResult] = {

    logger.debug(s"Updating timeline cache of $ownerIds with $postId.")

    timelineCacheRepo
      .updateMany(
        in(ownerIdKey, ownerIds: _*),
        pushEach(TimelineCacheNames.topPostsKey, new PushOptions().slice(numberOfPostsToKeepInCache).sort(-1), postId),
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
    timelineCacheRepo
      .findByCondition[TimelineCache](equal(ownerIdKey, ownerId))
      .sort(descending(TimelineCacheNames.topPostsKey))
      .flatMap(tc => Observable(tc.topPostIds))
  }

  def cacheTimelineForOwner(ownerId: OwnerId, postIds: Seq[ObjectId])(implicit clock: Clock) = {
    timelineCacheRepo
      .put(TimelineCache(ownerId, postIds.take(numberOfPostsToKeepInCache).toList, ZonedDateTime.now(clock).toInstant))
  }
}
