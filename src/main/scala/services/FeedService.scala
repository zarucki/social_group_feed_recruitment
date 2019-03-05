package services

import java.time.{Clock, Instant, Duration => JDuration}

import mongo.entities.{GroupId, Post, UserId}
import org.mongodb.scala.Completed

trait FeedService[F[_]] {
  def postOnGroup(userId: UserId, groupId: GroupId, content: String)(implicit clock: Clock): F[Completed]

  def getTopPostsFromAllUserGroups(userId: UserId, after: Instant): F[Post]

  def getTopPostsFromAllUserGroups(
      userId: UserId,
      untilPostCount: Int,
      noLaterThan: Instant,
      timeSpanRequestedInOneRequest: JDuration
  )(implicit clock: Clock): F[Post]
}
