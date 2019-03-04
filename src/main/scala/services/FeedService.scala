package services
import java.time.{Clock, ZonedDateTime}

import entities.Post
import mongo.{MembershipService, PostsService}
import org.mongodb.scala.{Completed, MongoDatabase, Observable}

class FeedService(mongoDatabase: MongoDatabase) {
  private val postService = new PostsService(mongoDatabase)
  private val membershipService = new MembershipService(mongoDatabase)

  def getTopPostsFromAllGroupsFeedForUser(userId: String, postCount: Int): Observable[Post] = {
    for {
      groupIds <- membershipService.getAllGroupsForUser(userId).collect()
      post     <- postService.getLatestPostsForOwners(groupIds, postCount)
    } yield post
  }

  def postOnGroup(userId: String, groupId: String, content: String)(implicit clock: Clock): Observable[Completed] = {
    // TODO: order of params here is completely different
    postService.addPostToGroup(content, ZonedDateTime.now(clock), userId, groupId)
  }
}
