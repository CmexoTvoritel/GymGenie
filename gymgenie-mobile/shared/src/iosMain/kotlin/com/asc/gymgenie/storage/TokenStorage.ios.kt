package com.asc.gymgenie.storage

import platform.Foundation.NSUserDefaults

private class IosTokenStorage : TokenStorage {

    private val defaults: NSUserDefaults
        get() = NSUserDefaults.standardUserDefaults

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        defaults.setObject(accessToken, forKey = KEY_ACCESS_TOKEN)
        defaults.setObject(refreshToken, forKey = KEY_REFRESH_TOKEN)
    }

    override suspend fun getAccessToken(): String? {
        return defaults.stringForKey(KEY_ACCESS_TOKEN)
    }

    override suspend fun getRefreshToken(): String? {
        return defaults.stringForKey(KEY_REFRESH_TOKEN)
    }

    override suspend fun clearTokens() {
        defaults.removeObjectForKey(KEY_ACCESS_TOKEN)
        defaults.removeObjectForKey(KEY_REFRESH_TOKEN)
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}

actual fun createTokenStorage(): TokenStorage = IosTokenStorage()
