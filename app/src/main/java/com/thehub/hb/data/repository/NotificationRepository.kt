package com.thehub.hb.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.thehub.hb.data.model.NotificationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class NotificationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUserId: String?
        get() = auth.currentUser?.uid

    private suspend fun getCurrentActorInfo(): Triple<String, String, String?> {
        val user = auth.currentUser
        val uid = user?.uid ?: ""
        var username = user?.displayName ?: ""
        var photoUrl = user?.photoUrl?.toString()

        if (uid.isNotEmpty()) {
            try {
                val doc = firestore.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    val dbUsername = doc.getString("username")
                    val dbDisplayName = doc.getString("displayName")
                    val dbPhoto = doc.getString("photoUrl")
                    if (!dbUsername.isNullOrBlank()) username = dbUsername
                    else if (!dbDisplayName.isNullOrBlank()) username = dbDisplayName
                    if (!dbPhoto.isNullOrBlank()) photoUrl = dbPhoto
                }
            } catch (_: Exception) {}
        }

        if (username.isBlank()) {
            val email = user?.email ?: ""
            username = if (email.contains("@")) email.substringBefore("@") else "utilisateur"
        }

        return Triple(uid, username, photoUrl)
    }

    /**
     * Register the current Android device for push notifications.
     * The token is stored under the authenticated user and identified by a
     * one-way SHA-256 document id so the raw token is never used as a path.
     */
    suspend fun registerCurrentFcmToken(): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId
            ?: return@withContext Result.success(Unit)

        try {
            val token = FirebaseMessaging.getInstance().token.await()
            if (auth.currentUser?.uid != uid) {
                return@withContext Result.success(Unit)
            }
            if (token.isBlank()) {
                return@withContext Result.failure(Exception("Token FCM vide."))
            }

            val tokenId = fcmTokenDocumentId(token)
            firestore.collection("users")
                .document(uid)
                .collection("fcmTokens")
                .document(tokenId)
                .set(
                    mapOf(
                        "token" to token,
                        "platform" to "android",
                        "updatedAt" to Timestamp.now()
                    ),
                    SetOptions.merge()
                )
                .await()

            Log.d("NotificationRepository", "FCM token registered for user "+uid)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("NotificationRepository", "FCM token registration failed: "+e.message)
            Result.failure(e)
        }
    }

    /**
     * Remove the current Android device token from a specific user.
     * Used during logout so a signed-out account stops receiving pushes on this device.
     */
    suspend fun unregisterFcmTokenForUser(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.success(Unit)

        try {
            val token = FirebaseMessaging.getInstance().token.await()
            if (token.isBlank()) return@withContext Result.success(Unit)

            firestore.collection("users")
                .document(userId)
                .collection("fcmTokens")
                .document(fcmTokenDocumentId(token))
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("NotificationRepository", "FCM token removal failed: "+e.message)
            Result.failure(e)
        }
    }

    private fun fcmTokenDocumentId(token: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    /**
     * Real-time stream of notifications for the current user.
     * Filtered by recipientId and sorted in Kotlin to avoid requiring a composite index in Firestore.
     */
    fun getNotifications(): Flow<List<NotificationItem>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("notifications")
            .whereEqualTo("recipientId", uid)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NotificationRepository", "Error observing notifications: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            NotificationItem.fromSnapshot(doc)
                        } catch (e: Exception) {
                            Log.w("NotificationRepository", "Error parsing notification ${doc.id}: ${e.message}")
                            null
                        }
                    }.sortedByDescending { it.createdAt.toDate().time }

                    trySend(list)
                }
            }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    /**
     * Real-time stream of unread notification count.
     * Derived from getNotifications() to avoid requiring composite Firestore indexes
     * and guarantee 100% synchronization with the real-time notification list.
     */
    fun getUnreadCount(): Flow<Int> = getNotifications().map { list ->
        list.count { !it.isRead }
    }.flowOn(Dispatchers.IO)

    /**
     * Mark a single notification as read.
     */
    suspend fun markAsRead(notificationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("notifications").document(notificationId)
                .update("isRead", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error marking notification as read: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Mark all notifications for current user as read.
     */
    suspend fun markAllAsRead(): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        try {
            val snapshot = firestore.collection("notifications")
                .whereEqualTo("recipientId", uid)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val batch = firestore.batch()
                for (doc in snapshot.documents) {
                    batch.update(doc.reference, "isRead", true)
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error marking all as read: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Create a notification in Firestore.
     * Prevents notifications when actor == recipient (self-actions).
     */
    suspend fun createNotification(
        recipientId: String,
        type: String,
        postId: String? = null,
        commentText: String? = null,
        commentId: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val (actorUid, actorUsername, actorPhotoUrl) = getCurrentActorInfo()
            if (actorUid.isBlank() || recipientId.isBlank() || actorUid == recipientId) {
                // Ignore self-notification
                return@withContext Result.success(Unit)
            }

            val docRef = firestore.collection("notifications").document()
            val notification = NotificationItem(
                id = docRef.id,
                recipientId = recipientId,
                actorId = actorUid,
                actorUsername = actorUsername,
                actorPhotoUrl = actorPhotoUrl,
                type = type,
                postId = postId,
                commentText = commentText,
                commentId = commentId,
                createdAt = Timestamp.now(),
                isRead = false
            )

            docRef.set(notification.toMap()).await()
            Log.d("NotificationRepository", "Created notification ${docRef.id} for recipient $recipientId (type=$type)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Failed to create notification: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Follow a user: creates records in users/{targetUid}/followers/{currentUid}
     * and users/{currentUid}/following/{targetUid}, and triggers a "follow" notification.
     */
    suspend fun followUser(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        if (currentUid == targetUid) {
            return@withContext Result.failure(Exception("Impossible de se suivre soi-même"))
        }

        try {
            val currentUserRef = firestore.collection("users").document(currentUid)
            val targetUserRef = firestore.collection("users").document(targetUid)
            val followerRef = targetUserRef.collection("followers").document(currentUid)
            val followingRef = currentUserRef.collection("following").document(targetUid)
            val followOpRef = currentUserRef.collection("followOps").document(currentUid)
            val now = Timestamp.now()

            val created = firestore.runTransaction { transaction ->
                val currentUser = transaction.get(currentUserRef)
                val targetUser = transaction.get(targetUserRef)
                val existingFollower = transaction.get(followerRef)
                val existingFollowing = transaction.get(followingRef)

                if (!currentUser.exists()) {
                    throw IllegalStateException("Profil utilisateur introuvable")
                }
                if (!targetUser.exists()) {
                    throw IllegalStateException("Utilisateur cible introuvable")
                }

                val createFollower = !existingFollower.exists()
                val createFollowing = !existingFollowing.exists()

                if (!createFollower && !createFollowing) {
                    return@runTransaction false
                }

                if (createFollower) {
                    transaction.set(
                        followerRef,
                        mapOf(
                            "followedAt" to now,
                            "followerId" to currentUid,
                            "uid" to currentUid
                        )
                    )
                    val followersCount = targetUser.getLong("followersCount") ?: 0L
                    transaction.update(targetUserRef, "followersCount", followersCount + 1L)
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

                true
            }.await()

            if (created) {
                createNotification(
                    recipientId = targetUid,
                    type = NotificationItem.TYPE_FOLLOW
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error following user: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Unfollow a user atomically and keep both relation counters synchronized.
     */
    suspend fun unfollowUser(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))

        try {
            val currentUserRef = firestore.collection("users").document(currentUid)
            val targetUserRef = firestore.collection("users").document(targetUid)
            val followerRef = targetUserRef.collection("followers").document(currentUid)
            val followingRef = currentUserRef.collection("following").document(targetUid)
            val followOpRef = currentUserRef.collection("followOps").document(currentUid)

            firestore.runTransaction { transaction ->
                val currentUser = transaction.get(currentUserRef)
                val targetUser = transaction.get(targetUserRef)
                val followerDoc = transaction.get(followerRef)
                val followingDoc = transaction.get(followingRef)

                if (!currentUser.exists()) {
                    throw IllegalStateException("Profil utilisateur introuvable")
                }
                if (!targetUser.exists()) {
                    throw IllegalStateException("Utilisateur cible introuvable")
                }

                if (followerDoc.exists()) {
                    transaction.delete(followerRef)
                    val followersCount = targetUser.getLong("followersCount") ?: 0L
                    transaction.update(
                        targetUserRef,
                        "followersCount",
                        (followersCount - 1L).coerceAtLeast(0L)
                    )
                }

                if (followingDoc.exists()) {
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
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error unfollowing user: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if current user is following target user.
     */
    fun isFollowing(targetUid: String): Flow<Boolean> = callbackFlow {
        val currentUid = currentUserId
        if (currentUid == null || currentUid == targetUid) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users").document(targetUid)
            .collection("followers").document(currentUid)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.exists() == true)
            }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)
}
