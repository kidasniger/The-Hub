package com.thehub.hb.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class MessageReaction(
    val id: String = "",
    val messageId: String = "",
    val userId: String = "",
    val emoji: String = "",
    val updatedAt: Timestamp = Timestamp.now()
) {
    companion object {
        fun fromSnapshot(doc: DocumentSnapshot): MessageReaction {
            return MessageReaction(
                id = doc.id,
                messageId = doc.getString("messageId") ?: "",
                userId = doc.getString("userId") ?: "",
                emoji = doc.getString("emoji") ?: "",
                updatedAt = doc.getTimestamp("updatedAt") ?: Timestamp.now()
            )
        }
    }
}
