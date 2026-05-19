package com.asc.gymgenie.activity

import com.asc.gymgenie.auth.NetworkException
import com.asc.gymgenie.common.ApiException
import com.asc.gymgenie.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class ActivityApi(
    private val client: HttpClient,
    private val baseUrl: String = AppConfig.BASE_URL,
) {
    suspend fun getTodayActivities(date: String? = null): Result<List<ActivityTodayResponse>> {
        return try {
            val response = client.get("$baseUrl/api/v1/activities/today") {
                if (date != null) {
                    url { parameters.append("date", date) }
                }
            }
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

    suspend fun addToPlan(
        activityId: String,
        request: AddActivityToPlanRequest? = null,
    ): Result<Unit> {
        return try {
            val response = client.post("$baseUrl/api/v1/activities/$activityId/plan") {
                if (request != null) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            }
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

    suspend fun updateSchedule(
        activityId: String,
        request: UpdateActivityScheduleRequest,
    ): Result<Unit> {
        return try {
            val response = client.put("$baseUrl/api/v1/activities/$activityId/plan/schedule") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
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
