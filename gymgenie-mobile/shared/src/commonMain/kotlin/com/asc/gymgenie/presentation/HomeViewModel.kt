package com.asc.gymgenie.presentation

import com.asc.gymgenie.common.ApiException
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.user.UserApi
import com.asc.gymgenie.user.UserProfileResponse
import com.asc.gymgenie.workout.ActiveWorkoutSession
import com.asc.gymgenie.workout.WorkoutApi
import com.asc.gymgenie.workout.WorkoutPlanShortResponse
import com.asc.gymgenie.workout.toActiveSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val username: String = "",
    val subscriptionType: String = "FREE",
    val streakDays: Int = 0,
    val activeWorkoutPlans: List<WorkoutPlanShortResponse> = emptyList(),
    val userProfile: UserProfileResponse? = null,
    val pendingSession: ActiveWorkoutSession? = null,
    val isLoadingSession: Boolean = false,
    val sessionError: String? = null,
)

class HomeViewModel(
    private val userApi: UserApi,
    private val workoutApi: WorkoutApi,
    private val tokenStorage: TokenStorage,
    private val onLogout: () -> Unit = {},
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun load() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, errorMessage = null) }

        scope.launch {
            if (tokenStorage.getAccessToken() == null) {
                onLogout()
                return@launch
            }

            // Load profile. The injected UserApi uses the authenticated client,
            // so a 401 may have already triggered a refresh + retry by the time
            // we get a result here. If we still see a 401, the refresh failed
            // and the user must re-authenticate.
            val profileResult = userApi.getProfile()
            profileResult.fold(
                onSuccess = { profile ->
                    _state.update {
                        it.copy(
                            userProfile = profile,
                            username = profile.username,
                            subscriptionType = profile.subscriptionType,
                        )
                    }
                },
                onFailure = { error ->
                    if (shouldLogOut(error)) {
                        tokenStorage.clearTokens()
                        onLogout()
                        return@launch
                    }
                    _state.update {
                        it.copy(errorMessage = "Ошибка загрузки профиля: ${error.message}")
                    }
                },
            )

            // Load active workout plans.
            val plansResult = workoutApi.getActivePlans()
            plansResult.fold(
                onSuccess = { plans ->
                    _state.update { it.copy(activeWorkoutPlans = plans) }
                },
                onFailure = { error ->
                    if (shouldLogOut(error)) {
                        tokenStorage.clearTokens()
                        onLogout()
                        return@launch
                    }
                    if (_state.value.errorMessage == null) {
                        _state.update {
                            it.copy(errorMessage = "Ошибка загрузки планов: ${error.message}")
                        }
                    }
                },
            )

            _state.update { it.copy(isLoading = false) }
        }
    }

    fun retry() {
        load()
    }

    fun startWorkout(planId: String, planName: String) {
        if (_state.value.isLoadingSession) return
        _state.update { it.copy(isLoadingSession = true, sessionError = null) }
        scope.launch {
            if (tokenStorage.getAccessToken() == null) {
                _state.update { it.copy(isLoadingSession = false) }
                onLogout()
                return@launch
            }
            workoutApi.getPlanById(planId).fold(
                onSuccess = { plan ->
                    _state.update {
                        it.copy(isLoadingSession = false, pendingSession = plan.toActiveSession())
                    }
                },
                onFailure = { error ->
                    if (shouldLogOut(error)) {
                        tokenStorage.clearTokens()
                        _state.update { it.copy(isLoadingSession = false) }
                        onLogout()
                        return@launch
                    }
                    _state.update {
                        it.copy(
                            isLoadingSession = false,
                            sessionError = "Не удалось загрузить тренировку: ${error.message}",
                        )
                    }
                },
            )
        }
    }

    fun clearPendingSession() {
        _state.update { it.copy(pendingSession = null, sessionError = null) }
    }

    fun onCleared() {
        scope.cancel()
    }

    /**
     * Auth failures fall into two cases that both demand a forced logout:
     *  1. The Ktor bearer plugin already cleared tokens after a failed refresh.
     *  2. The endpoint itself returned 401 for any other reason (e.g. revoked
     *     session) and we must drop the user back to login instead of showing
     *     a misleading error banner.
     */
    private suspend fun shouldLogOut(error: Throwable): Boolean {
        val is401 = (error as? ApiException)?.statusCode == 401
        return is401 || tokenStorage.getAccessToken() == null
    }
}
