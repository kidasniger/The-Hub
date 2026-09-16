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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class NotificationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUserId: String?
        get() = auth.currentUser?.uid

    private fun userRef(uid: String) = firestore.collection("users").document(uid)

    private suspend fun getCurrentActorInfo(): Triple<String, String, String?> {
        val user = auth.currentUser
        val uid = user?.uid ?: ""
        var username = user?.displayName ?: ""
        var photoUrl = user?.photoUrl?.toString()
        if (uid.isNotEmpty()) {
            runCatching {
                val doc = userRef(uid).get().await()
                if (doc.exists()) {
                    username = doc.getString("username")?.takeIf { it.isNotBlank() } ?: username
                    if (username.isBlank()) username = doc.getString("displayName") ?: username
                    photoUrl = doc.getString("photoUrl") ?: photoUrl
                }
            }
        }
        if (username.isBlank()) username = user?.email?.substringBefore("@")?.takeIf { it.isNotBlank() } ?: "utilisateur"
        return Triple(uid, username.take(64), photoUrl)
    }

    fun getNotifications(): Flow<List<NotificationItem>> = callbackFlow {
        val uid = currentUserId
        if (uid.isNullOrBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("notifications")
            .whereEqualTo("recipientId", uid)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NotificationRepository", "Erreur notifications", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    runCatching { NotificationItem.fromSnapshot(doc) }.getOrNull()
                }.sortedByDescending { it.createdAt.toDate().time }
                trySend(items)
            }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    fun getUnreadCount(): Flow<Int> = getNotifications().map { list -> list.count { !it.isRead } }

    suspend fun markAsRead(notificationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        try {
            val ref = firestore.collection("notifications").document(notificationId)
            val doc = ref.get().await()
            if (!doc.exists() || doc.getString("recipientId") != uid) return@withContext Result.failure(Exception("Notification introuvable"))
            ref.update("isRead", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllAsRead(): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        try {
            val snapshot = firestore.collection("notifications")
                .whereEqualTo("recipientId", uid)
                .limit(100)
                .get().await()
            val unread = snapshot.documents.filter { it.getBoolean("isRead") != true }
            unread.chunked(450).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { batch.update(it.reference, "isRead", true) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createNotification(
        recipientId: String,
        type: String,
        postId: String? = null,
        commentText: String? = null,
        commentId: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (recipientId.isBlank()) return@withContext Result.failure(Exception("Destinataire invalide"))
        if (type !in setOf(
                NotificationItem.TYPE_LIKE,
                NotificationItem.TYPE_COMMENT,
                NotificationItem.TYPE_FOLLOW,
                NotificationItem.TYPE_MESSAGE,
                NotificationItem.TYPE_LIKE_COMMENT,
                NotificationItem.TYPE_REPLY_COMMENT
            )) return@withContext Result.failure(Exception("Type de notification invalide"))
        try {
            val (actorUid, username, photoUrl) = getCurrentActorInfo()
            if (actorUid.isBlank() || actorUid == recipientId) return@withContext Result.success(Unit)
            val ref = firestore.collection("notifications").document()
            val item = NotificationItem(
                id = ref.id,
                recipientId = recipientId,
                actorId = actorUid,
                actorUsername = username,
                actorPhotoUrl = photoUrl,
                type = type,
                postId = postId,
                commentText = commentText?.take(500),
                commentId = commentId,
                createdAt = Timestamp.now(),
                isRead = false
            )
            ref.set(item.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Erreur création notification", e)
            Result.failure(e)
        }
    }

    /**
     * Transactional follow used by search/profile surfaces that historically called this repository.
     */
    suspend fun followUser(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        if (uid == targetUid) return@withContext Result.failure(Exception("Impossible de se suivre soi-même"))
        try {
            val me = userRef(uid)
            val target = userRef(targetUid)
            val following = me.collection("following").document(targetUid)
            val follower = target.collection("followers").document(uid)
            var created = false
            firestore.runTransaction { tx ->
                val meDoc = tx.get(me)
                val targetDoc = tx.get(target)
                if (!meDoc.exists() || !targetDoc.exists() || meDoc.getBoolean("isDeleted") == true || targetDoc.getBoolean("isDeleted") == true) {
                    throw IllegalStateException("Compte indisponible")
                }
                if (tx.get(me.collection("blockedUsers").document(targetUid)).exists() || tx.get(target.collection("blockedUsers").document(uid)).exists()) {
                    throw IllegalStateException("Action impossible entre comptes bloqués")
                }
                if (!tx.get(following).exists()) {
                    val now = Timestamp.now()
                    tx.set(following, mapOf("followedAt" to now, "followingId" to targetUid, "uid" to targetUid))
                    tx.set(follower, mapOf("followedAt" to now, "followerId" to uid, "uid" to uid))
                    tx.update(me, "followingCount", FieldValue.increment(1))
                    tx.update(target, "followersCount", FieldValue.increment(1))
                    created = true
                }
            }.await()
            if (created) createNotification(targetUid, NotificationItem.TYPE_FOLLOW)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Erreur follow", e)
            Result.failure(e)
        }
    }

    suspend fun unfollowUser(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Non connecté"))
        if (uid == targetUid) return@withContext Result.failure(Exception("Action non permise"))
        try {
            val me = userRef(uid)
            val target = userRef(targetUid)
            val following = me.collection("following").document(targetUid)
            val follower = target.collection("followers").document(uid)
            firestore.runTransaction { tx ->
                if (tx.get(following).exists()) {
                    tx.delete(following)
                    tx.delete(follower)
                    tx.update(me, "followingCount", FieldValue.increment(-1))
                    tx.update(target, "followersCount", FieldValue.increment(-1))
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Erreur unfollow", e)
            Result.failure(e)
        }
    }

    fun isFollowing(targetUid: String): Flow<Boolean> = callbackFlow {
        val uid = currentUserId
        if (uid.isNullOrBlank() || uid == targetUid) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val listener = userRef(uid).collection("following").document(targetUid)
            .addSnapshotListener { snapshot, _ -> trySend(snapshot?.exists() == true) }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)
}
