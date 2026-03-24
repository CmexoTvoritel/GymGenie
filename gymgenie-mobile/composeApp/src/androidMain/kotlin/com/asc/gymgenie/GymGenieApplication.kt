package com.asc.gymgenie

import android.app.Application
import com.asc.gymgenie.storage.AndroidTokenStorageContext

class GymGenieApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidTokenStorageContext.init(this)
    }
}
