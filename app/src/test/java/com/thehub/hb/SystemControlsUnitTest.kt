package com.thehub.hb

import com.thehub.hb.data.repository.SystemControls
import com.thehub.hb.data.repository.SystemEmergencyState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemControlsUnitTest {

    @Test
    fun defaults_keep_optional_features_available() {
        val controls = SystemControls()
        assertTrue(controls.postsEnabled())
        assertTrue(controls.messagingEnabled())
    }

    @Test
    fun emergency_switch_overrides_feature_availability() {
        val controls = SystemControls(
            emergency = SystemEmergencyState(
                posts = false,
                messaging = false
            )
        )

        assertFalse(controls.postsEnabled())
        assertFalse(controls.messagingEnabled())
    }

    @Test
    fun feature_flags_can_disable_selected_features() {
        val controls = SystemControls(
            featureFlags = mapOf(
                "create_post" to false,
                "messaging" to false
            )
        )

        assertFalse(controls.postsEnabled())
        assertFalse(controls.messagingEnabled())
    }

    @Test
    fun unknown_feature_flags_fail_open() {
        val controls = SystemControls(
            featureFlags = mapOf("unrelated" to false)
        )

        assertTrue(controls.postsEnabled())
        assertTrue(controls.messagingEnabled())
    }
}
