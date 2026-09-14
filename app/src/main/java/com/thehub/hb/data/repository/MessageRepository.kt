package com.thehub.hb.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.thehub.hb.data.model.Conversation
import com.thehub.hb.data.model.Message
import com.thehub.hb.data.model.NotificationItem
import com.thehub.hb.data.model.ParticipantInfo
import com.thehub.hb.data.model.User
import com.thehub.hb.data.remote.ImgbbService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MessageRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val imgbbService: ImgbbService = ImgbbService(),
    private val notificationRepository: NotificationRepository = NotificationRepository(firestore, auth)
) {
    val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Upload an image to ImgBB before sending.
     */
    suspend fun uploadMessageImage(imageBytes: ByteArray): Result<String> {
        return imgbbService.uploadImage(imageBytes)
    }

    /**
     * Real-time stream of conversations for the current user,
     * sorted by lastMessageAt descending.
     */
    fun getConversations(): Flow<List<Conversation>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("conversations")
            .whereArrayContains("participantIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val conversations = snapshot.documents.mapNotNull { doc ->
                        try {
                            Conversation.fromSnapshot(doc)
                        } catch (_: Exception) {
                            null
                        }
                    }.sortedByDescending { it.lastMessageAt.toDate().time }

                    trySend(conversations)
                }
            }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    /**
     * Stream total unread conversation count for the current user.
     */
    fun getUnreadConversationsCount(): Flow<Int> {
        return getConversations().map { convList ->
            val uid = currentUserId
            convList.count { it.getUnreadCountFor(uid) > 0 }
        }
    }

    /**
     * Real-time stream of a specific conversation.
     */
    fun getConversation(conversationId: String): Flow<Conversation?> = callbackFlow {
        val listener = firestore.collection("conversations")
            .document(conversationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                try {
                    val conversation = Conversation.fromSnapshot(snapshot)
                    trySend(conversation)
                } catch (_: Exception) {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    /**
     * Real-time stream of messages in a conversation, sorted by createdAt ascending.
     */
    fun getMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        try {
                            Message.fromSnapshot(doc)
                        } catch (_: Exception) {
                            null
                        }
                    }
                    trySend(messages)
                }
            }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    /**
     * Fetches user profile for a given uid.
     */
    suspend fun getUserProfile(userId: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("users").document(userId).get().await()
            if (!doc.exists()) {
                return@withContext Result.failure(Exception("Utilisateur introuvable."))
            }
            Result.success(User.fromMap(doc.data ?: emptyMap()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get or create a deterministic conversation between current user and other user.
     */
    suspend fun getOrCreateConversation(otherUserId: String): Result<Conversation> = withContext(Dispatchers.IO) {
        val currentUid = currentUserId
            ?: return@withContext Result.failure(Exception("Utilisateur non connecté."))

        if (currentUid == otherUserId) {
            return@withContext Result.failure(Exception("Impossible d'ouvrir une conversation avec soi-même."))
        }

        try {
            val convId = Conversation.generateDeterministicId(currentUid, otherUserId)
            val convRef = firestore.collection("conversations").document(convId)
            val doc = convRef.get().await()

            if (doc.exists()) {
                return@withContext Result.success(Conversation.fromSnapshot(doc))
            }

            // Resolve info for both users
            val currentInfo = resolveParticipantInfo(currentUid)
            val otherInfo = resolveParticipantInfo(otherUserId)

            val participantsInfo = mapOf(
                currentUid to currentInfo,
                otherUserId to otherInfo
            )

            val unreadCount = mapOf(
                currentUid to 0,
                otherUserId to 0
            )

            val newConversation = Conversation(
                id = convId,
                participantIds = listOf(currentUid, otherUserId),
                participantsInfo = participantsInfo,
                lastMessageText = "",
                lastMessageAt = Timestamp.now(),
                lastMessageSenderId = "",
                unreadCount = unreadCount
            )

            convRef.set(newConversation.toMap()).await()
            Result.success(newConversation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun resolveParticipantInfo(userId: String): ParticipantInfo {
        var username = "utilisateur"
        var displayName: String? = null
        var photoUrl: String? = null

        try {
            val doc = firestore.collection("users").document(userId).get().await()
            if (doc.exists()) {
                val dbUsername = doc.getString("username")
                val dbDisplayName = doc.getString("displayName")
                val dbPhoto = doc.getString("photoUrl")
                if (!dbUsername.isNullOrBlank()) username = dbUsername
                if (!dbDisplayName.isNullOrBlank()) displayName = dbDisplayName
                if (!dbPhoto.isNullOrBlank()) photoUrl = dbPhoto
            } else {
                val authUser = auth.currentUser
                if (authUser != null && authUser.uid == userId) {
                    if (!authUser.displayName.isNullOrBlank()) {
                        username = authUser.displayName ?: username
                        displayName = authUser.displayName
                    }
                    photoUrl = authUser.photoUrl?.toString()
                }
            }
        } catch (_: Exception) {}

        return ParticipantInfo(
            username = username,
            displayName = displayName,
            photoUrl = photoUrl
        )
    }

    /**
     * Send a message in a conversation and update conversation metadata.
     */
    suspend fun sendMessage(
        conversationId: String,
        text: String?,
        imageUrl: String?
    ): Result<Message> = withContext(Dispatchers.IO) {
        val currentUid = currentUserId
            ?: return@withContext Result.failure(Exception("Utilisateur non connecté."))

        val trimmedText = text?.trim()
        if (trimmedText.isNullOrBlank() && imageUrl.isNullOrBlank()) {
            return@withContext Result.failure(Exception("Le message ne peut pas être vide."))
        }

        try {
            val convRef = firestore.collection("conversations").document(conversationId)
            val convDoc = convRef.get().await()
            if (!convDoc.exists()) {
                return@withContext Result.failure(Exception("Conversation introuvable."))
            }

            val conversation = Conversation.fromSnapshot(convDoc)
            val otherUid = conversation.getOtherParticipantId(currentUid)

            val now = Timestamp.now()
            val messageData = hashMapOf<String, Any?>(
                "senderId" to currentUid,
                "text" to (if (trimmedText.isNullOrBlank()) null else trimmedText),
                "imageUrl" to (if (imageUrl.isNullOrBlank()) null else imageUrl),
                "createdAt" to now,
                "status" to Message.STATUS_SENT
            )

            val messageRef = convRef.collection("messages").document()
            messageRef.set(messageData).await()

            // Update conversation document
            val previewText = when {
                !trimmedText.isNullOrBlank() -> trimmedText
                !imageUrl.isNullOrBlank() -> "📷 Photo"
                else -> ""
            }

            val updates = hashMapOf<String, Any>(
                "lastMessageText" to previewText,
                "lastMessageAt" to now,
                "lastMessageSenderId" to currentUid
            )

            if (otherUid.isNotEmpty()) {
                updates["unreadCount.$otherUid"] = FieldValue.increment(1)
            }

            convRef.update(updates).await()

            if (otherUid.isNotBlank()) {
                try {
                    notificationRepository.createNotification(
                        recipientId = otherUid,
                        type = NotificationItem.TYPE_MESSAGE,
                        commentText = previewText
                    )
                } catch (_: Exception) {}
            }

            val message = Message(
                id = messageRef.id,
                senderId = currentUid,
                text = if (trimmedText.isNullOrBlank()) null else trimmedText,
                imageUrl = if (imageUrl.isNullOrBlank()) null else imageUrl,
                createdAt = now,
                status = Message.STATUS_SENT
            )

            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark conversation as read for current user:
     * - Resets unreadCount[currentUid] to 0
     * - Sets status = "read" for all unread messages sent by other user
     */
    suspend fun markAsRead(conversationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUid = currentUserId ?: return@withContext Result.success(Unit)

        try {
            val convRef = firestore.collection("conversations").document(conversationId)
            convRef.update("unreadCount.$currentUid", 0).await()

            // Update messages from other participant that are not read yet
            val unreadMessagesQuery = convRef.collection("messages")
                .whereNotEqualTo("senderId", currentUid)
                .get()
                .await()

            val batch = firestore.batch()
            var hasUpdates = false
            for (doc in unreadMessagesQuery.documents) {
                val status = doc.getString("status")
                if (status != Message.STATUS_READ) {
                    batch.update(doc.reference, "status", Message.STATUS_READ)
                    hasUpdates = true
                }
            }

            if (hasUpdates) {
                batch.commit().await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete conversation document and all its messages.
     */
    suspend fun deleteConversation(conversationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val convRef = firestore.collection("conversations").document(conversationId)
            
            // Delete messages sub-collection in batches
            val messagesQuery = convRef.collection("messages").get().await()
            if (!messagesQuery.isEmpty) {
                val batch = firestore.batch()
                for (doc in messagesQuery.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }

            convRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Block a user by adding them to users/{currentUid}/blockedUsers/{otherUid}.
     */
    suspend fun blockUser(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUid = currentUserId
            ?: return@withContext Result.failure(Exception("Utilisateur non connecté."))

        try {
            firestore.collection("users")
                .document(currentUid)
                .collection("blockedUsers")
                .document(userId)
                .set(
                    mapOf(
                        "userId" to userId,
                        "blockedAt" to Timestamp.now()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if a user is blocked by current user.
     */
    suspend fun isUserBlocked(userId: String): Boolean = withContext(Dispatchers.IO) {
        val currentUid = currentUserId ?: return@withContext false
        try {
            val doc = firestore.collection("users")
                .document(currentUid)
                .collection("blockedUsers")
                .document(userId)
                .get()
                .await()
            doc.exists()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Search users by username, case-insensitive.
     * Excludes current user.
     */
    suspend fun searchUsers(query: String): Result<List<User>> = withContext(Dispatchers.IO) {
        val currentUid = currentUserId
        val cleanQuery = query.trim().lowercase()

        try {
            val users = if (cleanQuery.isEmpty()) {
                val snapshot = firestore.collection("users")
                    .limit(20)
                    .get()
                    .await()
                snapshot.documents.mapNotNull { doc ->
                    val user = User.fromMap(doc.data ?: emptyMap())
                    if (user.uid != currentUid) user else null
                }
            } else {
                val snapshot = firestore.collection("users")
                    .orderBy("username")
                    .startAt(cleanQuery)
                    .endAt(cleanQuery + "\uf8ff")
                    .limit(20)
                    .get()
                    .await()

                snapshot.documents.mapNotNull { doc ->
                    val user = User.fromMap(doc.data ?: emptyMap())
                    if (user.uid != currentUid) user else null
                }
            }

            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
