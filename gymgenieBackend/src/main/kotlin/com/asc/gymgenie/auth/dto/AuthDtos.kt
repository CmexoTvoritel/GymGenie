package com.asc.gymgenie.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.*

data class RegisterRequest(
    @field:NotBlank
    @field:Size(min = 3, max = 30)
    val username: String,

    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    @field:Size(min = 8, max = 100)
    val password: String
)

data class LoginRequest(
    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    val password: String
)

data class RefreshTokenRequest(
    @field:NotBlank
    val refreshToken: String
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthUserResponse
)

data class AuthUserResponse(
    val id: UUID,
    val username: String,
    val email: String,
    val subscriptionType: String,
)
