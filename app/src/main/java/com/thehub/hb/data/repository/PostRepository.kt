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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
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
            try {
                firestore.collection("users").document(uid)
                    .set(mapOf("postsCount" to FieldValue.increment(1)), com.google.firebase.firestore.SetOptions.merge())
                    .await()
            } catch (_: Exception) {}
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
