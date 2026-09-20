package com.thehub.hb

import android.app.Application
import android.content.Context
import android.os.Environment
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.thehub.hb.di.AppContainer
import com.thehub.hb.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class HubApplication : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    private var currentImageLoader: ImageLoader? = null
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pushRegistrationUserId: String? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        ensureFirebaseInitialized(this)
        container = DefaultAppContainer(this)
        applicationScope.launch {
            cleanupDownloadedUpdateApks(this@HubApplication)
        }

        applicationScope.launch {
            container.authRepository.currentUserFlow.collect { firebaseUser ->
                val previousUserId = pushRegistrationUserId
                val currentUserId = firebaseUser?.uid

                if (previousUserId != null && previousUserId != currentUserId) {
                    container.notificationRepository.unregisterFcmTokenForUser(previousUserId)
                }

                if (currentUserId != null) {
                    container.notificationRepository.registerCurrentFcmToken()
                }

                pushRegistrationUserId = currentUserId
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val response = chain.proceed(chain.request())
                if (response.code == 404 || response.code == 410) {
                    throw java.io.IOException("Image deleted on remote host (HTTP ${response.code})")
                }
                response
            }
            .build()

        val loader = ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .respectCacheHeaders(true)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache_v3"))
                    .maxSizeBytes(40L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
        currentImageLoader = loader
        return loader
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    private fun clearInternalImageCache() {
        try {
            currentImageLoader?.memoryCache?.clear()
            currentImageLoader?.diskCache?.clear()
        } catch (_: Exception) {}
    }

    companion object {
        private var instance: HubApplication? = null

        fun clearImageCache(context: Context? = null) {
            instance?.clearInternalImageCache()
        }

        fun cleanupDownloadedUpdateApks(context: Context) {
            val downloadDir =
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return
            val installedVersion = BuildConfig.VERSION_NAME

            downloadDir.listFiles()
                ?.filter { it.isFile && it.extension.equals("apk", ignoreCase = true) }
                ?.forEach { apkFile ->
                    val packageInfo = try {
                        @Suppress("DEPRECATION")
                        context.packageManager.getPackageArchiveInfo(
                            apkFile.absolutePath,
                            0
                        )
                    } catch (e: Exception) {
                        Log.w(
                            "HubApplication",
                            "Could not inspect cached APK: ${apkFile.absolutePath}",
                            e
                        )
                        null
                    }

                    if (packageInfo?.packageName != context.packageName) {
                        return@forEach
                    }

                    val cachedVersion = packageInfo.versionName?.trim().orEmpty()
                    if (cachedVersion.isNotEmpty() &&
                        isVersionAtLeast(
                            installedVersion = installedVersion,
                            candidateVersion = cachedVersion
                        )
                    ) {
                        try {
                            if (apkFile.delete()) {
                                Log.i(
                                    "HubApplication",
                                    "Removed obsolete update APK ${apkFile.name} " +
                                        "(installed=$installedVersion, cached=$cachedVersion)"
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(
                                "HubApplication",
                                "Could not delete obsolete update APK: ${apkFile.absolutePath}",
                                e
                            )
                        }
                    }
                }
        }

        private fun isVersionAtLeast(
            installedVersion: String,
            candidateVersion: String
        ): Boolean {
            val installed = versionParts(installedVersion)
            val candidate = versionParts(candidateVersion)
            val maxSize = maxOf(installed.size, candidate.size)

            for (index in 0 until maxSize) {
                val installedPart = installed.getOrElse(index) { 0 }
                val candidatePart = candidate.getOrElse(index) { 0 }

                if (installedPart > candidatePart) return true
                if (installedPart < candidatePart) return false
            }

            return true
        }

        private fun versionParts(raw: String): List<Int> {
            return raw.trim()
                .removePrefix("v")
                .removePrefix("V")
                .substringBefore("-")
                .split(".")
                .map { it.toIntOrNull() ?: 0 }
        }

        fun ensureFirebaseInitialized(context: Context) {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val app = try {
                    FirebaseApp.initializeApp(context)
                } catch (e: Exception) {
                    Log.w("HubApplication", "Default FirebaseApp init failed: ${e.message}")
                    null
                }
                if (app == null && FirebaseApp.getApps(context).isEmpty()) {
                    try {
                        val options = FirebaseOptions.Builder()
                            .setApplicationId("1:183373607979:android:ab8055f86cb765a66de66e")
                            .setApiKey("AIzaSyDEhYwtXx6PejTL2RbJcjpzCBKkJPYLAtg")
                            .setProjectId("the-hub-f95f4")
                            .setStorageBucket("the-hub-f95f4.firebasestorage.app")
                            .build()
                        FirebaseApp.initializeApp(context, options)
                        Log.i("HubApplication", "FirebaseApp initialized with fallback options")
                    } catch (e: Exception) {
                        Log.e("HubApplication", "Fallback FirebaseApp init failed: ${e.message}")
                    }
                }
            }
        }
    }
}
