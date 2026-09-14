package com.thehub.hb

import android.app.Application
import com.thehub.hb.di.AppContainer
import com.thehub.hb.di.DefaultAppContainer

class HubApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
