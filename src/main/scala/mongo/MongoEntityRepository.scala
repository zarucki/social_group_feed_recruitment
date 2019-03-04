package mongo
import entities._
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.{Completed, FindObservable, MongoCollection, MongoDatabase, Observable}

import scala.reflect.ClassTag

abstract class MongoEntityRepository[TEntity](mongoDatabase: MongoDatabase)(
    implicit entityClassTag: ClassTag[TEntity]
) {
  protected def entityCollectionName: String

  // TODO: some validation?
  def put(entity: TEntity): Observable[Completed] = {
    getEntityCollection().insertOne(entity)
  }

  def findByCondition[T: ClassTag](filter: Bson): FindObservable[T] = {
    getEntityCollection[T]().find(filter)
  }

  protected def getEntityCollection[T: ClassTag](): MongoCollection[T] = {
    mongoDatabase.getCollection[T](entityCollectionName)
  }
}

object SimpleMongoEntityRepository {
  type PostRepo = SimpleMongoEntityRepository[Post]
  type GroupUserMembersRepo = SimpleMongoEntityRepository[GroupUserMember]
  type UserGroupsRepo = SimpleMongoEntityRepository[UserGroup]
  type PostOwnershipsRepo = SimpleMongoEntityRepository[PostOwnership]
}

class SimpleMongoEntityRepository[TEntity](mongoDatabase: MongoDatabase)(
    implicit ct: ClassTag[TEntity],
    storedInCollection: StoredInCollection[TEntity]
) extends MongoEntityRepository[TEntity](mongoDatabase) {
  override protected def entityCollectionName: String = storedInCollection.collectionName
}
