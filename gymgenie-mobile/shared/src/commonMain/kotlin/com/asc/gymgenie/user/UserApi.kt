package com.asc.gymgenie.user

import com.asc.gymgenie.auth.NetworkException
import com.asc.gymgenie.config.AppConfig
import com.asc.gymgenie.common.ApiException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class UserApi(
    private val client: HttpClient,
    private val baseUrl: String = AppConfig.BASE_URL,
) {
    suspend fun getProfile(): Result<UserProfileResponse> {
        return try {
            val response = client.get("$baseUrl/api/v1/users/me")
            if (response.status.isSuccess()) {
                Result.success(response.body<UserProfileResponse>())
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(ApiException(response.status.value, errorBody))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun updateProfile(request: UpdateUserProfileRequest): Result<UserProfileResponse> {
        return try {
            val response = client.put("$baseUrl/api/v1/users/me") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<UserProfileResponse>())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun activateSubscription(): Result<UserProfileResponse> {
        return try {
            val response = client.put("$baseUrl/api/v1/users/me/subscription")
            if (response.status.isSuccess()) {
                Result.success(response.body<UserProfileResponse>())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }
}
