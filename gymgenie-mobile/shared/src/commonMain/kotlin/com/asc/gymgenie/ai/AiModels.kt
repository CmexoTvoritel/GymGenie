package com.asc.gymgenie.ai

import kotlinx.serialization.Serializable

@Serializable
enum class AiResponseType {
    CLARIFICATION,
    WORKOUT,
}

@Serializable
data class AiChatRequest(
    val message: String,
    val ageYears: Int? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val experience: String? = null,
    val frequency: String? = null,
    val healthIssues: String? = null,
)

@Serializable
data class AiChatResponse(
    val type: AiResponseType,
    val message: String,
    val workout: AiWorkoutDto? = null,
)

@Serializable
data class AiWorkoutDto(
    val name: String,
    val description: String? = null,
    val estimatedDurationMinutes: Int,
    val restSeconds: Int = 60,
    val exercises: List<AiWorkoutExerciseDto>,
)

@Serializable
data class AiWorkoutExerciseDto(
    val exerciseId: String,
    val sets: Int,
    val reps: Int,
    val notes: String? = null,
    val setWeightsKg: List<Double?>? = null,
)

@Serializable
data class SaveWorkoutRequest(
    val exercises: List<AiWorkoutExerciseDto>,
    val name: String,
    val description: String? = null,
    val restSeconds: Int = 60,
)

@Serializable
data class SaveWorkoutResponse(
    val workoutPlanId: String,
)
