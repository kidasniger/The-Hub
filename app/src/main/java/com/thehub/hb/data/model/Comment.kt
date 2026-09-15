package com.thehub.hb.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class Comment(
    val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorPhotoUrl: String? = null,
    val text: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val likesCount: Int = 0,
    val parentCommentId: String? = null,
    val isLikedByCurrentUser: Boolean = false,
    val isHidden: Boolean = false,
    val isEdited: Boolean = false,
    val editedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "authorId" to authorId,
            "authorUsername" to authorUsername,
            "authorPhotoUrl" to authorPhotoUrl,
            "text" to text,
            "createdAt" to createdAt,
            "likesCount" to likesCount,
            "parentCommentId" to parentCommentId,
            "isHidden" to isHidden,
            "isEdited" to isEdited,
            "editedAt" to editedAt
        )
    }

    companion object {
        fun fromSnapshot(doc: DocumentSnapshot, currentUserId: String? = null): Comment {
            return Comment(
                id = doc.id,
                authorId = doc.getString("authorId") ?: "",
                authorUsername = doc.getString("authorUsername") ?: "thehub_user",
                authorPhotoUrl = doc.getString("authorPhotoUrl"),
                text = doc.getString("text") ?: "",
                createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now(),
                likesCount = (doc.getLong("likesCount") ?: 0L).toInt().coerceAtLeast(0),
                parentCommentId = doc.getString("parentCommentId"),
                isLikedByCurrentUser = false,
                isHidden = doc.getBoolean("isHidden") ?: false,
                isEdited = doc.getBoolean("isEdited") ?: false,
                editedAt = doc.getTimestamp("editedAt")
            )
        }
    }
}
