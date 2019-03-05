package services
import java.time.{Clock, Duration, Instant, ZonedDateTime}
import java.util.concurrent.atomic.AtomicInteger

import mongo.entities.{GroupId, Post, UserId}
import mongo.{MembershipService, PostsService}
import org.apache.logging.log4j.scala.Logging
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.{MongoDatabase, Observable}

// TODO: fetching group timeline?
class CachedMongoFeedService(mongoDatabase: MongoDatabase, underlyingService: FeedService[Observable, ObjectId])
    extends FeedService[Observable, ObjectId]
    with Logging {
  private val cacheHits = new AtomicInteger(0)
  private val cacheMisses = new AtomicInteger(0)

  private val maxCachedPostCountInTimeline = 50
  private val postService = new PostsService(mongoDatabase)
  private val membershipService = new MembershipService(mongoDatabase)
  // numberOfPosts should be configurable somewhere on top
  private val timelineCacheService = new TimelineCacheService(mongoDatabase, maxCachedPostCountInTimeline)

  override def postOnGroup(
      userId: UserId,
      groupId: GroupId,
      content: String,
      createdAt: ZonedDateTime
  )(implicit clock: Clock): Observable[ObjectId] = {
    val allGroupUsersIds = membershipService.getAllUsersForGroup(groupId).collect()

    allGroupUsersIds.map(_.toSet).flatMap { userIds =>
      if (userIds.contains(userId.id)) {
        postService.addPostToGroup(userId, groupId, content, createdAt).flatMap { postId =>
          timelineCacheService.updateAllOwnersCachedTimelinesWithNewPost(userIds.toSeq, postId).map { _ =>
            postId
          }
        }
      } else {
        logger.warn(s"${userId.id} tried to post on ${groupId.id} which he is not a member of.")
        Observable(Seq.empty)
      }
    }
  }

  override def getTopPostsFromAllUserGroups(
      userId: UserId,
      after: Instant
  ): Observable[Post] = {
    // TODO: use cache here?
    underlyingService.getTopPostsFromAllUserGroups(userId, after)
    // check if we can satisfy this from cache if not fallback to old
  }

  override def getTopPostsFromAllUserGroups(
      userId: UserId,
      untilPostCount: Int,
      noOlderThan: Instant,
      timeSpanRequestedInOneRequest: Duration
  )(implicit clock: Clock): Observable[Post] = {
    // TODO: check if other params also nonstandard, if so don't use cache
    if (untilPostCount > maxCachedPostCountInTimeline) {
      // weird request, asking for more than default, let's just run it on live
      underlyingService.getTopPostsFromAllUserGroups(userId, untilPostCount, noOlderThan, timeSpanRequestedInOneRequest)
    } else {
      timelineCacheService.getCachedTimelineForOwner(userId.id).collect().flatMap { postsFromCache =>
        if (postsFromCache.nonEmpty) {
          cacheHits.incrementAndGet()
          logger.debug(
            s"Got cache hit for timeline for $userId and $untilPostCount post count, in cache ${postsFromCache.size}"
          )
          postService.fetchPostsByIds(postsFromCache.take(untilPostCount))
        } else {
          logger.debug(
            s"Got cache miss for timeline for $userId and $untilPostCount post count."
          )

          underlyingService
            .getTopPostsFromAllUserGroups(
              userId,
              maxCachedPostCountInTimeline,
              noOlderThan,
              timeSpanRequestedInOneRequest
            )
            .collect()
            .flatMap { livePosts =>
              logger.debug(s"Savinng timeline cache for ${userId.id} with ${livePosts.size} posts.")
              timelineCacheService.cacheTimelineForOwner(userId.id, livePosts.map(_._id)).flatMap { _ =>
                Observable(livePosts.take(untilPostCount))
              }
            }
        }
      }
    }
  }
}
