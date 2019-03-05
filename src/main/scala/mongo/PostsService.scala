package mongo

import java.time._
import java.util.Date

import entities.MongoKeyNames._
import entities._
import mongo.repository.SimpleMongoEntityRepository.{PostOwnershipsRepo, PostRepo}
import org.apache.logging.log4j.scala.Logging
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.Sorts._
import org.mongodb.scala.{Completed, MongoDatabase, Observable}
import persistance.entities.{GroupId, PersistenceContentOwnerId, UserId}

class PostsService(mongoDatabase: MongoDatabase) extends Logging {
  // TODO: make it that post themselves could be even in a different db
  private val postsRepo = new PostRepo(mongoDatabase)
  private val postOwnershipsRepo = new PostOwnershipsRepo(mongoDatabase)

  // TODO: this does not check permissions for writing to this group
  def addPostToGroup(
      userId: UserId,
      groupId: GroupId,
      content: String,
      createdAt: ZonedDateTime,
      userName: Option[String] = None
  )(implicit clock: Clock): Observable[ObjectId] = {
    val postId = new ObjectId(Date.from(createdAt.toInstant))

    val postToInsert = Post(
      _id = postId,
      insertedAt = Instant.now(clock),
      content = content,
      userId = userId.id,
      groupId = groupId.id,
      userName = userName
    )

    addPostToGroup(postToInsert).map(_ => postId)
  }

  protected def addPostToGroup(post: Post): Observable[Completed] = {
    for {
      _          <- postsRepo.put(post)
      _          <- postOwnershipsRepo.put(PostOwnership(post.groupId, post._id))
      lastInsert <- postOwnershipsRepo.put(PostOwnership(post.userId, post._id))
    } yield lastInsert
  }

  def getLatestPostsIdsForOwners(
      ownerIds: Seq[String],
      after: ObjectId,
      before: Option[ObjectId] = None
  ): Observable[ObjectId] = {
    throwExceptionIfSequenceHasDuplicates(ownerIds)

    if (ownerIds.isEmpty) {
      Observable(List.empty)
    } else {
      val ownerFilter = if (ownerIds.tail.isEmpty) {
        equal(ownerIdKey, ownerIds.head)
      } else {
        in(ownerIdKey, ownerIds: _*)
      }

      val afterFilter = gte(postIdKey, after)
      val timeFilter = before
        .map { beforeObjectId =>
          and(afterFilter, lt(postIdKey, beforeObjectId))
        }
        .getOrElse(afterFilter)

      val latestPostsInGroup = postOwnershipsRepo
        .findByCondition[Document](and(ownerFilter, timeFilter))
        .sort(orderBy(ascending(ownerIdKey), descending(postIdKey))) // TODO: this duplicates what is in index definition
        .projection(fields(excludeId(), include(postIdKey)))
        .map(_.getObjectId(postIdKey))

      latestPostsInGroup
    }
  }

  def getLatestPostsForOwner(ownerId: PersistenceContentOwnerId, after: Instant): Observable[Post] = {
    getLatestPostsForOwners(Seq(ownerId.id), after)
  }

  def getLatestPostsForOwners(ownerIds: Seq[String], after: Instant): Observable[Post] = {
    getPostsForOwners(ownerIds, after = new ObjectId(Date.from(after)))
  }

  def getPostsForOwners(ownerIds: Seq[String], after: ObjectId, before: Option[ObjectId] = None): Observable[Post] = {
    for {
      postIds     <- getLatestPostsIdsForOwners(ownerIds, after, before).collect()
      fetchedPost <- fetchPostsByIds(postIds)
    } yield fetchedPost
  }

  def fetchPostsByIds(postIds: Seq[ObjectId]): Observable[Post] = {
    // TODO: watch out for to huge seqs here?

    if (postIds.isEmpty) {
      Observable(List.empty)
    } else {
      postsRepo
        .findByCondition[Post](in(_id, postIds: _*))
        .limit(postIds.size)
        .sort(descending(_id))
    }
  }

  def throwExceptionIfSequenceHasDuplicates[T](seq: Seq[T]) = {
    if (seq.toSet.size != seq.size) {
      logger.error("There are duplicates in: " + seq)
    }
  }
}
