import java.time.{ZoneId, Duration => JDuration}

import mongo.entities.{Post, UserId}
import mongo.{MembershipService, PostsService}
import org.mongodb.scala.Observable
import org.mongodb.scala.bson.ObjectId
import services.{FeedService, MongoFeedService}

class MongoFeedServiceSpec extends MongoSpec {
  protected val oldestContentDate = fixedDateInPast.minusDays(7).minusHours(1)

  it should "return empty collection if the user is not in any group" in {
    multipleUsersGroupsAndPostsDataSetup()

    assert(
      awaitResults(
        sut.getTopPostsFromAllUserGroups(UserId("10"), after = fixedDateInPast.minusDays(14).toInstant)
      ) == Seq()
    )
  }

  it should "return empty collection if groups user is dont have any posts" in {
    multipleUsersGroupsAndPostsDataSetup()

    assert(
      awaitResults(
        sut.getTopPostsFromAllUserGroups(user4, after = fixedDateInPast.minusDays(14).toInstant)
      ) == Seq()
    )
  }

  it should "return empty collection if groups content in group is older that what we look for" in {
    multipleUsersGroupsAndPostsDataSetup()

    assert(
      awaitResults(
        sut.getTopPostsFromAllUserGroups(user4, after = fixedDateInPast.minusHours(1).toInstant)
      ) == Seq()
    )
  }

  it should "should return all posts from user groups" in {
    multipleUsersGroupsAndPostsDataSetup()

    assert(
      awaitResults(sut.getTopPostsFromAllUserGroups(user1, after = fixedDateInPast.minusDays(14).toInstant))
        .map(postToTestTuple) == List(
        (1548633600, "yeah u 2", group1.id),
        (1548550800, "nice 2 meet u", group1.id),
        (1548548100, "yeah, the group is pretty much dead", group2.id),
        (1548461700, "first post", group1.id),
        (1548457200, "there is no content here", group2.id)
      )
    )
  }

  it should "should return feeds of multiple groups" in {
    multipleUsersGroupsAndPostsDataSetup()

    assert(
      awaitResults(sut.getTopPostsFromAllUserGroups(user1, after = fixedDateInPast.minusDays(14).toInstant))
        .map(postToTestTuple) == List(
        (1548633600, "yeah u 2", group1.id),
        (1548550800, "nice 2 meet u", group1.id),
        (1548548100, "yeah, the group is pretty much dead", group2.id),
        (1548461700, "first post", group1.id),
        (1548457200, "there is no content here", group2.id)
      )
    )
  }

  it should "should change user feed if there is a change in membership" in {
    multipleUsersGroupsAndPostsDataSetup()

    assert(
      awaitResults(sut.getTopPostsFromAllUserGroups(user1, after = fixedDateInPast.minusDays(14).toInstant))
        .map(postToTestTuple) == List(
        (1548633600, "yeah u 2", group1.id),
        (1548550800, "nice 2 meet u", group1.id),
        (1548548100, "yeah, the group is pretty much dead", group2.id),
        (1548461700, "first post", group1.id),
        (1548457200, "there is no content here", group2.id)
      )
    )

    awaitResults(getMembershipService.addUserToGroup(user1, group3))

    assert(
      awaitResults(sut.getTopPostsFromAllUserGroups(user1, after = fixedDateInPast.minusDays(14).toInstant))
        .map(postToTestTuple) == List(
        (1548633600, "yeah u 2", group1.id),
        (1548550800, "nice 2 meet u", group1.id),
        (1548548100, "yeah, the group is pretty much dead", group2.id),
        (1548546300, "though they better come fast", group3.id),
        (1548461700, "first post", group1.id),
        (1548459900, "maybe someone will see my awesome content", group3.id),
        (1548457200, "there is no content here", group2.id)
      )
    )

  }

  it should "should correctly take latest posts when user in 2 groups" in {
    multipleUsersGroupsAndPostsDataSetup()

    assert(
      awaitResults(sut.getTopPostsFromAllUserGroups(user1, after = fixedDateInPast.minusDays(14).toInstant))
        .map(postToTestTuple) == List(
        (1548633600, "yeah u 2", group1.id),
        (1548550800, "nice 2 meet u", group1.id),
        (1548548100, "yeah, the group is pretty much dead", group2.id),
        (1548461700, "first post", group1.id),
        (1548457200, "there is no content here", group2.id)
      )
    )

    assert(
      awaitResults(sut.getTopPostsFromAllUserGroups(user1, after = fixedDateInPast.minusDays(6).toInstant))
        .map(postToTestTuple) == List(
        (1548633600, "yeah u 2", group1.id),
        (1548550800, "nice 2 meet u", group1.id),
        (1548548100, "yeah, the group is pretty much dead", group2.id)
      )
    )
  }

  it should "should correctly take latest posts when user in 3 groups" in {
    multipleUsersGroupsAndPostsDataSetup()
    awaitResults(getMembershipService.addUserToGroup(user1, group3))

    assert(
      awaitResults(sut.getTopPostsFromAllUserGroups(user1, after = fixedDateInPast.minusDays(14).toInstant))
        .map(postToTestTuple) == List(
        (1548633600, "yeah u 2", group1.id),
        (1548550800, "nice 2 meet u", group1.id),
        (1548548100, "yeah, the group is pretty much dead", group2.id),
        (1548546300, "though they better come fast", group3.id),
        (1548461700, "first post", group1.id),
        (1548459900, "maybe someone will see my awesome content", group3.id),
        (1548457200, "there is no content here", group2.id)
      )
    )

    assert(
      awaitResults(sut.getTopPostsFromAllUserGroups(user1, after = oldestContentDate.plusSeconds(1).toInstant))
        .map(postToTestTuple) == List(
        (1548633600, "yeah u 2", group1.id),
        (1548550800, "nice 2 meet u", group1.id),
        (1548548100, "yeah, the group is pretty much dead", group2.id),
        (1548546300, "though they better come fast", group3.id),
        (1548461700, "first post", group1.id),
        (1548459900, "maybe someone will see my awesome content", group3.id)
      )
    )
  }

  it should "should correctly take latest posts in simple example" in {
    oneUserTwoGroupsDataSetup()

    assert(
      awaitResults(sut.getTopPostsFromAllUserGroups(user1, after = fixedDateInPast.minusDays(7).toInstant))
        .map(postToTestTuple) ==
        List(
          (1549062000, "second group is much better", group2.id),
          (1548979200, "second group is better", group2.id),
          (1548896400, "second post", group1.id),
          (1548807300, "first post", group1.id)
        )
    )

    assert(
      awaitResults(sut.getTopPostsFromAllUserGroups(user1, after = fixedDateInPast.minusHours(30).toInstant))
        .map(postToTestTuple) ==
        List(
          (1549062000, "second group is much better", group2.id),
          (1548979200, "second group is better", group2.id)
        )
    )

    assert(
      awaitResults(sut.getTopPostsFromAllUserGroups(user1, after = fixedDateInPast.minusHours(50).toInstant))
        .map(postToTestTuple) ==
        List(
          (1549062000, "second group is much better", group2.id),
          (1548979200, "second group is better", group2.id),
          (1548896400, "second post", group1.id)
        )
    )
  }

  it should "correctly fetch requested number of posts with hour sliding" in {
    oneUserTwoGroupsDataSetup()

    implicit val clock = java.time.Clock.fixed(fixedDateInPast.toInstant, utcZoneId)

    assert(
      awaitResults(
        sut.getTopPostsFromAllUserGroups(
          user1,
          untilPostCount = 4,
          noOlderThan = fixedDateInPast.minusDays(7).toInstant,
          timeSpanRequestedInOneRequest = JDuration.ofHours(1)
        )
      ).map(postToTestTuple) ==
        List(
          (1549062000, "second group is much better", group2.id),
          (1548979200, "second group is better", group2.id),
          (1548896400, "second post", group1.id),
          (1548807300, "first post", group1.id)
        )
    )
  }

  it should "correctly fetch requested number of posts with daily sliding" in {
    oneUserTwoGroupsDataSetup()

    implicit val clock = java.time.Clock.fixed(fixedDateInPast.toInstant, utcZoneId)

    assert(
      awaitResults(
        sut.getTopPostsFromAllUserGroups(
          user1,
          untilPostCount = 4,
          noOlderThan = fixedDateInPast.minusDays(7).toInstant,
          timeSpanRequestedInOneRequest = JDuration.ofDays(1)
        )
      ).map(postToTestTuple) ==
        List(
          (1549062000, "second group is much better", group2.id),
          (1548979200, "second group is better", group2.id),
          (1548896400, "second post", group1.id),
          (1548807300, "first post", group1.id)
        )
    )
  }

  it should "return all post if found even though it couldn't fulfill whole count" in {
    oneUserTwoGroupsDataSetup()

    implicit val clock = java.time.Clock.fixed(fixedDateInPast.toInstant, utcZoneId)

    assert(
      awaitResults(
        sut.getTopPostsFromAllUserGroups(
          user1,
          untilPostCount = 10,
          noOlderThan = fixedDateInPast.minusDays(7).toInstant,
          timeSpanRequestedInOneRequest = JDuration.ofDays(1)
        )
      ).map(postToTestTuple) ==
        List(
          (1549062000, "second group is much better", group2.id),
          (1548979200, "second group is better", group2.id),
          (1548896400, "second post", group1.id),
          (1548807300, "first post", group1.id)
        )
    )
  }

  it should "return only requested count even if there are more" in {
    oneUserTwoGroupsDataSetup()

    implicit val clock = java.time.Clock.fixed(fixedDateInPast.toInstant, utcZoneId)

    assert(
      awaitResults(
        sut.getTopPostsFromAllUserGroups(
          user1,
          untilPostCount = 3,
          noOlderThan = fixedDateInPast.minusDays(7).toInstant,
          timeSpanRequestedInOneRequest = JDuration.ofDays(1)
        )
      ).map(postToTestTuple) ==
        List(
          (1549062000, "second group is much better", group2.id),
          (1548979200, "second group is better", group2.id),
          (1548896400, "second post", group1.id)
        )
    )
  }

  it should "return empty if it cannot fulfill requested count but search whole space" in {
    oneUserTwoGroupsDataSetup()

    implicit val clock = java.time.Clock.fixed(fixedDateInPast.toInstant, utcZoneId)

    assert(
      awaitResults(
        sut.getTopPostsFromAllUserGroups(
          UserId("10"),
          untilPostCount = 1,
          noOlderThan = fixedDateInPast.minusDays(14).toInstant,
          timeSpanRequestedInOneRequest = JDuration.ofDays(1)
        )
      ).map(postToTestTuple) == List()
    )
  }

  def multipleUsersGroupsAndPostsDataSetup(): Unit = {
    val membershipService = getMembershipService()

    val setupMemberships = for {
      _ <- membershipService.addUserToGroup(user1, group1)
      _ <- membershipService.addUserToGroup(user1, group2)
      _ <- membershipService.addUserToGroup(user2, group1)
      _ <- membershipService.addUserToGroup(user2, group2)
      _ <- membershipService.addUserToGroup(user3, group2)
      _ <- membershipService.addUserToGroup(user3, group3)
      _ <- membershipService.addUserToGroup(user4, emptyGroup)
    } yield ()

    awaitResults(setupMemberships)

    implicit val clock = java.time.Clock.fixed(fixedDateInPast.toInstant, utcZoneId)

    val insertPosts = for {
      _ <- sut.postOnGroup(
        content = "first post",
        createdAt = fixedDateInPast.minusDays(7).plusMinutes(15),
        userId = user1,
        groupId = group1
      )
      _ <- sut.postOnGroup(
        content = "nice 2 meet u",
        createdAt = fixedDateInPast.minusDays(6).plusHours(1),
        userId = user2,
        groupId = group1
      )
      _ <- sut.postOnGroup(
        content = "yeah u 2",
        createdAt = fixedDateInPast.minusDays(5),
        userId = user1,
        groupId = group1
      )
      _ <- sut.postOnGroup(
        content = "there is no content here",
        createdAt = oldestContentDate,
        userId = user2,
        groupId = group2
      )
      _ <- sut.postOnGroup(
        content = "yeah, the group is pretty much dead",
        createdAt = fixedDateInPast.minusDays(6).plusMinutes(15),
        userId = user3,
        groupId = group2
      )
      _ <- sut.postOnGroup(
        content = "maybe someone will see my awesome content",
        createdAt = fixedDateInPast.minusDays(7).minusMinutes(15),
        userId = user3,
        groupId = group3
      )
      _ <- sut.postOnGroup(
        content = "though they better come fast",
        createdAt = fixedDateInPast.minusDays(6).minusMinutes(15),
        userId = user3,
        groupId = group3
      )
    } yield ()

    awaitResults(insertPosts)
  }

  def oneUserTwoGroupsDataSetup() = {
    val membershipService = getMembershipService()

    val setupMemberships = for {
      _ <- membershipService.addUserToGroup(user1, group1)
      _ <- membershipService.addUserToGroup(user1, group2)
    } yield ()

    awaitResults(setupMemberships)
    val utcZoneId = ZoneId.of("UTC")

    implicit val clock = java.time.Clock.fixed(fixedDateInPast.toInstant, utcZoneId)

    val insertPosts = for {
      _ <- sut.postOnGroup(
        content = "first post",
        createdAt = fixedDateInPast.minusDays(3).plusMinutes(15),
        userId = user1,
        groupId = group1
      )
      _ <- sut.postOnGroup(
        content = "second post",
        createdAt = fixedDateInPast.minusDays(2).plusHours(1),
        userId = user1,
        groupId = group1
      )
      _ <- sut.postOnGroup(
        content = "second group is better",
        createdAt = fixedDateInPast.minusDays(1),
        userId = user1,
        groupId = group2
      )
      _ <- sut.postOnGroup(
        content = "second group is much better",
        createdAt = fixedDateInPast.minusHours(1),
        userId = user1,
        groupId = group2
      )
    } yield ()

    awaitResults(insertPosts)
  }

  protected def postToTestTuple(post: Post): (Int, String, String) = {
    (post._id.getTimestamp, post.content, post.groupId)
  }

  def getMembershipService() = new MembershipService(mongoDB)
  def sut: FeedService[Observable, ObjectId] = new MongoFeedService(mongoDB)
}
