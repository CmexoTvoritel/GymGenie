package com.asc.gymgenie.workout

import com.asc.gymgenie.auth.NetworkException
import com.asc.gymgenie.config.AppConfig
import com.asc.gymgenie.common.ApiException
import com.asc.gymgenie.common.PagedResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * Workout backend client.
 *
 * Authentication is delegated to the injected [HttpClient]: the bearer token,
 * 401-driven refresh, and storage-clearing on refresh failure are handled by
 * the Ktor `Auth` plugin configured in `createAuthenticatedClient`. Callers
 * must therefore inject an authenticated client; passing a plain client will
 * cause every endpoint to fail with 401 because no `Authorization` header
 * will be attached.
 */
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

    /**
     * Creates a user-authored workout plan from a list of exercises with
     * per-exercise sets/reps and a global rest interval.
     */
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

    /**
     * Submits a finished workout session in a single request.
     *
     * The client-side flow accumulates set-level entries in a local DB while
     * the user trains; once they finish, the full payload is uploaded here.
     * This keeps the protocol simple (one round-trip, no server-side session
     * state) and resilient to flaky networks during the workout itself.
     */
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
}
