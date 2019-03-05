package mongo.repository

import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.UpdateOptions
import org.mongodb.scala.result.{DeleteResult, UpdateResult}
import org.mongodb.scala.{Completed, FindObservable, MongoCollection, MongoDatabase, Observable, SingleObservable}

import scala.reflect.ClassTag

abstract class MongoEntityRepository[TEntity](mongoDatabase: MongoDatabase)(
    implicit entityClassTag: ClassTag[TEntity]
) {
  protected def entityCollectionName: String

  def put(entity: TEntity): Observable[Completed] = {
    getEntityCollection().insertOne(entity)
  }

  def delete(filter: Bson): SingleObservable[DeleteResult] = {
    getEntityCollection().deleteOne(filter)
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
