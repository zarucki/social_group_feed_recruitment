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

  it should "should return empty if user has no group" in {
    val sut = getSut()
    assert(awaitResults(sut.getAllGroupsForUser(user1)) == Seq())
  }

  it should "should return if group has no users" in {
    val sut = getSut()
    assert(awaitResults(sut.getAllUsersForGroup(group1)) == Seq())
  }

  def getSut() = new MembershipService(mongoDB)
}
