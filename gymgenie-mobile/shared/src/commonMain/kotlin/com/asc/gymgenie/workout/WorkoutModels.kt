package com.asc.gymgenie.workout

import kotlinx.serialization.Serializable

/**
 * Whether a user-defined workout is meant to be performed once (a one-off
 * session) or on a recurring weekly cadence on selected weekdays.
 *
 * Backed by the same string identifiers the backend uses, so it can be safely
 * round-tripped through JSON without a custom serializer.
 */
@Serializable
enum class WorkoutScheduleType {
    ONE_TIME, RECURRING
}

@Serializable
data class WorkoutPlanShortResponse(
    val id: String,
    val name: String,
    val description: String? = null,
    val isActive: Boolean,
    val daysCount: Int = 0,
    val createdBy: String = "USER",
    /**
     * The schedule type the plan was created with. Stored as a plain string
     * to be tolerant of older plans / backend versions that may not yet emit
     * the enum value — falls back to [WorkoutScheduleType.ONE_TIME].
     */
    val scheduleType: String = WorkoutScheduleType.ONE_TIME.name,
)

data class ActiveWorkoutSession(
    val planId: String,
    val planName: String,
    val exercises: List<ActiveExercise>,
    val restSeconds: Int = 60,
)

data class ActiveExercise(
    val exerciseId: String,
    val exerciseName: String,
    val muscleGroupLabel: String,
    val imageUrl: String?,
    val techniqueTip: String?,
    val sets: Int,
    val reps: Int,
    val weightKg: Double?,
)

data class CompletedSet(
    val exerciseIndex: Int,
    val setIndex: Int,
    val repsActual: Int,
    val weightActual: Double,
    val completedAt: Long,
)

/**
 * Transport payload used to create a user-defined workout plan from a list of
 * exercises with per-exercise sets/reps and a global rest timer.
 *
 * [scheduleType] / [scheduleDays] are optional on creation: a one-time workout
 * does not need any days, while a recurring workout must include at least one
 * `DayOfWeek` name (`"MONDAY"`, `"TUESDAY"`, …). The validation lives in the
 * presenter so the rule is enforced consistently from both Android and iOS.
 */
@Serializable
data class CreateSimpleWorkoutRequest(
    val name: String,
    val restSeconds: Int,
    val scheduleType: WorkoutScheduleType = WorkoutScheduleType.ONE_TIME,
    val scheduleDays: List<String> = emptyList(),
    val exercises: List<SimpleWorkoutExerciseItem>,
)

@Serializable
data class SimpleWorkoutExerciseItem(
    val exerciseId: String,
    val sets: Int,
    val reps: Int,
)

/**
 * Request body for `POST /api/v1/workout-sessions/submit`.
 *
 * We submit the full session in one go after the user finishes locally — sets
 * are accumulated on the device through [LocalWorkoutRepository] and only
 * surfaced here at the boundary.
 */
@Serializable
data class SubmitWorkoutSessionRequest(
    val name: String,
    val workoutPlanDayId: String? = null,
    val startedAt: Long,
    val finishedAt: Long,
    val status: String = "COMPLETED",
    val notes: String? = null,
    val sets: List<SubmitSessionSetItem> = emptyList(),
)

@Serializable
data class SubmitSessionSetItem(
    val exerciseId: String,
    val setNumber: Int,
    val reps: Int? = null,
    val weightKg: Double? = null,
    val completed: Boolean = true,
    val durationSeconds: Int? = null,
)

/**
 * Backend acknowledgement after a successful session submit. Only the fields
 * we currently consume on the client are modeled; additional fields returned
 * by the API are tolerated thanks to `ignoreUnknownKeys`.
 */
@Serializable
data class WorkoutSessionResponse(
    val id: String,
    val name: String,
    val status: String,
)

@Serializable
data class WorkoutPlanResponse(
    val id: String,
    val name: String,
    val description: String? = null,
    val createdBy: String = "USER",
    val isActive: Boolean = true,
    val scheduleType: String = "ONE_TIME",
    val days: List<WorkoutPlanDayResponse> = emptyList(),
)

@Serializable
data class WorkoutPlanDayResponse(
    val id: String,
    val dayOfWeek: String,
    val name: String,
    val orderIndex: Int = 0,
    val exercises: List<WorkoutPlanExerciseResponse> = emptyList(),
)

@Serializable
data class WorkoutPlanExerciseResponse(
    val id: String,
    val exerciseId: String,
    val exerciseNameRu: String,
    val exerciseNameEn: String,
    val muscleGroup: String,
    val sets: Int,
    val reps: Int,
    val weightKg: Double? = null,
    val restSeconds: Int = 60,
    val orderIndex: Int = 0,
)

fun WorkoutPlanResponse.toActiveSession(): ActiveWorkoutSession {
    val day = days.minByOrNull { it.orderIndex }
    val exercises = day?.exercises
        ?.sortedBy { it.orderIndex }
        ?.map { ex ->
            ActiveExercise(
                exerciseId = ex.exerciseId,
                exerciseName = ex.exerciseNameRu,
                muscleGroupLabel = ex.muscleGroup,
                imageUrl = null,
                techniqueTip = null,
                sets = ex.sets,
                reps = ex.reps,
                weightKg = ex.weightKg,
            )
        } ?: emptyList()
    val restSeconds = day?.exercises?.firstOrNull()?.restSeconds ?: 60
    return ActiveWorkoutSession(
        planId = id,
        planName = name,
        exercises = exercises,
        restSeconds = restSeconds,
    )
}
