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

/**
 * Nutrition (meal-plan / meal-item) backend client.
 *
 * Mirrors the call/auth conventions of [com.asc.gymgenie.activity.ActivityApi]:
 * the injected [HttpClient] must already be authenticated; bearer-token
 * lifecycle and 401-driven refresh are handled by the Ktor `Auth` plugin
 * configured in `createAuthenticatedClient`.
 *
 * All endpoints map the wire-format DTOs to the domain models in
 * [NutritionModels.kt] before returning, so callers never observe transport
 * types.
 */
class NutritionApi(
    private val client: HttpClient,
    private val baseUrl: String = AppConfig.BASE_URL,
) {
    /**
     * Returns the user's currently active meal plan, or `null` when the
     * server replies with 204 (no active plan yet).
     *
     * The 204 case is intentionally not modelled as a failure: an empty plan
     * is a normal state for new users and the Home view should render the
     * "create a plan" empty state, not an error banner.
     */
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

    /**
     * Creates a new meal plan from the provided request. The server returns
     * the full plan tree, including server-assigned ids for every day, meal,
     * and item — required so the caller can mutate individual nodes by id
     * without a second round-trip.
     */
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

    /**
     * Adds a single item to an existing meal. The backend echoes the meal's
     * recomputed totals (calories + macros) so the caller can update the
     * matching node without having to refetch the whole plan if the rest of
     * the tree has not changed.
     */
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

    /**
     * Removes a meal item by id. The server is expected to recompute the
     * parent meal's totals; clients should refetch the active plan after a
     * successful delete to stay consistent.
     */
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

    /**
     * Renames / re-saves an existing plan. Currently unused by the Android
     * UI but exposed so a future "Edit plan" flow does not need a second API
     * class.
     */
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

    /**
     * Deletes a plan. Idempotent on the server side; the client treats any
     * 2xx status — including the 204 the API contract documents — as success.
     */
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
