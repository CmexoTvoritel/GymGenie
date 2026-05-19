package com.asc.gymgenie.presentation

import com.asc.gymgenie.common.ApiException
import com.asc.gymgenie.common.SessionManager
import com.asc.gymgenie.exercise.ExerciseApi
import com.asc.gymgenie.exercise.ExerciseShortResponse
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.workout.ActiveWorkoutSession
import com.asc.gymgenie.workout.WorkoutApi
import com.asc.gymgenie.workout.WorkoutPlanShortResponse
import com.asc.gymgenie.workout.toActiveSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    val isRefreshing: Boolean = false,
    val workoutPlans: List<WorkoutPlanShortResponse> = emptyList(),
    val workoutPlansLoaded: Boolean = false,
    val exercises: List<ExerciseShortResponse> = emptyList(),
    val exercisesLoaded: Boolean = false,
    val searchQuery: String = "",
    val selectedMuscleGroup: String? = null,
    val selectedDifficulties: List<String> = emptyList(),
    val requiresEquipment: Boolean? = null,
    val sortByDifficulty: String? = null,
    val sortByCalories: String? = null,
    val errorMessage: String? = null,
    val hasMoreExercises: Boolean = true,
    val currentExercisePage: Int = 0,
    val isLoadingSession: Boolean = false,
    val pendingSession: ActiveWorkoutSession? = null,
    val sessionError: String? = null,
)

class WorkoutsViewModel(
    private val workoutApi: WorkoutApi,
    private val exerciseApi: ExerciseApi,
    private val tokenStorage: TokenStorage,
    private val sessionManager: SessionManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(WorkoutsUiState())
    val state: StateFlow<WorkoutsUiState> = _state.asStateFlow()

    /**
     * Tracks the in-flight network call so [refresh] cannot stack concurrent
     * requests if the user pulls multiple times in a row.
     */
    private var loadJob: Job? = null

    fun loadWorkoutPlans() {
        // A pull-to-refresh is the source of truth while in flight; we don't
        // want a parallel "after create-plan" reload to race it.
        if (_state.value.isRefreshing) return
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, errorMessage = null) }

        loadJob = scope.launch { runWorkoutPlansLoad(isRefresh = false) }
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

    fun applyFilters(
        difficulties: List<String>,
        requiresEquipment: Boolean?,
        sortByDifficulty: String?,
        sortByCalories: String?,
    ) {
        val s = _state.value
        if (s.selectedDifficulties.toSet() == difficulties.toSet() &&
            s.requiresEquipment == requiresEquipment &&
            s.sortByDifficulty == sortByDifficulty &&
            s.sortByCalories == sortByCalories
        ) return
        _state.update {
            it.copy(
                selectedDifficulties = difficulties,
                requiresEquipment = requiresEquipment,
                sortByDifficulty = sortByDifficulty,
                sortByCalories = sortByCalories,
            )
        }
        loadExercises(reset = true)
    }

    fun loadExercises(reset: Boolean = false) {
        // A pull-to-refresh is the source of truth while in flight: it owns
        // the page reset, so we must not race a parallel pagination/first-load
        // call against it.
        if (_state.value.isRefreshing) return

        if (reset) {
            // Cancel any in-flight load and clear loading flags so the new
            // request can start. Keep `exercises` intact so the UI does not
            // unmount the search bar / filter chips while the next page loads.
            loadJob?.cancel()
            _state.update {
                it.copy(
                    currentExercisePage = 0,
                    hasMoreExercises = true,
                    isLoading = false,
                    isLoadingMore = false,
                )
            }
        }

        val state = _state.value
        if (state.isLoading || state.isLoadingMore || !state.hasMoreExercises) return

        // Pick the indicator based on whether we already have something to show:
        // first ever load → full-screen spinner; refresh of existing list →
        // footer spinner so the header and current items stay on screen.
        if (state.currentExercisePage == 0 && state.exercises.isEmpty()) {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
        } else {
            _state.update { it.copy(isLoadingMore = true, errorMessage = null) }
        }

        loadJob = scope.launch { runExercisesLoad(isRefresh = false) }
    }

    fun loadMoreExercises() {
        loadExercises()
    }

    fun searchExercises() {
        loadExercises(reset = true)
    }

    /**
     * User-initiated background refresh of the currently visible tab.
     *
     * Unlike [load*] entry points this never flips [WorkoutsUiState.isLoading]
     * to true — it keeps existing content on screen and only toggles
     * [WorkoutsUiState.isRefreshing] so the platform pull-to-refresh affordance
     * stays visible. Pagination state for the exercises tab is reset so the
     * user gets the freshest first page; subsequent infinite-scroll triggers
     * continue to work normally afterwards.
     */
    fun refresh() {
        if (loadJob?.isActive == true) return

        when (_state.value.selectedTab) {
            WorkoutsTab.WORKOUTS -> {
                _state.update { it.copy(isRefreshing = true, errorMessage = null) }
                loadJob = scope.launch { runWorkoutPlansLoad(isRefresh = true) }
            }
            WorkoutsTab.EXERCISES -> {
                _state.update {
                    it.copy(
                        isRefreshing = true,
                        errorMessage = null,
                        currentExercisePage = 0,
                        hasMoreExercises = true,
                    )
                }
                loadJob = scope.launch { runExercisesLoad(isRefresh = true) }
            }
        }
    }

    fun retry() {
        _state.update { it.copy(errorMessage = null) }
        when (_state.value.selectedTab) {
            WorkoutsTab.WORKOUTS -> loadWorkoutPlans()
            WorkoutsTab.EXERCISES -> loadExercises(reset = true)
        }
    }

    fun startWorkout(planId: String, planName: String) {
        if (_state.value.isLoadingSession) return
        _state.update { it.copy(isLoadingSession = true, sessionError = null) }
        scope.launch {
            workoutApi.getPlanById(planId).fold(
                onSuccess = { plan ->
                    _state.update {
                        it.copy(
                            isLoadingSession = false,
                            pendingSession = plan.toActiveSession(),
                        )
                    }
                },
                onFailure = { error ->
                    if (shouldLogOut(error)) {
                        tokenStorage.clearTokens()
                        sessionManager.triggerLogout()
                        _state.update { it.copy(isLoadingSession = false) }
                        return@fold
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

    private suspend fun runWorkoutPlansLoad(isRefresh: Boolean) {
        val result = workoutApi.getPlans(page = 0, size = 20)
        result.fold(
            onSuccess = { pagedResponse ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = if (isRefresh) false else it.isRefreshing,
                        workoutPlans = pagedResponse.content,
                        workoutPlansLoaded = true,
                        errorMessage = null,
                    )
                }
            },
            onFailure = { error ->
                if (shouldLogOut(error)) {
                    tokenStorage.clearTokens()
                    sessionManager.triggerLogout()
                    return
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = if (isRefresh) false else it.isRefreshing,
                        errorMessage = "Ошибка загрузки: ${error.message}",
                    )
                }
            },
        )
    }

    private suspend fun runExercisesLoad(isRefresh: Boolean) {
        val query = _state.value.searchQuery.trim()
        val page = _state.value.currentExercisePage
        val result = if (query.isEmpty()) {
            exerciseApi.getExercises(
                muscleGroup = _state.value.selectedMuscleGroup,
                difficultyLevels = _state.value.selectedDifficulties,
                requiresEquipment = _state.value.requiresEquipment,
                sortByDifficulty = _state.value.sortByDifficulty,
                sortByCalories = _state.value.sortByCalories,
                page = page,
                size = 20,
            )
        } else {
            exerciseApi.searchExercises(
                query = query,
                difficultyLevels = _state.value.selectedDifficulties,
                requiresEquipment = _state.value.requiresEquipment,
                sortByDifficulty = _state.value.sortByDifficulty,
                sortByCalories = _state.value.sortByCalories,
                page = page,
                size = 20,
            )
        }

        // If the user switched category/query while the request was in flight,
        // the surrounding job has been cancelled but cooperative cancellation
        // does not interrupt a completed suspending call. Check here before
        // mutating state so a stale result cannot overwrite the new selection.
        currentCoroutineContext().ensureActive()

        result.fold(
            onSuccess = { pagedResponse ->
                val newItems = pagedResponse.content
                _state.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        isRefreshing = if (isRefresh) false else it.isRefreshing,
                        // On refresh we replace the list wholesale; on the
                        // first non-refresh page we also replace; later pages
                        // append to support pagination.
                        exercises = if (isRefresh || page == 0) newItems else it.exercises + newItems,
                        hasMoreExercises = !(pagedResponse.last ?: true),
                        currentExercisePage = page + 1,
                        exercisesLoaded = true,
                        errorMessage = null,
                    )
                }
            },
            onFailure = { error ->
                if (shouldLogOut(error)) {
                    tokenStorage.clearTokens()
                    sessionManager.triggerLogout()
                    return
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        isRefreshing = if (isRefresh) false else it.isRefreshing,
                        errorMessage = "Ошибка загрузки: ${error.message}",
                    )
                }
            },
        )
    }

    private suspend fun shouldLogOut(error: Throwable): Boolean {
        val is401 = (error as? ApiException)?.statusCode == 401
        return is401 || tokenStorage.getAccessToken() == null
    }
}
