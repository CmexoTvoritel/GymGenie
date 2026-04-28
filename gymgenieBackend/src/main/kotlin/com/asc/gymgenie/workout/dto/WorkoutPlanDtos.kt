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
    val daysCount: Int
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
    val weightKg: Double?,
    val restSeconds: Int,
    val orderIndex: Int,
    val notes: String?
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

    val isActive: Boolean? = null
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
    val reps: Int = 10
)
