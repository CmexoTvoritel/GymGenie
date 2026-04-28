package com.asc.gymgenie.presentation

import com.asc.gymgenie.common.ApiException
import com.asc.gymgenie.exercise.ExerciseApi
import com.asc.gymgenie.exercise.ExerciseShortResponse
import com.asc.gymgenie.storage.TokenStorage
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

enum class WorkoutsTab {
    WORKOUTS,
    EXERCISES,
}

data class WorkoutsUiState(
    val selectedTab: WorkoutsTab = WorkoutsTab.WORKOUTS,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val workoutPlans: List<WorkoutPlanShortResponse> = emptyList(),
    val workoutPlansLoaded: Boolean = false,
    val exercises: List<ExerciseShortResponse> = emptyList(),
    val exercisesLoaded: Boolean = false,
    val searchQuery: String = "",
    val selectedMuscleGroup: String? = null,
    val errorMessage: String? = null,
    val hasMoreExercises: Boolean = true,
    val currentExercisePage: Int = 0,
)

class WorkoutsViewModel(
    private val workoutApi: WorkoutApi,
    private val exerciseApi: ExerciseApi,
    private val tokenStorage: TokenStorage,
    private val onLogout: () -> Unit = {},
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(WorkoutsUiState())
    val state: StateFlow<WorkoutsUiState> = _state.asStateFlow()

    fun loadWorkoutPlans() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, errorMessage = null) }

        scope.launch {
            val result = workoutApi.getPlans(page = 0, size = 20)
            result.fold(
                onSuccess = { pagedResponse ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            workoutPlans = pagedResponse.content,
                            workoutPlansLoaded = true,
                            errorMessage = null,
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
                        it.copy(
                            isLoading = false,
                            errorMessage = "Ошибка загрузки: ${error.message}",
                        )
                    }
                },
            )
        }
    }

    fun selectTab(tab: WorkoutsTab) {
        _state.update { it.copy(selectedTab = tab, errorMessage = null) }
        if (tab == WorkoutsTab.EXERCISES && !_state.value.exercisesLoaded) {
            _state.update { it.copy(exercisesLoaded = true) }
            loadExercises(reset = true)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun filterByMuscleGroup(group: String?) {
        if (_state.value.selectedMuscleGroup == group) return
        _state.update { it.copy(selectedMuscleGroup = group) }
        loadExercises(reset = true)
    }

    fun loadExercises(reset: Boolean = false) {
        if (reset) {
            _state.update {
                it.copy(
                    currentExercisePage = 0,
                    exercises = emptyList(),
                    hasMoreExercises = true,
                )
            }
        }

        val state = _state.value
        if (state.isLoading || state.isLoadingMore || !state.hasMoreExercises) return

        if (state.currentExercisePage == 0) {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
        } else {
            _state.update { it.copy(isLoadingMore = true) }
        }

        scope.launch {
            val query = _state.value.searchQuery.trim()
            val page = _state.value.currentExercisePage
            val result = if (query.isEmpty()) {
                exerciseApi.getExercises(
                    muscleGroup = _state.value.selectedMuscleGroup,
                    page = page,
                    size = 20,
                )
            } else {
                exerciseApi.searchExercises(
                    query = query,
                    page = page,
                    size = 20,
                )
            }

            result.fold(
                onSuccess = { pagedResponse ->
                    val newItems = pagedResponse.content
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            exercises = if (page == 0) newItems else it.exercises + newItems,
                            hasMoreExercises = !(pagedResponse.last ?: true),
                            currentExercisePage = page + 1,
                            errorMessage = null,
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
        _state.update { it.copy(errorMessage = null) }
        when (_state.value.selectedTab) {
            WorkoutsTab.WORKOUTS -> loadWorkoutPlans()
            WorkoutsTab.EXERCISES -> loadExercises(reset = true)
        }
    }

    fun onCleared() {
        scope.cancel()
    }

    private suspend fun shouldLogOut(error: Throwable): Boolean {
        val is401 = (error as? ApiException)?.statusCode == 401
        return is401 || tokenStorage.getAccessToken() == null
    }
}
