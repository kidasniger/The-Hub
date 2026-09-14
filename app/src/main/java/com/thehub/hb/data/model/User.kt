package com.thehub.hb.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val usernameLower: String = username.lowercase(),
    val displayName: String? = null,
    val photoUrl: String? = null,
    val bio: String? = null,
    val birthdate: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val postsCount: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0
) {
    fun toMap(): Map<String, Any?> {
        val lower = if (usernameLower.isNotBlank()) usernameLower else username.lowercase()
        return mapOf(
            "uid" to uid,
            "email" to email,
            "username" to username,
            "usernameLower" to lower,
            "displayName" to displayName,
            "photoUrl" to photoUrl,
            "bio" to bio,
            "birthdate" to birthdate,
            "createdAt" to createdAt,
            "postsCount" to postsCount,
            "followersCount" to followersCount,
            "followingCount" to followingCount
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): User {
            val uName = map["username"] as? String ?: ""
            val uLower = (map["usernameLower"] as? String)?.takeIf { it.isNotBlank() } ?: uName.lowercase()
            return User(
                uid = map["uid"] as? String ?: "",
                email = map["email"] as? String ?: "",
                username = uName,
                usernameLower = uLower,
                displayName = map["displayName"] as? String,
                photoUrl = map["photoUrl"] as? String,
                bio = map["bio"] as? String,
                birthdate = map["birthdate"] as? String,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                postsCount = (map["postsCount"] as? Number)?.toInt() ?: 0,
                followersCount = (map["followersCount"] as? Number)?.toInt() ?: 0,
                followingCount = (map["followingCount"] as? Number)?.toInt() ?: 0
            )
        }
    }
}
