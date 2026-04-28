package com.asc.gymgenie.ai.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.util.UUID

data class AiChatRequest(
    @field:NotBlank
    val message: String,
    val healthIssues: String? = null
)

data class AiChatResponse(
    val type: AiResponseType,
    val message: String,
    val workout: AiWorkoutDto? = null
)

enum class AiResponseType {
    CLARIFICATION, WORKOUT
}

data class AiWorkoutDto(
    val name: String,
    val description: String?,
    val estimatedDurationMinutes: Int,
    val exercises: List<AiWorkoutExerciseDto>
)

data class AiWorkoutExerciseDto(
    val exerciseId: UUID,
    val sets: Int,
    val reps: Int,
    val restSeconds: Int,
    val notes: String? = null
)

data class SaveWorkoutRequest(
    @field:Valid
    @field:NotEmpty
    val exercises: List<AiWorkoutExerciseDto>,
    val name: String,
    val description: String? = null
)

data class SaveWorkoutResponse(
    val workoutPlanId: UUID
)
