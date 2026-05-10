package com.asc.gymgenie.ai

import kotlinx.serialization.Serializable

/**
 * Transport models for the AI workout chat feature.
 *
 * These mirror the backend contract one-to-one and stay confined to the
 * `ai` package so they do not leak into UI code. The chat surface keeps a
 * lightweight in-memory message list (see [AiChatMessage] in `AiViewModel`)
 * which is built from these DTOs at the presenter layer.
 */

/**
 * Discriminator for the AI response kind.
 *
 * - [CLARIFICATION]: the assistant needs more information from the user
 *   before it can produce a workout. The UI keeps the chat input enabled
 *   and renders the assistant text as a regular bubble.
 * - [WORKOUT]: the assistant returned a generated plan inside the same
 *   response. The UI shows the message and offers a "save workout" CTA.
 */
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
    val exercises: List<AiWorkoutExerciseDto>,
)

@Serializable
data class AiWorkoutExerciseDto(
    val exerciseId: String,
    val sets: Int,
    val reps: Int,
    val restSeconds: Int,
    val notes: String? = null,
)

@Serializable
data class SaveWorkoutRequest(
    val exercises: List<AiWorkoutExerciseDto>,
    val name: String,
    val description: String? = null,
)

@Serializable
data class SaveWorkoutResponse(
    val workoutPlanId: String,
)
