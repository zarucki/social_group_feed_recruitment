package entities

trait StoredInCollection[A] {
  def collectionName: String
}

object StoredInCollection {
  implicit val groupPostStoredInCollection = new StoredInCollection[GroupPost] {
    override def collectionName: String = GroupPost.collection
  }

  implicit val userGroupsStoredInCollection = new StoredInCollection[UserGroups] {
    override def collectionName: String = UserGroups.collection
  }

  implicit val storedInCollection = new StoredInCollection[GroupUserMembers] {
    override def collectionName: String = GroupUserMembers.collection
  }
}
