package mongo

import com.mongodb.ConnectionString
import mongo.entities.MongoEntities._
import mongo.entities._
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes._
import org.mongodb.scala.{MongoClient, MongoClientSettings, MongoDatabase, Observable, SingleObservable}

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

  // TODO: move it somewhere closer to collection definition
  private def collectionAndTheirIndexes = Map(
    UserGroup.collection -> Seq(
      IndexThatShouldBePresent(
        s"index_${userIdKey}_1_${groupIdKey}_1",
        (compoundIndex(ascending(userIdKey), ascending(groupIdKey)), IndexOptions().unique(true))
      )
    ),
    GroupUserMember.collection -> Seq(
      IndexThatShouldBePresent(
        s"index_${groupIdKey}_1_${userIdKey}_1",
        (compoundIndex(ascending(groupIdKey), ascending(userIdKey)), IndexOptions().unique(true))
      )
    ),
    PostOwnership.collection -> Seq(
      IndexThatShouldBePresent(
        s"index_${ownerIdKey}_1_${postIdKey}_-1",
        (compoundIndex(ascending(ownerIdKey), descending(postIdKey)), IndexOptions().unique(true))
      )
    )
  )
}
