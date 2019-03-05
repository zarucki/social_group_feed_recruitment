package mongo

import entities.MongoKeyNames._
import entities.{GroupId, GroupUserMember, UserGroup, UserId}
import mongo.SimpleMongoEntityRepository.{GroupUserMembersRepo, UserGroupsRepo}
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.{Completed, MongoDatabase, Observable}

class MembershipService(mongoDatabase: MongoDatabase) {
  private val groupUserMembersRepo = new GroupUserMembersRepo(mongoDatabase)
  private val userGroupsRepo = new UserGroupsRepo(mongoDatabase)

  def addUserToGroup(userId: UserId, groupId: GroupId): Observable[Completed] = {
    for {
      _           <- userGroupsRepo.put(UserGroup(userId.id, groupId.id))
      writeResult <- groupUserMembersRepo.put(GroupUserMember(groupId.id, userId.id))
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
