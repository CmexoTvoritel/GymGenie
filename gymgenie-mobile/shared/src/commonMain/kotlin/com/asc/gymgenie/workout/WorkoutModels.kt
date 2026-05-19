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
    /**
     * `DayOfWeek` enum names (e.g. `"MONDAY"`) — populated only when
     * [scheduleType] is `RECURRING`. Empty otherwise.
     */
    val scheduleDays: List<String> = emptyList(),
    val restSeconds: Int = 60,
    /**
     * Backend-derived primary muscle group of the plan, used by the catalog
     * card to colour the icon tile and badge. Stored as the raw enum name
     * (e.g. `"CHEST"`) so the mapping table lives entirely on the client.
     */
    val primaryMuscleGroup: String? = null,
    val exercisesCount: Int = 0,
    val totalSets: Int = 0,
    val estimatedMinutes: Int = 0,
)

/**
 * Partial-update payload for `PUT /api/v1/workout-plans/{id}`.
 *
 * Every field is nullable on purpose: `null` means "do not modify". The full
 * exercise list is on the other hand replace-or-untouched — passing an empty
 * list explicitly clears the plan, which is intentional and matches the
 * backend contract.
 */
@Serializable
data class UpdateWorkoutPlanRequest(
    val name: String? = null,
    val description: String? = null,
    val isActive: Boolean? = null,
    val scheduleType: WorkoutScheduleType? = null,
    val scheduleDays: List<String>? = null,
    val restSeconds: Int? = null,
    val exercises: List<SimpleWorkoutExerciseItem>? = null,
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
    val setWeightsKg: List<Double?>? = null,
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
    val description: String? = null,
    val restSeconds: Int,
    val scheduleType: WorkoutScheduleType = WorkoutScheduleType.ONE_TIME,
    val scheduleDays: List<String> = emptyList(),
    val exercises: List<SimpleWorkoutExerciseItem>,
)

/**
 * Single exercise entry in a create/update workout-plan payload.
 *
 * [setWeightsKg] is optional: `null` means the exercise is bodyweight (or the
 * server should fall back to its default weight), while a non-null list
 * must have exactly [sets] elements — one weight per set. Individual entries
 * may still be `null` if the user opted to leave a set's weight unrecorded.
 */
@Serializable
data class SimpleWorkoutExerciseItem(
    val exerciseId: String,
    val sets: Int,
    val reps: Int,
    val setWeightsKg: List<Double?>? = null,
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
    /**
     * Seconds it takes the average user to perform 10 reps of this exercise.
     * Used in time estimation formulas on the detail screen. `null` for older
     * plans where the backend did not yet backfill this value — callers should
     * default to 30 seconds.
     */
    val secondsPer10Reps: Int? = null,
    /**
     * Per-set weight history when the plan was created with weighted sets.
     *
     * `null` keeps the legacy contract for plans saved before this field
     * existed; UI should fall back to [weightKg] in that case. When non-null,
     * the list length matches [sets] (entries may be `null` if a single set
     * was logged without a weight).
     */
    val setWeightsKg: List<Double?>? = null,
)

/**
 * Lightweight session summary returned by `GET /api/v1/workout-sessions/by-date`.
 *
 * [startedAt] and [finishedAt] arrive as epoch milliseconds (Jackson with
 * `WRITE_DATES_AS_TIMESTAMPS` + `WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS=false`).
 * Consumers convert via `Instant.fromEpochMilliseconds(startedAt.toLong())`.
 */
@Serializable
data class WorkoutSessionHistoryItem(
    val id: String,
    val name: String,
    val startedAt: Double,
    val finishedAt: Double? = null,
    val status: String,
    val totalSets: Int = 0,
    val completedSets: Int = 0,
    val totalExercises: Int = 0,
    val completedExercises: Int = 0,
    val totalReps: Int = 0,
    val primaryMuscleGroup: String? = null,
    val durationMinutes: Int? = null,
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
                setWeightsKg = ex.setWeightsKg,
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
