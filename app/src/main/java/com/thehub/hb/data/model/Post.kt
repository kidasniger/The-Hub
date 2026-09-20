package com.thehub.hb.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class Post(
    val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorPhotoUrl: String? = null,
    val text: String = "",
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val repostsCount: Int = 0,
    val isRepost: Boolean = false,
    val originalPostId: String? = null,
    val isLikedByCurrentUser: Boolean = false,
    val isBookmarkedByCurrentUser: Boolean = false,
    val hashtags: List<String> = emptyList(),
    val originalPost: Post? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "authorId" to authorId,
            "authorUsername" to authorUsername,
            "authorPhotoUrl" to authorPhotoUrl,
            "text" to text,
            "imageUrl" to imageUrl,
            "videoUrl" to videoUrl,
            "createdAt" to createdAt,
            "likesCount" to likesCount,
            "commentsCount" to commentsCount,
            "repostsCount" to repostsCount,
            "isRepost" to isRepost,
            "originalPostId" to originalPostId,
            "hashtags" to hashtags
        )
    }

    companion object {
        fun fromSnapshot(doc: DocumentSnapshot, currentUserId: String? = null): Post {
            val data = doc.data ?: emptyMap<String, Any?>()
            val createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
            val likesCount = (doc.getLong("likesCount") ?: 0L).toInt()
            val commentsCount = (doc.getLong("commentsCount") ?: 0L).toInt()
            val repostsCount = (doc.getLong("repostsCount") ?: 0L).toInt()

            @Suppress("UNCHECKED_CAST")
            val hashtags = (doc.get("hashtags") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            return Post(
                id = doc.id,
                authorId = doc.getString("authorId") ?: "",
                authorUsername = doc.getString("authorUsername") ?: "thehub_user",
                authorPhotoUrl = doc.getString("authorPhotoUrl"),
                text = doc.getString("text") ?: "",
                imageUrl = doc.getString("imageUrl"),
                videoUrl = doc.getString("videoUrl"),
                createdAt = createdAt,
                likesCount = if (likesCount < 0) 0 else likesCount,
                commentsCount = if (commentsCount < 0) 0 else commentsCount,
                repostsCount = if (repostsCount < 0) 0 else repostsCount,
                isRepost = doc.getBoolean("isRepost") ?: false,
                originalPostId = doc.getString("originalPostId"),
                isLikedByCurrentUser = false,
                isBookmarkedByCurrentUser = false,
                hashtags = hashtags,
                originalPost = null
            )
        }
    }
}
