package com.asc.gymgenie.workout

import com.asc.gymgenie.auth.NetworkException
import com.asc.gymgenie.config.AppConfig
import com.asc.gymgenie.common.ApiException
import com.asc.gymgenie.common.PagedResponse
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

class WorkoutApi(
    private val client: HttpClient,
    private val baseUrl: String = AppConfig.BASE_URL,
) {
    suspend fun getActivePlans(): Result<List<WorkoutPlanShortResponse>> {
        return try {
            val response = client.get("$baseUrl/api/v1/workout-plans/active")
            if (response.status.isSuccess()) {
                Result.success(response.body<List<WorkoutPlanShortResponse>>())
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(ApiException(response.status.value, errorBody))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun getPlans(
        page: Int = 0,
        size: Int = 20,
    ): Result<PagedResponse<WorkoutPlanShortResponse>> {
        return try {
            val response = client.get("$baseUrl/api/v1/workout-plans") {
                parameter("page", page)
                parameter("size", size)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<PagedResponse<WorkoutPlanShortResponse>>())
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(ApiException(response.status.value, errorBody))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun createSimpleWorkout(
        request: CreateSimpleWorkoutRequest,
    ): Result<WorkoutPlanShortResponse> {
        return try {
            val response = client.post("$baseUrl/api/v1/workout-plans/simple") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<WorkoutPlanShortResponse>())
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(ApiException(response.status.value, errorBody))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun submitSession(
        request: SubmitWorkoutSessionRequest,
    ): Result<WorkoutSessionResponse> {
        return try {
            val response = client.post("$baseUrl/api/v1/workout-sessions/submit") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<WorkoutSessionResponse>())
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(ApiException(response.status.value, errorBody))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun getSessionsByDate(date: String): Result<List<WorkoutSessionHistoryItem>> {
        return try {
            val response = client.get("$baseUrl/api/v1/workout-sessions/by-date") {
                parameter("date", date)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<List<WorkoutSessionHistoryItem>>())
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(ApiException(response.status.value, errorBody))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun getPlanById(planId: String): Result<WorkoutPlanResponse> {
        return try {
            val response = client.get("$baseUrl/api/v1/workout-plans/$planId")
            if (response.status.isSuccess()) {
                Result.success(response.body<WorkoutPlanResponse>())
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(ApiException(response.status.value, errorBody))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun deletePlan(planId: String): Result<Unit> {
        return try {
            val response = client.delete("$baseUrl/api/v1/workout-plans/$planId")
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(ApiException(response.status.value, errorBody))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun updatePlan(
        planId: String,
        request: UpdateWorkoutPlanRequest,
    ): Result<WorkoutPlanResponse> {
        return try {
            val response = client.put("$baseUrl/api/v1/workout-plans/$planId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<WorkoutPlanResponse>())
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(ApiException(response.status.value, errorBody))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }
}
