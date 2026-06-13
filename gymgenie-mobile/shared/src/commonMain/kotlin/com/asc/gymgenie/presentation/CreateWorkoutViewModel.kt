package com.asc.gymgenie.presentation

import com.asc.gymgenie.exercise.ExerciseApi
import com.asc.gymgenie.exercise.MuscleGroupInfo
import com.asc.gymgenie.workout.CreateSimpleWorkoutRequest
import com.asc.gymgenie.workout.SimpleWorkoutExerciseItem
import com.asc.gymgenie.workout.WorkoutApi
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

object CreateWorkoutLimits {
    const val MIN_SETS = 1
    const val MAX_SETS = 10
    const val DEFAULT_SETS = 3

    const val MIN_REPS = 2
    const val MAX_REPS = 25
    const val DEFAULT_REPS = 12

    const val MIN_REST_SECONDS = 10
    const val MAX_REST_SECONDS = 600
    const val REST_STEP_SECONDS = 5
    const val DEFAULT_REST_SECONDS = 60

    const val MIN_WEIGHT_KG = 0.0
    const val MAX_WEIGHT_KG = 500.0
    const val DEFAULT_WEIGHT_KG = 20.0
    const val WEIGHT_STEP_KG = 2.5
}

data class PendingExercise(
    val exerciseId: String,
    val exerciseNameRu: String,
    val exerciseNameEn: String,
    val muscleGroupKey: String,
    val sets: Int,
    val reps: Int,
    val requiresWeight: Boolean = false,

    val setWeightsKg: List<Double?>? = null,
)

data class CreateWorkoutUiState(
    val workoutName: String = "",
    val description: String = "",
    val restSeconds: Int = CreateWorkoutLimits.DEFAULT_REST_SECONDS,
    val exercises: List<PendingExercise> = emptyList(),
    val muscleGroups: List<MuscleGroupInfo> = emptyList(),
    val isMuscleGroupsLoading: Boolean = false,
    val isMuscleGroupsLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val scheduleType: WorkoutScheduleType = WorkoutScheduleType.ONE_TIME,
    val scheduleDays: Set<String> = emptySet(),
)

class CreateWorkoutViewModel(
    private val exerciseApi: ExerciseApi,
    private val workoutApi: WorkoutApi,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(CreateWorkoutUiState())
    val state: StateFlow<CreateWorkoutUiState> = _state.asStateFlow()

    fun loadMuscleGroups(forceReload: Boolean = false) {
        val current = _state.value
        if (current.isMuscleGroupsLoading) return
        if (current.isMuscleGroupsLoaded && !forceReload) return

        _state.update { it.copy(isMuscleGroupsLoading = true, errorMessage = null) }

        scope.launch {
            exerciseApi.getMuscleGroups().fold(
                onSuccess = { groups ->
                    _state.update {
                        it.copy(
                            isMuscleGroupsLoading = false,
                            isMuscleGroupsLoaded = true,
                            muscleGroups = groups,
                            errorMessage = null,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isMuscleGroupsLoading = false,
                            errorMessage = "Ошибка загрузки: ${error.message}",
                        )
                    }
                },
            )
        }
    }

    fun setWorkoutName(name: String) {
        _state.update { it.copy(workoutName = name) }
    }

    fun setDescription(description: String) {
        _state.update { it.copy(description = description) }
    }

    fun setRestSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(
            CreateWorkoutLimits.MIN_REST_SECONDS,
            CreateWorkoutLimits.MAX_REST_SECONDS,
        )
        _state.update { it.copy(restSeconds = clamped) }
    }

    fun incrementRestSeconds() {
        val next = _state.value.restSeconds + CreateWorkoutLimits.REST_STEP_SECONDS
        setRestSeconds(next)
    }

    fun decrementRestSeconds() {
        val next = _state.value.restSeconds - CreateWorkoutLimits.REST_STEP_SECONDS
        setRestSeconds(next)
    }

    fun setScheduleType(type: WorkoutScheduleType) {
        _state.update {
            if (it.scheduleType == type) it
            else it.copy(scheduleType = type, scheduleDays = emptySet())
        }
    }

    fun toggleScheduleDay(day: String) {
        _state.update { current ->
            val updated = if (day in current.scheduleDays)
                current.scheduleDays - day
            else
                current.scheduleDays + day
            current.copy(scheduleDays = updated)
        }
    }

    fun addExercise(exercise: PendingExercise) {
        _state.update { it.copy(exercises = it.exercises + normalizeExercise(exercise)) }
    }

    fun updateExerciseAt(index: Int, updated: PendingExercise) {
        _state.update { current ->
            if (index !in current.exercises.indices) return@update current
            current.copy(
                exercises = current.exercises.toMutableList().apply {
                    this[index] = normalizeExercise(updated)
                },
            )
        }
    }

    fun removeExerciseAt(index: Int) {
        _state.update { current ->
            if (index !in current.exercises.indices) return@update current
            current.copy(
                exercises = current.exercises.toMutableList().apply { removeAt(index) },
            )
        }
    }

    private fun normalizeExercise(exercise: PendingExercise): PendingExercise {
        val clampedSets = exercise.sets.coerceIn(CreateWorkoutLimits.MIN_SETS, CreateWorkoutLimits.MAX_SETS)
        val clampedReps = exercise.reps.coerceIn(CreateWorkoutLimits.MIN_REPS, CreateWorkoutLimits.MAX_REPS)
        val normalizedWeights: List<Double?>? = when {
            !exercise.requiresWeight -> null
            exercise.setWeightsKg == null -> List(clampedSets) { CreateWorkoutLimits.DEFAULT_WEIGHT_KG }
            exercise.setWeightsKg.size == clampedSets -> exercise.setWeightsKg.map { clampWeight(it) }
            exercise.setWeightsKg.size < clampedSets -> {
                val padded = exercise.setWeightsKg.map { clampWeight(it) }
                padded + List(clampedSets - padded.size) { padded.lastOrNull() ?: CreateWorkoutLimits.DEFAULT_WEIGHT_KG }
            }
            else -> exercise.setWeightsKg.take(clampedSets).map { clampWeight(it) }
        }
        return exercise.copy(sets = clampedSets, reps = clampedReps, setWeightsKg = normalizedWeights)
    }

    private fun clampWeight(value: Double?): Double? = value?.coerceIn(
        CreateWorkoutLimits.MIN_WEIGHT_KG,
        CreateWorkoutLimits.MAX_WEIGHT_KG,
    )

    fun saveWorkout() {
        val current = _state.value
        if (current.isSaving || current.isSaved) return

        val name = current.workoutName.trim()
        if (name.isEmpty()) {
            _state.update { it.copy(errorMessage = "Введите название тренировки") }
            return
        }
        if (current.exercises.isEmpty()) {
            _state.update { it.copy(errorMessage = "Добавьте хотя бы одно упражнение") }
            return
        }
        if (current.scheduleType == WorkoutScheduleType.RECURRING && current.scheduleDays.isEmpty()) {
            _state.update { it.copy(errorMessage = "Выберите хотя бы один день недели") }
            return
        }

        _state.update { it.copy(isSaving = true, errorMessage = null) }

        scope.launch {
            val request = CreateSimpleWorkoutRequest(
                name = name,
                description = current.description.trim().ifEmpty { null },
                restSeconds = current.restSeconds,
                scheduleType = current.scheduleType,
                scheduleDays = if (current.scheduleType == WorkoutScheduleType.RECURRING) {
                    current.scheduleDays.toList()
                } else {
                    emptyList()
                },
                exercises = current.exercises.map {
                    SimpleWorkoutExerciseItem(
                        exerciseId = it.exerciseId,
                        sets = it.sets,
                        reps = it.reps,
                        setWeightsKg = it.setWeightsKg,
                    )
                },
            )

            workoutApi.createSimpleWorkout(request).fold(
                onSuccess = {
                    _state.update {
                        it.copy(isSaving = false, isSaved = true, errorMessage = null)
                    }
                },
                onFailure = { error ->
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

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun reset() {
        _state.value = CreateWorkoutUiState()
    }

    fun onCleared() {
        scope.cancel()
    }
}
