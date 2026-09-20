package com.thehub.hb.data.model

import com.google.firebase.Timestamp

data class PresenceState(
    val online: Boolean = false,
    val lastSeen: Timestamp? = null
) {
    fun isFresh(nowSeconds: Long, freshnessSeconds: Long = 90L): Boolean {
        val seen = lastSeen?.seconds ?: return online
        return online && nowSeconds - seen <= freshnessSeconds
    }
}
