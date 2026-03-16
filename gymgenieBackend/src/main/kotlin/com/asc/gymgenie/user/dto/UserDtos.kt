package com.asc.gymgenie.user.dto

import com.asc.gymgenie.user.entity.Gender
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.util.*

data class UserProfileResponse(
    val id: UUID,
    val username: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val gender: Gender?,
    val birthDate: LocalDate?,
    val weightKg: Double?,
    val heightCm: Double?,
    val profilePhotoUrl: String?,
    val subscriptionType: String
)

data class UpdateProfileRequest(
    @field:Size(min = 1, max = 50)
    val firstName: String? = null,

    @field:Size(min = 1, max = 50)
    val lastName: String? = null,

    val gender: Gender? = null,
    val birthDate: LocalDate? = null,
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val profilePhotoUrl: String? = null
)
