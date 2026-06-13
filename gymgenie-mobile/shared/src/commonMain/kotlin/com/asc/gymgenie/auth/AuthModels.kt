package com.asc.gymgenie.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RegisterRequest(
    val firstName: String,
    val email: String,
    val password: String,
)

@Serializable
data class UserResponse(
    val id: String,
    val firstName: String,
    val email: String,
    val subscriptionType: String = "FREE",
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String,
)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse,
)
