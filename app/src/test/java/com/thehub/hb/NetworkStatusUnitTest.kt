package com.thehub.hb

import android.net.NetworkCapabilities
import com.thehub.hb.utils.NetworkStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkStatusUnitTest {

    @Test
    fun internet_requires_validated_network() {
        assertFalse(NetworkStatus.hasInternet(false, false))
        assertFalse(NetworkStatus.hasInternet(true, false))
        assertTrue(NetworkStatus.hasInternet(true, true))
    }
}
