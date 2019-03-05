package mongo
import entities._
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.UpdateOptions
import org.mongodb.scala.result.UpdateResult
import org.mongodb.scala.{Completed, FindObservable, MongoCollection, MongoDatabase, Observable, SingleObservable}

import scala.reflect.ClassTag

abstract class MongoEntityRepository[TEntity](mongoDatabase: MongoDatabase)(
    implicit entityClassTag: ClassTag[TEntity]
) {
  protected def entityCollectionName: String

  def put(entity: TEntity): Observable[Completed] = {
    getEntityCollection().insertOne(entity)
  }

  def updateMany(filter: Bson, update: Bson, options: UpdateOptions): SingleObservable[UpdateResult] = {
    getEntityCollection().updateMany(filter, update, options)
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
  type TimelineCacheRepo = SimpleMongoEntityRepository[TimelineCache]
}

class SimpleMongoEntityRepository[TEntity](mongoDatabase: MongoDatabase)(
    implicit ct: ClassTag[TEntity],
    storedInCollection: StoredInCollection[TEntity]
) extends MongoEntityRepository[TEntity](mongoDatabase) {
  override protected def entityCollectionName: String = storedInCollection.collectionName
}
