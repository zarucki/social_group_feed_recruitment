package services

import java.time.{Instant, ZonedDateTime, Duration => JDuration}

import mongo.entities.Post
import persistance.entities.{GroupId, UserId}

trait FeedService[F[_], TEntityId] {
  def postOnGroup(
      userId: UserId,
      groupId: GroupId,
      content: String,
      createdAt: ZonedDateTime,
      userName: Option[String] = None
  ): F[Either[Throwable, TEntityId]]

  def getTopPostsFromAllUserGroups(userId: UserId, after: Instant): F[Post]

  def getTopPostsFromAllUserGroups(
      userId: UserId,
      untilPostCount: Int,
      noOlderThan: Instant,
      timeSpanRequestedInOneRequest: JDuration
  ): F[Post]

  def getTopPostsForGroup(groupId: GroupId): F[Post]
}
