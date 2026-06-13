package com.asc.gymgenie.presentation

import com.asc.gymgenie.common.ApiException
import com.asc.gymgenie.common.SessionManager
import com.asc.gymgenie.exercise.ExerciseShortResponse
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.workout.SimpleWorkoutExerciseItem
import com.asc.gymgenie.workout.UpdateWorkoutPlanRequest
import com.asc.gymgenie.workout.WorkoutApi
import com.asc.gymgenie.workout.WorkoutPlanResponse
import com.asc.gymgenie.workout.WorkoutScheduleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkoutDetailUiState(
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,

    val plan: WorkoutPlanResponse? = null,

    val editName: String = "",
    val editDescription: String = "",
    val editScheduleType: WorkoutScheduleType = WorkoutScheduleType.ONE_TIME,
    val editScheduleDays: Set<String> = emptySet(),
    val editRestSeconds: Int = CreateWorkoutLimits.DEFAULT_REST_SECONDS,
    val editExercises: List<PendingExercise> = emptyList(),
)

class WorkoutDetailViewModel(
    private val planId: String,
    private val workoutApi: WorkoutApi,
    private val tokenStorage: TokenStorage,
    private val sessionManager: SessionManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(WorkoutDetailUiState())
    val state: StateFlow<WorkoutDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        scope.launch { runLoad() }
    }

    fun retry() {
        _state.update { it.copy(errorMessage = null) }
        load()
    }

    fun startEditing() {
        val plan = _state.value.plan ?: return
        _state.update { current ->
            current.copy(
                isEditing = true,
                isSaved = false,
                errorMessage = null,
                editName = plan.name,
                editDescription = plan.description.orEmpty(),
                editScheduleType = parseScheduleType(plan.scheduleType),
                editScheduleDays = collectScheduleDays(plan),
                editRestSeconds = collectRestSeconds(plan),
                editExercises = collectPendingExercises(plan),
            )
        }
    }

    fun cancelEditing() {
        _state.update { it.copy(isEditing = false, errorMessage = null) }
    }

    fun setEditName(name: String) {
        _state.update { it.copy(editName = name) }
    }

    fun setEditDescription(description: String) {
        _state.update { it.copy(editDescription = description) }
    }

    fun setEditScheduleType(type: WorkoutScheduleType) {
        _state.update { current ->
            if (current.editScheduleType == type) return@update current
            current.copy(
                editScheduleType = type,
                editScheduleDays = if (type == WorkoutScheduleType.ONE_TIME) {
                    emptySet()
                } else {
                    current.editScheduleDays
                },
            )
        }
    }

    fun toggleEditScheduleDay(day: String) {
        _state.update { current ->
            val updated = if (day in current.editScheduleDays) {
                current.editScheduleDays - day
            } else {
                current.editScheduleDays + day
            }
            current.copy(editScheduleDays = updated)
        }
    }

    fun setEditRestSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(
            CreateWorkoutLimits.MIN_REST_SECONDS,
            CreateWorkoutLimits.MAX_REST_SECONDS,
        )
        _state.update { it.copy(editRestSeconds = clamped) }
    }

    fun incrementEditRestSeconds() {
        setEditRestSeconds(_state.value.editRestSeconds + CreateWorkoutLimits.REST_STEP_SECONDS)
    }

    fun decrementEditRestSeconds() {
        setEditRestSeconds(_state.value.editRestSeconds - CreateWorkoutLimits.REST_STEP_SECONDS)
    }

    fun addExercise(exercise: ExerciseShortResponse) {

        val seededWeights: List<Double?>? = if (exercise.requiresWeight) {
            List(CreateWorkoutLimits.DEFAULT_SETS) { CreateWorkoutLimits.DEFAULT_WEIGHT_KG }
        } else {
            null
        }
        val pending = PendingExercise(
            exerciseId = exercise.id,
            exerciseNameRu = exercise.nameRu,
            exerciseNameEn = exercise.nameEn,
            muscleGroupKey = exercise.muscleGroup,
            sets = CreateWorkoutLimits.DEFAULT_SETS,
            reps = CreateWorkoutLimits.DEFAULT_REPS,
            requiresWeight = exercise.requiresWeight,
            setWeightsKg = seededWeights,
        )
        _state.update { it.copy(editExercises = it.editExercises + pending) }
    }

    fun addPendingExercise(exercise: PendingExercise) {
        val normalized = exercise.copy(
            sets = exercise.sets.coerceIn(
                CreateWorkoutLimits.MIN_SETS,
                CreateWorkoutLimits.MAX_SETS,
            ),
            reps = exercise.reps.coerceIn(
                CreateWorkoutLimits.MIN_REPS,
                CreateWorkoutLimits.MAX_REPS,
            ),
        )
        _state.update { it.copy(editExercises = it.editExercises + normalized) }
    }

    fun removeExercise(index: Int) {
        _state.update { current ->
            if (index !in current.editExercises.indices) return@update current
            current.copy(
                editExercises = current.editExercises.toMutableList().apply { removeAt(index) },
            )
        }
    }

    fun updatePendingExerciseAt(index: Int, updated: PendingExercise) {
        _state.update { current ->
            if (index !in current.editExercises.indices) return@update current
            val clampedSets = updated.sets.coerceIn(
                CreateWorkoutLimits.MIN_SETS,
                CreateWorkoutLimits.MAX_SETS,
            )
            val clampedReps = updated.reps.coerceIn(
                CreateWorkoutLimits.MIN_REPS,
                CreateWorkoutLimits.MAX_REPS,
            )
            val normalizedWeights: List<Double?>? = when {
                !updated.requiresWeight -> null
                updated.setWeightsKg == null ->
                    List(clampedSets) { CreateWorkoutLimits.DEFAULT_WEIGHT_KG }
                updated.setWeightsKg.size == clampedSets ->
                    updated.setWeightsKg.map { it?.coerceIn(CreateWorkoutLimits.MIN_WEIGHT_KG, CreateWorkoutLimits.MAX_WEIGHT_KG) }
                updated.setWeightsKg.size < clampedSets -> {
                    val padded = updated.setWeightsKg
                        .map { it?.coerceIn(CreateWorkoutLimits.MIN_WEIGHT_KG, CreateWorkoutLimits.MAX_WEIGHT_KG) }
                    padded + List(clampedSets - padded.size) {
                        padded.lastOrNull() ?: CreateWorkoutLimits.DEFAULT_WEIGHT_KG
                    }
                }
                else -> updated.setWeightsKg
                    .take(clampedSets)
                    .map { it?.coerceIn(CreateWorkoutLimits.MIN_WEIGHT_KG, CreateWorkoutLimits.MAX_WEIGHT_KG) }
            }
            current.copy(
                editExercises = current.editExercises.toMutableList().apply {
                    this[index] = updated.copy(
                        sets = clampedSets,
                        reps = clampedReps,
                        setWeightsKg = normalizedWeights,
                    )
                },
            )
        }
    }

    fun moveExercise(fromIndex: Int, toIndex: Int) {
        _state.update { current ->
            if (fromIndex !in current.editExercises.indices || toIndex !in current.editExercises.indices) return@update current
            val list = current.editExercises.toMutableList()
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            current.copy(editExercises = list)
        }
    }

    fun consumeSavedFlag() {
        _state.update { it.copy(isSaved = false) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun saveEdit() {
        val current = _state.value
        if (current.isSaving) return

        val name = current.editName.trim()
        if (name.isEmpty()) {
            _state.update { it.copy(errorMessage = "Введите название тренировки") }
            return
        }
        if (current.editExercises.isEmpty()) {
            _state.update { it.copy(errorMessage = "Добавьте хотя бы одно упражнение") }
            return
        }
        if (current.editScheduleType == WorkoutScheduleType.RECURRING && current.editScheduleDays.isEmpty()) {
            _state.update { it.copy(errorMessage = "Выберите хотя бы один день недели") }
            return
        }

        _state.update { it.copy(isSaving = true, errorMessage = null) }

        val description = current.editDescription.trim()
        val request = UpdateWorkoutPlanRequest(
            name = name,
            description = description.ifEmpty { null },
            scheduleType = current.editScheduleType,
            scheduleDays = if (current.editScheduleType == WorkoutScheduleType.RECURRING) {
                current.editScheduleDays.toList()
            } else {
                emptyList()
            },
            restSeconds = current.editRestSeconds,
            exercises = current.editExercises.map {
                SimpleWorkoutExerciseItem(
                    exerciseId = it.exerciseId,
                    sets = it.sets,
                    reps = it.reps,
                    setWeightsKg = it.setWeightsKg,
                )
            },
        )

        scope.launch {
            workoutApi.updatePlan(planId, request).fold(
                onSuccess = { updated ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            isEditing = false,
                            isSaved = true,
                            errorMessage = null,
                            plan = updated,
                        )
                    }
                },
                onFailure = { error ->
                    if (handleAuthError(error)) return@launch
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "Не удалось сохранить: ${error.message}",
                        )
                    }
                },
            )
        }
    }

    fun deletePlan() {
        if (_state.value.isDeleting || _state.value.isDeleted) return
        _state.update { it.copy(isDeleting = true, errorMessage = null) }
        scope.launch {
            workoutApi.deletePlan(planId).fold(
                onSuccess = {
                    _state.update {
                        it.copy(isDeleting = false, isDeleted = true, errorMessage = null)
                    }
                },
                onFailure = { error ->
                    if (handleAuthError(error)) return@launch
                    _state.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = "Не удалось удалить: ${error.message}",
                        )
                    }
                },
            )
        }
    }

    fun onCleared() {
        scope.cancel()
    }

    private suspend fun runLoad() {
        workoutApi.getPlanById(planId).fold(
            onSuccess = { plan ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        plan = plan,
                        errorMessage = null,
                    )
                }
            },
            onFailure = { error ->
                if (handleAuthError(error)) return
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Не удалось загрузить тренировку: ${error.message}",
                    )
                }
            },
        )
    }

    private suspend fun handleAuthError(error: Throwable): Boolean {
        val is401 = (error as? ApiException)?.statusCode == 401
        val noToken = tokenStorage.getAccessToken() == null
        return if (is401 || noToken) {
            tokenStorage.clearTokens()
            sessionManager.triggerLogout()
            true
        } else {
            false
        }
    }

    private fun parseScheduleType(value: String): WorkoutScheduleType {
        return runCatching { WorkoutScheduleType.valueOf(value.uppercase()) }
            .getOrDefault(WorkoutScheduleType.ONE_TIME)
    }

    private fun collectScheduleDays(plan: WorkoutPlanResponse): Set<String> {
        return plan.days
            .map { it.dayOfWeek.uppercase() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun collectRestSeconds(plan: WorkoutPlanResponse): Int {
        val perExerciseRest = plan.days.flatMap { it.exercises }.map { it.restSeconds }
        if (perExerciseRest.isEmpty()) return CreateWorkoutLimits.DEFAULT_REST_SECONDS
        val mostCommon = perExerciseRest
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
        return (mostCommon ?: perExerciseRest.first()).coerceIn(
            CreateWorkoutLimits.MIN_REST_SECONDS,
            CreateWorkoutLimits.MAX_REST_SECONDS,
        )
    }

    private fun collectPendingExercises(plan: WorkoutPlanResponse): List<PendingExercise> {

        val referenceDay = plan.days.minByOrNull { it.orderIndex } ?: return emptyList()
        return referenceDay.exercises
            .sortedBy { it.orderIndex }
            .map { ex ->
                val perSetWeights: List<Double?>? = when {
                    ex.setWeightsKg != null -> ex.setWeightsKg
                    ex.weightKg != null -> List(ex.sets) { ex.weightKg }
                    else -> null
                }
                PendingExercise(
                    exerciseId = ex.exerciseId,
                    exerciseNameRu = ex.exerciseNameRu,
                    exerciseNameEn = ex.exerciseNameEn,
                    muscleGroupKey = ex.muscleGroup,
                    sets = ex.sets,
                    reps = ex.reps,
                    requiresWeight = perSetWeights != null,
                    setWeightsKg = perSetWeights,
                )
            }
    }
}
