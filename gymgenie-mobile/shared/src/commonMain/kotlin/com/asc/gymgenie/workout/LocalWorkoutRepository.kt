package com.asc.gymgenie.workout

import com.asc.gymgenie.db.DatabaseDriverFactory
import com.asc.gymgenie.db.GymGenieDatabase
import kotlinx.datetime.Clock

/**
 * Offline-first persistence for an in-progress workout session.
 *
 * Each completed (or skipped) set is written here as soon as the user records
 * it. When the session ends, [getSession] + [getSetsForSession] feed the
 * "submit session" backend call and [clearSession] erases the local rows on
 * success. Failed submits keep the rows around for a future retry.
 *
 * The repository purposely exposes plain domain-shaped data classes
 * ([PendingSession] / [PendingSet]) and never leaks SQLDelight types so that
 * presenters and use cases stay decoupled from the persistence engine.
 */
class LocalWorkoutRepository(driverFactory: DatabaseDriverFactory) {

    private val database = GymGenieDatabase(driverFactory.createDriver())
    private val queries = database.workoutSessionQueries

    /** Persist (or replace) the metadata row for a workout session. */
    fun saveSession(
        sessionId: String,
        planId: String?,
        planDayId: String?,
        name: String,
        startedAtEpochMillis: Long = Clock.System.now().toEpochMilliseconds(),
    ) {
        queries.insertSession(
            id = sessionId,
            plan_id = planId,
            plan_day_id = planDayId,
            name = name,
            started_at = startedAtEpochMillis,
        )
    }

    /** Append a single completed/skipped set to the session. */
    fun saveSet(
        sessionId: String,
        exerciseId: String,
        setNumber: Int,
        reps: Int?,
        weightKg: Double?,
        completed: Boolean,
        durationSeconds: Int?,
    ) {
        queries.insertSet(
            session_id = sessionId,
            exercise_id = exerciseId,
            set_number = setNumber.toLong(),
            reps = reps?.toLong(),
            weight_kg = weightKg,
            completed = if (completed) 1L else 0L,
            duration_seconds = durationSeconds?.toLong(),
        )
    }

    fun getSession(sessionId: String): PendingSession? {
        val row = queries.getSession(sessionId).executeAsOneOrNull() ?: return null
        return PendingSession(
            id = row.id,
            planId = row.plan_id,
            planDayId = row.plan_day_id,
            name = row.name,
            startedAt = row.started_at,
        )
    }

    fun getSetsForSession(sessionId: String): List<PendingSet> {
        return queries.getSetsForSession(sessionId).executeAsList().map { row ->
            PendingSet(
                sessionId = row.session_id,
                exerciseId = row.exercise_id,
                setNumber = row.set_number.toInt(),
                reps = row.reps?.toInt(),
                weightKg = row.weight_kg,
                completed = row.completed == 1L,
                durationSeconds = row.duration_seconds?.toInt(),
            )
        }
    }

    /** Remove session row and all its child sets. Called after a successful submit. */
    fun clearSession(sessionId: String) {
        queries.transaction {
            queries.deleteSetsBySession(sessionId)
            queries.deleteSession(sessionId)
        }
    }

    data class PendingSession(
        val id: String,
        val planId: String?,
        val planDayId: String?,
        val name: String,
        val startedAt: Long,
    )

    data class PendingSet(
        val sessionId: String,
        val exerciseId: String,
        val setNumber: Int,
        val reps: Int?,
        val weightKg: Double?,
        val completed: Boolean,
        val durationSeconds: Int?,
    )
}
