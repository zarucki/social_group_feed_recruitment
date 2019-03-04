import java.util.concurrent.TimeUnit

import mongo.{MembershipService, MongoService, PostsService}
import org.mongodb.scala.MongoDatabase

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import org.apache.logging.log4j.scala.Logging

object Main extends App with Logging {
  val mongoClient = MongoService.getMongoClient("mongodb://root:example@localhost:27017")
  val mongoDB: MongoDatabase = MongoService.getMongoDBWhichUnderstandsEntities(mongoClient, "mydb")
  val membershipService = new MembershipService(mongoDB)
  val postsService = new PostsService(mongoDB)

  val result = Await.result(MongoService.createIndexesIfMissing(mongoDB).toFuture(), Duration(10, TimeUnit.SECONDS))
  result.foreach(logger.info(_))

  mongoClient.close()
}
