package mongo

import com.mongodb.ConnectionString
import entities.MongoEntities._
import entities.{MongoEntities, GroupPost, GroupUserMembers, UserGroups}
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.{MongoClient, MongoClientSettings, MongoDatabase, Observable}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.IndexOptions
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._

// TODO: rename this
object MongoService {

  case class IndexThatShouldBePresent(indexName: String, indexDefinition: (Bson, IndexOptions))

  def getMongoClient(connectionString: String): MongoClient = {
    val settings: MongoClientSettings = MongoClientSettings
      .builder()
      .applyConnectionString(new ConnectionString(connectionString))
      .build()

    MongoClient(settings)
  }

  def getMongoDBWhichUnderstandsEntities(mongoClient: MongoClient, dbName: String): MongoDatabase = {
    val codecRegistry = fromRegistries(
      fromProviders(
        classOf[GroupPost],
        classOf[UserGroups],
        classOf[GroupUserMembers]
      ),
      DEFAULT_CODEC_REGISTRY
    )

    mongoClient.getDatabase(dbName).withCodecRegistry(codecRegistry)
  }

  def createIndexesIfMissing(mongoDatabase: MongoDatabase): Observable[String] = {
    indexesThatShouldBePresent.foldLeft[Observable[String]](Observable(List.empty)) {
      case (aggr, (collectionName, indexToCheck)) =>
        aggr.collect().flatMap { currentList =>
          checkIfNecessaryIndexesArePresent(mongoDatabase, collectionName, indexToCheck).collect().flatMap { newList =>
            Observable(currentList ++ newList)
          }
        }
    }
  }

  private def checkIfNecessaryIndexesArePresent(
      mongoDatabase: MongoDatabase,
      collectionName: String,
      indexThatShouldBePresent: IndexThatShouldBePresent
  ): Observable[String] = {
    val collection = mongoDatabase.getCollection(collectionName)
    val (indexKey, indexOptions) = indexThatShouldBePresent.indexDefinition

    val allCollectionIndexesNames = collection
      .listIndexes()
      .map(_.get("name").map(_.asString().getValue).getOrElse(""))

    allCollectionIndexesNames.collect().flatMap { indexNames =>
      if (indexNames.exists(_ == indexThatShouldBePresent.indexName)) {
        Observable(
          Seq(s"Index ${indexThatShouldBePresent.indexName} was already present on collection ${collectionName}.")
        )
      } else {
        collection.createIndex(indexKey, indexOptions.name(indexThatShouldBePresent.indexName)).map { o =>
          s"Created index $o on collection ${collectionName}."
        }
      }
    }
  }

  private def indexesThatShouldBePresent = Map(
    UserGroups.collection -> IndexThatShouldBePresent(
      s"index_${userIdKey}_1_${groupIdKey}_1",
      (Document(userIdKey -> 1, groupIdKey -> 1), IndexOptions().unique(true))
    ),
    GroupUserMembers.collection -> IndexThatShouldBePresent(
      s"index_${groupIdKey}_1_${userIdKey}_1",
      (Document(groupIdKey -> 1, userIdKey -> 1), IndexOptions().unique(true))
    )
  )
}
