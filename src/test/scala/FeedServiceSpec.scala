import java.time.{ZoneId, ZonedDateTime}

import mongo.{MembershipService, PostsService}
import services.FeedService

class FeedServiceSpec extends MongoSpec {
  val (user1, user2, user3) = ("user_1", "user_2", "user_3")
  val (group1, group2, group3) = ("group_1", "group_2", "group_3")

  it should "should return feeds of multiple groups" in {
    val sut = getSut()
    val membershipService = getMembershipService()
    val postService = getPostService()

    val setupMemberships = for {
      _ <- membershipService.addUserToGroup(user1, group1)
      _ <- membershipService.addUserToGroup(user1, group2)
      _ <- membershipService.addUserToGroup(user2, group2)
      _ <- membershipService.addUserToGroup(user2, group3)
      _ <- membershipService.addUserToGroup(user3, group3)
    } yield ()

    awaitResults(setupMemberships)
    val utcZoneId = ZoneId.of("UTC")

    val dateInPast = ZonedDateTime.of(2019, 2, 2, 0, 0, 0, 0, utcZoneId)
    implicit val clock = java.time.Clock.fixed(dateInPast.toInstant, utcZoneId)

    val insertPosts = for {
      _ <- postService.addPostToGroup(
        content = "first post",
        createdAt = dateInPast.minusDays(7).plusMinutes(15),
        userId = user1,
        groupId = group1
      )
      _ <- postService.addPostToGroup(
        content = "nice 2 meet u",
        createdAt = dateInPast.minusDays(6).plusHours(1),
        userId = user2,
        groupId = group1
      )
      _ <- postService.addPostToGroup(
        content = "yeah u 2",
        createdAt = dateInPast.minusDays(5),
        userId = user1,
        groupId = group1
      )
      _ <- postService.addPostToGroup(
        content = "there is no content here",
        createdAt = dateInPast.minusDays(7).minusHours(1),
        userId = user2,
        groupId = group2
      )
      _ <- postService.addPostToGroup(
        content = "yeah, the group is pretty much dead",
        createdAt = dateInPast.minusDays(6).plusMinutes(15),
        userId = user3,
        groupId = group2
      )
      _ <- postService.addPostToGroup(
        content = "maybe someone will see my awesome content",
        createdAt = dateInPast.minusDays(7).minusMinutes(15),
        userId = user3,
        groupId = group3
      )
      _ <- postService.addPostToGroup(
        content = "though they better come fast",
        createdAt = dateInPast.minusDays(6).minusMinutes(15),
        userId = user3,
        groupId = group3
      )
    } yield ()

    awaitResults(insertPosts)

    assert(awaitResults(sut.getTopPostsFromAllGroupsFeedForUser("user_10", 10)) == Seq())

    assert(
      awaitResults(sut.getTopPostsFromAllGroupsFeedForUser(user1, postCount = 20))
        .map(p => (p._id.getTimestamp, p.content, p.groupId)) == List(
        (1548633600, "yeah u 2", group1),
        (1548550800, "nice 2 meet u", group1),
        (1548548100, "yeah, the group is pretty much dead", group2),
        (1548461700, "first post", group1),
        (1548457200, "there is no content here", group2)
      )
    )

    awaitResults(membershipService.addUserToGroup(user1, group3))

    assert(
      awaitResults(sut.getTopPostsFromAllGroupsFeedForUser(user1, postCount = 20))
        .map(p => (p._id.getTimestamp, p.content, p.groupId)) == List(
        (1548633600, "yeah u 2", group1),
        (1548550800, "nice 2 meet u", group1),
        (1548548100, "yeah, the group is pretty much dead", group2),
        (1548546300, "though they better come fast", group3),
        (1548461700, "first post", group1),
        (1548459900, "maybe someone will see my awesome content", group3),
        (1548457200, "there is no content here", group2)
      )
    )

    assert(
      awaitResults(sut.getTopPostsFromAllGroupsFeedForUser(user1, postCount = 6))
        .map(p => (p._id.getTimestamp, p.content, p.groupId)) == List(
        (1548633600, "yeah u 2", group1),
        (1548550800, "nice 2 meet u", group1),
        (1548548100, "yeah, the group is pretty much dead", group2),
        (1548546300, "though they better come fast", group3),
        (1548461700, "first post", group1),
        (1548457200, "there is no content here", group2)
      )
    )

    // Below shit is confusing (where did group3 go?), we need to change how we take a top posts
    assert(
      awaitResults(sut.getTopPostsFromAllGroupsFeedForUser(user1, postCount = 5))
        .map(p => (p._id.getTimestamp, p.content, p.groupId)) == List(
        (1548633600, "yeah u 2", group1),
        (1548550800, "nice 2 meet u", group1),
        (1548548100, "yeah, the group is pretty much dead", group2),
        (1548461700, "first post", group1),
        (1548457200, "there is no content here", group2)
      )
    )
  }

  // TODO: fix this weird behavior
  it should "should correctly take latest posts" in {
    val sut = getSut()
    val membershipService = getMembershipService()
    val postService = getPostService()

    val setupMemberships = for {
      _ <- membershipService.addUserToGroup(user1, group1)
      _ <- membershipService.addUserToGroup(user1, group2)
    } yield ()

    awaitResults(setupMemberships)
    val utcZoneId = ZoneId.of("UTC")

    val dateInpast = ZonedDateTime.of(2019, 2, 2, 0, 0, 0, 0, utcZoneId)
    implicit val clock = java.time.Clock.fixed(dateInpast.toInstant, utcZoneId)

    val insertPosts = for {
      _ <- postService.addPostToGroup(
        content = "first post",
        createdAt = dateInpast.minusDays(3).plusMinutes(15),
        userId = user1,
        groupId = group1
      )
      _ <- postService.addPostToGroup(
        content = "second post",
        createdAt = dateInpast.minusDays(2).plusHours(1),
        userId = user1,
        groupId = group1
      )
      _ <- postService.addPostToGroup(
        content = "second group is better",
        createdAt = dateInpast.minusDays(1),
        userId = user1,
        groupId = group2
      )
      _ <- postService.addPostToGroup(
        content = "second group is much better",
        createdAt = dateInpast.minusHours(1),
        userId = user1,
        groupId = group2
      )
    } yield ()

    awaitResults(insertPosts)

    assert(
      awaitResults(sut.getTopPostsFromAllGroupsFeedForUser(user1, postCount = 10))
        .map(p => (p._id.getTimestamp, p.content, p.groupId)) ==
        List(
          (1549062000, "second group is much better", group2),
          (1548979200, "second group is better", group2),
          (1548896400, "second post", group1),
          (1548807300, "first post", group1)
        )
    )

    assert(
      awaitResults(sut.getTopPostsFromAllGroupsFeedForUser(user1, postCount = 2))
        .map(p => (p._id.getTimestamp, p.content, p.groupId)) ==
        List(
          (1548896400, "second post", group1),
          (1548807300, "first post", group1)
        )
    )
  }

  def getMembershipService() = new MembershipService(mongoDB)
  def getPostService() = new PostsService(mongoDB)
  def getSut() = new FeedService(mongoDB)
}
