import mongo.MembershipService
class MembershipServiceSpec extends MongoSpec {
  it should "should work in user direction" in {
    val sut = getSut()

    val userGroups = for {
      _      <- sut.addUserToGroup(user1, group1)
      _      <- sut.addUserToGroup(user1, group2)
      _      <- sut.addUserToGroup(user2, group1)
      result <- sut.getAllGroupsForUser(user1)
    } yield result

    assert(awaitResults(userGroups) == Seq(group1.id, group2.id))
  }

  it should "should work in group direction" in {
    val sut = getSut()

    val groupUserMembers = for {
      _      <- sut.addUserToGroup(user1, group1)
      _      <- sut.addUserToGroup(user1, group2)
      _      <- sut.addUserToGroup(user2, group1)
      result <- sut.getAllUsersForGroup(group1)
    } yield result

    assert(awaitResults(groupUserMembers) == Seq(user1.id, user2.id))
  }

  def getSut() = new MembershipService(mongoDB)
}
