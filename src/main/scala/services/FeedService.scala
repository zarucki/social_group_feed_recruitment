package services

import java.time.{Clock, Instant, ZonedDateTime, Duration => JDuration}

import mongo.entities.{GroupId, Post, UserId}

trait FeedService[F[_], TEntityId] {
  def postOnGroup(userId: UserId, groupId: GroupId, content: String, createdAt: ZonedDateTime)(
      implicit clock: Clock
  ): F[TEntityId]

  def getTopPostsFromAllUserGroups(userId: UserId, after: Instant): F[Post]

  def getTopPostsFromAllUserGroups(
      userId: UserId,
      untilPostCount: Int,
      noOlderThan: Instant,
      timeSpanRequestedInOneRequest: JDuration
  )(implicit clock: Clock): F[Post]
}
