package mongo.indexes
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.IndexOptions

case class IndexThatShouldBePresent(indexName: String, indexDefinition: (Bson, IndexOptions))
