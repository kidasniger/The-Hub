package com.thehub.hb.data.model

import com.google.firebase.Timestamp

data class BlockedUser(
    val uid: String = "",
    val username: String = "",
    val displayName: String? = null,
    val photoUrl: String? = null,
    val blockedAt: Timestamp = Timestamp.now()
)
