package com.asc.gymgenie.presentation

import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.user.UserApi
import com.asc.gymgenie.user.UserProfileResponse
import com.asc.gymgenie.workout.WorkoutApi
import com.asc.gymgenie.workout.WorkoutPlanShortResponse
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
)

class HomeViewModel(
    private val userApi: UserApi = UserApi(),
    private val workoutApi: WorkoutApi = WorkoutApi(),
    private val tokenStorage: TokenStorage,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun load() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, errorMessage = null) }

        scope.launch {
            val token = tokenStorage.getAccessToken()
            if (token == null) {
                _state.update {
                    it.copy(isLoading = false, errorMessage = "Не авторизован")
                }
                return@launch
            }

            // Load profile
            val profileResult = userApi.getProfile(token)
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
                    _state.update {
                        it.copy(errorMessage = "Ошибка загрузки профиля: ${error.message}")
                    }
                },
            )

            // Load active workout plans
            val plansResult = workoutApi.getActivePlans(token)
            plansResult.fold(
                onSuccess = { plans ->
                    _state.update { it.copy(activeWorkoutPlans = plans) }
                },
                onFailure = { error ->
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

    fun onCleared() {
        scope.cancel()
    }
}
