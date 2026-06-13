package com.asc.gymgenie.workout

import com.asc.gymgenie.db.DatabaseDriverFactory
import com.asc.gymgenie.db.GymGenieDatabase
import kotlinx.datetime.Clock

class LocalWorkoutRepository(driverFactory: DatabaseDriverFactory) {

    private val database = GymGenieDatabase(driverFactory.createDriver())
    private val queries = database.workoutSessionQueries

    fun saveSession(
        sessionId: String,
        planId: String?,
        planDayId: String?,
        name: String,
        startedAtEpochMillis: Long = Clock.System.now().toEpochMilliseconds(),
        status: String = "COMPLETED",
    ) {
        queries.insertSession(
            id = sessionId,
            plan_id = planId,
            plan_day_id = planDayId,
            name = name,
            started_at = startedAtEpochMillis,
            status = status,
        )
    }

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

    fun markSessionFinished(sessionId: String, finishedAtEpochMillis: Long) {
        queries.markSessionFinished(finishedAtEpochMillis, sessionId)
    }

    fun markSessionFinishedWithStatus(sessionId: String, finishedAtEpochMillis: Long, status: String) {
        queries.transaction {
            queries.markSessionFinished(finishedAtEpochMillis, sessionId)
            queries.updateSessionStatus(status, sessionId)
        }
    }

    fun updateSessionStatus(sessionId: String, status: String) {
        queries.updateSessionStatus(status, sessionId)
    }

    fun getSession(sessionId: String): PendingSession? {
        val row = queries.getSession(sessionId).executeAsOneOrNull() ?: return null
        return PendingSession(
            id = row.id,
            planId = row.plan_id,
            planDayId = row.plan_day_id,
            name = row.name,
            startedAt = row.started_at,
            finishedAt = row.finished_at,
            status = row.status,
        )
    }

    fun getAllPendingSessions(): List<PendingSession> {
        return queries.getAllSessions().executeAsList().map { row ->
            PendingSession(
                id = row.id,
                planId = row.plan_id,
                planDayId = row.plan_day_id,
                name = row.name,
                startedAt = row.started_at,
                finishedAt = row.finished_at,
                status = row.status,
            )
        }
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
        val finishedAt: Long? = null,
        val status: String = "COMPLETED",
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
