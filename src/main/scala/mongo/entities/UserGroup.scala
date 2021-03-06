package mongo.entities
import mongo.StoredInCollection
import mongo.entities.MongoKeyNames.{OwnerId, _}
import mongo.indexes.IndexThatShouldBePresent
import org.mongodb.scala.bson.annotations.BsonProperty
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes._

object UserGroup {
  val collection: String = "user_groups"

  val indexes: Seq[IndexThatShouldBePresent] = Seq(
    IndexThatShouldBePresent(
      s"index_${userIdKey}_1_${groupIdKey}_1",
      (compoundIndex(ascending(userIdKey), ascending(groupIdKey)), IndexOptions().unique(true))
    )
  )

  implicit val userGroupsStoredInCollection = new StoredInCollection[UserGroup] {
    override def collectionName: String = UserGroup.collection
  }

}

case class UserGroup(
    @BsonProperty("uid") userId: OwnerId,
    @BsonProperty("gid") groupId: OwnerId
)
