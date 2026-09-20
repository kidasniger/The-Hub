package com.thehub.hb.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class NotificationItem(
    val id: String = "",
    val recipientId: String = "",
    val actorId: String = "",
    val actorUsername: String = "",
    val actorPhotoUrl: String? = null,
    val type: String = TYPE_LIKE, // "like" | "comment" | "follow" | "message" | "like_comment" | "reply_comment"
    val postId: String? = null,
    val commentText: String? = null,
    val commentId: String? = null,
    val conversationId: String? = null,
    val messageId: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val isRead: Boolean = false
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "recipientId" to recipientId,
            "actorId" to actorId,
            "actorUsername" to actorUsername,
            "actorPhotoUrl" to actorPhotoUrl,
            "type" to type,
            "postId" to postId,
            "commentText" to commentText,
            "commentId" to commentId,
            "conversationId" to conversationId,
            "messageId" to messageId,
            "createdAt" to createdAt,
            "isRead" to isRead,
            "pushSent" to false
        )
    }

    companion object {
        const val TYPE_LIKE = "like"
        const val TYPE_COMMENT = "comment"
        const val TYPE_FOLLOW = "follow"
        const val TYPE_MESSAGE = "message"
        const val TYPE_LIKE_COMMENT = "like_comment"
        const val TYPE_REPLY_COMMENT = "reply_comment"

        fun fromSnapshot(doc: DocumentSnapshot): NotificationItem {
            return NotificationItem(
                id = doc.id,
                recipientId = doc.getString("recipientId") ?: "",
                actorId = doc.getString("actorId") ?: "",
                actorUsername = doc.getString("actorUsername") ?: "utilisateur",
                actorPhotoUrl = doc.getString("actorPhotoUrl"),
                type = doc.getString("type") ?: TYPE_LIKE,
                postId = doc.getString("postId"),
                commentText = doc.getString("commentText"),
                commentId = doc.getString("commentId"),
                conversationId = doc.getString("conversationId"),
                messageId = doc.getString("messageId"),
                createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now(),
                isRead = doc.getBoolean("isRead") ?: false
            )
        }
    }
}
