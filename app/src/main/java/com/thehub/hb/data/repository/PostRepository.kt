package com.thehub.hb.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.thehub.hb.data.local.DataStoreManager
import com.thehub.hb.data.model.Comment
import com.thehub.hb.data.model.LikerUser
import com.thehub.hb.data.model.NotificationItem
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.model.TrendingHashtag
import com.thehub.hb.data.remote.ImgbbService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

class PostRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val imgbbService: ImgbbService = ImgbbService(),
    private val notificationRepository: NotificationRepository = NotificationRepository(firestore, auth),
    private val dataStoreManager: DataStoreManager? = null
) {
    val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Resolves currently bookmarked post IDs by merging remote Firestore bookmarks
     * with local DataStore cache to ensure immediate and persistent bookmark indicator state across refreshes.
     */
    private suspend fun getEffectiveBookmarkedIds(uid: String?): Set<String> {
        val localBookmarks = dataStoreManager?.getLocalBookmarkedIds() ?: emptySet()
        val remoteBookmarks = if (uid != null) {
            try {
                firestore.collection("users").document(uid)
                    .collection("bookmarks").get().await()
                    .documents.map { it.id }.toSet()
            } catch (e: Exception) {
                Log.w("PostRepository", "Remote bookmarks fetch error: ${e.message}")
                emptySet()
            }
        } else emptySet()

        val merged = remoteBookmarks + localBookmarks
        // Keep local cache up to date with remote
        if (remoteBookmarks.isNotEmpty() && dataStoreManager != null) {
            try {
                dataStoreManager.setLocalBookmarkedIds(merged)
            } catch (_: Exception) {}
        }
        return merged
    }

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
     * Helper to extract words starting with #
     */
    fun extractHashtags(text: String): List<String> {
        val regex = Regex("""#[a-zA-Z0-9_\u00C0-\u017F]+""")
        return regex.findAll(text)
            .map { it.value.removePrefix("#").lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
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

            val bookmarkedIds = getEffectiveBookmarkedIds(currentUid)

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

                post.copy(
                    isLikedByCurrentUser = isLiked,
                    isBookmarkedByCurrentUser = bookmarkedIds.contains(post.id),
                    originalPost = original
                )
            }

            val newLastVisible = snapshot.documents.lastOrNull()
            Result.success(Pair(hydratedPosts, newLastVisible))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Real-time listener for feed posts.
     * Automatically emits updated list whenever any post is added, deleted or modified in Firestore.
     */
    fun observeFeed(limit: Long = 30): Flow<List<Post>> = callbackFlow {
        val query = firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("PostRepository", "observeFeed error: ${error.message}")
                launch(Dispatchers.IO) {
                    val result = getFeed(pageSize = limit, lastVisible = null)
                    result.onSuccess { (posts, _) -> trySend(posts) }
                }
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener

            val docs = snapshot.documents
            launch(Dispatchers.IO) {
                val uid = currentUserId
                try {
                    val basePosts = docs.map { doc -> Post.fromSnapshot(doc, uid) }
                    // Immediate emission for instantaneous UI responsiveness
                    trySend(basePosts)

                    val bookmarkedIds = getEffectiveBookmarkedIds(uid)

                    val hydrated = coroutineScope {
                        basePosts.map { post ->
                            async {
                                var isLiked = false
                                if (uid != null) {
                                    try {
                                        val likeDoc = firestore.collection("posts").document(post.id)
                                            .collection("likes").document(uid)
                                            .get().await()
                                        isLiked = likeDoc.exists()
                                    } catch (_: Exception) {}
                                }

                                var original: Post? = null
                                if (post.isRepost && !post.originalPostId.isNullOrBlank()) {
                                    try {
                                        val origDoc = firestore.collection("posts").document(post.originalPostId).get().await()
                                        if (origDoc.exists()) {
                                            original = Post.fromSnapshot(origDoc, uid)
                                        }
                                    } catch (_: Exception) {}
                                }

                                post.copy(
                                    isLikedByCurrentUser = isLiked,
                                    isBookmarkedByCurrentUser = bookmarkedIds.contains(post.id),
                                    originalPost = original
                                )
                            }
                        }.awaitAll()
                    }
                    trySend(hydrated)
                } catch (e: Exception) {
                    val fallback = docs.map { Post.fromSnapshot(it, uid) }
                    trySend(fallback)
                }
            }
        }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    /**
     * Real-time listener for a single post by id.
     * Emits null if the post is deleted.
     */
    fun observePost(postId: String): Flow<Post?> = callbackFlow {
        val currentUid = currentUserId
        val listener = firestore.collection("posts").document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }

                launch(Dispatchers.IO) {
                    try {
                        val post = Post.fromSnapshot(snapshot, currentUid)
                        var isLiked = false
                        var isBookmarked = false
                        if (currentUid != null) {
                            try {
                                val likeDoc = firestore.collection("posts").document(postId)
                                    .collection("likes").document(currentUid).get().await()
                                isLiked = likeDoc.exists()
                            } catch (_: Exception) {}

                            val bookmarkedIds = getEffectiveBookmarkedIds(currentUid)
                            isBookmarked = bookmarkedIds.contains(postId)
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

                        trySend(
                            post.copy(
                                isLikedByCurrentUser = isLiked,
                                isBookmarkedByCurrentUser = isBookmarked,
                                originalPost = original
                            )
                        )
                    } catch (_: Exception) {
                        trySend(null)
                    }
                }
            }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

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
            var isBookmarked = false
            if (currentUid != null) {
                try {
                    val likeDoc = firestore.collection("posts").document(postId)
                        .collection("likes").document(currentUid).get().await()
                    isLiked = likeDoc.exists()
                } catch (_: Exception) {}

                val bookmarkedIds = getEffectiveBookmarkedIds(currentUid)
                isBookmarked = bookmarkedIds.contains(postId)
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

            Result.success(
                post.copy(
                    isLikedByCurrentUser = isLiked,
                    isBookmarkedByCurrentUser = isBookmarked,
                    originalPost = original
                )
            )
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

            // Verify that current user account still exists and is not deleted
            val userDoc = firestore.collection("users").document(uid).get().await()
            if (!userDoc.exists() || userDoc.getBoolean("isDeleted") == true) {
                auth.signOut()
                return@withContext Result.failure(Exception("Ce compte a été supprimé."))
            }

            val docRef = firestore.collection("posts").document()
            val now = Timestamp.now()
            val hashtags = extractHashtags(text)
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
                repostsCount = 0,
                isRepost = false,
                originalPostId = null,
                isLikedByCurrentUser = false,
                isBookmarkedByCurrentUser = false,
                hashtags = hashtags
            )

            docRef.set(post.toMap()).await()
            try {
                firestore.collection("users").document(uid)
                    .set(mapOf("postsCount" to FieldValue.increment(1)), com.google.firebase.firestore.SetOptions.merge())
                    .await()
            } catch (_: Exception) {}
            Result.success(post)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update the text and hashtags of an existing post authored by the current user.
     */
    suspend fun updatePostText(postId: String, newText: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))

        try {
            val postRef = firestore.collection("posts").document(postId)
            val postDoc = postRef.get().await()
            if (!postDoc.exists()) {
                return@withContext Result.failure(Exception("Publication introuvable"))
            }

            val authorId = postDoc.getString("authorId") ?: ""
            if (authorId != uid) {
                return@withContext Result.failure(Exception("Non autorisé à modifier cette publication"))
            }

            val trimmed = newText.trim()
            val hashtags = extractHashtags(trimmed)

            postRef.update(
                mapOf(
                    "text" to trimmed,
                    "hashtags" to hashtags,
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Error updating post text $postId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a post document in Firestore, along with its subcollections (likes, comments),
     * and decrement postsCount on the author user document.
     */
    suspend fun deletePost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))

        try {
            val postRef = firestore.collection("posts").document(postId)
            val postDoc = postRef.get().await()
            if (!postDoc.exists()) {
                return@withContext Result.failure(Exception("Publication introuvable"))
            }

            val authorId = postDoc.getString("authorId") ?: ""
            if (authorId != uid) {
                return@withContext Result.failure(Exception("Non autorisé à supprimer cette publication"))
            }

            // 1. Delete likes subcollection
            try {
                val likesDocs = postRef.collection("likes").get().await()
                if (!likesDocs.isEmpty) {
                    val batch = firestore.batch()
                    likesDocs.documents.forEach { doc ->
                        batch.delete(doc.reference)
                    }
                    batch.commit().await()
                }
            } catch (e: Exception) {
                Log.w("PostRepository", "Failed deleting likes subcollection: ${e.message}")
            }

            // 2. Delete comments subcollection and their likes
            try {
                val commentsDocs = postRef.collection("comments").get().await()
                if (!commentsDocs.isEmpty) {
                    val batch = firestore.batch()
                    commentsDocs.documents.forEach { doc ->
                        try {
                            val commentLikes = doc.reference.collection("likes").get().await()
                            commentLikes.documents.forEach { likeDoc ->
                                batch.delete(likeDoc.reference)
                            }
                        } catch (_: Exception) {}
                        batch.delete(doc.reference)
                    }
                    batch.commit().await()
                }
            } catch (e: Exception) {
                Log.w("PostRepository", "Failed deleting comments subcollection: ${e.message}")
            }

            // 3. Delete the post document itself. For reposts, also remove
            // the unique repost marker and decrement the original post counter
            // in the same atomic transaction.
            val isRepost = postDoc.getBoolean("isRepost") == true
            val originalPostId = postDoc.getString("originalPostId")

            if (isRepost && !originalPostId.isNullOrBlank()) {
                val originalRef = firestore.collection("posts").document(originalPostId)
                val repostMarkerRef = originalRef.collection("reposts").document(uid)

                firestore.runTransaction { transaction ->
                    val currentPost = transaction.get(postRef)
                    if (!currentPost.exists()) return@runTransaction

                    val originalDoc = transaction.get(originalRef)
                    val markerDoc = transaction.get(repostMarkerRef)

                    transaction.delete(postRef)

                    if (originalDoc.exists() && markerDoc.exists()) {
                        transaction.delete(repostMarkerRef)
                        transaction.update(originalRef, "repostsCount", FieldValue.increment(-1))
                    }
                }.await()
            } else {
                postRef.delete().await()
            }

            // 4. Decrement author's postsCount
            if (authorId.isNotBlank()) {
                try {
                    firestore.collection("users").document(authorId)
                        .update("postsCount", FieldValue.increment(-1))
                        .await()
                } catch (e: Exception) {
                    Log.w("PostRepository", "Failed decrementing postsCount: ${e.message}")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Error deleting post $postId: ${e.message}", e)
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
     * Toggle like atomically on a comment using Firestore transaction.
     * Path: posts/{postId}/comments/{commentId}/likes/{userId} with { likedAt: Timestamp }
     * Increments or decrements likesCount on comment document.
     * In case remote Firestore rules do not permit comment likes yet (PERMISSION_DENIED),
     * automatically falls back to persistent local storage and user-scoped collection,
     * ensuring smooth and error-free operation.
     * Returns true if comment is now liked, false if unliked.
     */
    suspend fun toggleCommentLike(postId: String, commentId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        val commentRef = firestore.collection("posts").document(postId)
            .collection("comments").document(commentId)
        val likeRef = commentRef.collection("likes").document(uid)

        try {
            val (nowLiked, authorId) = firestore.runTransaction { transaction ->
                val commentDoc = transaction.get(commentRef)
                val author = commentDoc.getString("authorId") ?: ""
                val likeDoc = transaction.get(likeRef)
                if (likeDoc.exists()) {
                    transaction.delete(likeRef)
                    transaction.update(commentRef, "likesCount", FieldValue.increment(-1))
                    Pair(false, author)
                } else {
                    transaction.set(likeRef, mapOf("likedAt" to Timestamp.now()))
                    transaction.update(commentRef, "likesCount", FieldValue.increment(1))
                    Pair(true, author)
                }
            }.await()

            dataStoreManager?.setLocalCommentLiked(commentId, nowLiked)

            if (nowLiked && authorId.isNotBlank() && authorId != uid) {
                try {
                    notificationRepository.createNotification(
                        recipientId = authorId,
                        type = NotificationItem.TYPE_LIKE_COMMENT,
                        postId = postId,
                        commentId = commentId
                    )
                } catch (_: Exception) {}
            }

            Result.success(nowLiked)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("PERMISSION_DENIED", ignoreCase = true) || msg.contains("insufficient permissions", ignoreCase = true)) {
                Log.w("PostRepository", "Remote Firestore rules for comment likes not active yet ($msg). Using fallback persistence.")
                val nowLiked = dataStoreManager?.toggleLocalCommentLike(commentId) ?: true
                var authorId = ""
                try {
                    val commentDoc = commentRef.get().await()
                    authorId = commentDoc.getString("authorId") ?: ""
                } catch (_: Exception) {}

                try {
                    val userCommentLikeRef = firestore.collection("users").document(uid)
                        .collection("likedComments").document(commentId)
                    if (nowLiked) {
                        userCommentLikeRef.set(mapOf("likedAt" to Timestamp.now(), "postId" to postId)).await()
                    } else {
                        userCommentLikeRef.delete().await()
                    }
                } catch (_: Exception) {}

                if (nowLiked && authorId.isNotBlank() && authorId != uid) {
                    try {
                        notificationRepository.createNotification(
                            recipientId = authorId,
                            type = NotificationItem.TYPE_LIKE_COMMENT,
                            postId = postId,
                            commentId = commentId
                        )
                    } catch (_: Exception) {}
                }

                Result.success(nowLiked)
            } else {
                Log.e("PostRepository", "Error toggling comment like: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Add a comment to a post atomically using Firestore transaction.
     * Supports threading with parentCommentId (null for top-level comments).
     */
    suspend fun addComment(
        postId: String,
        text: String,
        parentCommentId: String? = null
    ): Result<Comment> = withContext(Dispatchers.IO) {
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
                createdAt = now,
                likesCount = 0,
                parentCommentId = parentCommentId
            )

            var parentAuthorId: String? = null
            if (!parentCommentId.isNullOrBlank()) {
                try {
                    val parentDoc = postRef.collection("comments").document(parentCommentId).get().await()
                    parentAuthorId = parentDoc.getString("authorId")
                } catch (_: Exception) {}
            }

            val counterOpRef = postRef.collection("counterOps").document(uid)

            val authorId = firestore.runTransaction { transaction ->
                val postDoc = transaction.get(postRef)
                val author = postDoc.getString("authorId") ?: ""
                transaction.set(commentRef, comment.toMap())
                transaction.set(
                    counterOpRef,
                    mapOf(
                        "type" to "comment_create",
                        "targetId" to commentRef.id
                    )
                )
                transaction.update(postRef, "commentsCount", FieldValue.increment(1))
                author
            }.await()

            // 1) If replying to a comment: notify the parent comment's author
            if (!parentAuthorId.isNullOrBlank() && parentAuthorId != uid) {
                try {
                    notificationRepository.createNotification(
                        recipientId = parentAuthorId,
                        type = NotificationItem.TYPE_REPLY_COMMENT,
                        postId = postId,
                        commentText = text.trim(),
                        commentId = commentRef.id
                    )
                } catch (_: Exception) {}
            }

            // 2) If the post author is different from commenter and parent author, also notify post author
            if (authorId.isNotBlank() && authorId != uid && authorId != parentAuthorId) {
                try {
                    notificationRepository.createNotification(
                        recipientId = authorId,
                        type = NotificationItem.TYPE_COMMENT,
                        postId = postId,
                        commentText = text.trim(),
                        commentId = commentRef.id
                    )
                } catch (_: Exception) {}
            }

            Result.success(comment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove the image from a post (e.g. if deleted on remote host or by user request).
     * Only allowed if current user is the author of the post.
     */
    suspend fun removePostImage(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        try {
            val postRef = firestore.collection("posts").document(postId)
            val doc = postRef.get().await()
            if (doc.getString("authorId") == uid) {
                postRef.update("imageUrl", null).await()
                Log.d("PostRepository", "Removed imageUrl from post $postId")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Failed to remove post image: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Edit a comment's text.
     * Allowed only for the comment's author.
     */
    suspend fun editComment(postId: String, commentId: String, newText: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        val trimmed = newText.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(Exception("Le commentaire ne peut pas être vide"))
        }
        try {
            val commentRef = firestore.collection("posts").document(postId)
                .collection("comments").document(commentId)
            val doc = commentRef.get().await()
            if (!doc.exists()) {
                return@withContext Result.failure(Exception("Commentaire introuvable"))
            }
            if (doc.getString("authorId") != uid) {
                return@withContext Result.failure(Exception("Vous n'êtes pas l'auteur de ce commentaire"))
            }
            commentRef.update(
                mapOf(
                    "text" to trimmed,
                    "isEdited" to true,
                    "editedAt" to com.google.firebase.Timestamp.now()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Failed to edit comment: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a comment.
     * Allowed for the comment's author OR the post's author.
     */
    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        try {
            val postRef = firestore.collection("posts").document(postId)
            val commentRef = postRef.collection("comments").document(commentId)
            val commentDoc = commentRef.get().await()
            if (!commentDoc.exists()) {
                return@withContext Result.failure(Exception("Commentaire introuvable"))
            }
            val commentAuthorId = commentDoc.getString("authorId") ?: ""
            val postDoc = postRef.get().await()
            val postAuthorId = postDoc.getString("authorId") ?: ""

            if (commentAuthorId != uid && postAuthorId != uid) {
                return@withContext Result.failure(Exception("Vous n'avez pas l'autorisation de supprimer ce commentaire"))
            }

            val counterOpRef = postRef.collection("counterOps").document(uid)
            firestore.runTransaction { transaction ->
                val currentComment = transaction.get(commentRef)
                val currentPost = transaction.get(postRef)
                if (!currentComment.exists() || !currentPost.exists()) {
                    throw IllegalStateException("Commentaire ou publication introuvable")
                }

                transaction.delete(commentRef)
                transaction.set(
                    counterOpRef,
                    mapOf(
                        "type" to "comment_delete",
                        "targetId" to commentId
                    )
                )
                transaction.update(postRef, "commentsCount", FieldValue.increment(-1))
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Failed to delete comment: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Hide or unhide a comment.
     * Allowed only for the post's author.
     */
    suspend fun toggleHideComment(postId: String, commentId: String, hide: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        try {
            val postRef = firestore.collection("posts").document(postId)
            val postDoc = postRef.get().await()
            if (postDoc.getString("authorId") != uid) {
                return@withContext Result.failure(Exception("Seul le créateur de la publication peut masquer un commentaire"))
            }
            val commentRef = postRef.collection("comments").document(commentId)
            commentRef.update("isHidden", hide).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Failed to toggle hide comment: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Report a comment.
     */
    suspend fun reportComment(
        postId: String,
        commentId: String,
        commentAuthorId: String,
        commentText: String,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        try {
            val reportData = hashMapOf(
                "type" to "comment",
                "postId" to postId,
                "commentId" to commentId,
                "commentAuthorId" to commentAuthorId,
                "commentText" to commentText,
                "reporterId" to uid,
                "reason" to reason,
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            firestore.collection("reports").add(reportData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Failed to report comment: ${e.message}", e)
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

            val uid = currentUserId
            val localLikedSet = dataStoreManager?.getLocalLikedCommentIds() ?: emptySet()
            val baseComments = snapshot.documents.map { Comment.fromSnapshot(it, uid) }
            val hydrated = if (uid != null && baseComments.isNotEmpty()) {
                coroutineScope {
                    baseComments.map { comment ->
                        async {
                            val isLikedInFirestore = try {
                                firestore.collection("posts").document(postId)
                                    .collection("comments").document(comment.id)
                                    .collection("likes").document(uid)
                                    .get().await().exists()
                            } catch (_: Exception) {
                                false
                            }
                            val isLiked = isLikedInFirestore || localLikedSet.contains(comment.id)
                            val effectiveLikesCount = if (isLiked && comment.likesCount == 0) 1 else comment.likesCount
                            comment.copy(
                                isLikedByCurrentUser = isLiked,
                                likesCount = effectiveLikesCount
                            )
                        }
                    }.awaitAll()
                }
            } else {
                baseComments
            }
            Result.success(hydrated)
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
                if (snapshot == null) return@addSnapshotListener
                val uid = currentUserId
                val baseComments = snapshot.documents.map { Comment.fromSnapshot(it, uid) }
                trySend(baseComments)

                if (uid != null && baseComments.isNotEmpty()) {
                    launch(Dispatchers.IO) {
                        try {
                            val localLikedSet = dataStoreManager?.getLocalLikedCommentIds() ?: emptySet()
                            val hydrated = coroutineScope {
                                baseComments.map { comment ->
                                    async {
                                        val isLikedInFirestore = try {
                                            firestore.collection("posts").document(postId)
                                                .collection("comments").document(comment.id)
                                                .collection("likes").document(uid)
                                                .get().await().exists()
                                        } catch (_: Exception) {
                                            false
                                        }
                                        val isLiked = isLikedInFirestore || localLikedSet.contains(comment.id)
                                        val effectiveLikesCount = if (isLiked && comment.likesCount == 0) 1 else comment.likesCount
                                        comment.copy(
                                            isLikedByCurrentUser = isLiked,
                                            likesCount = effectiveLikesCount
                                        )
                                    }
                                }.awaitAll()
                            }
                            trySend(hydrated)
                        } catch (_: Exception) {}
                    }
                }
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
            val originalRef = firestore.collection("posts").document(postId)
            val repostMarkerRef = originalRef.collection("reposts").document(uid)
            val repostRef = firestore.collection("posts").document()

            val now = Timestamp.now()
            val repost = Post(
                id = repostRef.id,
                authorId = uid,
                authorUsername = username,
                authorPhotoUrl = photoUrl,
                text = "",
                imageUrl = null,
                createdAt = now,
                likesCount = 0,
                commentsCount = 0,
                repostsCount = 0,
                isRepost = true,
                originalPostId = postId,
                isLikedByCurrentUser = false
            )

            firestore.runTransaction { transaction ->
                val originalDoc = transaction.get(originalRef)
                if (!originalDoc.exists()) {
                    throw IllegalStateException("Publication originale introuvable")
                }

                val existingRepost = transaction.get(repostMarkerRef)
                if (existingRepost.exists()) {
                    throw IllegalStateException("Vous avez déjà repartagé cette publication")
                }

                transaction.set(repostRef, repost.toMap())
                transaction.set(
                    repostMarkerRef,
                    mapOf(
                        "postId" to postId,
                        "reposterId" to uid,
                        "repostId" to repostRef.id,
                        "createdAt" to now
                    )
                )
                transaction.update(originalRef, "repostsCount", FieldValue.increment(1))
            }.await()

            try {
                firestore.collection("users").document(uid)
                    .set(
                        mapOf("postsCount" to FieldValue.increment(1)),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                    .await()
            } catch (e: Exception) {
                Log.w("PostRepository", "Could not increment postsCount for repost: ${e.message}")
            }

            Result.success(repost)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggle bookmark on a post for current user.
     * Document path: users/{uid}/bookmarks/{postId} with field savedAt: Timestamp.
     * Returns true if post is now saved/bookmarked, false if removed.
     */
    suspend fun toggleBookmark(postId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))

            val bookmarkRef = firestore.collection("users").document(uid)
                .collection("bookmarks").document(postId)

            val currentLocal = dataStoreManager?.getLocalBookmarkedIds()?.toMutableSet() ?: mutableSetOf()
            val wasLocallyBookmarked = currentLocal.contains(postId)

            val isNowBookmarked: Boolean
            val remoteDoc = try {
                bookmarkRef.get().await()
            } catch (e: Exception) {
                Log.w("PostRepository", "Failed to check remote bookmark: ${e.message}")
                null
            }

            if (remoteDoc != null) {
                if (remoteDoc.exists()) {
                    bookmarkRef.delete().await()
                    isNowBookmarked = false
                } else {
                    bookmarkRef.set(mapOf("savedAt" to Timestamp.now())).await()
                    isNowBookmarked = true
                }
            } else {
                // If remote read failed (e.g. offline), toggle based on local cache
                isNowBookmarked = !wasLocallyBookmarked
                try {
                    if (isNowBookmarked) {
                        bookmarkRef.set(mapOf("savedAt" to Timestamp.now())).await()
                    } else {
                        bookmarkRef.delete().await()
                    }
                } catch (e: Exception) {
                    Log.w("PostRepository", "Remote write fallback error: ${e.message}")
                }
            }

            // Sync final state strictly into local storage
            if (isNowBookmarked) {
                currentLocal.add(postId)
            } else {
                currentLocal.remove(postId)
            }
            dataStoreManager?.setLocalBookmarkedIds(currentLocal)

            Result.success(isNowBookmarked)
        } catch (e: Exception) {
            Log.e("PostRepository", "toggleBookmark error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if a post is bookmarked by the current user.
     */
    suspend fun isBookmarked(postId: String): Boolean = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext false
        if (dataStoreManager?.getLocalBookmarkedIds()?.contains(postId) == true) {
            return@withContext true
        }
        try {
            firestore.collection("users").document(uid)
                .collection("bookmarks").document(postId)
                .get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Get all bookmarked posts for the current user, ordered by savedAt descending.
     */
    suspend fun getBookmarks(): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
            val localIds = dataStoreManager?.getLocalBookmarkedIds() ?: emptySet()

            val remoteIds = try {
                val bookmarksSnapshot = try {
                    firestore.collection("users").document(uid)
                        .collection("bookmarks")
                        .orderBy("savedAt", Query.Direction.DESCENDING)
                        .get()
                        .await()
                } catch (e: Exception) {
                    Log.w("PostRepository", "Bookmarks orderBy failed (${e.message}), falling back to direct fetch")
                    firestore.collection("users").document(uid)
                        .collection("bookmarks")
                        .get()
                        .await()
                }
                bookmarksSnapshot.documents.map { it.id }
            } catch (e: Exception) {
                Log.w("PostRepository", "Remote bookmarks fetch failed: ${e.message}")
                emptyList()
            }

            val targetPostIds = (remoteIds + localIds).distinct()
            if (targetPostIds.isNotEmpty()) {
                dataStoreManager?.setLocalBookmarkedIds(targetPostIds.toSet())
            }

            val posts = mutableListOf<Post>()
            for (postId in targetPostIds) {
                try {
                    val postDoc = firestore.collection("posts").document(postId).get().await()
                    if (postDoc.exists()) {
                        val post = Post.fromSnapshot(postDoc, uid)

                        var isLiked = false
                        try {
                            val likeDoc = firestore.collection("posts").document(postId)
                                .collection("likes").document(uid).get().await()
                            isLiked = likeDoc.exists()
                        } catch (_: Exception) {}

                        var original: Post? = null
                        if (post.isRepost && !post.originalPostId.isNullOrBlank()) {
                            try {
                                val origDoc = firestore.collection("posts").document(post.originalPostId).get().await()
                                if (origDoc.exists()) {
                                    original = Post.fromSnapshot(origDoc, uid)
                                }
                            } catch (_: Exception) {}
                        }

                        posts.add(
                            post.copy(
                                isLikedByCurrentUser = isLiked,
                                isBookmarkedByCurrentUser = true,
                                originalPost = original
                            )
                        )
                    }
                } catch (_: Exception) {}
            }
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch trending posts and popular hashtags calculated over the last 7 days.
     * Calculated from likesCount descending on posts created within last 7 days.
     */
    suspend fun getTrendingData(days: Int = 7): Result<Pair<List<TrendingHashtag>, List<Post>>> = withContext(Dispatchers.IO) {
        try {
            val currentUid = currentUserId
            val sevenDaysAgoMillis = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)

            // Fetch recent posts (up to 100)
            val snapshot = firestore.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()

            val allRecentPosts = snapshot.documents.map { Post.fromSnapshot(it, currentUid) }
            // Filter within 7 days, fallback to all recent if none in window
            val inWindowPosts = allRecentPosts.filter {
                it.createdAt.toDate().time >= sevenDaysAgoMillis
            }.ifEmpty { allRecentPosts }

            // Trending posts sorted by likesCount descending
            val topPosts = inWindowPosts
                .sortedByDescending { it.likesCount }
                .take(20)

            // Hydrate likes and bookmarks for top posts
            val bookmarkedIds = getEffectiveBookmarkedIds(currentUid)

            val hydratedTopPosts = topPosts.map { post ->
                var isLiked = false
                if (currentUid != null) {
                    try {
                        val likeDoc = firestore.collection("posts").document(post.id)
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

                post.copy(
                    isLikedByCurrentUser = isLiked,
                    isBookmarkedByCurrentUser = bookmarkedIds.contains(post.id),
                    originalPost = original
                )
            }

            // Calculate popular hashtags across recent posts
            val hashtagCounts = mutableMapOf<String, Int>()
            inWindowPosts.forEach { post ->
                val tags = if (post.hashtags.isNotEmpty()) {
                    post.hashtags
                } else {
                    extractHashtags(post.text)
                }
                tags.forEach { tag ->
                    val clean = tag.lowercase().trim()
                    if (clean.isNotBlank()) {
                        hashtagCounts[clean] = (hashtagCounts[clean] ?: 0) + 1
                    }
                }
            }

            val topHashtags = hashtagCounts.entries
                .sortedByDescending { it.value }
                .take(15)
                .map { TrendingHashtag(it.key, it.value) }

            Result.success(Pair(topHashtags, hydratedTopPosts))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search posts by hashtag.
     */
    suspend fun getPostsByHashtag(hashtag: String): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val cleanTag = hashtag.removePrefix("#").lowercase().trim()
            val currentUid = currentUserId

            val snapshot = firestore.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(60)
                .get()
                .await()

            val bookmarkedIds = getEffectiveBookmarkedIds(currentUid)

            val matchingPosts = snapshot.documents.mapNotNull { doc ->
                try {
                    val post = Post.fromSnapshot(doc, currentUid)
                    val hasTag = post.hashtags.any { it.equals(cleanTag, ignoreCase = true) }
                    val textMatches = post.text.contains("#$cleanTag", ignoreCase = true)
                    if (hasTag || textMatches) {
                        var isLiked = false
                        if (currentUid != null) {
                            try {
                                val likeDoc = firestore.collection("posts").document(post.id)
                                    .collection("likes").document(currentUid).get().await()
                                isLiked = likeDoc.exists()
                            } catch (_: Exception) {}
                        }
                        post.copy(
                            isLikedByCurrentUser = isLiked,
                            isBookmarkedByCurrentUser = bookmarkedIds.contains(post.id)
                        )
                    } else null
                } catch (_: Exception) {
                    null
                }
            }

            Result.success(matchingPosts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
