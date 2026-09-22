package com.thehub.hb.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.thehub.hb.data.local.DataStoreManager
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private data class DiscoverySources(
    val followingIds: Set<String>,
    val blockedIds: Set<String>,
    val popularSnapshot: com.google.firebase.firestore.QuerySnapshot,
    val recentSnapshot: com.google.firebase.firestore.QuerySnapshot
)

internal fun rankSuggestedUsers(
    candidates: List<User>,
    excludedIds: Set<String>,
    currentUserId: String?,
    limit: Int
): List<User> {
    if (limit <= 0) return emptyList()

    val comparator = compareByDescending<User> { it.followersCount }
        .thenByDescending { it.postsCount }
        .thenByDescending { it.createdAt }
        .thenBy { it.usernameLower }

    return candidates.asSequence()
        .filter { it.uid.isNotBlank() }
        .filter { it.uid != currentUserId }
        .filter { it.uid !in excludedIds }
        .groupBy { it.uid }
        .values
        .asSequence()
        .mapNotNull { group -> group.minWithOrNull(comparator) }
        .sortedWith(comparator)
        .take(limit)
        .toList()
}

class SearchRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val dataStoreManager: DataStoreManager
) {
    val currentUserId: String?
        get() = auth.currentUser?.uid

    val searchHistory: Flow<List<String>> = dataStoreManager.searchHistory

    suspend fun addSearchQuery(query: String) {
        dataStoreManager.addSearchQuery(query)
    }

    suspend fun removeSearchQuery(query: String) {
        dataStoreManager.removeSearchQuery(query)
    }

    suspend fun clearSearchHistory() {
        dataStoreManager.clearSearchHistory()
    }

    /**
     * Build a lightweight discovery list from existing public profiles.
     * Excludes the current user, followed users and blocked users.
     */
    suspend fun getSuggestedUsers(limit: Int = 8): Result<List<User>> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.success(emptyList())
        val safeLimit = limit.coerceIn(1, 20)

        try {
            val followingDeferred = async {
                firestore.collection("users").document(uid)
                    .collection("following").get().await()
                    .documents.map { it.id }.filter { it.isNotBlank() }.toSet()
            }
            val blockedDeferred = async {
                firestore.collection("users").document(uid)
                    .collection("blockedUsers").get().await()
                    .documents.map { it.id }.filter { it.isNotBlank() }.toSet()
            }
            val popularDeferred = async {
                firestore.collection("users")
                    .orderBy("followersCount", Query.Direction.DESCENDING)
                    .limit(80)
                    .get().await()
            }
            val recentDeferred = async {
                firestore.collection("users")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(80)
                    .get().await()
            }

            val followingIds = followingDeferred.await()
            val blockedIds = blockedDeferred.await()
            val popularSnapshot = popularDeferred.await()
            val recentSnapshot = recentDeferred.await()

            val candidates = (popularSnapshot.documents + recentSnapshot.documents).mapNotNull { document ->
                try {
                    if (document.getBoolean("isDeleted") == true) return@mapNotNull null
                    User.fromMap(document.data ?: emptyMap()).takeIf { it.uid.isNotBlank() }
                } catch (e: Exception) {
                    Log.w("SearchRepository", "Error parsing discovery user " + document.id + ": " + e.message)
                    null
                }
            }

            val excludedIds = followingIds + blockedIds
            Result.success(
                rankSuggestedUsers(
                    candidates = candidates,
                    excludedIds = excludedIds,
                    currentUserId = uid,
                    limit = safeLimit
                )
            )
        } catch (e: Exception) {
            Log.e("SearchRepository", "Error loading suggested users: " + e.message, e)
            Result.failure(e)
        }
    }

    /**
     * Search users by prefix query on usernameLower, with fallback to client-side matching.
     */
    suspend fun searchUsers(query: String): Result<List<User>> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        if (q.isBlank()) return@withContext Result.success(emptyList())

        try {
            val endQ = q + "\uf8ff"
            val prefixSnapshot = firestore.collection("users")
                .orderBy("usernameLower")
                .startAt(q)
                .endAt(endQ)
                .limit(20)
                .get()
                .await()

            val results = mutableListOf<User>()
            for (doc in prefixSnapshot.documents) {
                try {
                    val map = doc.data ?: continue
                    results.add(User.fromMap(map))
                } catch (e: Exception) {
                    Log.w("SearchRepository", "Error parsing user doc: ${e.message}")
                }
            }

            // If prefix matching returned few results, perform secondary scan on recent users
            if (results.size < 5) {
                try {
                    val fallbackSnapshot = firestore.collection("users")
                        .limit(50)
                        .get()
                        .await()

                    for (doc in fallbackSnapshot.documents) {
                        val map = doc.data ?: continue
                        val user = User.fromMap(map)
                        val matchesUsername = user.username.contains(q, ignoreCase = true)
                        val matchesDisplay = user.displayName?.contains(q, ignoreCase = true) == true
                        if ((matchesUsername || matchesDisplay) && results.none { it.uid == user.uid }) {
                            results.add(user)
                        }
                    }
                } catch (e: Exception) {
                    Log.w("SearchRepository", "Fallback scan failed: ${e.message}")
                }
            }

            // Exclude current user from results if desired or put at bottom
            val currentUid = currentUserId
            val sorted = results.sortedBy { it.uid == currentUid }
            Result.success(sorted)
        } catch (e: Exception) {
            Log.e("SearchRepository", "Error searching users: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Search posts by text query or author username across recent posts.
     */
    suspend fun searchPosts(query: String): Result<List<Post>> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext Result.success(emptyList())

        try {
            val snapshot = firestore.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(60)
                .get()
                .await()

            val currentUid = currentUserId
            val rawTag = q.removePrefix("#").lowercase().trim()

            // Fetch user's bookmarks to hydrate bookmark status (remote + local)
            val localBookmarks = dataStoreManager.getLocalBookmarkedIds()
            val remoteBookmarks = if (currentUid != null) {
                try {
                    firestore.collection("users").document(currentUid)
                        .collection("bookmarks").get().await()
                        .documents.map { it.id }.toSet()
                } catch (_: Exception) {
                    emptySet()
                }
            } else emptySet()
            val bookmarkedIds = remoteBookmarks + localBookmarks

            val matchingPosts = snapshot.documents.mapNotNull { doc ->
                try {
                    val post = Post.fromSnapshot(doc, currentUid)
                    val matchText = post.text.contains(q, ignoreCase = true)
                    val matchAuthor = post.authorUsername.contains(q, ignoreCase = true)
                    val matchHashtag = post.hashtags.any { it.equals(rawTag, ignoreCase = true) }
                    if (matchText || matchAuthor || matchHashtag) post else null
                } catch (_: Exception) {
                    null
                }
            }

            // Hydrate like and bookmark status
            val hydrated = matchingPosts.map { post ->
                var isLiked = false
                if (currentUid != null) {
                    try {
                        val likeDoc = firestore.collection("posts").document(post.id)
                            .collection("likes").document(currentUid)
                            .get().await()
                        isLiked = likeDoc.exists()
                    } catch (_: Exception) {}
                }
                post.copy(
                    isLikedByCurrentUser = isLiked,
                    isBookmarkedByCurrentUser = bookmarkedIds.contains(post.id)
                )
            }

            Result.success(hydrated)
        } catch (e: Exception) {
            Log.e("SearchRepository", "Error searching posts: ${e.message}", e)
            Result.failure(e)
        }
    }
}
