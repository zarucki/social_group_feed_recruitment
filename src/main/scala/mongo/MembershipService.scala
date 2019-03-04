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
    // TODO: what to do on failure here?
    // TODO: maybe update user aggregate number about count of groups he is in
    // TODO: maybe update group aggregate number about count of users are there in

    // Here to fire off them at once
    val userGroupsWrite = userGroupsRepo.put(UserGroup(userId.id, groupId.id))
    val groupUserMembersWrite = groupUserMembersRepo.put(GroupUserMember(groupId.id, userId.id))

    for {
      _           <- userGroupsWrite
      writeResult <- groupUserMembersWrite
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
