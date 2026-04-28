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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * Drives a single in-progress workout session.
 *
 * Offline-first behavior: every recorded set is persisted to the local
 * SQLDelight DB through [localRepository] as it happens. Only when the
 * session completes do we POST a single submit request to the backend; on
 * failure the local rows stay around so a future surface can retry the
 * upload.
 *
 * The view model intentionally owns no UI concerns — Android Compose and
 * iOS SwiftUI both observe [state] and call action methods.
 */
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
        val restSecondsRemaining: Int = 0,
        val currentWeight: Double = 0.0,
        val currentReps: Int = 12,
        val completedSets: List<CompletedSet> = emptyList(),
        val isFinished: Boolean = false,
        val sessionDurationSeconds: Int = 0,
        val isSubmitting: Boolean = false,
        val submitError: String? = null,
        val isSubmitted: Boolean = false,
    ) {
        val currentExercise: ActiveExercise? get() = session.exercises.getOrNull(currentExerciseIndex)
        val nextExercise: ActiveExercise? get() = session.exercises.getOrNull(currentExerciseIndex + 1)
        val totalSets: Int get() = currentExercise?.sets ?: 0
        val displaySetNumber: Int get() = currentSetIndex + 1
        val totalExercises: Int get() = session.exercises.size
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val sessionStartedAtEpochMillis = Clock.System.now().toEpochMilliseconds()

    private val _state = MutableStateFlow(
        State(
            sessionId = sessionIdSeed,
            session = session,
            currentWeight = session.exercises.firstOrNull()?.weightKg ?: 60.0,
            currentReps = session.exercises.firstOrNull()?.reps ?: 12,
        ),
    )
    val state: StateFlow<State> = _state.asStateFlow()

    private var restTimerJob: Job? = null
    private var durationJob: Job? = null

    init {
        // Persist the session metadata up-front so that the rows we write per
        // set always have a parent to point at.
        localRepository.saveSession(
            sessionId = sessionIdSeed,
            planId = session.planId.takeUnless { it.isBlank() },
            planDayId = null,
            name = session.planName,
            startedAtEpochMillis = sessionStartedAtEpochMillis,
        )
        startDurationTimer()
    }

    private fun startDurationTimer() {
        durationJob = scope.launch {
            while (true) {
                delay(1000)
                _state.value = _state.value.copy(
                    sessionDurationSeconds = _state.value.sessionDurationSeconds + 1,
                )
            }
        }
    }

    fun completeSet() {
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
            durationJob?.cancel()
            submitWorkout()
            return
        }

        _state.value = current.copy(
            completedSets = updatedSets,
            phase = Phase.REST,
            restSecondsRemaining = current.session.restSeconds,
        )
        startRestTimer()
    }

    private fun startRestTimer() {
        restTimerJob?.cancel()
        restTimerJob = scope.launch {
            while (true) {
                delay(1000)
                val current = _state.value.restSecondsRemaining
                if (current <= 0) break
                _state.value = _state.value.copy(restSecondsRemaining = current - 1)
            }
            if (_state.value.restSecondsRemaining <= 0) {
                advanceToNextSet()
            }
        }
    }

    fun skipRest() {
        restTimerJob?.cancel()
        advanceToNextSet()
    }

    /**
     * Mark the current set as skipped and advance to the next set.
     *
     * A skipped set is recorded as a [CompletedSet] with `repsActual = 0` and
     * `weightActual = 0.0`. We intentionally do not introduce a separate
     * `skipped` flag on the model: callers that need to detect skipped sets can
     * check `repsActual == 0 && weightActual == 0.0`. The persisted DB row
     * carries the explicit `completed = false` flag for downstream analytics.
     *
     * If invoked during the rest phase, the rest timer is cancelled before
     * advancing so the UI immediately moves to the next exercise/set.
     */
    fun skipSet() {
        restTimerJob?.cancel()
        val current = _state.value
        val skippedSet = CompletedSet(
            exerciseIndex = current.currentExerciseIndex,
            setIndex = current.currentSetIndex,
            repsActual = 0,
            weightActual = 0.0,
            completedAt = currentTimeMillis(),
        )

        val exercise = current.currentExercise
        if (exercise != null) {
            persistSet(
                exercise = exercise,
                setIndex = current.currentSetIndex,
                reps = 0,
                weightKg = 0.0,
                completed = false,
            )
        }
        val isLastSet = exercise != null && current.currentSetIndex >= exercise.sets - 1
        val isLastExercise = current.currentExerciseIndex >= current.session.exercises.size - 1

        if (isLastSet && isLastExercise) {
            _state.value = current.copy(
                completedSets = current.completedSets + skippedSet,
                isFinished = true,
            )
            durationJob?.cancel()
            submitWorkout()
            return
        }

        _state.value = current.copy(completedSets = current.completedSets + skippedSet)
        advanceToNextSet()
    }

    fun adjustRest(deltaSeconds: Int) {
        val newVal = (_state.value.restSecondsRemaining + deltaSeconds).coerceAtLeast(5)
        _state.value = _state.value.copy(restSecondsRemaining = newVal)
        // Restart timer so it picks up the new value from state
        if (_state.value.phase == Phase.REST) {
            startRestTimer()
        }
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
                currentWeight = nextExercise?.weightKg ?: current.currentWeight,
                currentReps = nextExercise?.reps ?: current.currentReps,
            )
        } else {
            _state.value = current.copy(
                phase = Phase.EXERCISE,
                currentSetIndex = current.currentSetIndex + 1,
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

    /**
     * Re-attempts the backend submit when a previous attempt failed. Safe to
     * call from a "retry" UI affordance because [submitWorkout] guards
     * against parallel/duplicate submissions.
     */
    fun retrySubmit() {
        if (!_state.value.isFinished) return
        if (_state.value.isSubmitting || _state.value.isSubmitted) return
        submitWorkout()
    }

    fun dispose() {
        scope.cancel()
    }

    // -- Internals ------------------------------------------------------------

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
        // Failures here are intentionally swallowed: the in-memory state is
        // still authoritative for the rest of the session and we don't want
        // a transient sqlite error to derail the UX.
    }

    private fun submitWorkout() {
        val current = _state.value
        if (current.isSubmitting || current.isSubmitted) return

        val pendingSession = localRepository.getSession(current.sessionId) ?: return
        val pendingSets = localRepository.getSetsForSession(current.sessionId)

        _state.update { it.copy(isSubmitting = true, submitError = null) }

        scope.launch {
            val request = SubmitWorkoutSessionRequest(
                name = pendingSession.name,
                workoutPlanDayId = pendingSession.planDayId,
                startedAt = pendingSession.startedAt,
                finishedAt = Clock.System.now().toEpochMilliseconds(),
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
        /**
         * Generates a stable, unique session id without pulling in
         * `kotlin.uuid.Uuid` (still beta as of Kotlin 2.3 in this project).
         * The combination of monotonic millis + a random suffix is more than
         * enough to disambiguate concurrent local sessions on a single device.
         */
        fun generateSessionId(): String {
            val now = Clock.System.now().toEpochMilliseconds()
            val random = (0..9).joinToString("") { kotlin.random.Random.nextInt(0, 36).toString(36) }
            return "ws-$now-$random"
        }
    }
}
