package com.asc.gymgenie.feature.workouts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.asc.gymgenie.ACCESS_TOKEN_KEY
import com.asc.gymgenie.dataStore
import com.asc.gymgenie.exercise.ExerciseApi
import com.asc.gymgenie.exercise.ExerciseShortResponse
import com.asc.gymgenie.workout.WorkoutApi
import com.asc.gymgenie.workout.WorkoutPlanShortResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WorkoutsTab {
    WORKOUTS,
    EXERCISES,
}

data class WorkoutsUiState(
    val selectedTab: WorkoutsTab = WorkoutsTab.WORKOUTS,
    val workoutPlans: List<WorkoutPlanShortResponse> = emptyList(),
    val exercises: List<ExerciseShortResponse> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreExercises: Boolean = true,
    val exercisesLoaded: Boolean = false,
    val errorMessage: String? = null,
)

class WorkoutsViewModel(application: Application) : AndroidViewModel(application) {

    private val workoutApi = WorkoutApi(baseUrl = "http://10.0.2.2:8081/api/v1")
    private val exerciseApi = ExerciseApi(baseUrl = "http://10.0.2.2:8081/api/v1")

    private val _uiState = MutableStateFlow(WorkoutsUiState())
    val uiState: StateFlow<WorkoutsUiState> = _uiState.asStateFlow()

    private var currentExercisePage = 0

    fun selectTab(tab: WorkoutsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (tab == WorkoutsTab.EXERCISES && !_uiState.value.exercisesLoaded) {
            _uiState.update { it.copy(exercisesLoaded = true) }
            loadExercises(reset = true)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun loadWorkoutPlans() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val token = getAccessToken()
            if (token == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Не авторизован") }
                return@launch
            }

            val result = workoutApi.getPlans(token, page = 0, size = 20)
            result.fold(
                onSuccess = { pagedResponse ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            workoutPlans = pagedResponse.content,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Ошибка загрузки: ${error.message}",
                        )
                    }
                },
            )
        }
    }

    fun loadExercises(reset: Boolean = false) {
        val state = _uiState.value
        if (reset) {
            currentExercisePage = 0
            _uiState.update {
                it.copy(
                    exercises = emptyList(),
                    hasMoreExercises = true,
                )
            }
        }

        if (state.isLoading || state.isLoadingMore || !state.hasMoreExercises) return

        if (currentExercisePage == 0) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        } else {
            _uiState.update { it.copy(isLoadingMore = true) }
        }

        viewModelScope.launch {
            val query = _uiState.value.searchQuery.trim()
            val result = if (query.isEmpty()) {
                exerciseApi.getExercises(page = currentExercisePage, size = 20)
            } else {
                exerciseApi.searchExercises(query = query, page = currentExercisePage, size = 20)
            }

            result.fold(
                onSuccess = { pagedResponse ->
                    val newItems = pagedResponse.content
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            exercises = if (currentExercisePage == 0) newItems else it.exercises + newItems,
                            hasMoreExercises = !pagedResponse.last,
                        )
                    }
                    currentExercisePage++
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            errorMessage = "Ошибка загрузки: ${error.message}",
                        )
                    }
                },
            )
        }
    }

    fun loadMoreExercises() {
        loadExercises()
    }

    fun searchExercises() {
        loadExercises(reset = true)
    }

    fun retry() {
        when (_uiState.value.selectedTab) {
            WorkoutsTab.WORKOUTS -> loadWorkoutPlans()
            WorkoutsTab.EXERCISES -> loadExercises(reset = true)
        }
    }

    private suspend fun getAccessToken(): String? {
        return getApplication<Application>().dataStore.data
            .map { prefs -> prefs[ACCESS_TOKEN_KEY] }
            .first()
    }
}
