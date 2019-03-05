package services

import java.time.{Instant, ZonedDateTime, Duration => JDuration}

import mongo.entities.{GroupId, Post, UserId}

trait FeedService[F[_], TEntityId] {
  def postOnGroup(userId: UserId, groupId: GroupId, content: String, createdAt: ZonedDateTime): F[TEntityId]

  def getTopPostsFromAllUserGroups(userId: UserId, after: Instant): F[Post]

  def getTopPostsFromAllUserGroups(
      userId: UserId,
      untilPostCount: Int,
      noOlderThan: Instant,
      timeSpanRequestedInOneRequest: JDuration
  ): F[Post]
}
