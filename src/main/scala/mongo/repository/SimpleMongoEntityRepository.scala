package mongo.repository

import mongo.StoredInCollection
import mongo.entities._
import org.mongodb.scala.MongoDatabase

import scala.reflect.ClassTag

object SimpleMongoEntityRepository {
  type PostRepo = SimpleMongoEntityRepository[Post]
  type GroupUserMembersRepo = SimpleMongoEntityRepository[GroupUserMember]
  type UserGroupsRepo = SimpleMongoEntityRepository[UserGroup]
  type PostOwnershipsRepo = SimpleMongoEntityRepository[PostOwnership]
  type TimelineCacheRepo = SimpleMongoEntityRepository[TimelineCache]
}

class SimpleMongoEntityRepository[TEntity](mongoDatabase: MongoDatabase)(
    implicit ct: ClassTag[TEntity],
    storedInCollection: StoredInCollection[TEntity]
) extends MongoEntityRepository[TEntity](mongoDatabase) {
  override protected def entityCollectionName: String = storedInCollection.collectionName
}
