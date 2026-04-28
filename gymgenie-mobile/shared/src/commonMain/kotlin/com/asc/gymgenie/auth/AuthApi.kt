package com.asc.gymgenie.auth

import com.asc.gymgenie.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class AuthApi(
    private val baseUrl: String = AppConfig.BASE_URL,
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun login(email: String, password: String): Result<TokenResponse> {
        return try {
            val response = client.post("$baseUrl/api/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email = email, password = password))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<TokenResponse>())
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(AuthException(response.status.value, errorBody))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun refreshToken(refreshToken: String): Result<TokenResponse> {
        return try {
            val response = client.post("$baseUrl/api/v1/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshTokenRequest(refreshToken = refreshToken))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<TokenResponse>())
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(AuthException(response.status.value, errorBody))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun register(username: String, email: String, password: String): Result<TokenResponse> {
        return try {
            val response = client.post("$baseUrl/api/v1/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(username = username, email = email, password = password))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<TokenResponse>())
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(AuthException(response.status.value, errorBody))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }
}

class AuthException(val statusCode: Int, val errorBody: String) :
    Exception("HTTP $statusCode: $errorBody")

class NetworkException(message: String) : Exception(message)
