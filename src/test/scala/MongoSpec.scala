import java.time.{ZoneId, ZonedDateTime}
import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}

import mongo.entities.{GroupId, UserId}
import mongo.MongoService
import org.mongodb.scala.{MongoClient, MongoDatabase, Observable}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class MongoSpec extends UnitSpec with BeforeAndAfter with BeforeAndAfterAll {
  protected var mongoClient: MongoClient = _
  protected var mongoDB: MongoDatabase = _

  protected val utcZoneId = ZoneId.of("UTC")
  protected val fixedDateInPast = ZonedDateTime.of(2019, 2, 2, 0, 0, 0, 0, utcZoneId)
  protected val (user1, user2, user3, user4) = (UserId("1"), UserId("2"), UserId("3"), UserId("4"))
  protected val (group1, group2, group3, emptyGroup) = (GroupId("1"), GroupId("2"), GroupId("3"), GroupId("4"))

  override def beforeAll() = {
    Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING)
    mongoClient = MongoService.getMongoClient("mongodb://root:example@localhost:27017")
  }

  override def afterAll(): Unit = {
    mongoClient.close()
  }

  before {
    mongoDB = MongoService.getMongoDBWhichUnderstandsEntities(mongoClient, "test_db")

    val setup = for {
      _ <- mongoDB.drop()
      _ <- MongoService.createIndexesIfMissing(mongoDB)
    } yield ()
    awaitResults(setup)
  }

  after {
//    awaitResults(mongoDB.drop())
  }

  protected def awaitResults[TResult](
      observable: Observable[TResult],
      duration: Duration = Duration(10, TimeUnit.SECONDS)
  ): Seq[TResult] = {
    Await.result(observable.toFuture(), duration)
  }
}
