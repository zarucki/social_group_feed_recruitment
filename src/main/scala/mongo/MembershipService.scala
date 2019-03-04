package mongo

import entities.MongoEntities._
import entities.{GroupUserMember, UserGroup}
import mongo.SimpleMongoEntityRepository.{GroupUserMembersRepo, UserGroupsRepo}
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.{Completed, MongoDatabase, Observable}

class MembershipService(mongoDatabase: MongoDatabase) {
  private val groupUserMembersRepo = new GroupUserMembersRepo(mongoDatabase)
  private val userGroupsRepo = new UserGroupsRepo(mongoDatabase)

  def addUserToGroup(userId: String, groupId: String): Observable[Completed] = {
    // TODO: what to do on failure here?
    // TODO: maybe update user aggregate number about count of groups he is in
    // TODO: maybe update group aggregate number about count of users are there in

    // Here to fire off them at once
    val userGroupsWrite = userGroupsRepo.put(UserGroup(userId = userId, groupId = groupId))
    val groupUserMembersWrite = groupUserMembersRepo.put(GroupUserMember(groupId = groupId, userId = userId))

    for {
      _           <- userGroupsWrite
      writeResult <- groupUserMembersWrite
    } yield writeResult
  }

  def getAllGroupsForUser(userId: String): Observable[String] = {
    userGroupsRepo
      .findByCondition[Document](equal(userIdKey, userId))
      .projection(fields(excludeId(), include(groupIdKey)))
      .map(_.getString(groupIdKey))
  }

  def getAllUsersForGroup(groupId: String): Observable[String] = {
    groupUserMembersRepo
      .findByCondition[Document](equal(groupIdKey, groupId))
      .projection(fields(excludeId(), include(userIdKey)))
      .map(_.getString(userIdKey))
  }
}
