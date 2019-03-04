import java.time.{Instant, ZoneId, ZonedDateTime}

import mongo.PostsService

class PostsServiceSpec extends MongoSpec {
  val user1 = "user_1"
  val group1 = "group_1"

  it should "should return correct posts in correct order" in {
    val sut = getSut()

    implicit val clock = java.time.Clock.fixed(Instant.now(), ZoneId.of("UTC"))

    val now = ZonedDateTime.now(clock)

    val insertPosts = for {
      _ <- sut.addPostToGroup(
        content = "content 1 in group 1 from user 1",
        createdAt = now.minusHours(1),
        userId = user1,
        groupId = group1
      )
      _ <- sut.addPostToGroup(
        content = "content 2 in group 1 from user 1",
        createdAt = now.minusHours(2),
        userId = "user_2",
        groupId = group1
      )
      _ <- sut.addPostToGroup(
        content = "content 3 in group 2 from user 1",
        createdAt = now.minusHours(3),
        userId = user1,
        groupId = "group_2"
      )
      _ <- sut.addPostToGroup(
        content = "content 4 in group 1 from user 3",
        createdAt = now.minusMinutes(15),
        userId = "user_3",
        groupId = group1
      )
    } yield ()
    awaitResults(insertPosts)

    assert(awaitResults(sut.getLatestPostsForOwners("nonexistinguser", 2)).map(_.content) == Seq())

    assert(
      awaitResults(sut.getLatestPostsForOwners(group1, 10)).map(_.content) == Seq(
        "content 4 in group 1 from user 3",
        "content 1 in group 1 from user 1",
        "content 2 in group 1 from user 1"
      )
    )

    assert(
      awaitResults(sut.getLatestPostsForOwners(group1, 2)).map(_.content) == Seq(
        "content 4 in group 1 from user 3",
        "content 1 in group 1 from user 1"
      )
    )

    assert(
      awaitResults(sut.getLatestPostsForOwners("user_1", 100)).map(_.content) == Seq(
        "content 1 in group 1 from user 1",
        "content 3 in group 2 from user 1"
      )
    )
  }

  def getSut() = new PostsService(mongoDB)
}
