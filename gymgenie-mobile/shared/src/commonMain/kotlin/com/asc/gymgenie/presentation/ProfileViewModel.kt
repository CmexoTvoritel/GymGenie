package com.asc.gymgenie.presentation

import com.asc.gymgenie.user.UpdateUserProfileRequest
import com.asc.gymgenie.user.UserApi
import com.asc.gymgenie.user.UserProfileStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUpdateState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
)

class ProfileViewModel(
    private val userApi: UserApi,
    private val userProfileStore: UserProfileStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(ProfileUpdateState())
    val state: StateFlow<ProfileUpdateState> = _state.asStateFlow()

    private var updateJob: Job? = null

    fun updateProfile(request: UpdateUserProfileRequest) {
        if (updateJob?.isActive == true) return
        _state.update { it.copy(isLoading = true, error = null, success = false) }
        updateJob = scope.launch {
            userApi.updateProfile(request).fold(
                onSuccess = { profile ->
                    userProfileStore.update(profile)
                    _state.update { it.copy(isLoading = false, success = true, error = null) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            success = false,
                            error = error.message ?: "Не удалось обновить профиль",
                        )
                    }
                },
            )
        }
    }

    fun consumeSuccess() {
        _state.update { it.copy(success = false) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun onCleared() {
        scope.cancel()
    }
}
