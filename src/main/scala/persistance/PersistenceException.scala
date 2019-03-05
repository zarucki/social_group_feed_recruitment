package persistance

sealed trait PersistenceException {
  self: Throwable =>

  val message: String
}

case class DuplicateWriteException(message: String, throwable: Throwable)
    extends Exception(message, throwable)
    with PersistenceException

case class UserPostedToNotHisGroupException(userId: String, groupId: String, message: String = "")
    extends Exception(s"User $userId posted to $groupId while not being a member." + message)
    with PersistenceException
