package com.thehub.hb.data.model

data class ParticipantInfo(
    val username: String = "",
    val displayName: String? = null,
    val photoUrl: String? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "username" to username,
            "displayName" to displayName,
            "photoUrl" to photoUrl
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>?): ParticipantInfo {
            if (map == null) return ParticipantInfo()
            return ParticipantInfo(
                username = map["username"] as? String ?: "",
                displayName = map["displayName"] as? String,
                photoUrl = map["photoUrl"] as? String
            )
        }
    }
}
