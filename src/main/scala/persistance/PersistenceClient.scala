package persistance
import config.AppConfig
import mongo.{MembershipService, MongoHelper}
import org.apache.logging.log4j.scala.Logging
import org.mongodb.scala.MongoDatabase
import persistance.entities._

import scala.concurrent.Future

trait PersistenceClient[F[_]] {
  def getUserGroups(userId: UserId): F[Seq[UserGroup]]
  def addUserToGroup(userId: UserId, groupId: GroupId): F[Unit]
}

class MongoPersistenceClient(appConfig: AppConfig) extends PersistenceClient[Future] with Logging {
  private val mongoClient = MongoHelper.getMongoClient(appConfig.connectionString)
  private val mongoDB: MongoDatabase = MongoHelper.getMongoDBWhichUnderstandsEntities(mongoClient, appConfig.dbName)
  private val membershipService = new MembershipService(mongoDB)

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
