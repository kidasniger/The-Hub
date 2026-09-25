package com.thehub.hb.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

object NetworkStatus {
    fun hasInternet(capabilities: NetworkCapabilities?): Boolean {
        return hasInternet(
            hasInternetCapability = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
            isValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        )
    }

    internal fun hasInternet(
        hasInternetCapability: Boolean,
        isValidated: Boolean
    ): Boolean = hasInternetCapability && isValidated
}

class NetworkConnectivityMonitor(context: Context) {
    private val refreshRequests =
        kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Gives Compose a real initial value instead of briefly assuming the device is offline
     * while callbackFlow is starting its first collection.
     */
    val initialIsOnline: Boolean
        get() = currentStatus()

    val isOnline: Flow<Boolean> = callbackFlow {
        trySend(currentStatus())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(currentStatus())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(NetworkStatus.hasInternet(networkCapabilities))
            }

            override fun onLost(network: Network) {
                trySend(currentStatus())
            }
        }

        try {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } catch (_: SecurityException) {
            trySend(false)
        }

        val refreshJob = launch {
            refreshRequests.collect {
                trySend(currentStatus())
            }
        }

        awaitClose {
            refreshJob.cancel()
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
    }.distinctUntilChanged().conflate()

    fun refresh() {
        refreshRequests.tryEmit(Unit)
    }

    private fun currentStatus(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return NetworkStatus.hasInternet(capabilities)
    }
}
