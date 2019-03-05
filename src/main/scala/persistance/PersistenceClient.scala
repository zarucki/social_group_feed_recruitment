package persistance
import config.AppConfig
import mongo.{MembershipService, MongoHelper}
import org.apache.logging.log4j.scala.Logging
import org.mongodb.scala.{MongoClient, MongoDatabase, Observable}
import persistance.entities._

import scala.concurrent.Future

trait PersistenceClient[F[_]] {
  def init(): F[Unit]
  def getUserGroups(userId: UserId): F[Seq[UserGroup]]
  def addUserToGroup(userId: UserId, groupId: GroupId): F[Unit]
}
