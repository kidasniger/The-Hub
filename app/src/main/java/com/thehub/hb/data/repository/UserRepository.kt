package com.thehub.hb.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.thehub.hb.data.local.DataStoreManager
import com.thehub.hb.data.model.BlockedUser
import com.thehub.hb.data.model.NotificationItem
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.model.Report
import com.thehub.hb.data.model.User
import com.thehub.hb.data.remote.ImgbbService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val dataStoreManager: DataStoreManager,
    private val imgbbService: ImgbbService = ImgbbService(),
    private val notificationRepository: NotificationRepository = NotificationRepository(firestore, auth)
) {
    val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Real-time listener for a user profile.
     */
    fun observeUserProfile(uid: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                try {
                    val user = User.fromMap(snapshot.data ?: emptyMap())
                    trySend(user)
                } catch (e: Exception) {
                    Log.e("UserRepository", "Error parsing user $uid: ${e.message}")
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    /**
     * Get user profile once.
     */
    suspend fun getUserProfile(uid: String): Result<User?> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                val user = User.fromMap(doc.data ?: emptyMap())
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error fetching user profile $uid: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if username is taken by another user.
     */
    suspend fun checkUsernameUnique(username: String, currentUid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val lower = username.trim().lowercase()
            val query = firestore.collection("users")
                .whereEqualTo("usernameLower", lower)
                .limit(2)
                .get()
                .await()

            if (query.isEmpty) return@withContext true
            val docs = query.documents
            // If the only document with this username is the current user, it is valid/unique
            return@withContext docs.all { it.id == currentUid }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error checking username uniqueness: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Upload an image to ImgBB for profile photo.
     */
    suspend fun uploadProfilePhoto(imageBytes: ByteArray): Result<String> {
        return imgbbService.uploadImage(imageBytes)
    }

    /**
     * Update user profile fields.
     */
    suspend fun updateProfile(
        displayName: String,
        username: String,
        bio: String?,
        birthdate: String?,
        photoUrl: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        val cleanUsername = username.trim()

        if (cleanUsername.length < 3) {
            return@withContext Result.failure(Exception("Le nom d'utilisateur doit contenir au moins 3 caractères."))
        }

        // Check uniqueness
        val isUnique = checkUsernameUnique(cleanUsername, uid)
        if (!isUnique) {
            return@withContext Result.failure(Exception("Ce nom d'utilisateur est déjà pris."))
        }

        try {
            val updates = mutableMapOf<String, Any?>(
                "displayName" to displayName.trim(),
                "username" to cleanUsername,
                "usernameLower" to cleanUsername.lowercase(),
                "bio" to bio?.trim(),
                "birthdate" to birthdate?.trim()
            )
            if (photoUrl != null) {
                updates["photoUrl"] = photoUrl
            }

            firestore.collection("users").document(uid)
                .set(updates, SetOptions.merge())
                .await()

            // Update Firebase Auth profile
            val authUser = auth.currentUser
            if (authUser != null) {
                val profileUpdates = userProfileChangeRequest {
                    this.displayName = displayName.trim()
                    if (photoUrl != null) {
                        this.photoUri = Uri.parse(photoUrl)
                    }
                }
                authUser.updateProfile(profileUpdates).await()
            }

            // Update local DataStore
            dataStoreManager.saveLastUser(
                email = auth.currentUser?.email ?: "",
                username = cleanUsername,
                name = displayName.trim(),
                photoUrl = photoUrl
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if current user is following target user in real-time.
     */
    fun isFollowing(targetUid: String): Flow<Boolean> = callbackFlow {
        val uid = currentUserId
        if (uid == null || uid == targetUid) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users").document(uid)
            .collection("following").document(targetUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(false)
                    return@addSnapshotListener
                }
                trySend(snapshot?.exists() == true)
            }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    /**
     * Check if current user is following target user once.
     */
    suspend fun checkIsFollowing(targetUid: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.success(false)
        if (uid == targetUid) return@withContext Result.success(false)
        try {
            val doc = firestore.collection("users").document(uid)
                .collection("following").document(targetUid)
                .get()
                .await()
            Result.success(doc.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Follow a user atomically using Firestore transaction.
     * Updates followers/following subcollections and denormalized counters.
     */
    suspend fun followUser(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        if (uid == targetUid) return@withContext Result.failure(Exception("Impossible de se suivre soi-même"))

        try {
            val currentUserRef = firestore.collection("users").document(uid)
            val targetUserRef = firestore.collection("users").document(targetUid)
            val followingRef = currentUserRef.collection("following").document(targetUid)
            val followerRef = targetUserRef.collection("followers").document(uid)

            firestore.runTransaction { transaction ->
                val followingDoc = transaction.get(followingRef)
                if (!followingDoc.exists()) {
                    val now = Timestamp.now()
                    transaction.set(followingRef, mapOf("followedAt" to now, "uid" to targetUid))
                    transaction.set(followerRef, mapOf("followedAt" to now, "uid" to uid))
                    transaction.update(currentUserRef, "followingCount", FieldValue.increment(1))
                    transaction.update(targetUserRef, "followersCount", FieldValue.increment(1))
                }
                null
            }.await()

            // Send notification to target user
            try {
                notificationRepository.createNotification(
                    recipientId = targetUid,
                    type = NotificationItem.TYPE_FOLLOW
                )
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error following user $targetUid: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Unfollow a user atomically using Firestore transaction.
     */
    suspend fun unfollowUser(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        if (uid == targetUid) return@withContext Result.failure(Exception("Action non permise"))

        try {
            val currentUserRef = firestore.collection("users").document(uid)
            val targetUserRef = firestore.collection("users").document(targetUid)
            val followingRef = currentUserRef.collection("following").document(targetUid)
            val followerRef = targetUserRef.collection("followers").document(uid)

            firestore.runTransaction { transaction ->
                val followingDoc = transaction.get(followingRef)
                if (followingDoc.exists()) {
                    transaction.delete(followingRef)
                    transaction.delete(followerRef)
                    transaction.update(currentUserRef, "followingCount", FieldValue.increment(-1))
                    transaction.update(targetUserRef, "followersCount", FieldValue.increment(-1))
                }
                null
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error unfollowing user $targetUid: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get list of followers for a user.
     */
    suspend fun getFollowers(uid: String): Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("users").document(uid)
                .collection("followers")
                .orderBy("followedAt", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()

            val users = mutableListOf<User>()
            for (doc in snapshot.documents) {
                val followerId = doc.id
                val userDoc = firestore.collection("users").document(followerId).get().await()
                if (userDoc.exists()) {
                    users.add(User.fromMap(userDoc.data ?: emptyMap()))
                }
            }
            Result.success(users)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting followers for $uid: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get list of users followed by a user.
     */
    suspend fun getFollowing(uid: String): Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("users").document(uid)
                .collection("following")
                .orderBy("followedAt", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()

            val users = mutableListOf<User>()
            for (doc in snapshot.documents) {
                val followingId = doc.id
                val userDoc = firestore.collection("users").document(followingId).get().await()
                if (userDoc.exists()) {
                    users.add(User.fromMap(userDoc.data ?: emptyMap()))
                }
            }
            Result.success(users)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting following for $uid: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Block a user.
     */
    suspend fun blockUser(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        if (uid == targetUid) return@withContext Result.failure(Exception("Impossible de se bloquer soi-même"))

        try {
            // Write to users/{uid}/blockedUsers/{targetUid}
            firestore.collection("users").document(uid)
                .collection("blockedUsers").document(targetUid)
                .set(mapOf("blockedAt" to Timestamp.now()))
                .await()

            // Also unfollow if currently following
            try {
                unfollowUser(targetUid)
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error blocking user $targetUid: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Unblock a user.
     */
    suspend fun unblockUser(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))

        try {
            firestore.collection("users").document(uid)
                .collection("blockedUsers").document(targetUid)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error unblocking user $targetUid: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get list of blocked users for the current user.
     */
    suspend fun getBlockedUsers(): Result<List<BlockedUser>> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.success(emptyList())

        try {
            val snapshot = firestore.collection("users").document(uid)
                .collection("blockedUsers")
                .orderBy("blockedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val blockedList = mutableListOf<BlockedUser>()
            for (doc in snapshot.documents) {
                val blockedUid = doc.id
                val blockedAt = doc.getTimestamp("blockedAt") ?: Timestamp.now()
                var username = "thehub_user"
                var displayName: String? = null
                var photoUrl: String? = null

                try {
                    val userDoc = firestore.collection("users").document(blockedUid).get().await()
                    if (userDoc.exists()) {
                        username = userDoc.getString("username") ?: username
                        displayName = userDoc.getString("displayName")
                        photoUrl = userDoc.getString("photoUrl")
                    }
                } catch (_: Exception) {}

                blockedList.add(
                    BlockedUser(
                        uid = blockedUid,
                        username = username,
                        displayName = displayName,
                        photoUrl = photoUrl,
                        blockedAt = blockedAt
                    )
                )
            }

            Result.success(blockedList)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error fetching blocked users: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get IDs of users blocked by current user.
     */
    suspend fun getBlockedUserIds(): Set<String> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext emptySet()
        try {
            val snapshot = firestore.collection("users").document(uid)
                .collection("blockedUsers")
                .get()
                .await()
            snapshot.documents.map { it.id }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * Check two-way blocking between current user and target user.
     * Returns true if either user blocked the other.
     */
    suspend fun isBlocked(targetUid: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.success(false)
        if (uid == targetUid) return@withContext Result.success(false)

        try {
            // Check if current user blocked target
            val blockedByMe = firestore.collection("users").document(uid)
                .collection("blockedUsers").document(targetUid)
                .get()
                .await()
                .exists()

            if (blockedByMe) return@withContext Result.success(true)

            // Check if target user blocked current user
            val blockedByTarget = try {
                firestore.collection("users").document(targetUid)
                    .collection("blockedUsers").document(uid)
                    .get()
                    .await()
                    .exists()
            } catch (_: Exception) {
                false
            }

            Result.success(blockedByTarget)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check specifically if current user blocked target.
     */
    suspend fun isBlockedByMe(targetUid: String): Boolean = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext false
        try {
            firestore.collection("users").document(uid)
                .collection("blockedUsers").document(targetUid)
                .get()
                .await()
                .exists()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Report content (user or post).
     */
    suspend fun reportContent(
        targetType: String,
        targetId: String,
        reason: String,
        details: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))

        try {
            val reportDoc = firestore.collection("reports").document()
            val report = Report(
                id = reportDoc.id,
                reporterId = uid,
                targetType = targetType,
                targetId = targetId,
                reason = reason,
                details = details?.trim()?.takeIf { it.isNotBlank() },
                createdAt = Timestamp.now(),
                status = "pending"
            )

            reportDoc.set(report.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error reporting content: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch posts by a specific user.
     */
    suspend fun getUserPosts(userId: String): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("posts")
                .whereEqualTo("authorId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(60)
                .get()
                .await()

            val currentUid = currentUserId
            val posts = snapshot.documents.map { doc ->
                Post.fromSnapshot(doc, currentUid)
            }
            Result.success(posts)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error fetching user posts for $userId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a post authored by the current user.
     */
    suspend fun deletePost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))

        try {
            val postRef = firestore.collection("posts").document(postId)
            val postDoc = postRef.get().await()
            if (!postDoc.exists()) {
                return@withContext Result.failure(Exception("Publication introuvable"))
            }

            val authorId = postDoc.getString("authorId")
            if (authorId != uid) {
                return@withContext Result.failure(Exception("Non autorisé à supprimer cette publication"))
            }

            postRef.delete().await()

            // Decrement postsCount on user doc
            try {
                firestore.collection("users").document(uid)
                    .update("postsCount", FieldValue.increment(-1))
                    .await()
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error deleting post $postId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete user account completely.
     * Deletes Firestore users/{uid} document and then deletes the Firebase Auth user.
     */
    suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        val authUser = auth.currentUser ?: return@withContext Result.failure(Exception("Utilisateur introuvable"))

        try {
            // Delete user document in Firestore
            try {
                firestore.collection("users").document(uid).delete().await()
            } catch (e: Exception) {
                Log.w("UserRepository", "Failed to delete user document: ${e.message}")
            }

            // Clear local DataStore
            dataStoreManager.clearAll()

            // Delete Firebase Auth account
            authUser.delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error deleting account: ${e.message}", e)
            Result.failure(e)
        }
    }
}
