package com.asc.gymgenie.presentation

import com.asc.gymgenie.currentTimeMillis
import com.asc.gymgenie.workout.ActiveExercise
import com.asc.gymgenie.workout.ActiveWorkoutSession
import com.asc.gymgenie.workout.CompletedSet
import com.asc.gymgenie.workout.LocalWorkoutRepository
import com.asc.gymgenie.workout.SubmitSessionSetItem
import com.asc.gymgenie.workout.SubmitWorkoutSessionRequest
import com.asc.gymgenie.workout.WorkoutApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class WorkoutSessionViewModel(
    private val session: ActiveWorkoutSession,
    private val localRepository: LocalWorkoutRepository,
    private val workoutApi: WorkoutApi,
    private val sessionIdSeed: String = generateSessionId(),
) {

    enum class Phase { EXERCISE, REST }

    data class State(
        val sessionId: String,
        val session: ActiveWorkoutSession,
        val currentExerciseIndex: Int = 0,
        val currentSetIndex: Int = 0,
        val phase: Phase = Phase.EXERCISE,
        val restDurationSeconds: Int = 0,
        val currentWeight: Double = 0.0,
        val currentReps: Int = 12,
        val completedSets: List<CompletedSet> = emptyList(),
        val isFinished: Boolean = false,
        val isSubmitting: Boolean = false,
        val submitError: String? = null,
        val isSubmitted: Boolean = false,
    ) {
        val currentExercise: ActiveExercise? get() = session.exercises.getOrNull(currentExerciseIndex)
        val nextExercise: ActiveExercise? get() = session.exercises.getOrNull(currentExerciseIndex + 1)
        val totalSets: Int get() = currentExercise?.sets ?: 0
        val displaySetNumber: Int get() = currentSetIndex + 1
        val totalExercises: Int get() = session.exercises.size
        val requiresWeight: Boolean get() = currentExercise?.weightKg != null
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val sessionStartedAtEpochMillis = Clock.System.now().toEpochMilliseconds()

    private val _state = MutableStateFlow(
        run {
            val firstEx = session.exercises.firstOrNull()
            State(
                sessionId = sessionIdSeed,
                session = session,
                currentWeight = firstEx?.setWeightsKg?.getOrNull(0) ?: firstEx?.weightKg ?: 0.0,
                currentReps = firstEx?.reps ?: 12,
            )
        },
    )
    val state: StateFlow<State> = _state.asStateFlow()

    init {

        localRepository.saveSession(
            sessionId = sessionIdSeed,
            planId = session.planId.takeUnless { it.isBlank() },
            planDayId = null,
            name = session.planName,
            startedAtEpochMillis = sessionStartedAtEpochMillis,
        )
    }

    fun completeSet(elapsedSeconds: Int) {
        val current = _state.value
        val newSet = CompletedSet(
            exerciseIndex = current.currentExerciseIndex,
            setIndex = current.currentSetIndex,
            repsActual = current.currentReps,
            weightActual = current.currentWeight,
            completedAt = currentTimeMillis(),
        )
        val updatedSets = current.completedSets + newSet

        val exercise = current.currentExercise ?: return
        persistSet(
            exercise = exercise,
            setIndex = current.currentSetIndex,
            reps = current.currentReps,
            weightKg = current.currentWeight,
            completed = true,
        )

        val isLastSet = current.currentSetIndex >= exercise.sets - 1
        val isLastExercise = current.currentExerciseIndex >= current.session.exercises.size - 1

        if (isLastSet && isLastExercise) {
            _state.value = current.copy(completedSets = updatedSets, isFinished = true)
            submitWorkout()
            return
        }

        _state.value = current.copy(
            completedSets = updatedSets,
            phase = Phase.REST,
            restDurationSeconds = current.session.restSeconds,
        )
    }

    fun restComplete() {
        if (_state.value.phase != Phase.REST) return
        advanceToNextSet()
    }

    fun skipRest() {
        advanceToNextSet()
    }

    fun skipSet() {
        val current = _state.value
        val exercise = current.currentExercise ?: return
        val isLastSet = current.currentSetIndex >= exercise.sets - 1
        val isLastExercise = current.currentExerciseIndex >= current.session.exercises.size - 1

        if (isLastSet && isLastExercise) {
            _state.value = current.copy(isFinished = true)
            submitWorkout()
            return
        }

        if (current.phase == Phase.REST) {
            advanceToNextSet()
            val after = _state.value
            val afterExercise = after.currentExercise
            if (afterExercise == null ||
                (after.currentSetIndex >= afterExercise.sets - 1 &&
                    after.currentExerciseIndex >= after.session.exercises.size - 1)
            ) {
                _state.value = after.copy(isFinished = true)
                submitWorkout()
                return
            }
            _state.value = after.copy(
                phase = Phase.REST,
                restDurationSeconds = current.restDurationSeconds,
            )
            return
        }

        advanceToNextSet()
    }

    fun adjustRest(deltaSeconds: Int) {
        val newDuration = (_state.value.restDurationSeconds + deltaSeconds).coerceAtLeast(5)
        _state.value = _state.value.copy(restDurationSeconds = newDuration)
    }

    private fun advanceToNextSet() {
        val current = _state.value
        val exercise = current.currentExercise ?: return
        val isLastSet = current.currentSetIndex >= exercise.sets - 1

        if (isLastSet) {
            val nextIndex = current.currentExerciseIndex + 1
            val nextExercise = current.session.exercises.getOrNull(nextIndex)
            _state.value = current.copy(
                phase = Phase.EXERCISE,
                currentExerciseIndex = nextIndex,
                currentSetIndex = 0,
                currentWeight = nextExercise?.setWeightsKg?.getOrNull(0)
                    ?: nextExercise?.weightKg
                    ?: 0.0,
                currentReps = nextExercise?.reps ?: current.currentReps,
            )
        } else {
            _state.value = current.copy(
                phase = Phase.EXERCISE,
                currentSetIndex = current.currentSetIndex + 1,
                currentWeight = exercise.setWeightsKg?.getOrNull(current.currentSetIndex + 1)
                    ?: exercise.weightKg
                    ?: current.currentWeight,
            )
        }
    }

    fun adjustWeight(delta: Double) {
        val newWeight = (_state.value.currentWeight + delta).coerceAtLeast(0.0)
        _state.value = _state.value.copy(currentWeight = newWeight)
    }

    fun adjustReps(delta: Int) {
        val newReps = (_state.value.currentReps + delta).coerceAtLeast(1)
        _state.value = _state.value.copy(currentReps = newReps)
    }

    fun cancelWorkout(totalDurationSeconds: Int) {
        val current = _state.value
        if (current.isFinished) return

        val finishedAtMillis = Clock.System.now().toEpochMilliseconds()
        runCatching { localRepository.markSessionFinishedWithStatus(current.sessionId, finishedAtMillis, "CANCELLED") }

        _state.update { it.copy(isFinished = true) }

        val pendingSession = localRepository.getSession(current.sessionId) ?: return
        val pendingSets = localRepository.getSetsForSession(current.sessionId)

        _state.update { it.copy(isSubmitting = true, submitError = null) }

        scope.launch {
            val request = SubmitWorkoutSessionRequest(
                name = pendingSession.name,
                workoutPlanDayId = pendingSession.planDayId,
                startedAt = pendingSession.startedAt,
                finishedAt = finishedAtMillis,
                status = "CANCELLED",
                sets = pendingSets.map { set ->
                    SubmitSessionSetItem(
                        exerciseId = set.exerciseId,
                        setNumber = set.setNumber,
                        reps = set.reps,
                        weightKg = set.weightKg,
                        completed = set.completed,
                        durationSeconds = set.durationSeconds,
                    )
                },
                totalPlannedSets = session.exercises.sumOf { it.sets },
                totalPlannedExercises = session.exercises.size,
            )
            workoutApi.submitSession(request).fold(
                onSuccess = {
                    runCatching { localRepository.clearSession(current.sessionId) }
                    _state.update { it.copy(isSubmitting = false, isSubmitted = true) }
                },
                onFailure = {
                    _state.update { it.copy(isSubmitting = false, submitError = "Не удалось сохранить.") }
                },
            )
        }
    }

    fun retrySubmit() {
        if (!_state.value.isFinished) return
        if (_state.value.isSubmitting || _state.value.isSubmitted) return
        submitWorkout()
    }

    fun dispose() {
        scope.cancel()
    }

    private fun persistSet(
        exercise: ActiveExercise,
        setIndex: Int,
        reps: Int,
        weightKg: Double,
        completed: Boolean,
    ) {
        runCatching {
            localRepository.saveSet(
                sessionId = _state.value.sessionId,
                exerciseId = exercise.exerciseId,
                setNumber = setIndex + 1,
                reps = reps,
                weightKg = weightKg,
                completed = completed,
                durationSeconds = null,
            )
        }

    }

    private fun submitWorkout() {
        val current = _state.value
        if (current.isSubmitting || current.isSubmitted) return

        val pendingSession = localRepository.getSession(current.sessionId) ?: return
        val pendingSets = localRepository.getSetsForSession(current.sessionId)

        val finishedAtMillis = Clock.System.now().toEpochMilliseconds()
        runCatching { localRepository.markSessionFinished(current.sessionId, finishedAtMillis) }

        _state.update { it.copy(isSubmitting = true, submitError = null) }

        scope.launch {
            val request = SubmitWorkoutSessionRequest(
                name = pendingSession.name,
                workoutPlanDayId = pendingSession.planDayId,
                startedAt = pendingSession.startedAt,
                finishedAt = finishedAtMillis,
                status = "COMPLETED",
                sets = pendingSets.map { set ->
                    SubmitSessionSetItem(
                        exerciseId = set.exerciseId,
                        setNumber = set.setNumber,
                        reps = set.reps,
                        weightKg = set.weightKg,
                        completed = set.completed,
                        durationSeconds = set.durationSeconds,
                    )
                },
                totalPlannedSets = session.exercises.sumOf { it.sets },
                totalPlannedExercises = session.exercises.size,
            )

            workoutApi.submitSession(request).fold(
                onSuccess = {
                    runCatching { localRepository.clearSession(current.sessionId) }
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            isSubmitted = true,
                            submitError = null,
                        )
                    }
                },
                onFailure = {
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            submitError = "Не удалось сохранить тренировку. Попробуйте позже.",
                        )
                    }
                },
            )
        }
    }

    private companion object {

        fun generateSessionId(): String {
            val now = Clock.System.now().toEpochMilliseconds()
            val random = (0..9).joinToString("") { kotlin.random.Random.nextInt(0, 36).toString(36) }
            return "ws-$now-$random"
        }
    }
}
