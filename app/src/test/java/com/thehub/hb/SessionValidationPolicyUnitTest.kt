package com.thehub.hb

import com.thehub.hb.utils.SessionValidationPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionValidationPolicyUnitTest {

    @Test
    fun offline_failure_never_clears_session() {
        assertFalse(
            SessionValidationPolicy.shouldClearSession(
                isOnline = false,
                isTransientNetworkFailure = true
            )
        )
        assertFalse(
            SessionValidationPolicy.shouldClearSession(
                isOnline = false,
                isTransientNetworkFailure = false
            )
        )
    }

    @Test
    fun transient_online_failure_preserves_session() {
        assertFalse(
            SessionValidationPolicy.shouldClearSession(
                isOnline = true,
                isTransientNetworkFailure = true
            )
        )
    }

    @Test
    fun permanent_online_failure_clears_session() {
        assertTrue(
            SessionValidationPolicy.shouldClearSession(
                isOnline = true,
                isTransientNetworkFailure = false
            )
        )
    }
}
