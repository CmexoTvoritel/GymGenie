package com.asc.gymgenie.workout

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

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

            }
        }
    }
}
