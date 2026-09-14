package com.thehub.hb

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.thehub.hb.di.AppContainer
import com.thehub.hb.di.DefaultAppContainer

class HubApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        ensureFirebaseInitialized(this)
        container = DefaultAppContainer(this)
    }

    companion object {
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
