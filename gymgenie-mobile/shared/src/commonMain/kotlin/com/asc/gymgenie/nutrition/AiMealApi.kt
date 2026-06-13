package com.asc.gymgenie.nutrition

import com.asc.gymgenie.auth.NetworkException
import com.asc.gymgenie.common.ApiException
import com.asc.gymgenie.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class AiMealApi(
    private val client: HttpClient,
    private val baseUrl: String = AppConfig.BASE_URL,
) {

    suspend fun chat(request: AiMealChatRequest): Result<AiMealChatResponse> {
        return try {
            val response = client.post("$baseUrl/api/v1/ai/meal/chat") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<AiMealChatResponse>())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun saveMealPlan(request: SaveMealPlanRequest): Result<SaveMealPlanResponse> {
        return try {
            val response = client.post("$baseUrl/api/v1/ai/meal/save") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<SaveMealPlanResponse>())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun replaceMealPlan(
        planId: String,
        request: SaveMealPlanRequest,
    ): Result<SaveMealPlanResponse> {
        return try {
            val response = client.put("$baseUrl/api/v1/ai/meal/save/$planId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<SaveMealPlanResponse>())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun getBookedDays(mealType: String? = null): Result<AiMealBookedDaysResponse> {
        return try {
            val response = client.get("$baseUrl/api/v1/ai/meal/booked-days") {
                mealType?.let { parameter("mealType", it) }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<AiMealBookedDaysResponse>())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun checkConflicts(
        scheduleType: String,
        oneOffDate: String? = null,
        scheduleDays: List<String> = emptyList(),
        mealType: String? = null,
    ): Result<AiMealConflictCheckResponse> {
        return try {
            val response = client.get("$baseUrl/api/v1/ai/meal/check-conflicts") {
                parameter("scheduleType", scheduleType)
                oneOffDate?.let { parameter("oneOffDate", it) }
                scheduleDays.forEach { parameter("scheduleDays", it) }
                mealType?.let { parameter("mealType", it) }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<AiMealConflictCheckResponse>())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun clearSession(): Result<Unit> {
        return try {
            val response = client.delete("$baseUrl/api/v1/ai/meal/session")
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
