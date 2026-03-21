package com.asc.gymgenie.workout

import com.asc.gymgenie.auth.NetworkException
import com.asc.gymgenie.common.ApiException
import com.asc.gymgenie.common.PagedResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class WorkoutApi(
    private val baseUrl: String,
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun getActivePlans(accessToken: String): Result<List<WorkoutPlanShortResponse>> {
        return try {
            val response = client.get("$baseUrl/workout-plans/active") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
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
        accessToken: String,
        page: Int = 0,
        size: Int = 20,
    ): Result<PagedResponse<WorkoutPlanShortResponse>> {
        return try {
            val response = client.get("$baseUrl/workout-plans") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
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
}
