package com.thehub.hb.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.thehub.hb.data.model.Comment
import com.thehub.hb.data.model.LikerUser
import com.thehub.hb.data.model.NotificationItem
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.remote.ImgbbService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PostRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val imgbbService: ImgbbService = ImgbbService(),
    private val notificationRepository: NotificationRepository = NotificationRepository(firestore, auth)
) {
    val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Resolves author info (username, photoUrl) for the current user.
     */
    private suspend fun getCurrentAuthorInfo(): Triple<String, String, String?> {
        val user = auth.currentUser
        val uid = user?.uid ?: ""
        var username = user?.displayName ?: ""
        var photoUrl = user?.photoUrl?.toString()

        try {
            if (uid.isNotEmpty()) {
                val userDoc = firestore.collection("users").document(uid).get().await()
                if (userDoc.exists()) {
                    val dbUsername = userDoc.getString("username")
                    val dbDisplayName = userDoc.getString("displayName")
                    val dbPhoto = userDoc.getString("photoUrl")
                    if (!dbUsername.isNullOrBlank()) username = dbUsername
                    else if (!dbDisplayName.isNullOrBlank()) username = dbDisplayName
                    if (!dbPhoto.isNullOrBlank()) photoUrl = dbPhoto
                }
            }
        } catch (_: Exception) {}

        if (username.isBlank()) {
            val email = user?.email ?: ""
            username = if (email.contains("@")) email.substringBefore("@") else "utilisateur"
        }

        return Triple(uid, username, photoUrl)
    }

    /**
     * Upload an image to ImgBB before posting.
     */
    suspend fun uploadPostImage(imageBytes: ByteArray): Result<String> {
        return imgbbService.uploadImage(imageBytes)
    }

    /**
     * Fetch feed posts ordered by createdAt descending with pagination.
     */
    suspend fun getFeed(
        pageSize: Long = 15,
        lastVisible: DocumentSnapshot? = null
    ): Result<Pair<List<Post>, DocumentSnapshot?>> = withContext(Dispatchers.IO) {
        try {
            var query = firestore.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(pageSize)

            if (lastVisible != null) {
                query = query.startAfter(lastVisible)
            }

            val snapshot = query.get().await()
            val currentUid = currentUserId

            val posts = snapshot.documents.map { doc ->
                val post = Post.fromSnapshot(doc, currentUid)
                post
            }

            // Hydrate like state for current user and originalPost for reposts
            val hydratedPosts = posts.map { post ->
                var isLiked = false
                if (currentUid != null) {
                    try {
                        val likeDoc = firestore.collection("posts").document(post.id)
                            .collection("likes").document(currentUid)
                            .get().await()
                        isLiked = likeDoc.exists()
                    } catch (_: Exception) {}
                }

                var original: Post? = null
                if (post.isRepost && !post.originalPostId.isNullOrBlank()) {
                    try {
                        val origDoc = firestore.collection("posts").document(post.originalPostId).get().await()
                        if (origDoc.exists()) {
                            original = Post.fromSnapshot(origDoc, currentUid)
                        }
                    } catch (_: Exception) {}
                }

                post.copy(isLikedByCurrentUser = isLiked, originalPost = original)
            }

            val newLastVisible = snapshot.documents.lastOrNull()
            Result.success(Pair(hydratedPosts, newLastVisible))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch a single post by id.
     */
    suspend fun getPost(postId: String): Result<Post> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("posts").document(postId).get().await()
            if (!doc.exists()) {
                return@withContext Result.failure(Exception("Publication introuvable"))
            }
            val currentUid = currentUserId
            val post = Post.fromSnapshot(doc, currentUid)

            var isLiked = false
            if (currentUid != null) {
                try {
                    val likeDoc = firestore.collection("posts").document(postId)
                        .collection("likes").document(currentUid).get().await()
                    isLiked = likeDoc.exists()
                } catch (_: Exception) {}
            }

            var original: Post? = null
            if (post.isRepost && !post.originalPostId.isNullOrBlank()) {
                try {
                    val origDoc = firestore.collection("posts").document(post.originalPostId).get().await()
                    if (origDoc.exists()) {
                        original = Post.fromSnapshot(origDoc, currentUid)
                    }
                } catch (_: Exception) {}
            }

            Result.success(post.copy(isLikedByCurrentUser = isLiked, originalPost = original))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a new post document in Firestore.
     */
    suspend fun createPost(text: String, imageUrl: String? = null): Result<Post> = withContext(Dispatchers.IO) {
        try {
            val (uid, username, photoUrl) = getCurrentAuthorInfo()
            if (uid.isEmpty()) {
                return@withContext Result.failure(Exception("Utilisateur non connecté"))
            }

            val docRef = firestore.collection("posts").document()
            val now = Timestamp.now()
            val post = Post(
                id = docRef.id,
                authorId = uid,
                authorUsername = username,
                authorPhotoUrl = photoUrl,
                text = text.trim(),
                imageUrl = imageUrl,
                createdAt = now,
                likesCount = 0,
                commentsCount = 0,
                isRepost = false,
                originalPostId = null,
                isLikedByCurrentUser = false
            )

            docRef.set(post.toMap()).await()
            Result.success(post)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggle like atomically on a post using Firestore transaction.
     * Returns true if post is now liked, false if unliked.
     */
    suspend fun toggleLike(postId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
            val postRef = firestore.collection("posts").document(postId)
            val likeRef = postRef.collection("likes").document(uid)

            val (nowLiked, authorId) = firestore.runTransaction { transaction ->
                val postDoc = transaction.get(postRef)
                val author = postDoc.getString("authorId") ?: ""
                val likeDoc = transaction.get(likeRef)
                if (likeDoc.exists()) {
                    transaction.delete(likeRef)
                    transaction.update(postRef, "likesCount", FieldValue.increment(-1))
                    Pair(false, author)
                } else {
                    transaction.set(likeRef, mapOf("likedAt" to Timestamp.now()))
                    transaction.update(postRef, "likesCount", FieldValue.increment(1))
                    Pair(true, author)
                }
            }.await()

            if (nowLiked && authorId.isNotBlank() && authorId != uid) {
                try {
                    notificationRepository.createNotification(
                        recipientId = authorId,
                        type = NotificationItem.TYPE_LIKE,
                        postId = postId
                    )
                } catch (_: Exception) {}
            }

            Result.success(nowLiked)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add a comment to a post atomically using Firestore transaction.
     */
    suspend fun addComment(postId: String, text: String): Result<Comment> = withContext(Dispatchers.IO) {
        try {
            val (uid, username, photoUrl) = getCurrentAuthorInfo()
            if (uid.isEmpty()) {
                return@withContext Result.failure(Exception("Non connecté"))
            }

            val postRef = firestore.collection("posts").document(postId)
            val commentRef = postRef.collection("comments").document()
            val now = Timestamp.now()

            val comment = Comment(
                id = commentRef.id,
                authorId = uid,
                authorUsername = username,
                authorPhotoUrl = photoUrl,
                text = text.trim(),
                createdAt = now
            )

            val authorId = firestore.runTransaction { transaction ->
                val postDoc = transaction.get(postRef)
                val author = postDoc.getString("authorId") ?: ""
                transaction.set(commentRef, comment.toMap())
                transaction.update(postRef, "commentsCount", FieldValue.increment(1))
                author
            }.await()

            if (authorId.isNotBlank() && authorId != uid) {
                try {
                    notificationRepository.createNotification(
                        recipientId = authorId,
                        type = NotificationItem.TYPE_COMMENT,
                        postId = postId,
                        commentText = text.trim()
                    )
                } catch (_: Exception) {}
            }

            Result.success(comment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all comments for a post, sorted from oldest to newest.
     */
    suspend fun getComments(postId: String): Result<List<Comment>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("posts").document(postId)
                .collection("comments")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()

            val comments = snapshot.documents.map { Comment.fromSnapshot(it) }
            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Real-time listener for comments on a post.
     */
    fun observeComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val listener = firestore.collection("posts").document(postId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val comments = snapshot?.documents?.map { Comment.fromSnapshot(it) } ?: emptyList()
                trySend(comments)
            }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    /**
     * Get list of users who liked the post.
     */
    suspend fun getLikers(postId: String): Result<List<LikerUser>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("posts").document(postId)
                .collection("likes")
                .orderBy("likedAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            val likers = snapshot.documents.map { doc ->
                val userId = doc.id
                val likedAt = doc.getTimestamp("likedAt") ?: Timestamp.now()
                var username = "thehub_user"
                var displayName: String? = null
                var photoUrl: String? = null

                try {
                    val userDoc = firestore.collection("users").document(userId).get().await()
                    if (userDoc.exists()) {
                        username = userDoc.getString("username") ?: username
                        displayName = userDoc.getString("displayName")
                        photoUrl = userDoc.getString("photoUrl")
                    }
                } catch (_: Exception) {}

                LikerUser(
                    uid = userId,
                    username = username,
                    displayName = displayName,
                    photoUrl = photoUrl,
                    likedAt = likedAt
                )
            }

            Result.success(likers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Repost a post to the current user's profile.
     */
    suspend fun repost(postId: String): Result<Post> = withContext(Dispatchers.IO) {
        try {
            val (uid, username, photoUrl) = getCurrentAuthorInfo()
            if (uid.isEmpty()) {
                return@withContext Result.failure(Exception("Non connecté"))
            }

            val docRef = firestore.collection("posts").document()
            val now = Timestamp.now()
            val repost = Post(
                id = docRef.id,
                authorId = uid,
                authorUsername = username,
                authorPhotoUrl = photoUrl,
                text = "",
                imageUrl = null,
                createdAt = now,
                likesCount = 0,
                commentsCount = 0,
                isRepost = true,
                originalPostId = postId,
                isLikedByCurrentUser = false
            )

            docRef.set(repost.toMap()).await()
            Result.success(repost)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
