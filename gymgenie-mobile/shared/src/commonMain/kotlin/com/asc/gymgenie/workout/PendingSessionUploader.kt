package com.asc.gymgenie.workout

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * Retries any workout sessions that were persisted locally and explicitly
 * marked as finished but never successfully submitted to the backend (e.g.
 * network outage during or right after a workout).
 *
 * Invoked from screens that surface after a session may have completed
 * (currently the Home screen). Failures are silently swallowed so the host
 * surface is never blocked. Per-session failures leave the local rows in
 * place so the next attempt picks them up again.
 *
 * Concurrency:
 *  - The uploader is a process-wide singleton with its own
 *    [SupervisorJob]-backed scope. We do NOT borrow the caller's scope: the
 *    caller may be re-created (e.g. [com.asc.gymgenie.presentation.HomeViewModel]
 *    is a factory) and would otherwise spawn parallel upload loops against
 *    the same local rows, risking duplicate server submissions.
 *  - [tryUploadPending] is idempotent while a previous run is still in
 *    flight — re-entrant calls are dropped.
 *
 * Recovery semantics:
 *  - Only rows with a non-null `finished_at` are considered (see
 *    [LocalWorkoutRepository.getAllPendingSessions]). In-progress sessions
 *    written by an active [com.asc.gymgenie.presentation.WorkoutSessionViewModel]
 *    are filtered out by the SQL layer.
 *  - The persisted `finishedAt` is used in the request, preserving the
 *    original finish time across process restarts. We fall back to "now"
 *    only if the column is unexpectedly null (defensive).
 */
class PendingSessionUploader(
    private val localRepository: LocalWorkoutRepository,
    private val workoutApi: WorkoutApi,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var uploadJob: Job? = null

    fun tryUploadPending() {
        if (uploadJob?.isActive == true) return
        uploadJob = scope.launch {
            val sessions = runCatching { localRepository.getAllPendingSessions() }
                .getOrElse { return@launch }

            for (session in sessions) {
                val sets = runCatching { localRepository.getSetsForSession(session.id) }
                    .getOrElse { continue }

                val request = SubmitWorkoutSessionRequest(
                    name = session.name,
                    workoutPlanDayId = session.planDayId,
                    startedAt = session.startedAt,
                    finishedAt = session.finishedAt ?: Clock.System.now().toEpochMilliseconds(),
                    status = session.status,
                    sets = sets.map { set ->
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

                workoutApi.submitSession(request)
                    .onSuccess {
                        runCatching { localRepository.clearSession(session.id) }
                    }
                // onFailure: leave rows for the next attempt — no log needed.
            }
        }
    }
}
