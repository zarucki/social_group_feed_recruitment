package mongo

import java.time._
import java.util.Date

import entities.MongoEntities._
import entities.{Post, PostOwnership}
import mongo.SimpleMongoEntityRepository.{PostOwnershipsRepo, PostRepo}
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.Sorts._
import org.mongodb.scala.{Completed, MongoDatabase, Observable}

class PostsService(mongoDatabase: MongoDatabase) {
  // TODO: make it that post themselves could be even in a different db
  private val postsRepo = new PostRepo(mongoDatabase)
  private val postOwnershipsRepo = new PostOwnershipsRepo(mongoDatabase)

  // TODO: this does not check permissions for writing to this group
  def addPostToGroup(
      content: String,
      createdAt: ZonedDateTime,
      userId: String,
      groupId: String
  )(implicit clock: Clock): Observable[Completed] = {
    val postId = new ObjectId(Date.from(createdAt.toInstant))

    val postToInsert = Post(
      _id = postId,
      dateInserted = Instant.now(clock),
      content = content,
      userId = userId,
      groupId = groupId
    )

    // On purpose everything is not stared at once, don't want to have ownership to non existing stuff inserted
    for {
      _          <- postsRepo.put(postToInsert)
      _          <- postOwnershipsRepo.put(PostOwnership(groupId, postId))
      lastInsert <- postOwnershipsRepo.put(PostOwnership(userId, postId))
    } yield lastInsert
  }

  def getLatestPostsIdsForOwners(ownerIds: Seq[String], postCount: Int): Observable[ObjectId] = {
    throwExceptionIfSequenceHasDuplicates(ownerIds)

    val ownerFilter = if (ownerIds.tail.isEmpty) {
      equal(ownerIdKey, ownerIds.head)
    } else {
      in(ownerIdKey, ownerIds: _*)
    }

    val latestPostsInGroup = postOwnershipsRepo
      .findByCondition[Document](ownerFilter)
      .limit(postCount)
      .sort(orderBy(ascending(ownerIdKey), descending(postIdKey))) // TODO: this duplicates what is in index definition
      .projection(fields(excludeId(), include(postIdKey)))
      .map(_.getObjectId(postIdKey))

    latestPostsInGroup
  }

  def getLatestPostsForOwners(ownerId: String, postCount: Int): Observable[Post] = {
    getLatestPostsForOwners(Seq(ownerId), postCount)
  }

  def getLatestPostsForOwners(ownerIds: Seq[String], postCount: Int): Observable[Post] = {
    throwExceptionIfSequenceHasDuplicates(ownerIds)

    for {
      postIds     <- getLatestPostsIdsForOwners(ownerIds, postCount).collect()
      fetchedPost <- fetchPostsByIds(postIds)
    } yield fetchedPost
  }

  def fetchPostsByIds(postIds: Seq[ObjectId]): Observable[Post] = {
    throwExceptionIfSequenceHasDuplicates(postIds)

    postsRepo
      .findByCondition[Post](in(_id, postIds: _*))
      .limit(postIds.size)
      .sort(descending(_id))
  }

  def throwExceptionIfSequenceHasDuplicates[T](seq: Seq[T]) = {
    if (seq.toSet.size != seq.size) {
      throw new Exception("Probably something weird happening, cause I got duplicates in: " + seq)
    }
  }
}
