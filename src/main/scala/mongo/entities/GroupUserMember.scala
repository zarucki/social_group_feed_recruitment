package mongo.entities
import mongo.StoredInCollection
import mongo.entities.MongoKeyNames.{OwnerId, _}
import org.mongodb.scala.bson.annotations.BsonProperty
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes._

object GroupUserMember {
  val collection: String = "group_user_members"

  val indexes = Seq(
    IndexThatShouldBePresent(
      s"index_${groupIdKey}_1_${userIdKey}_1",
      (compoundIndex(ascending(groupIdKey), ascending(userIdKey)), IndexOptions().unique(true))
    )
  )

  implicit val groupUsersStoredInCollection = new StoredInCollection[GroupUserMember] {
    override def collectionName: String = GroupUserMember.collection
  }

}

case class GroupUserMember(
    @BsonProperty("gid") groupId: OwnerId,
    @BsonProperty("uid") userId: OwnerId
)
