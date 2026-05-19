package com.asc.gymgenie.ai.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.util.UUID

data class AiChatRequest(
    @field:NotBlank
    val message: String,
    val ageYears: Int? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val experience: String? = null,
    val frequency: String? = null,
    val healthIssues: String? = null,
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
    val restSeconds: Int? = null,
    val exercises: List<AiWorkoutExerciseParsedDto>
)

/**
 * Lenient DTO used only for deserializing GigaChat JSON responses.
 *
 * `reps` and `restSeconds` are nullable because GigaChat omits these fields
 * for bodyweight or time-based exercises. This DTO must NOT be exposed on
 * mobile-facing endpoints — use [AiWorkoutExerciseDto] there.
 *
 * `setWeightsKg` is optional and only emitted by GigaChat for exercises whose
 * catalog entry has `requiresWeight = true`. When present, each element
 * corresponds to a single set; nulls mark sets without a target weight.
 */
data class AiWorkoutExerciseParsedDto(
    val exerciseId: UUID,
    val sets: Int,
    val reps: Int? = null,
    val restSeconds: Int? = null,
    val notes: String? = null,
    val setWeightsKg: List<Double?>? = null
)

/**
 * Strict DTO used by the mobile-facing save/replace workout endpoints.
 *
 * All numeric prescription fields are required: clients must explicitly
 * specify `reps` before persistence. Rest time is now a workout-level
 * concern — see [SaveWorkoutRequest.restSeconds]. `setWeightsKg` stays
 * optional because not every exercise tracks per-set weight; when present,
 * the list size must match `sets` (validated server-side).
 */
data class AiWorkoutExerciseDto(
    val exerciseId: UUID,
    val sets: Int,
    val reps: Int,
    val notes: String? = null,
    val setWeightsKg: List<Double?>? = null
)

data class SaveWorkoutRequest(
    @field:Valid
    @field:NotEmpty
    val exercises: List<AiWorkoutExerciseDto>,
    val name: String,
    val description: String? = null,
    val restSeconds: Int = 60
)

data class SaveWorkoutResponse(
    val workoutPlanId: UUID
)
