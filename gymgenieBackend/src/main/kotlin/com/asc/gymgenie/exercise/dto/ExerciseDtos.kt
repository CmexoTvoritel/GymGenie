package com.asc.gymgenie.exercise.dto

import com.asc.gymgenie.exercise.entity.DifficultyLevel
import com.asc.gymgenie.exercise.entity.ExerciseCategory
import com.asc.gymgenie.exercise.entity.MuscleGroup
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.*

data class ExerciseResponse(
    val id: UUID,
    val nameRu: String,
    val nameEn: String,
    val description: String?,
    val muscleGroup: MuscleGroup,
    val category: ExerciseCategory,
    val difficultyLevel: DifficultyLevel,
    val durationMinutes: Int?,
    val caloriesBurned: Int?,
    val rating: Double?,
    val imageUrl: String?,
    val videoUrl: String?,
    val instructions: List<String>,
    val equipment: List<String>
)

data class ExerciseShortResponse(
    val id: UUID,
    val nameRu: String,
    val nameEn: String,
    val muscleGroup: MuscleGroup,
    val category: ExerciseCategory,
    val difficultyLevel: DifficultyLevel,
    val durationMinutes: Int?,
    val caloriesBurned: Int?,
    val rating: Double?,
    val imageUrl: String?
)

data class CreateExerciseRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val nameRu: String,

    @field:NotBlank
    @field:Size(max = 100)
    val nameEn: String,

    @field:Size(max = 2000)
    val description: String? = null,

    val muscleGroup: MuscleGroup,
    val category: ExerciseCategory,
    val difficultyLevel: DifficultyLevel,
    val durationMinutes: Int? = null,
    val caloriesBurned: Int? = null,
    val rating: Double? = null,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val instructions: List<String> = emptyList(),
    val equipment: List<String> = emptyList()
)

data class UpdateExerciseRequest(
    @field:Size(max = 100)
    val nameRu: String? = null,

    @field:Size(max = 100)
    val nameEn: String? = null,

    @field:Size(max = 2000)
    val description: String? = null,

    val muscleGroup: MuscleGroup? = null,
    val category: ExerciseCategory? = null,
    val difficultyLevel: DifficultyLevel? = null,
    val durationMinutes: Int? = null,
    val caloriesBurned: Int? = null,
    val rating: Double? = null,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val instructions: List<String>? = null,
    val equipment: List<String>? = null
)

data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean
)
