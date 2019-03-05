package persistance

sealed trait PersistenceException {
  self: Throwable =>

  val message: String
  val throwable: Throwable
}

case class DuplicateWriteException(message: String, throwable: Throwable)
    extends Exception(message, throwable)
    with PersistenceException
