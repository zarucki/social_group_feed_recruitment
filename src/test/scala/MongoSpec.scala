import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}

import mongo.MongoService
import org.mongodb.scala.{MongoClient, MongoDatabase, Observable}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class MongoSpec extends UnitSpec with BeforeAndAfter with BeforeAndAfterAll {
  protected var mongoClient: MongoClient = _

  override def beforeAll() = {
    Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING)
  }

  before {
    mongoClient = MongoService.getMongoClient("mongodb://root:example@localhost:27017")

    val setup = for {
      _ <- getTestMongoDB.drop()
      _ <- MongoService.createIndexesIfMissing(getTestMongoDB)
    } yield ()
    awaitResults(setup)
  }

  after {
    awaitResults(getTestMongoDB.drop())
    mongoClient.close()
  }

  protected def getTestMongoDB: MongoDatabase = {
    MongoService.getMongoDBWhichUnderstandsEntities(mongoClient, "test_db")
  }

  protected def awaitResults[TResult](
      observable: Observable[TResult],
      duration: Duration = Duration(10, TimeUnit.SECONDS)
  ): Seq[TResult] = {
    Await.result(observable.toFuture(), duration)
  }
}
