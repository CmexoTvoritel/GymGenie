package com.asc.gymgenie.nutrition

import com.asc.gymgenie.auth.NetworkException
import com.asc.gymgenie.common.ApiException
import com.asc.gymgenie.config.AppConfig
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

class ManualMealPlanApi(
    private val client: HttpClient,
    private val baseUrl: String = AppConfig.BASE_URL,
) {

    suspend fun getBookedDays(mealType: String): Result<BookedDaysResponse> {
        return try {
            val response = client.get("$baseUrl/api/v1/meal-plans/booked-days") {
                parameter("mealType", mealType)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<BookedDaysResponse>())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun createManualMealPlan(
        request: CreateManualMealPlanRequest,
    ): Result<MealPlanDetail> {
        return try {
            val response = client.post("$baseUrl/api/v1/meal-plans/manual") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<MealPlanDetail>())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }
}
