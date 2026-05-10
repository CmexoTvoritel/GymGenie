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

/**
 * Snapshot of an in-flight profile update.
 *
 * Modelled as a single state object — instead of three independent flows —
 * so the UI only ever has to react to one consistent value: at any moment
 * we are either idle, loading, succeeded once (until consumed), or failed
 * with a message (until consumed). Mutually exclusive states are encoded by
 * convention: [isLoading] suppresses [success] and [error] for the duration
 * of the request, and a new request clears any prior [error].
 */
data class ProfileUpdateState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
)

/**
 * Drives profile-edit interactions on the Profile screen.
 *
 * Responsibilities:
 *  - call [UserApi.updateProfile] with a freshly-built request,
 *  - on success, push the authoritative profile returned by the backend into
 *    [UserProfileStore] so any other observer (Home, AI flow, ...) re-renders
 *    without an extra fetch,
 *  - expose [state] so the screen can render loading/error/success once,
 *    while leaving consumption of the success/error flags to the UI to keep
 *    the lifecycle of those one-shot signals explicit.
 *
 * The view model intentionally does not own draft form state — that lives
 * in the screen because it is platform-specific (Compose `MutableState`s,
 * SwiftUI `@State`s) and we don't want to reach into KMM types from the
 * platform UI just to back a few text fields.
 */
class ProfileViewModel(
    private val userApi: UserApi,
    private val userProfileStore: UserProfileStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(ProfileUpdateState())
    val state: StateFlow<ProfileUpdateState> = _state.asStateFlow()

    /**
     * In-flight update job. Tracked explicitly so we never stack concurrent
     * PUTs if the user double-taps "Save" — the second tap is a no-op until
     * the first request resolves.
     */
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
