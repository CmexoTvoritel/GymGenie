package com.asc.gymgenie.user

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory holder for the authenticated user's profile.
 *
 * Provides a single shared source of truth that any presenter (Home, AI flow,
 * Profile, ...) can observe or push into. Keeping this as a thin process-scoped
 * cache lets unrelated screens read the latest profile without each one issuing
 * its own request, while still leaving the underlying [UserApi] as the only
 * authority for fetching from the backend.
 */
class UserProfileStore(private val userApi: UserApi) {

    private val _profile = MutableStateFlow<UserProfileResponse?>(null)
    val profile: StateFlow<UserProfileResponse?> = _profile.asStateFlow()

    /**
     * Best-effort refresh from the backend. Failures are intentionally
     * swallowed: the store is a cache, callers that need user-visible error
     * handling should keep using [UserApi.getProfile] directly.
     */
    suspend fun load() {
        userApi.getProfile().onSuccess { _profile.value = it }
    }

    fun update(profile: UserProfileResponse) {
        _profile.value = profile
    }

    fun clear() {
        _profile.value = null
    }
}
