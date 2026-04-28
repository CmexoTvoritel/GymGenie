package com.asc.gymgenie.storage

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

@SuppressLint("StaticFieldLeak")
object AndroidTokenStorageContext {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    internal fun requireContext(): Context {
        return appContext ?: error(
            "AndroidTokenStorageContext is not initialized. " +
                "Call AndroidTokenStorageContext.init(context) in Application.onCreate()."
        )
    }
}

private class AndroidTokenStorage : TokenStorage {

    private val prefs: SharedPreferences
        get() = AndroidTokenStorageContext.requireContext()
            .getSharedPreferences("gymgenie_tokens", Context.MODE_PRIVATE)

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    override suspend fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    override suspend fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    override suspend fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .commit()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}

actual fun createTokenStorage(): TokenStorage = AndroidTokenStorage()
