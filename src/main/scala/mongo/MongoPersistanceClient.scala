package mongo
import config.AppConfig
import org.apache.logging.log4j.scala.Logging
import org.mongodb.scala.{MongoClient, MongoDatabase}
import persistance.PersistenceClient
import persistance.entities.{GroupId, UserGroup, UserId}

import scala.concurrent.{ExecutionContext, Future}

object MongoPersistenceClient {
  def apply(appConfig: AppConfig)(implicit executionContext: ExecutionContext): Future[MongoPersistenceClient] = {
    val client = new MongoPersistenceClient(appConfig)
    client.init().map { _ =>
      client
    }
  }
}

class MongoPersistenceClient private (appConfig: AppConfig) extends PersistenceClient[Future] with Logging {
  private val mongoClient = MongoHelper.getMongoClient(appConfig.connectionString)
  private val mongoDB = MongoHelper.getMongoDBWhichUnderstandsEntities(mongoClient, appConfig.dbName)

  private val membershipService = new MembershipService(mongoDB)

  override def init(): Future[Unit] = {

    MongoHelper
      .createIndexesIfMissing(mongoDB)
      .collect()
      .map { messages =>
        messages.foreach(logger.info(_))
        ()
      }
      .head()
  }

  override def getUserGroups(userId: UserId): Future[Seq[UserGroup]] = {
    membershipService
      .getAllGroupsForUser(userId)
      .map(UserGroup(_))
      .toFuture()
  }

  override def addUserToGroup(
      userId: UserId,
      groupId: GroupId
  ): Future[Unit] = {
    membershipService
      .addUserToGroup(userId, groupId)
      .map(_ => ())
      .head()
  }
}
