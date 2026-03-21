package com.asc.gymgenie.exercise

import kotlinx.serialization.Serializable

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
