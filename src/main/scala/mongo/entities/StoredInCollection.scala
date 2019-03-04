package mongo.entities

trait StoredInCollection[A] {
  def collectionName: String
}
