package com.thehub.hb.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String? = null,
    val imageUrl: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val status: String = STATUS_SENT
) {
    fun isSentBy(uid: String?): Boolean = uid != null && senderId == uid

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "senderId" to senderId,
            "text" to text,
            "imageUrl" to imageUrl,
            "createdAt" to createdAt,
            "status" to status
        )
    }

    companion object {
        const val STATUS_SENT = "sent"
        const val STATUS_DELIVERED = "delivered"
        const val STATUS_READ = "read"

        fun fromSnapshot(doc: DocumentSnapshot): Message {
            val createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
            return Message(
                id = doc.id,
                senderId = doc.getString("senderId") ?: "",
                text = doc.getString("text"),
                imageUrl = doc.getString("imageUrl"),
                createdAt = createdAt,
                status = doc.getString("status") ?: STATUS_SENT
            )
        }
    }
}
