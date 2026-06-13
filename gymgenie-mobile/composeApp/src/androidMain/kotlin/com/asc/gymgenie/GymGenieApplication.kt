package com.asc.gymgenie

import android.app.Application
import com.asc.gymgenie.di.databaseModule
import com.asc.gymgenie.di.networkModule
import com.asc.gymgenie.di.profileModule
import com.asc.gymgenie.di.viewModelModule
import com.asc.gymgenie.storage.AndroidTokenStorageContext
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class GymGenieApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        AndroidTokenStorageContext.init(this)

        startKoin {
            androidContext(this@GymGenieApplication)
            modules(networkModule, profileModule, databaseModule, viewModelModule)
        }
    }
}
