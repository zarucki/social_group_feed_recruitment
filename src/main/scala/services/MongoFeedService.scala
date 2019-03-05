package services
import java.time.{Clock, Instant, ZonedDateTime, Duration => JDuration}
import java.util.Date

import mongo.entities.{GroupId, Post, UserId}
import mongo.{MembershipService, PostsService}
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.{Completed, MongoDatabase, Observable}

class MongoFeedService(mongoDatabase: MongoDatabase) extends FeedService[Observable] {
  private val postService = new PostsService(mongoDatabase)
  private val membershipService = new MembershipService(mongoDatabase)

  def getTopPostsFromAllUserGroups(userId: UserId, after: Instant): Observable[Post] = {
    for {
      groupIds <- membershipService.getAllGroupsForUser(userId).collect()
      post     <- postService.getLatestPostsForOwners(groupIds, after)
    } yield post
  }

  def postOnGroup(userId: UserId, groupId: GroupId, content: String)(implicit clock: Clock): Observable[Completed] = {
    postService.addPostToGroup(userId, groupId, content, ZonedDateTime.now(clock))
  }

  // will be useful for getting first page
  def getTopPostsFromAllUserGroups(
      userId: UserId,
      untilPostCount: Int,
      noLaterThan: Instant,
      timeSpanRequestedInOneRequest: JDuration
  )(
      implicit clock: Clock
  ): Observable[Post] = {
    val now = ZonedDateTime.now(clock)

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

      if (leftToFetch <= 0 || newAfter.isBefore(noLaterThan)) {
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
}
