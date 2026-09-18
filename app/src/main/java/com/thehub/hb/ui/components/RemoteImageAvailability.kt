package com.thehub.hb.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.thehub.hb.utils.RemoteImageVerifier
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val remoteImageVerifier = RemoteImageVerifier()

/**
 * Returns true only when ImgBB has explicitly confirmed that the image is gone.
 * Other failures are left unresolved so transient network errors do not hide
 * a cached image.
 */
@Composable
fun rememberRemoteImageUnavailable(imageUrl: String): Boolean {
    var unavailable by remember(imageUrl) { mutableStateOf(false) }

    LaunchedEffect(imageUrl) {
        while (currentCoroutineContext().isActive && !unavailable) {
            when (remoteImageVerifier.checkIfAvailable(imageUrl)) {
                false -> {
                    unavailable = true
                    break
                }
                true, null -> Unit
            }

            delay(RemoteImageVerifier.RECHECK_INTERVAL_MILLIS)
        }
    }

    return unavailable
}
