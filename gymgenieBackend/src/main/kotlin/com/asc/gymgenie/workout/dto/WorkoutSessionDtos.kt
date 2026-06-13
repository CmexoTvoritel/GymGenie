package com.asc.gymgenie.workout.dto

import com.asc.gymgenie.workout.entity.SessionStatus
import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.*

data class WorkoutSessionResponse(
    val id: UUID,
    val name: String,
    val workoutPlanDayId: UUID?,
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    val startedAt: Instant,
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    val finishedAt: Instant?,
    val status: SessionStatus,
    val notes: String?,
    val sets: List<WorkoutSessionSetResponse>
)

data class WorkoutSessionShortResponse(
    val id: UUID,
    val name: String,
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    val startedAt: Instant,
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    val finishedAt: Instant?,
    val status: SessionStatus,
    val totalSets: Int,
    val completedSets: Int,
    val totalExercises: Int,
    val completedExercises: Int,
    val totalReps: Int,
    val primaryMuscleGroup: String?,
    val durationMinutes: Int?,
)

data class WorkoutSessionSetResponse(
    val id: UUID,
    val exerciseId: UUID,
    val exerciseNameRu: String,
    val exerciseNameEn: String,
    val setNumber: Int,
    val reps: Int?,
    val weightKg: Double?,
    val completed: Boolean,
    val durationSeconds: Int?
)

data class StartWorkoutSessionRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    val workoutPlanDayId: UUID? = null,

    @field:Size(max = 1000)
    val notes: String? = null
)

data class AddSessionSetRequest(
    val exerciseId: UUID,
    val setNumber: Int,
    val reps: Int? = null,
    val weightKg: Double? = null,
    val completed: Boolean = false,
    val durationSeconds: Int? = null
)

data class UpdateSessionSetRequest(
    val reps: Int? = null,
    val weightKg: Double? = null,
    val completed: Boolean? = null,
    val durationSeconds: Int? = null
)

data class FinishWorkoutSessionRequest(
    val status: SessionStatus = SessionStatus.COMPLETED,

    @field:Size(max = 1000)
    val notes: String? = null
)

data class SubmitWorkoutSessionRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    val workoutPlanDayId: UUID? = null,

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    val startedAt: Instant,

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    val finishedAt: Instant,

    val status: SessionStatus = SessionStatus.COMPLETED,

    @field:Size(max = 1000)
    val notes: String? = null,

    val totalPlannedSets: Int? = null,

    val totalPlannedExercises: Int? = null,

    @field:Valid
    val sets: List<SubmitSessionSetItem> = emptyList()
)

data class SubmitSessionSetItem(
    val exerciseId: UUID,
    val setNumber: Int,
    val reps: Int? = null,
    val weightKg: Double? = null,
    val completed: Boolean = true,
    val durationSeconds: Int? = null
)
