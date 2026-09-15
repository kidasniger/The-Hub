package com.thehub.hb

import android.app.Application
import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.thehub.hb.di.AppContainer
import com.thehub.hb.di.DefaultAppContainer
import okhttp3.OkHttpClient

class HubApplication : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    private var currentImageLoader: ImageLoader? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        ensureFirebaseInitialized(this)
        container = DefaultAppContainer(this)
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                // If remote host (like ImgBB) returns 404 or 410, the image was deleted
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
