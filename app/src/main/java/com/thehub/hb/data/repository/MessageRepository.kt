package com.thehub.hb.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
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

// CI build trigger: verify the full release pipeline on the current feature set.

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
                    // A listener can report a later network/cache/rules error after
                    // successfully delivering cached data. Never turn that event
                    // into an empty list, otherwise conversations visibly disappear.
                    android.util.Log.w(
                        "MessageRepository",
                        "Conversation listener error: " + error.message
                    )
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val conversations = snapshot.documents.mapNotNull { doc ->
                        try {
                            Conversation.fromSnapshot(doc)
                        } catch (e: Exception) {
                            android.util.Log.w(
                                "MessageRepository",
                                "Skipping malformed conversation " + doc.id + ": " + e.message
                            )
                            null
                        }
                    }.sortedWith(
                        compareByDescending<Conversation> { it.lastMessageAt.seconds }
                            .thenByDescending { it.lastMessageAt.nanoseconds }
                            .thenByDescending { it.id }
                    )

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
     * Real-time stream of every message in a conversation.
     *
     * Sort client-side so legacy messages without a createdAt field are not
     * silently excluded by Firestore's orderBy query.
     */
    fun getMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents
                        .sortedWith(
                            compareBy<com.google.firebase.firestore.DocumentSnapshot> {
                                it.getTimestamp("createdAt")?.seconds ?: Long.MIN_VALUE
                            }.thenBy {
                                it.getTimestamp("createdAt")?.nanoseconds ?: Int.MIN_VALUE
                            }.thenBy { it.id }
                        )
                        .mapNotNull { doc ->
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
     * Checks whether the current user is allowed to message the target user.
     * Allowed relations: mutual friendship, following, or being followed.
     */
    suspend fun checkCanSendMessage(targetUserId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val currentUid = currentUserId
            ?: return@withContext Result.success(false)

        if (targetUserId.isBlank() || currentUid == targetUserId) {
            return@withContext Result.success(false)
        }

        try {
            val users = firestore.collection("users")

            val isFriend = users.document(currentUid)
                .collection("friends")
                .document(targetUserId)
                .get()
                .await()

            if (isFriend.exists()) return@withContext Result.success(true)

            val isFollowing = users.document(currentUid)
                .collection("following")
                .document(targetUserId)
                .get()
                .await()

            if (isFollowing.exists()) return@withContext Result.success(true)

            val isFollower = users.document(targetUserId)
                .collection("followers")
                .document(currentUid)
                .get()
                .await()

            Result.success(isFollower.exists())
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

            // Avoid reading a missing conversation before Firestore can establish that
            // the caller is a participant. A create attempt is authorized server-side
            // by firestore.rules and does not expose existence of arbitrary chats.
            val currentInfo = resolveParticipantInfo(currentUid)
            val otherInfo = resolveParticipantInfo(otherUserId)

            val newConversation = Conversation(
                id = convId,
                participantIds = listOf(currentUid, otherUserId),
                participantsInfo = mapOf(
                    currentUid to currentInfo,
                    otherUserId to otherInfo
                ),
                lastMessageText = "",
                lastMessageAt = Timestamp.now(),
                lastMessageSenderId = "",
                unreadCount = mapOf(
                    currentUid to 0,
                    otherUserId to 0
                )
            )

            try {
                convRef.create(newConversation.toMap()).await()
                Result.success(newConversation)
            } catch (e: FirebaseFirestoreException) {
                when (e.code) {
                    FirebaseFirestoreException.Code.ALREADY_EXISTS -> {
                        val existingDoc = convRef.get().await()
                        if (existingDoc.exists()) {
                            Result.success(Conversation.fromSnapshot(existingDoc))
                        } else {
                            Result.failure(Exception("Conversation introuvable."))
                        }
                    }

                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                        Result.failure(
                            Exception("Vous devez être ami ou abonné pour envoyer un message à cet utilisateur.")
                        )
                    }

                    else -> throw e
                }
            }
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

        try {
            val userDoc = firestore.collection("users").document(currentUid).get().await()
            if (!userDoc.exists() || userDoc.getBoolean("isDeleted") == true) {
                auth.signOut()
                return@withContext Result.failure(Exception("Ce compte a été supprimé."))
            }
        } catch (e: Exception) {
            if (e.message?.contains("supprimé", ignoreCase = true) == true) {
                return@withContext Result.failure(e)
            }
        }

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
            if (!conversation.participantIds.contains(currentUid)) {
                return@withContext Result.failure(Exception("Accès refusé à cette conversation."))
            }

            val otherUid = conversation.getOtherParticipantId(currentUid)
            if (otherUid.isBlank()) {
                return@withContext Result.failure(Exception("Conversation invalide."))
            }

            val permission = checkCanSendMessage(otherUid)
            if (permission.isFailure) {
                return@withContext Result.failure(
                    permission.exceptionOrNull()
                        ?: Exception("Erreur lors de la vérification des permissions de messagerie.")
                )
            }
            if (!permission.getOrDefault(false)) {
                return@withContext Result.failure(
                    Exception("Vous devez être ami ou abonné pour envoyer un message à cet utilisateur.")
                )
            }

            // Rebuild the complete two-participant unread map. This also repairs
            // legacy conversations where unreadCount or one participant key is absent.
            val currentUnread = conversation.getUnreadCountFor(currentUid)
            val otherUnread = conversation.getUnreadCountFor(otherUid)
            val updatedUnreadCount = mapOf(
                currentUid to currentUnread,
                otherUid to otherUnread + 1
            )

            val messageRef = convRef.collection("messages").document()
            val messageOpRef = convRef.collection("messageOps").document(currentUid)

            val messageData = hashMapOf<String, Any?>(
                "senderId" to currentUid,
                "text" to (if (trimmedText.isNullOrBlank()) null else trimmedText),
                "imageUrl" to (if (imageUrl.isNullOrBlank()) null else imageUrl),
                "createdAt" to FieldValue.serverTimestamp(),
                "status" to Message.STATUS_SENT
            )

            val previewText = when {
                !trimmedText.isNullOrBlank() -> trimmedText
                !imageUrl.isNullOrBlank() -> "📷 Photo"
                else -> ""
            }

            val batch = firestore.batch()
            batch.set(messageRef, messageData)
            batch.set(
                messageOpRef,
                mapOf(
                    "type" to "send",
                    "targetId" to messageRef.id
                )
            )
            batch.update(
                convRef,
                mapOf(
                    "lastMessageText" to previewText,
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "lastMessageSenderId" to currentUid,
                    "unreadCount" to updatedUnreadCount
                )
            )
            batch.commit().await()

            val messageDoc = messageRef.get().await()
            if (!messageDoc.exists()) {
                return@withContext Result.failure(Exception("Le message n'a pas pu être confirmé."))
            }

            val message = Message.fromSnapshot(messageDoc)

            try {
                notificationRepository.createNotification(
                    recipientId = otherUid,
                    type = NotificationItem.TYPE_MESSAGE,
                    commentText = previewText
                )
            } catch (_: Exception) {}

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
            val convDoc = convRef.get().await()
            if (!convDoc.exists()) {
                return@withContext Result.success(Unit)
            }

            val conversation = Conversation.fromSnapshot(convDoc)
            if (!conversation.participantIds.contains(currentUid)) {
                return@withContext Result.failure(Exception("Accès refusé à cette conversation."))
            }

            val otherUid = conversation.getOtherParticipantId(currentUid)
            if (otherUid.isBlank()) {
                return@withContext Result.failure(Exception("Conversation invalide."))
            }

            // Always persist a complete two-key map so legacy conversations with
            // missing unreadCount fields become compatible with the secured rules.
            val unreadCount = mapOf(
                currentUid to 0,
                otherUid to conversation.getUnreadCountFor(otherUid)
            )

            val unreadMessagesQuery = convRef.collection("messages")
                .whereNotEqualTo("senderId", currentUid)
                .get()
                .await()

            val batch = firestore.batch()
            batch.update(convRef, "unreadCount", unreadCount)

            for (doc in unreadMessagesQuery.documents) {
                val status = doc.getString("status")
                if (status != Message.STATUS_READ) {
                    batch.update(doc.reference, "status", Message.STATUS_READ)
                }
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete conversation document and all its messages.
     */
    suspend fun deleteConversation(conversationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUid = currentUserId
            ?: return@withContext Result.failure(Exception("Utilisateur non connecté."))

        try {
            val convRef = firestore.collection("conversations").document(conversationId)
            val convDoc = convRef.get().await()
            if (!convDoc.exists()) {
                return@withContext Result.success(Unit)
            }

            val conversation = Conversation.fromSnapshot(convDoc)
            if (!conversation.participantIds.contains(currentUid)) {
                return@withContext Result.failure(Exception("Accès refusé."))
            }

            val messagesQuery = convRef.collection("messages").get().await()
            // Firestore WriteBatch supports at most 500 writes. The rules require
            // all message deletions and the parent conversation deletion to be
            // atomic, so refuse oversized conversations rather than falling back
            // to a less secure multi-step deletion.
            if (messagesQuery.size() >= 500) {
                return@withContext Result.failure(
                    Exception("Cette conversation contient trop de messages pour être supprimée en une seule opération.")
                )
            }

            val batch = firestore.batch()
            for (doc in messagesQuery.documents) {
                batch.delete(doc.reference)
            }
            batch.delete(convRef)
            batch.commit().await()

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