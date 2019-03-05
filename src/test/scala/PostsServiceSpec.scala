import mongo.PostsService
import persistance.entities.UserId

class PostsServiceSpec extends MongoSpec {
  it should "should return correct posts in correct order" in {
    val sut = getSut()

    implicit val clock = java.time.Clock.fixed(fixedDateInPast.toInstant, utcZoneId)

    val insertPosts = for {
      _ <- sut.addPostToGroup(
        content = "content 1 in group 1 from user 1",
        createdAt = fixedDateInPast.minusHours(1),
        userId = user1,
        groupId = group1
      )
      _ <- sut.addPostToGroup(
        content = "content 2 in group 1 from user 1",
        createdAt = fixedDateInPast.minusHours(2),
        userId = user2,
        groupId = group1
      )
      _ <- sut.addPostToGroup(
        content = "content 3 in group 2 from user 1",
        createdAt = fixedDateInPast.minusHours(3),
        userId = user1,
        groupId = group2
      )
      _ <- sut.addPostToGroup(
        content = "content 4 in group 1 from user 3",
        createdAt = fixedDateInPast.minusMinutes(15),
        userId = user3,
        groupId = group1
      )
    } yield ()
    awaitResults(insertPosts)

    assert(
      awaitResults(
        sut.getLatestPostsForOwners(UserId("nonexistinguser"), after = fixedDateInPast.minusDays(7).toInstant)
      ).map(_.content) == Seq()
    )

    assert(
      awaitResults(sut.getLatestPostsForOwners(group1, after = fixedDateInPast.minusDays(7).toInstant))
        .map(_.content) == Seq(
        "content 4 in group 1 from user 3",
        "content 1 in group 1 from user 1",
        "content 2 in group 1 from user 1"
      )
    )

    assert(
      awaitResults(sut.getLatestPostsForOwners(group1, after = fixedDateInPast.minusMinutes(90).toInstant))
        .map(_.content) == Seq(
        "content 4 in group 1 from user 3",
        "content 1 in group 1 from user 1"
      )
    )

    assert(
      awaitResults(sut.getLatestPostsForOwners(user1, after = fixedDateInPast.minusDays(7).toInstant))
        .map(_.content) == Seq(
        "content 1 in group 1 from user 1",
        "content 3 in group 2 from user 1"
      )
    )
  }

  def getSut() = new PostsService(mongoDB)
}
