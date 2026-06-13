package com.asc.gymgenie.user

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserProfileStore(private val userApi: UserApi) {

    private val _profile = MutableStateFlow<UserProfileResponse?>(null)
    val profile: StateFlow<UserProfileResponse?> = _profile.asStateFlow()

    suspend fun load() {
        userApi.getProfile().onSuccess { _profile.value = it }
    }

    fun update(profile: UserProfileResponse) {
        _profile.value = profile
    }

    suspend fun saveProfile(request: UpdateUserProfileRequest): Result<UserProfileResponse> {
        return userApi.updateProfile(request).onSuccess { _profile.value = it }
    }

    fun clear() {
        _profile.value = null
    }
}
