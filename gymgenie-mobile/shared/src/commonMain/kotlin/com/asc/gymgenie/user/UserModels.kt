package com.asc.gymgenie.user

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponse(
    val id: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val gender: String? = null,
    val birthDate: String? = null,
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val profilePhotoUrl: String? = null,
    val subscriptionType: String = "FREE",
    val ageYears: Int? = null,
    val experience: String? = null,
    val frequency: String? = null,
    val healthIssues: String? = null,
)

@Serializable
data class UpdateUserProfileRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val gender: String? = null,
    val birthDate: String? = null,
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val profilePhotoUrl: String? = null,
    val ageYears: Int? = null,
    val experience: String? = null,
    val frequency: String? = null,
    val healthIssues: String? = null,
)
