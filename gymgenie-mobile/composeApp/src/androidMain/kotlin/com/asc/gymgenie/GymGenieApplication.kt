package com.asc.gymgenie

import android.app.Application
import com.asc.gymgenie.di.networkModule
import com.asc.gymgenie.di.profileModule
import com.asc.gymgenie.storage.AndroidTokenStorageContext
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class GymGenieApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Must run before Koin resolves `TokenStorage`, since the Android
        // implementation reads from the application context lazily on first use.
        AndroidTokenStorageContext.init(this)

        startKoin {
            androidContext(this@GymGenieApplication)
            modules(networkModule, profileModule)
        }
    }
}
