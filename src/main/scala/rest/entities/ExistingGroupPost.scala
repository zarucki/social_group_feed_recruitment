package rest.entities
import java.time.Instant

case class ExistingGroupPost(
    postId: String,
    createdAt: Instant,
    content: String,
    userId: String,
    userName: String,
    groupId: String
)
