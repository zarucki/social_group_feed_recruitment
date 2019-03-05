package services
import java.time.{Clock, Instant, ZonedDateTime, Duration => JDuration}
import java.util.Date

import mongo.entities.Post
import mongo.{MembershipService, PostsService}
import org.apache.logging.log4j.scala.Logging
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.{MongoDatabase, Observable}
import persistance.UserPostedToNotHisGroupException
import persistance.entities.{GroupId, UserId}

// TODO: fetching group timeline?
class CachedMongoFeedService(mongoDatabase: MongoDatabase, underlyingService: FeedService[Observable, ObjectId])(
    implicit clock: Clock
) extends FeedService[Observable, ObjectId]
    with Logging {

  private val maxCachedPostCountInTimeline = 50
  private val postService = new PostsService(mongoDatabase)
  private val membershipService = new MembershipService(mongoDatabase)
  // numberOfPosts should be configurable somewhere on top
  private val timelineCacheService = new TimelineCacheService(mongoDatabase, maxCachedPostCountInTimeline)

  override def postOnGroup(
      userId: UserId,
      groupId: GroupId,
      content: String,
      createdAt: ZonedDateTime,
      userName: Option[String] = None
  ): Observable[Either[Throwable, ObjectId]] = {
    val allGroupUsersIds = membershipService.getAllUsersForGroup(groupId).collect()

    allGroupUsersIds.map(_.toSet).flatMap { userIds =>
      if (userIds.contains(userId.id)) {
        postService.addPostToGroup(userId, groupId, content, createdAt, userName).flatMap { postId =>
          timelineCacheService.updateExistingCachedTimelinesWithNewPost(userIds.toSeq, postId).map { _ =>
            Right(postId)
          }
        }
      } else {
        logger.warn(s"${userId.id} tried to post on ${groupId.id} which he is not a member of.")
        Observable(Seq(Left(new UserPostedToNotHisGroupException(userId.id, groupId.id))))
      }
    }
  }

  override def getTopPostsFromAllUserGroups(
      userId: UserId,
      after: Instant
  ): Observable[Post] = {
    val afterObjectId = new ObjectId(Date.from(after), 0)

    timelineCacheService.getCachedTimelineForOwner(userId.id).collect().flatMap { postsFromCache =>
      if (postsFromCache.nonEmpty) {
        logger.debug(
          s"Got cache hit for $userId and $after with ${postsFromCache.size} posts, not sure if good enough for us."
        )
        val postsNewerThanAfter = postsFromCache.takeWhile(_.getTimestamp > afterObjectId.getTimestamp)
        if (postsNewerThanAfter.size == postsFromCache.size) {
          // if all posts are newer than our after, than cache probably doesn't have all results we need
          loadFreshResultsIntoCache(userId.id, underlyingService.getTopPostsFromAllUserGroups(userId, after), identity)
        } else {
          postService.fetchPostsByIds(postsNewerThanAfter)
        }
      } else {
        loadFreshResultsIntoCache(userId.id, underlyingService.getTopPostsFromAllUserGroups(userId, after), identity)
      }
    }
  }

  override def getTopPostsFromAllUserGroups(
      userId: UserId,
      untilPostCount: Int,
      noOlderThan: Instant,
      timeSpanRequestedInOneRequest: JDuration
  ): Observable[Post] = {
    // TODO: check if other params also nonstandard, if so don't use cache
    if (untilPostCount > maxCachedPostCountInTimeline) {
      // weird request, asking for more than default, let's just run it on live
      underlyingService.getTopPostsFromAllUserGroups(userId, untilPostCount, noOlderThan, timeSpanRequestedInOneRequest)
    } else {
      timelineCacheService.getCachedTimelineForOwner(userId.id).collect().flatMap { postsFromCache =>
        if (postsFromCache.nonEmpty) {
          logger.info(
            s"Got cache hit for timeline for $userId and $untilPostCount post count, in cache ${postsFromCache.size}"
          )
          postService.fetchPostsByIds(postsFromCache.take(untilPostCount))
        } else {
          logger.info(
            s"Got cache miss for timeline for $userId and $untilPostCount post count."
          )

          val loadFresh = underlyingService.getTopPostsFromAllUserGroups(
            userId,
            maxCachedPostCountInTimeline,
            noOlderThan,
            timeSpanRequestedInOneRequest
          )

          loadFreshResultsIntoCache(userId.id, loadFresh, _.take(untilPostCount))
        }
      }
    }
  }

  private def loadFreshResultsIntoCache(
      ownerId: String,
      load: => Observable[Post],
      transformBeforeReturning: Seq[Post] => Seq[Post]
  ): Observable[Post] = {

    load
      .collect()
      .flatMap { livePosts =>
        logger.debug(s"Saving timeline cache for ${ownerId} with ${livePosts.size} posts.")
        timelineCacheService.cacheTimelineForOwner(ownerId, livePosts.map(_._id)).flatMap { _ =>
          Observable(transformBeforeReturning(livePosts))
        }
      }
  }

  // TODO: for now we cache only user stuff bet we could also cache top group feed, though it is not as expensive to fetch
  override def getTopPostsForGroup(groupId: GroupId): Observable[Post] = {
    underlyingService.getTopPostsForGroup(groupId)
  }

  override def getTopPostsFromAllUserGroups(userId: UserId): Observable[Post] = {
    getTopPostsFromAllUserGroups(
      userId = userId,
      untilPostCount = 20,
      noOlderThan = now.minusDays(14).toInstant,
      timeSpanRequestedInOneRequest = JDuration.ofDays(7)
    )
  }

  private def now: ZonedDateTime = ZonedDateTime.now(clock)
}
