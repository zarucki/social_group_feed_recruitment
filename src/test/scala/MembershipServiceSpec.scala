import mongo.MembershipService
class MembershipServiceSpec extends MongoSpec {
  it should "should work in user direction" in {
    val sut = getSut()

    val userGroups = for {
      _      <- sut.addUserToGroup("user_1", "group_1")
      _      <- sut.addUserToGroup("user_1", "group_2")
      _      <- sut.addUserToGroup("user_2", "group_1")
      result <- sut.getAllGroupsForUser("user_1")
    } yield result

    assert(awaitResults(userGroups) == Seq("group_1", "group_2"))
  }

  it should "should work in group direction" in {
    val sut = getSut()

    val groupUserMembers = for {
      _      <- sut.addUserToGroup("user_1", "group_1")
      _      <- sut.addUserToGroup("user_1", "group_2")
      _      <- sut.addUserToGroup("user_2", "group_1")
      result <- sut.getAllUsersForGroup("group_1")
    } yield result

    assert(awaitResults(groupUserMembers) == Seq("user_1", "user_2"))
  }

  def getSut() = new MembershipService(mongoDB)
}
