package com.thehub.hb.data.model

data class UserInfo(
    val uid: String = "",
    val displayName: String? = null,
    val username: String = "",
    val photoUrl: String? = null,
    val isVerified: Boolean = false,
    val verificationType: String? = null
) {
    /**
     * Display name according to rule:
     * Display displayName rather than @username.
     * Falls back to username without '@' if displayName is blank, or "Utilisateur".
     */
    val effectiveName: String
        get() = displayName?.trim()?.takeIf { it.isNotBlank() }
            ?: username.trim().removePrefix("@").takeIf { it.isNotBlank() }
            ?: "Utilisateur"
}
