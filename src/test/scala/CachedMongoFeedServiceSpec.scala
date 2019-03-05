import java.time.{Duration => JDuration}

import org.mongodb.scala.Observable
import org.mongodb.scala.bson.ObjectId
import services.{CachedMongoFeedService, FeedService, MongoFeedService, TimelineCacheService}

// TODO: test that TTL works?
// TODO: test slicing of timelineCache
class CachedMongoFeedServiceSpec extends MongoFeedServiceSpec {
  override def sut: FeedService[Observable, ObjectId] =
    new CachedMongoFeedService(mongoDatabase = mongoDB, new MongoFeedService(mongoDB))

  it should "use caching" in {
    oneUserTwoGroupsDataSetup()

    assert(awaitResults(timelineCacheService.getTimelineCacheObjectForOwner(user1.id)) == Seq())

    implicit val clock = java.time.Clock.systemUTC()

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

    val timelineCacheObjectInitialState =
      awaitResults(timelineCacheService.getTimelineCacheObjectForOwner(user1.id)).headOption

    assert(
      timelineCacheObjectInitialState.map(_.topPostIds.map(_.getTimestamp)).getOrElse(Seq.empty) == Seq(
        1549062000,
        1548979200,
        1548896400,
        1548807300
      )
    )

    // TODO: test that cache was cache was actually used?
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

    awaitResults(sut.postOnGroup(user1, group1, "out of nowhere", fixedDateInPast.minusDays(2).plusHours(6)))

    val timelineCacheObjectAfterOneInsert =
      awaitResults(timelineCacheService.getTimelineCacheObjectForOwner(user1.id)).headOption

    assert(
      timelineCacheObjectAfterOneInsert.map(_.topPostIds.map(_.getTimestamp)).getOrElse(Seq.empty) == Seq(
        1549062000,
        1548979200,
        1548914400,
        1548896400,
        1548807300
      )
    )

    assert(
      timelineCacheObjectAfterOneInsert
        .map(_.lastUpdated)
        .exists(_.isAfter(timelineCacheObjectInitialState.get.lastUpdated))
    )

    assert(
      awaitResults(
        sut.getTopPostsFromAllUserGroups(
          user1,
          untilPostCount = 5,
          noOlderThan = fixedDateInPast.minusDays(7).toInstant,
          timeSpanRequestedInOneRequest = JDuration.ofHours(1)
        )
      ).map(postToTestTuple) ==
        List(
          (1549062000, "second group is much better", group2.id),
          (1548979200, "second group is better", group2.id),
          (1548914400, "out of nowhere", group1.id),
          (1548896400, "second post", group1.id),
          (1548807300, "first post", group1.id)
        )
    )
  }

  def timelineCacheService = new TimelineCacheService(mongoDB, 50)
}
