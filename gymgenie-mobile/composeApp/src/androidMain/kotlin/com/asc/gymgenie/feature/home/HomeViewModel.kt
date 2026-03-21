package com.asc.gymgenie.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.asc.gymgenie.ACCESS_TOKEN_KEY
import com.asc.gymgenie.dataStore
import com.asc.gymgenie.user.UserApi
import com.asc.gymgenie.user.UserProfileResponse
import com.asc.gymgenie.workout.WorkoutApi
import com.asc.gymgenie.workout.WorkoutPlanShortResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val userProfile: UserProfileResponse? = null,
    val activeWorkoutPlans: List<WorkoutPlanShortResponse> = emptyList(),
    val errorMessage: String? = null,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val userApi = UserApi(baseUrl = "http://10.0.2.2:8081/api/v1")
    private val workoutApi = WorkoutApi(baseUrl = "http://10.0.2.2:8081/api/v1")

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadData() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val token = getAccessToken()
            if (token == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Не авторизован") }
                return@launch
            }

            // Load profile
            val profileResult = userApi.getProfile(token)
            profileResult.fold(
                onSuccess = { profile ->
                    _uiState.update { it.copy(userProfile = profile) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(errorMessage = "Ошибка загрузки профиля: ${error.message}")
                    }
                },
            )

            // Load active plans
            val plansResult = workoutApi.getActivePlans(token)
            plansResult.fold(
                onSuccess = { plans ->
                    _uiState.update { it.copy(activeWorkoutPlans = plans) }
                },
                onFailure = { error ->
                    if (_uiState.value.errorMessage == null) {
                        _uiState.update {
                            it.copy(errorMessage = "Ошибка загрузки планов: ${error.message}")
                        }
                    }
                },
            )

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun retry() {
        loadData()
    }

    private suspend fun getAccessToken(): String? {
        return getApplication<Application>().dataStore.data
            .map { prefs -> prefs[ACCESS_TOKEN_KEY] }
            .first()
    }
}
