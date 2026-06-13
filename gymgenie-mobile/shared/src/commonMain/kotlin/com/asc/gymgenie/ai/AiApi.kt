package com.asc.gymgenie.ai

import com.asc.gymgenie.auth.NetworkException
import com.asc.gymgenie.common.ApiException
import com.asc.gymgenie.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class AiApi(
    private val client: HttpClient,
    private val baseUrl: String = AppConfig.BASE_URL,
) {

    suspend fun chat(request: AiChatRequest): Result<AiChatResponse> {
        return try {
            val response = client.post("$baseUrl/api/v1/ai/chat") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<AiChatResponse>())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun saveWorkout(request: SaveWorkoutRequest): Result<SaveWorkoutResponse> {
        return try {
            val response = client.post("$baseUrl/api/v1/ai/chat/save") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<SaveWorkoutResponse>())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun replaceWorkout(planId: String, request: SaveWorkoutRequest): Result<SaveWorkoutResponse> {
        return try {
            val response = client.put("$baseUrl/api/v1/ai/chat/save/$planId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<SaveWorkoutResponse>())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun clearSession(): Result<Unit> {
        return try {
            val response = client.delete("$baseUrl/api/v1/ai/chat/session")
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }
}
