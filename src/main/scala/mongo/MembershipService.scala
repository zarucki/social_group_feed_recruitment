package mongo

import entities.MongoKeyNames._
import entities.{GroupUserMember, UserGroup}
import mongo.repository.SimpleMongoEntityRepository.{GroupUserMembersRepo, TimelineCacheRepo, UserGroupsRepo}
import org.apache.logging.log4j.scala.Logging
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.{Completed, MongoDatabase, Observable}
import persistance.entities.{GroupId, UserId}

class MembershipService(mongoDatabase: MongoDatabase) extends Logging {
  private val groupUserMembersRepo = new GroupUserMembersRepo(mongoDatabase)
  private val userGroupsRepo = new UserGroupsRepo(mongoDatabase)
  private val timelineCacheRepo = new TimelineCacheRepo(mongoDatabase)

  def addUserToGroup(userId: UserId, groupId: GroupId): Observable[Completed] = {
    for {
      _           <- userGroupsRepo.put(UserGroup(userId.id, groupId.id))
      writeResult <- groupUserMembersRepo.put(GroupUserMember(groupId.id, userId.id))
      _ <- timelineCacheRepo.delete(equal(ownerIdKey, userId.id)).map { _ =>
        logger.debug(s"Deleted time line cache for user ${userId}")
      }
    } yield writeResult
  }

  def getAllGroupsForUser(userId: UserId): Observable[String] = {
    userGroupsRepo
      .findByCondition[Document](equal(userIdKey, userId.id))
      .projection(fields(excludeId(), include(groupIdKey)))
      .map(_.getString(groupIdKey))
  }

  def getAllUsersForGroup(groupId: GroupId): Observable[String] = {
    groupUserMembersRepo
      .findByCondition[Document](equal(groupIdKey, groupId.id))
      .projection(fields(excludeId(), include(userIdKey)))
      .map(_.getString(userIdKey))
  }
}
