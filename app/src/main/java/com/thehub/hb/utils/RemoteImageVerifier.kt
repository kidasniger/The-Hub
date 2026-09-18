package com.thehub.hb.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Verifies whether remote ImgBB media is still available independently of Coil's
 * memory/disk caches. A network error is treated as unknown so a temporary
 * connection problem never hides an image that is still cached locally.
 */
class RemoteImageVerifier(
    private val client: OkHttpClient = defaultClient()
) {
    private data class CachedCheck(
        val checkedAtMillis: Long,
        val available: Boolean?
    )

    private val cache = ConcurrentHashMap<String, CachedCheck>()

    suspend fun checkIfAvailable(url: String, force: Boolean = false): Boolean? {
        if (!isImgBbUrl(url)) return null

        val now = System.currentTimeMillis()
        if (!force) {
            cache[url]?.let { cached ->
                if (now - cached.checkedAtMillis < CHECK_CACHE_TTL_MILLIS) {
                    return cached.available
                }
            }
        }

        val result = withContext(Dispatchers.IO) {
            performCheck(url)
        }
        cache[url] = CachedCheck(System.currentTimeMillis(), result)
        return result
    }

    private fun performCheck(url: String): Boolean? {
        return try {
            val headRequest = Request.Builder()
                .url(url)
                .head()
                .header("Cache-Control", "no-cache, no-store, max-age=0")
                .header("Pragma", "no-cache")
                .header("Accept", "image/*")
                .build()

            client.newCall(headRequest).execute().use { response ->
                when {
                    response.isSuccessful -> true
                    response.code == 404 || response.code == 410 -> false
                    response.code == 405 || response.code == 501 -> performGetCheck(url)
                    else -> null
                }
            }
        } catch (_: IOException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun performGetCheck(url: String): Boolean? {
        return try {
            val getRequest = Request.Builder()
                .url(url)
                .get()
                .header("Cache-Control", "no-cache, no-store, max-age=0")
                .header("Pragma", "no-cache")
                .header("Accept", "image/*")
                .header("Range", "bytes=0-0")
                .build()

            client.newCall(getRequest).execute().use { response ->
                when {
                    response.isSuccessful -> true
                    response.code == 404 || response.code == 410 -> false
                    else -> null
                }
            }
        } catch (_: IOException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val RECHECK_INTERVAL_MILLIS = 60_000L
        private const val CHECK_CACHE_TTL_MILLIS = 30_000L

        private fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .callTimeout(7, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }

        fun isImgBbUrl(url: String): Boolean {
            val host = try {
                java.net.URI(url).host?.lowercase(Locale.US)
            } catch (_: Exception) {
                null
            } ?: return false

            return host == "ibb.co" ||
                host.endsWith(".ibb.co") ||
                host == "imgbb.com" ||
                host.endsWith(".imgbb.com")
        }
    }
}
