package services
import java.time.{Clock, Instant, ZonedDateTime}

import mongo.entities.{GroupId, Post, UserId}
import mongo.{MembershipService, PostsService}
import org.mongodb.scala.{Completed, MongoDatabase, Observable}

class FeedService(mongoDatabase: MongoDatabase) {
  private val postService = new PostsService(mongoDatabase)
  private val membershipService = new MembershipService(mongoDatabase)

  // implement laters as paging on date until we have desired count
  // def getTopPostsFromAllGroupsFeedForUser(userId: UserId, postCount: Int)

  def getTopPostsFromAllGroupsFeedForUser(userId: UserId, after: Instant): Observable[Post] = {
    for {
      groupIds <- membershipService.getAllGroupsForUser(userId).collect()
      post     <- postService.getLatestPostsForOwners(groupIds, after)
    } yield post
  }

  def postOnGroup(userId: UserId, groupId: GroupId, content: String)(implicit clock: Clock): Observable[Completed] = {
    postService.addPostToGroup(userId, groupId, content, ZonedDateTime.now(clock))
  }
}
