package com.thehub.hb

import android.net.NetworkCapabilities
import com.thehub.hb.utils.NetworkStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkStatusUnitTest {

    @Test
    fun internet_requires_validated_network() {
        val internetOnly = NetworkCapabilities.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val validatedInternet = NetworkCapabilities.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        assertFalse(NetworkStatus.hasInternet(null))
        assertFalse(NetworkStatus.hasInternet(internetOnly))
        assertTrue(NetworkStatus.hasInternet(validatedInternet))
    }
}
