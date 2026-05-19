package com.asc.gymgenie.workout.dto

import com.asc.gymgenie.exercise.entity.DifficultyLevel
import com.asc.gymgenie.exercise.entity.MuscleGroup
import com.asc.gymgenie.workout.entity.CreatedBy
import com.asc.gymgenie.workout.entity.WorkoutScheduleType
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.DayOfWeek
import java.util.*

// ===== Responses =====

data class WorkoutPlanResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val createdBy: CreatedBy,
    val isActive: Boolean,
    val scheduleType: WorkoutScheduleType,
    val days: List<WorkoutPlanDayResponse>
)

data class WorkoutPlanShortResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val createdBy: CreatedBy,
    val isActive: Boolean,
    val scheduleType: WorkoutScheduleType,
    val daysCount: Int,
    /** Distinct days for [WorkoutScheduleType.RECURRING] plans; empty list for [WorkoutScheduleType.ONE_TIME]. */
    val scheduleDays: List<DayOfWeek>,
    /** Rest seconds taken from the first exercise of the first day; defaults to 60 when no exercises exist. */
    val restSeconds: Int,
    /** Most common [com.asc.gymgenie.exercise.entity.MuscleGroup] across all exercises of the plan; null when no exercises exist. */
    val primaryMuscleGroup: String?,
    /** Total exercises in the first day. For simple workouts this equals all exercises in the plan. */
    val exercisesCount: Int,
    /** Sum of sets across all exercises in the first day. */
    val totalSets: Int,
    /** Estimated workout duration in minutes, computed from exercise timing data and rest intervals. */
    val estimatedMinutes: Int
)

data class WorkoutPlanDayResponse(
    val id: UUID,
    val dayOfWeek: DayOfWeek,
    val name: String,
    val orderIndex: Int,
    val exercises: List<WorkoutPlanExerciseResponse>
)

data class WorkoutPlanExerciseResponse(
    val id: UUID,
    val exerciseId: UUID,
    val exerciseNameRu: String,
    val exerciseNameEn: String,
    val muscleGroup: MuscleGroup,
    val difficultyLevel: DifficultyLevel,
    val sets: Int,
    val reps: Int,
    /**
     * Unified weight for the exercise. For backward compatibility this stays populated when the
     * client uses a single weight for all sets, or carries the first non-null entry of
     * [setWeightsKg] when per-set weights are used. Clients should prefer [setWeightsKg] when present.
     */
    val weightKg: Double?,
    /**
     * Per-set weights aligned with the [sets] count: index 0 = set 1, index 1 = set 2, etc.
     * `null` when the exercise has no weight tracking. Individual entries may be `null` when a
     * specific set was not assigned a weight yet.
     */
    val setWeightsKg: List<Double?>? = null,
    val restSeconds: Int,
    val orderIndex: Int,
    val notes: String?,
    val secondsPer10Reps: Int? = null
)

// ===== Requests =====

data class CreateWorkoutPlanRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:Size(max = 500)
    val description: String? = null,

    val createdBy: CreatedBy = CreatedBy.USER,

    @field:Valid
    val days: List<CreateWorkoutPlanDayRequest> = emptyList()
)

data class UpdateWorkoutPlanRequest(
    @field:Size(max = 100)
    val name: String? = null,

    @field:Size(max = 500)
    val description: String? = null,

    val isActive: Boolean? = null,

    val scheduleType: WorkoutScheduleType? = null,

    /** Required when [scheduleType] is [WorkoutScheduleType.RECURRING]. */
    val scheduleDays: List<DayOfWeek>? = null,

    @field:Min(10)
    @field:Max(600)
    val restSeconds: Int? = null,

    /** When `null`, existing days/exercises are kept untouched. When non-null, the plan's days/exercises are fully replaced. */
    @field:Valid
    val exercises: List<SimpleWorkoutExerciseItem>? = null
)

data class CreateWorkoutPlanDayRequest(
    val dayOfWeek: DayOfWeek,

    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    val orderIndex: Int = 0,

    @field:Valid
    val exercises: List<CreateWorkoutPlanExerciseRequest> = emptyList()
)

data class CreateWorkoutPlanExerciseRequest(
    val exerciseId: UUID,
    val sets: Int = 3,
    val reps: Int = 10,
    val weightKg: Double? = null,
    val restSeconds: Int = 60,
    val orderIndex: Int = 0,
    val notes: String? = null
)

data class CreateSimpleWorkoutRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:Size(max = 500)
    val description: String? = null,

    @field:Min(10)
    @field:Max(600)
    val restSeconds: Int = 60,

    val scheduleType: WorkoutScheduleType = WorkoutScheduleType.ONE_TIME,

    /** Required when [scheduleType] is RECURRING; ignored for ONE_TIME. */
    val scheduleDays: List<DayOfWeek> = emptyList(),

    @field:Valid
    val exercises: List<SimpleWorkoutExerciseItem> = emptyList()
)

data class SimpleWorkoutExerciseItem(
    val exerciseId: UUID,

    @field:Min(1)
    @field:Max(10)
    val sets: Int = 3,

    @field:Min(2)
    @field:Max(25)
    val reps: Int = 10,

    /**
     * Per-set weights aligned with [sets]: index 0 = set 1, index 1 = set 2, etc.
     *
     * Contract:
     *  - `null` or empty list → no weight tracking for this exercise.
     *  - When non-empty, its size MUST equal [sets] (enforced in the service layer with a 400 response).
     *  - Individual entries MAY be `null` if a particular set has no weight yet (e.g. user
     *    completed the layout but hasn't decided the load for set 3).
     *  - If every non-null entry is identical, the exercise effectively uses a single weight for all sets.
     */
    val setWeightsKg: List<Double?>? = null
)
