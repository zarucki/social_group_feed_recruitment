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

class MongoFeedService(mongoDatabase: MongoDatabase)(implicit clock: Clock)
    extends FeedService[Observable, ObjectId]
    with Logging {
  private val defaultFeedDaysBack = 7
  private val postService = new PostsService(mongoDatabase)
  private val membershipService = new MembershipService(mongoDatabase)

  override def postOnGroup(
      userId: UserId,
      groupId: GroupId,
      content: String,
      createdAt: ZonedDateTime,
      userName: Option[String] = None
  ): Observable[Either[Throwable, ObjectId]] = {

    val allUserGroups = membershipService.getAllGroupsForUser(userId).collect()

    allUserGroups.map(_.toSet).flatMap { groupIds =>
      if (groupIds.contains(groupId.id)) {
        postService.addPostToGroup(userId, groupId, content, createdAt, userName).map(Right(_))
      } else {
        logger.warn(s"${userId.id} tried to post on ${groupId.id} which he is not a member of.")
        Observable(Seq(Left(new UserPostedToNotHisGroupException(userId.id, groupId.id))))
      }
    }
  }

  override def getTopPostsFromAllUserGroups(userId: UserId, after: Instant): Observable[Post] = {
    for {
      groupIds <- membershipService.getAllGroupsForUser(userId).collect()
      post     <- postService.getLatestPostsForOwners(groupIds, after)
    } yield post
  }

  override def getTopPostsFromAllUserGroups(
      userId: UserId,
      untilPostCount: Int,
      noOlderThan: Instant,
      timeSpanRequestedInOneRequest: JDuration
  ): Observable[Post] = {

    def nextAfter(currentAfter: Instant) = currentAfter.minusNanos(timeSpanRequestedInOneRequest.toNanos)

    // TODO: make it somehow tail recursive?
    def getPostIdsUntilYouFulfillRequest(
        groupIds: Seq[String],
        alreadyFetchedPosts: Observable[Seq[ObjectId]],
        leftToFetch: Int,
        currentAfter: Instant,
        currentBefore: Option[ObjectId] = None,
    ): Observable[Seq[ObjectId]] = {
      val newAfter = nextAfter(currentAfter)

      if (leftToFetch <= 0 || newAfter.isBefore(noOlderThan)) {
        alreadyFetchedPosts
      } else {
        val newIds = postService
          .getLatestPostsIdsForOwners(
            ownerIds = groupIds,
            after = new ObjectId(Date.from(newAfter), 0),
            before = currentBefore
          )
          .collect()

        newIds
          .map(_.sortBy(-_.getTimestamp)) // sort is compound on (ownerId, postId) sometimes posts will be in not correct order
          .flatMap { newIdsSorted =>
            val newBefore = newIdsSorted.lastOption.orElse(currentBefore)
            val combined = alreadyFetchedPosts.map(b => newIdsSorted ++ b)
            getPostIdsUntilYouFulfillRequest(groupIds, combined, leftToFetch - newIdsSorted.size, newAfter, newBefore)
          }
      }
    }

    for {
      groupIds <- membershipService.getAllGroupsForUser(userId).collect()
      postIds <- getPostIdsUntilYouFulfillRequest(
        groupIds = groupIds,
        alreadyFetchedPosts = Observable(List(Seq[ObjectId]())),
        currentAfter = now.plusMinutes(1).toInstant,
        leftToFetch = untilPostCount
      )
      post <- postService.fetchPostsByIds(postIds.take(untilPostCount))
    } yield post
  }

  override def getTopPostsForGroup(groupId: GroupId): Observable[Post] = {
    postService.getLatestPostsForOwner(
      groupId,
      defaultAfterForFeed
    )
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
  private def defaultAfterForFeed: Instant = now.minusDays(defaultFeedDaysBack).toInstant
}
