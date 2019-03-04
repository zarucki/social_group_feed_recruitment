package mongo

import com.mongodb.ConnectionString
import mongo.entities._
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.{MongoClient, MongoClientSettings, MongoDatabase, Observable, SingleObservable}

object MongoHelper {
  private val collectionAndTheirIndexes = Map(
    UserGroup.collection -> UserGroup.indexes,
    GroupUserMember.collection -> GroupUserMember.indexes,
    PostOwnership.collection -> PostOwnership.indexes
  )

  def getMongoClient(connectionString: String): MongoClient = {
    val settings: MongoClientSettings = MongoClientSettings
      .builder()
      .applyConnectionString(new ConnectionString(connectionString))
      .build()

    MongoClient(settings)
  }

  def getMongoDBWhichUnderstandsEntities(mongoClient: MongoClient, dbName: String): MongoDatabase = {
    // TODO: make list of those entities in a better way
    val codecRegistry = fromRegistries(
      fromProviders(
        classOf[Post],
        classOf[UserGroup],
        classOf[GroupUserMember],
        classOf[PostOwnership]
      ),
      DEFAULT_CODEC_REGISTRY
    )

    mongoClient.getDatabase(dbName).withCodecRegistry(codecRegistry)
  }

  // TODO: test this?
  def createIndexesIfMissing(mongoDatabase: MongoDatabase): Observable[String] = {
    val collectionAndIndexSeq: Seq[(String, IndexThatShouldBePresent)] = collectionAndTheirIndexes.toSeq.flatMap {
      case (collection, indexes) => indexes.map(collection -> _)
    }

    collectionAndIndexSeq.foldLeft[Observable[String]](Observable(List.empty)) {
      case (aggr, (collectionName, indexToCheck)) =>
        for {
          newMessageList     <- checkIfNecessaryIndexesArePresent(mongoDatabase, collectionName, indexToCheck)
          currentMessageList <- aggr.collect()
          result             <- Observable(currentMessageList :+ newMessageList)
        } yield result
    }
  }

  // Returns message after operation
  private def checkIfNecessaryIndexesArePresent(
      mongoDatabase: MongoDatabase,
      collectionName: String,
      indexThatShouldBePresent: IndexThatShouldBePresent
  ): SingleObservable[String] = {
    val collection = mongoDatabase.getCollection(collectionName)
    val (indexKey, indexOptions) = indexThatShouldBePresent.indexDefinition

    val allCollectionIndexesNames = collection
      .listIndexes()
      .map(_.get("name").map(_.asString().getValue).getOrElse(""))

    allCollectionIndexesNames.collect().flatMap { indexNames =>
      if (indexNames.exists(_ == indexThatShouldBePresent.indexName)) {
        SingleObservable(
          s"Index ${indexThatShouldBePresent.indexName} was already present on collection ${collectionName}."
        )
      } else {
        collection.createIndex(indexKey, indexOptions.name(indexThatShouldBePresent.indexName)).map { o =>
          s"Created index $o on collection ${collectionName}."
        }
      }
    }
  }
}
