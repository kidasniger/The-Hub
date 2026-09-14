package com.thehub.hb.data.model

import com.google.firebase.Timestamp

data class LikerUser(
    val uid: String = "",
    val username: String = "",
    val displayName: String? = null,
    val photoUrl: String? = null,
    val likedAt: Timestamp = Timestamp.now()
)
