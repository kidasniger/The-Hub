package com.thehub.hb

import android.net.NetworkCapabilities
import com.thehub.hb.utils.NetworkStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkStatusUnitTest {

    @Test
    fun internet_requires_validated_network() {
        val internetOnly = NetworkCapabilities()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        val validatedInternet = NetworkCapabilities()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        assertFalse(NetworkStatus.hasInternet(null))
        assertFalse(NetworkStatus.hasInternet(internetOnly))
        assertTrue(NetworkStatus.hasInternet(validatedInternet))
    }
}
