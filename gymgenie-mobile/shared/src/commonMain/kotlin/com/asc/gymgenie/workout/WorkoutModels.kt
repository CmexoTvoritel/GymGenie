package com.asc.gymgenie.workout

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutPlanShortResponse(
    val id: String,
    val name: String,
    val description: String? = null,
    val isActive: Boolean,
    val daysCount: Int,
    val createdBy: String = "USER",
)
