package mongo
import java.time.{Clock, ZonedDateTime}

import config.AppConfig
import mongo.entities.Post
import org.apache.logging.log4j.scala.Logging
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.{MongoWriteException, Observable}
import persistance.entities.{GroupId, UserId}
import persistance.{DuplicateWriteException, PersistenceClient}
import services.{CachedMongoFeedService, FeedService, MongoFeedService}

import scala.concurrent.{ExecutionContext, Future}

object MongoPersistenceClient {
  def apply(
      appConfig: AppConfig
  )(implicit executionContext: ExecutionContext, clock: Clock): Future[MongoPersistenceClient] = {
    val client = new MongoPersistenceClient(appConfig)
    client.init()
  }
}

class MongoPersistenceClient private (appConfig: AppConfig)(implicit executionContext: ExecutionContext, clock: Clock)
    extends PersistenceClient[Future]
    with Logging {
  private val mongoClient = MongoHelper.getMongoClient(appConfig.connectionString)
  private val mongoDB = MongoHelper.getMongoDBWhichUnderstandsEntities(mongoClient, appConfig.dbName)

  private val membershipService = new MembershipService(mongoDB)
  private val feedService: FeedService[Observable, ObjectId] =
    new CachedMongoFeedService(mongoDB, new MongoFeedService(mongoDB))

  override def init(): Future[MongoPersistenceClient] = {

    MongoHelper
      .createIndexesIfMissing(mongoDB)
      .collect()
      .map { messages =>
        messages.foreach(logger.info(_))
        ()
      }
      .head()
      .map { _ =>
        this
      }
  }

  override def getUserGroups(userId: UserId): Future[Seq[String]] = {
    membershipService
      .getAllGroupsForUser(userId)
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
      .recoverWith {
        case ex: MongoWriteException =>
          Future.failed(
            new DuplicateWriteException(s"Can't insert duplicate. User $userId is already in group $groupId.", ex)
          )
      }
  }

  override def addPostToGroup(
      userId: UserId,
      groupId: GroupId,
      content: String,
      userName: String
  ): Future[Either[Throwable, String]] = {
    feedService
      .postOnGroup(
        userId = userId,
        groupId = groupId,
        content = content,
        userName = Some(userName),
        createdAt = ZonedDateTime.now(clock)
      )
      .head()
      .map { _.right.map(_.toString) }
  }

  override def getGroupFeed(groupId: Long): Future[Seq[Post]] = {
    feedService.getTopPostsForGroup(GroupId(groupId)).collect().toFuture()
  }
}
