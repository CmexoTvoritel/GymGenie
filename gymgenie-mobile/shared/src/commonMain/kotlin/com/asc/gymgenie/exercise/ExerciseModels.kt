package com.asc.gymgenie.exercise

import kotlinx.serialization.Serializable

@Serializable
data class MuscleGroupInfo(
    val key: String,
    val nameRu: String,
    val nameEn: String,
    val imageUrl: String? = null,
)

@Serializable
data class ExerciseShortResponse(
    val id: String,
    val nameRu: String,
    val nameEn: String = "",
    val muscleGroup: String = "",
    val category: String = "",
    val difficultyLevel: String = "",
    val secondsPer10Reps: Int? = null,
    val caloriesBurned: Int? = null,
    val rating: Double? = null,
    val imageUrl: String? = null,

    val requiresWeight: Boolean = false,
)

@Serializable
data class ExerciseDetailResponse(
    val id: String,
    val nameRu: String,
    val nameEn: String = "",
    val description: String? = null,
    val muscleGroup: String = "",
    val secondaryMuscleGroups: List<String> = emptyList(),
    val category: String = "",
    val difficultyLevel: String = "",
    val secondsPer10Reps: Int? = null,
    val caloriesBurned: Int? = null,
    val rating: Double? = null,
    val imageUrl: String? = null,
    val instructions: List<String> = emptyList(),
    val equipment: List<String> = emptyList(),
    val techniqueTip: String? = null,

    val requiresWeight: Boolean = false,
)
