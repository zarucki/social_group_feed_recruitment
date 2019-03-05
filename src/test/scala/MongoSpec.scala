import java.time.{ZoneId, ZonedDateTime}
import java.util.concurrent.TimeUnit

import mongo.MongoHelper
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.mongodb.scala.{MongoClient, MongoDatabase, Observable}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import persistance.entities.{GroupId, UserId}

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
    Configurator.setLevel("org.mongodb.driver", Level.WARN)
    Configurator.setLevel("services", Level.DEBUG)
    Configurator.setLevel("mongo", Level.DEBUG)

    mongoClient = MongoHelper.getMongoClient("mongodb://root:example@localhost:27017")
  }

  override def afterAll(): Unit = {
    mongoClient.close()
  }

  before {
    mongoDB = MongoHelper.getMongoDBWhichUnderstandsEntities(mongoClient, "test-db")

    val setup = for {
      _ <- mongoDB.drop()
      _ <- MongoHelper.createIndexesIfMissing(mongoDB)
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
