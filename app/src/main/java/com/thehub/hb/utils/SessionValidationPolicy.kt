package com.thehub.hb.utils

object SessionValidationPolicy {
    fun shouldClearSession(
        isOnline: Boolean,
        isTransientNetworkFailure: Boolean
    ): Boolean = isOnline && !isTransientNetworkFailure
}
