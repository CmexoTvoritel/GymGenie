package com.asc.gymgenie.activity

import com.asc.gymgenie.auth.NetworkException
import com.asc.gymgenie.common.ApiException
import com.asc.gymgenie.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * Activity (check-in / habit) backend client.
 *
 * Mirrors the call/auth conventions of [com.asc.gymgenie.workout.WorkoutApi]:
 * the injected [HttpClient] must already be authenticated; bearer-token
 * lifecycle and 401-driven refresh are handled by the Ktor `Auth` plugin
 * configured in `createAuthenticatedClient`.
 */
class ActivityApi(
    private val client: HttpClient,
    private val baseUrl: String = AppConfig.BASE_URL,
) {
    /**
     * Returns today's activities for the signed-in user, including the
     * per-activity log value for the current day. Used to drive the home
     * rings + check-in rows.
     */
    suspend fun getTodayActivities(): Result<List<ActivityTodayResponse>> {
        return try {
            val response = client.get("$baseUrl/api/v1/activities/today")
            if (response.status.isSuccess()) {
                Result.success(response.body<List<ActivityTodayResponse>>())
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(ApiException(response.status.value, errorBody))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    /**
     * Full catalog of activities the user can add to their daily plan.
     * Independent from the per-day state — drives the catalog screen.
     */
    suspend fun getCatalog(): Result<List<ActivityCatalogResponse>> {
        return try {
            val response = client.get("$baseUrl/api/v1/activities/catalog")
            if (response.status.isSuccess()) {
                Result.success(response.body<List<ActivityCatalogResponse>>())
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(ApiException(response.status.value, errorBody))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    /**
     * Persists a check-in for the given activity on the date encoded in
     * [request]. The [request.value] semantics depend on the activity kind
     * — see [ActivityKind].
     */
    suspend fun checkin(
        activityId: String,
        request: ActivityCheckinRequest,
    ): Result<ActivityLogResponse> {
        return try {
            val response = client.post("$baseUrl/api/v1/activities/$activityId/checkin") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<ActivityLogResponse>())
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(ApiException(response.status.value, errorBody))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    /**
     * Adds a catalog activity to the user's daily plan. The backend is
     * expected to respond with 2xx and an empty body; any non-success status
     * surfaces as an [ApiException].
     */
    suspend fun addToPlan(activityId: String): Result<Unit> {
        return try {
            val response = client.post("$baseUrl/api/v1/activities/$activityId/plan")
            if (response.status.isSuccess() || response.status.value == 409) {
                Result.success(Unit)
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(ApiException(response.status.value, errorBody))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    /**
     * Removes a previously added activity from the user's daily plan.
     * Idempotent on the server side (a 2xx for an already-removed entry is
     * still a success from the client's perspective).
     */
    suspend fun removeFromPlan(activityId: String): Result<Unit> {
        return try {
            val response = client.delete("$baseUrl/api/v1/activities/$activityId/plan")
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

    /**
     * Returns the user's activity history bucketed per day for the inclusive
     * range `[startDate, endDate]`. Both bounds are ISO-8601 strings
     * (`YYYY-MM-DD`) — the backend parses them with `@DateTimeFormat` so
     * any other format is rejected with a 400.
     *
     * Used by the Activities → "История" tab to drive the 7-day progress
     * strip and the per-day log list.
     */
    suspend fun getHistory(
        startDate: String,
        endDate: String,
    ): Result<List<ActivityHistoryDayResponse>> {
        return try {
            val response = client.get("$baseUrl/api/v1/activities/history") {
                url {
                    parameters.append("startDate", startDate)
                    parameters.append("endDate", endDate)
                }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<List<ActivityHistoryDayResponse>>())
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(ApiException(response.status.value, errorBody))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }
}
