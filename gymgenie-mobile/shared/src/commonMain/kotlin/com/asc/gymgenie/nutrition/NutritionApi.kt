package com.asc.gymgenie.nutrition

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
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class NutritionApi(
    private val client: HttpClient,
    private val baseUrl: String = AppConfig.BASE_URL,
) {

    suspend fun getActivePlan(): Result<MealPlan?> {
        return try {
            val response = client.get("$baseUrl/api/v1/nutrition/plans/active")
            when {
                response.status == HttpStatusCode.NoContent -> Result.success(null)
                response.status.isSuccess() ->
                    Result.success(response.body<MealPlanResponse>().toDomain())
                else -> Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun createPlan(request: CreateMealPlanRequestDto): Result<MealPlan> {
        return try {
            val response = client.post("$baseUrl/api/v1/nutrition/plans") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<MealPlanResponse>().toDomain())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun addItemToMeal(
        mealId: String,
        request: AddMealItemRequestDto,
    ): Result<MealResponse> {
        return try {
            val response = client.post("$baseUrl/api/v1/nutrition/meals/$mealId/items") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<MealResponse>())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun deleteMealItem(itemId: String): Result<Unit> {
        return try {
            val response = client.delete("$baseUrl/api/v1/nutrition/meal-items/$itemId")
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun updatePlan(planId: String, request: CreateMealPlanRequestDto): Result<MealPlan> {
        return try {
            val response = client.put("$baseUrl/api/v1/nutrition/plans/$planId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<MealPlanResponse>().toDomain())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun deletePlan(planId: String): Result<Unit> {
        return try {
            val response = client.delete("$baseUrl/api/v1/nutrition/plans/$planId")
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
