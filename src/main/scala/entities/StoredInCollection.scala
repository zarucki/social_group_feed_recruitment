package entities

trait StoredInCollection[A] {
  def collectionName: String
}

object StoredInCollection {
  implicit val postsStoredInCollection = new StoredInCollection[Post] {
    override def collectionName: String = Post.collection
  }

  implicit val userGroupsStoredInCollection = new StoredInCollection[UserGroup] {
    override def collectionName: String = UserGroup.collection
  }

  implicit val groupUsersStoredInCollection = new StoredInCollection[GroupUserMember] {
    override def collectionName: String = GroupUserMember.collection
  }

  implicit val postOwnershipsStoredInCollection = new StoredInCollection[PostOwnership] {
    override def collectionName: String = PostOwnership.collection
  }
}
