package com.thehub.hb.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.thehub.hb.data.model.NotificationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
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
     */
    fun getUnreadCount(): Flow<Int> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(0)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("notifications")
            .whereEqualTo("recipientId", uid)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("NotificationRepository", "Error observing unread count: ${error.message}")
                    trySend(0)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    trySend(snapshot.size())
                }
            }

        awaitClose { listener.remove() }
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
        commentText: String? = null
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
        if (currentUid == targetUid) return@withContext Result.failure(Exception("Impossible de se suivre soi-même"))

        try {
            val now = Timestamp.now()
            val batch = firestore.batch()

            val followerDoc = firestore.collection("users").document(targetUid)
                .collection("followers").document(currentUid)
            batch.set(followerDoc, mapOf("followedAt" to now, "followerId" to currentUid), SetOptions.merge())

            val followingDoc = firestore.collection("users").document(currentUid)
                .collection("following").document(targetUid)
            batch.set(followingDoc, mapOf("followedAt" to now, "followingId" to targetUid), SetOptions.merge())

            batch.commit().await()

            // Trigger notification
            createNotification(
                recipientId = targetUid,
                type = NotificationItem.TYPE_FOLLOW
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error following user: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Unfollow a user: removes records from both subcollections.
     */
    suspend fun unfollowUser(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        try {
            val batch = firestore.batch()
            val followerDoc = firestore.collection("users").document(targetUid)
                .collection("followers").document(currentUid)
            batch.delete(followerDoc)

            val followingDoc = firestore.collection("users").document(currentUid)
                .collection("following").document(targetUid)
            batch.delete(followingDoc)

            batch.commit().await()
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
