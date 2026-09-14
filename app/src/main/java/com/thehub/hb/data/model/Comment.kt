package com.thehub.hb.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class Comment(
    val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorPhotoUrl: String? = null,
    val text: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "authorId" to authorId,
            "authorUsername" to authorUsername,
            "authorPhotoUrl" to authorPhotoUrl,
            "text" to text,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromSnapshot(doc: DocumentSnapshot): Comment {
            return Comment(
                id = doc.id,
                authorId = doc.getString("authorId") ?: "",
                authorUsername = doc.getString("authorUsername") ?: "thehub_user",
                authorPhotoUrl = doc.getString("authorPhotoUrl"),
                text = doc.getString("text") ?: "",
                createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
            )
        }
    }
}
