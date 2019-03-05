package mongo

trait StoredInCollection[A] {
  def collectionName: String
}
