package com.asc.gymgenie.exercise

import kotlinx.serialization.Serializable

/**
 * Describes a muscle group that exercises can be filtered by.
 *
 * The [key] is the stable backend identifier (e.g. "CHEST") used in follow-up
 * `getExercises(muscleGroup = key)` calls. [nameRu] / [nameEn] are display labels
 * so the mobile clients never have to hard-code localized names for server-driven
 * categories.
 */
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
    val durationMinutes: Int? = null,
    val caloriesBurned: Int? = null,
    val rating: Double? = null,
    val imageUrl: String? = null,
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
    val durationMinutes: Int? = null,
    val caloriesBurned: Int? = null,
    val rating: Double? = null,
    val imageUrl: String? = null,
    val instructions: List<String> = emptyList(),
    val equipment: List<String> = emptyList(),
    val techniqueTip: String? = null,
)
