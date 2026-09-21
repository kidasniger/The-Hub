package com.thehub.hb.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.firestore.ListenerRegistration
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

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val dataStoreManager: DataStoreManager,
    private val imgbbService: ImgbbService = ImgbbService(),
    private val notificationRepository: NotificationRepository = NotificationRepository(firestore, auth)
) {
    val currentUserId: String?
        get() = auth.currentUser?.uid

    private suspend fun signOutAndCleanPushToken() {
        val uid = auth.currentUser?.uid
        if (!uid.isNullOrBlank()) {
            try {
                notificationRepository.unregisterFcmTokenForUser(uid)
            } catch (e: Exception) {
                Log.w(
                    "UserRepository",
                    "Failed to unregister FCM token before sign out: " + e.message
                )
            }
        }
        try {
            FirebaseMessaging.getInstance().deleteToken().await()
        } catch (e: Exception) {
            Log.w(
                "UserRepository",
                "Failed to invalidate FCM token during sign out: " + e.message
            )
        }
        auth.signOut()
    }

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
     * Get user profile once, reconciling actual counts from collections.
     */
    suspend fun getUserProfile(uid: String): Result<User?> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (!doc.exists()) {
                return@withContext Result.success(null)
            }
            val baseUser = User.fromMap(doc.data ?: emptyMap())

            // Reconcile actual counts
            val actualPosts = try {
                firestore.collection("posts")
                    .whereEqualTo("authorId", uid)
                    .count()
                    .get(com.google.firebase.firestore.AggregateSource.SERVER)
                    .await().count.toInt()
            } catch (_: Exception) {
                baseUser.postsCount
            }

            val actualFollowers = try {
                firestore.collection("users").document(uid)
                    .collection("followers")
                    .count()
                    .get(com.google.firebase.firestore.AggregateSource.SERVER)
                    .await().count.toInt()
            } catch (_: Exception) {
                baseUser.followersCount
            }

            val actualFollowing = try {
                firestore.collection("users").document(uid)
                    .collection("following")
                    .count()
                    .get(com.google.firebase.firestore.AggregateSource.SERVER)
                    .await().count.toInt()
            } catch (_: Exception) {
                baseUser.followingCount
            }

            val reconciled = baseUser.copy(
                postsCount = actualPosts,
                followersCount = actualFollowers,
                followingCount = actualFollowing
            )

            // Asynchronously sync document if counts differ
            if (reconciled.postsCount != baseUser.postsCount ||
                reconciled.followersCount != baseUser.followersCount ||
                reconciled.followingCount != baseUser.followingCount) {
                try {
                    firestore.collection("users").document(uid).set(
                        mapOf(
                            "postsCount" to reconciled.postsCount,
                            "followersCount" to reconciled.followersCount,
                            "followingCount" to reconciled.followingCount
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                } catch (_: Exception) {}
            }

            Result.success(reconciled)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error fetching user profile $uid: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if username is taken by another user.
     */
    suspend fun checkUsernameUnique(username: String, currentUid: String): Boolean = withContext(Dispatchers.IO) {
        val lower = username.trim().lowercase()
        if (lower.length < 3 || lower.length > 30 || !lower.matches(Regex("^[a-zA-Z0-9._]+$"))) return@withContext false
        try {
            val usernameDoc = firestore.collection("usernames").document(lower).get().await()
            if (usernameDoc.exists()) {
                val docUid = usernameDoc.getString("uid")
                return@withContext (docUid == currentUid)
            }
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
        val newLower = cleanUsername.lowercase()

        if (cleanUsername.length < 3 || cleanUsername.length > 30 || !cleanUsername.matches(Regex("^[a-zA-Z0-9._]+$"))) {
            return@withContext Result.failure(Exception("Le nom d'utilisateur doit contenir entre 3 et 30 caractères (lettres, chiffres, tirets bas ou points)."))
        }

        // Check uniqueness
        val isUnique = checkUsernameUnique(cleanUsername, uid)
        if (!isUnique) {
            return@withContext Result.failure(Exception("Ce nom d'utilisateur est déjà pris."))
        }

        try {
            // Retrieve old username to release old claim if changed
            val currentUserDoc = firestore.collection("users").document(uid).get().await()
            val oldLower = currentUserDoc.getString("usernameLower") ?: currentUserDoc.getString("username")?.lowercase()

            val updates = mutableMapOf<String, Any?>(
                "displayName" to displayName.trim(),
                "username" to cleanUsername,
                "usernameLower" to newLower,
                "bio" to bio?.trim(),
                "birthdate" to birthdate?.trim()
            )
            if (photoUrl != null) {
                updates["photoUrl"] = photoUrl
            }

            // Reserve new username in usernames collection
            try {
                firestore.collection("usernames").document(newLower).set(
                    mapOf(
                        "uid" to uid,
                        "updatedAt" to Timestamp.now()
                    ),
                    SetOptions.merge()
                ).await()

                if (!oldLower.isNullOrBlank() && oldLower != newLower) {
                    firestore.collection("usernames").document(oldLower).delete().await()
                }
            } catch (e: Exception) {
                Log.w("UserRepository", "Could not update usernames collection: ${e.message}")
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

            // Update shared in-memory UserCacheRepository immediately
            com.thehub.hb.data.repository.UserCacheRepository.getInstance().putUser(
                com.thehub.hb.data.model.UserInfo(
                    uid = uid,
                    displayName = displayName.trim(),
                    username = cleanUsername,
                    photoUrl = photoUrl
                )
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
     * Updates followers/following subcollections, denormalized counters,
     * and reciprocal friends subcollections (users/{uid}/friends/{targetUid} and
     * users/{targetUid}/friends/{uid}) when the follow creates a mutual relationship.
     */
    suspend fun followUser(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        if (uid == targetUid) return@withContext Result.failure(Exception("Impossible de se suivre soi-même"))

        try {
            val currentUserRef = firestore.collection("users").document(uid)
            val targetUserRef = firestore.collection("users").document(targetUid)
            val followingRef = currentUserRef.collection("following").document(targetUid)
            val followerRef = targetUserRef.collection("followers").document(uid)
            val targetFollowingRef = targetUserRef.collection("following").document(uid)
            val currentUserFriendRef = currentUserRef.collection("friends").document(targetUid)
            val targetUserFriendRef = targetUserRef.collection("friends").document(uid)
            val followOpRef = currentUserRef.collection("followOps").document(uid)

            val now = Timestamp.now()
            val result = firestore.runTransaction { transaction ->
                val currentUser = transaction.get(currentUserRef)
                val targetUser = transaction.get(targetUserRef)
                val existingFollowing = transaction.get(followingRef)
                val existingFollower = transaction.get(followerRef)
                val targetFollowing = transaction.get(targetFollowingRef)

                if (!currentUser.exists()) {
                    throw IllegalStateException("Profil utilisateur introuvable")
                }
                if (!targetUser.exists()) {
                    throw IllegalStateException("Utilisateur cible introuvable")
                }

                val createFollowing = !existingFollowing.exists()
                val createFollower = !existingFollower.exists()

                if (!createFollowing && !createFollower) {
                    return@runTransaction Pair(false, targetFollowing.exists())
                }

                if (createFollowing) {
                    transaction.set(
                        followOpRef,
                        mapOf(
                            "type" to "follow",
                            "targetId" to targetUid
                        )
                    )
                    transaction.set(
                        followingRef,
                        mapOf(
                            "followedAt" to now,
                            "followingId" to targetUid,
                            "uid" to targetUid
                        )
                    )
                    val followingCount = currentUser.getLong("followingCount") ?: 0L
                    transaction.update(currentUserRef, "followingCount", followingCount + 1L)
                }

                if (createFollower) {
                    transaction.set(
                        followerRef,
                        mapOf(
                            "followedAt" to now,
                            "followerId" to uid,
                            "uid" to uid
                        )
                    )
                    val followersCount = targetUser.getLong("followersCount") ?: 0L
                    transaction.update(targetUserRef, "followersCount", followersCount + 1L)
                }

                Pair(true, targetFollowing.exists())
            }.await()

            val changed = result.first
            val isMutual = result.second

            if (!changed) {
                return@withContext Result.success(Unit)
            }

            if (isMutual) {
                try {
                    currentUserFriendRef.set(
                        mapOf("friendedAt" to now, "uid" to targetUid),
                        SetOptions.merge()
                    ).await()
                    targetUserFriendRef.set(
                        mapOf("friendedAt" to now, "uid" to uid),
                        SetOptions.merge()
                    ).await()
                } catch (e: Exception) {
                    Log.w("UserRepository", "Could not write friend relationship: ${e.message}")
                }
            }

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
     * Unfollow a user safely.
     * Removes both relationship records and decrements counters only for
     * relationship records that actually existed.
     */
    suspend fun unfollowUser(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        if (uid == targetUid) return@withContext Result.failure(Exception("Action non permise"))

        try {
            val currentUserRef = firestore.collection("users").document(uid)
            val targetUserRef = firestore.collection("users").document(targetUid)
            val followingRef = currentUserRef.collection("following").document(targetUid)
            val followerRef = targetUserRef.collection("followers").document(uid)
            val currentUserFriendRef = currentUserRef.collection("friends").document(targetUid)
            val targetUserFriendRef = targetUserRef.collection("friends").document(uid)
            val followOpRef = currentUserRef.collection("followOps").document(uid)

            val removed = firestore.runTransaction { transaction ->
                val currentUser = transaction.get(currentUserRef)
                val targetUser = transaction.get(targetUserRef)
                val followingDoc = transaction.get(followingRef)
                val followerDoc = transaction.get(followerRef)

                if (!targetUser.exists()) {
                    throw IllegalStateException("Utilisateur cible introuvable")
                }
                if (!currentUser.exists()) {
                    throw IllegalStateException("Profil utilisateur introuvable")
                }

                val deleteFollowing = followingDoc.exists()
                val deleteFollower = followerDoc.exists()

                if (!deleteFollowing && !deleteFollower) {
                    return@runTransaction false
                }

                if (deleteFollowing) {
                    transaction.set(
                        followOpRef,
                        mapOf(
                            "type" to "unfollow",
                            "targetId" to targetUid
                        )
                    )
                    transaction.delete(followingRef)
                    val followingCount = currentUser.getLong("followingCount") ?: 0L
                    transaction.update(
                        currentUserRef,
                        "followingCount",
                        (followingCount - 1L).coerceAtLeast(0L)
                    )
                }

                if (deleteFollower) {
                    transaction.delete(followerRef)
                    val followersCount = targetUser.getLong("followersCount") ?: 0L
                    transaction.update(
                        targetUserRef,
                        "followersCount",
                        (followersCount - 1L).coerceAtLeast(0L)
                    )
                }

                true
            }.await()

            if (!removed) {
                return@withContext Result.success(Unit)
            }

            try {
                currentUserFriendRef.delete().await()
                targetUserFriendRef.delete().await()
            } catch (e: Exception) {
                Log.w("UserRepository", "Could not delete friend relationship: ${e.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error unfollowing user $targetUid: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time listener to check if target user is a friend (mutual relationship).
     */
    fun isFriend(targetUid: String): Flow<Boolean> = callbackFlow {
        val uid = currentUserId
        if (uid == null || uid == targetUid) {
            trySend(false)
            close()
            return@callbackFlow
        }

        var listener: ListenerRegistration? = null
        try {
            listener = firestore.collection("users").document(uid)
                .collection("friends").document(targetUid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        trySend(snapshot.exists())
                    }
                }
        } catch (_: Exception) {}

        launch {
            val result = checkIsFriend(targetUid).getOrDefault(false)
            trySend(result)
        }

        awaitClose { listener?.remove() }
    }.flowOn(Dispatchers.IO)

    /**
     * Check if target user is in current user's friends subcollection once.
     * Falls back to checking mutual follow state if subcollection is restricted.
     */
    suspend fun checkIsFriend(targetUid: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.success(false)
        if (uid == targetUid) return@withContext Result.success(false)
        try {
            val doc = firestore.collection("users").document(uid)
                .collection("friends").document(targetUid)
                .get()
                .await()
            Result.success(doc.exists())
        } catch (_: Exception) {
            // Fallback: check mutual follows directly
            try {
                val followsTarget = checkIsFollowing(targetUid).getOrDefault(false)
                if (!followsTarget) return@withContext Result.success(false)
                val targetDoc = firestore.collection("users").document(targetUid)
                    .collection("following").document(uid)
                    .get()
                    .await()
                Result.success(targetDoc.exists())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get list of mutual friends (users who follow each other) for a user.
     * Reads from users/{uid}/friends subcollection. If the subcollection is empty
     * or inaccessible, checks mutual follows and backfills them automatically.
     */
    suspend fun getFriends(uid: String): Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            val friendIds = linkedSetOf<String>()

            // Materialized friends are useful, but legacy accounts may have an
            // incomplete cache. Always reconcile it with mutual following.
            try {
                val friendsSnap = firestore.collection("users").document(uid)
                    .collection("friends")
                    .get()
                    .await()
                friendsSnap.documents
                    .map { it.id }
                    .filter { it.isNotBlank() }
                    .forEach(friendIds::add)
            } catch (e: Exception) {
                Log.w("UserRepository", "Friends cache read failed: " + e.message)
            }

            try {
                val followingSnap = firestore.collection("users").document(uid)
                    .collection("following")
                    .get()
                    .await()
                val followingIds = followingSnap.documents.map { it.id }.filter { it.isNotBlank() }.toSet()

                if (followingIds.isNotEmpty()) {
                    val followersSnap = firestore.collection("users").document(uid)
                        .collection("followers")
                        .get()
                        .await()
                    val followerIds = followersSnap.documents.map { it.id }.filter { it.isNotBlank() }.toSet()
                    friendIds.addAll(followingIds.intersect(followerIds))
                }
            } catch (e: Exception) {
                Log.w("UserRepository", "Mutual follows reconciliation failed: " + e.message)
            }

            // Hydrate every friend profile concurrently. For legacy user documents
            // without a stored uid field, the Firestore document ID is authoritative.
            val users = coroutineScope {
                friendIds
                    .map { friendId ->
                        async {
                            try {
                                val userDoc = firestore.collection("users").document(friendId).get().await()
                                if (!userDoc.exists()) {
                                    null
                                } else {
                                    val parsed = User.fromMap(userDoc.data ?: emptyMap())
                                    parsed.copy(uid = parsed.uid.ifBlank { friendId })
                                }
                            } catch (e: Exception) {
                                Log.w("UserRepository", "Could not fetch user " + friendId + ": " + e.message)
                                null
                            }
                        }
                    }
                    .awaitAll()
                    .filterNotNull()
            }


            Result.success(users)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting friends for " + uid + ": " + e.message, e)
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
     * To avoid requiring a composite index on (authorId, createdAt) in Firestore,
     * we query by authorId and sort in-memory.
     */
    suspend fun getUserPosts(userId: String): Result<List<Post>> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) {
            return@withContext Result.success(emptyList())
        }
        try {
            val currentUid = currentUserId

            // Query posts by authorId without composite order to prevent FAILED_PRECONDITION
            val snapshot = firestore.collection("posts")
                .whereEqualTo("authorId", userId)
                .limit(100)
                .get()
                .await()

            // Fetch bookmarks to hydrate bookmark status (remote + local cache)
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

            val posts = coroutineScope {
                snapshot.documents.map { doc ->
                    async {
                        try {
                            val post = Post.fromSnapshot(doc, currentUid)
                            var isLiked = post.isLikedByCurrentUser
                            if (currentUid != null && !isLiked) {
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
                        } catch (e: Exception) {
                            Log.e("UserRepository", "Error parsing post in getUserPosts: ${e.message}")
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }.sortedByDescending { it.createdAt.toDate().time }

            Result.success(posts)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error fetching user posts for $userId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time listener for user posts.
     * Emits immediately whenever a user's post is added, deleted or modified in Firestore.
     */
    fun observeUserPosts(userId: String): Flow<List<Post>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val query = firestore.collection("posts")
            .whereEqualTo("authorId", userId)
            .limit(100)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("UserRepository", "observeUserPosts error: ${error.message}")
                launch(Dispatchers.IO) {
                    val result = getUserPosts(userId)
                    result.onSuccess { posts -> trySend(posts) }
                }
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener

            val docs = snapshot.documents
            launch(Dispatchers.IO) {
                val uid = currentUserId
                try {
                    val localBookmarks = dataStoreManager.getLocalBookmarkedIds()
                    val remoteBookmarks = if (uid != null) {
                        try {
                            firestore.collection("users").document(uid)
                                .collection("bookmarks").get().await()
                                .documents.map { it.id }.toSet()
                        } catch (_: Exception) {
                            emptySet()
                        }
                    } else emptySet()
                    val bookmarkedIds = remoteBookmarks + localBookmarks

                    val basePosts = docs.map { doc ->
                        Post.fromSnapshot(doc, uid).copy(
                            isBookmarkedByCurrentUser = bookmarkedIds.contains(doc.id)
                        )
                    }.sortedByDescending { it.createdAt.toDate().time }
                    // Immediate emission for instant reactivity
                    trySend(basePosts)

                    val hydrated = coroutineScope {
                        basePosts.map { post ->
                            async {
                                var isLiked = post.isLikedByCurrentUser
                                if (uid != null && !isLiked) {
                                    try {
                                        val likeDoc = firestore.collection("posts").document(post.id)
                                            .collection("likes").document(uid).get().await()
                                        isLiked = likeDoc.exists()
                                    } catch (_: Exception) {}
                                }
                                post.copy(isLikedByCurrentUser = isLiked)
                            }
                        }.awaitAll()
                    }
                    trySend(hydrated)
                } catch (e: Exception) {
                    val fallback = docs.map { Post.fromSnapshot(it, uid) }
                        .sortedByDescending { it.createdAt.toDate().time }
                    trySend(fallback)
                }
            }
        }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

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
                Log.w("UserRepository", "Failed deleting likes subcollection: ${e.message}")
            }

            // 2. Delete comments subcollection
            try {
                val commentsDocs = postRef.collection("comments").get().await()
                if (!commentsDocs.isEmpty) {
                    val batch = firestore.batch()
                    commentsDocs.documents.forEach { doc ->
                        batch.delete(doc.reference)
                    }
                    batch.commit().await()
                }
            } catch (e: Exception) {
                Log.w("UserRepository", "Failed deleting comments subcollection: ${e.message}")
            }

            // 3. Delete the post document itself
            postRef.delete().await()

            // 4. Decrement postsCount on user doc
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
            // Retrieve old username to release from usernames collection
            try {
                val userDoc = firestore.collection("users").document(uid).get().await()
                val lower = userDoc.getString("usernameLower") ?: userDoc.getString("username")?.lowercase()
                if (!lower.isNullOrBlank()) {
                    firestore.collection("usernames").document(lower).delete().await()
                }
            } catch (e: Exception) {
                Log.w("UserRepository", "Could not remove username claim: ${e.message}")
            }

            // 1. Mark user document as deleted in Firestore so no further messages/posts can be sent
            try {
                firestore.collection("users").document(uid)
                    .set(mapOf("isDeleted" to true), com.google.firebase.firestore.SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w("UserRepository", "Failed to set isDeleted flag: ${e.message}")
            }

            // 2. Delete user document in Firestore
            try {
                firestore.collection("users").document(uid).delete().await()
            } catch (e: Exception) {
                Log.w("UserRepository", "Failed to delete user document: ${e.message}")
            }

            // 3. Clear local DataStore and in-memory caches
            try {
                dataStoreManager.clearAll()
            } catch (e: Exception) {
                Log.w("UserRepository", "Failed to clear DataStore: ${e.message}")
            }
            try {
                com.thehub.hb.data.repository.UserCacheRepository.getInstance().clear()
            } catch (_: Exception) {}

            // 4. Delete Firebase Auth account
            try {
                authUser.delete().await()
            } catch (e: Exception) {
                Log.w("UserRepository", "Failed to delete Firebase Auth user: ${e.message}")
                // In case Firebase requires re-authentication, we sign out immediately so the session terminates
                signOutAndCleanPushToken()
            }

            // Ensure auth sign out is called
            signOutAndCleanPushToken()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error deleting account: ${e.message}", e)
            try {
                signOutAndCleanPushToken()
                dataStoreManager.clearAll()
            } catch (_: Exception) {}
            Result.failure(e)
        }
    }
}
