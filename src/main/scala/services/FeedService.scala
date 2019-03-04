package services
import java.time.{Clock, ZonedDateTime}

import mongo.entities.{GroupId, Post, UserId}
import mongo.{MembershipService, PostsService}
import org.mongodb.scala.{Completed, MongoDatabase, Observable}

class FeedService(mongoDatabase: MongoDatabase) {
  private val postService = new PostsService(mongoDatabase)
  private val membershipService = new MembershipService(mongoDatabase)

  def getTopPostsFromAllGroupsFeedForUser(userId: UserId, postCount: Int): Observable[Post] = {
    for {
      groupIds <- membershipService.getAllGroupsForUser(userId).collect()
      post     <- postService.getLatestPostsForOwners(groupIds, postCount)
    } yield post
  }

  def postOnGroup(userId: UserId, groupId: GroupId, content: String)(implicit clock: Clock): Observable[Completed] = {
    // TODO: order of params here is completely different
    postService.addPostToGroup(userId, groupId, content, ZonedDateTime.now(clock))
  }
}
