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

/**
 * Backend client for the manual meal-plan creation flow.
 *
 * Two responsibilities:
 *  - reading the user's already-booked weekdays / one-off dates so the date
 *    picker can disable conflicting slots
 *  - posting a fully-built manual plan
 *
 * Catalog reads (search + by-id) intentionally stay on the existing
 * [FoodProductApi]: the manual flow shares the same product lookup with the
 * legacy food picker, and duplicating the search endpoint here would split the
 * cache and complicate any future introduction of a shared catalog repository.
 *
 * Authentication is delegated to the injected [HttpClient] (the bearer plugin
 * configured in `createAuthenticatedClient` handles 401 + refresh).
 */
class ManualMealPlanApi(
    private val client: HttpClient,
    private val baseUrl: String = AppConfig.BASE_URL,
) {
    /**
     * Returns the slots already occupied for [mealType] across the user's
     * existing plans. Used to gray out booked weekdays / dates in the setup
     * screen so the user cannot create a duplicate booking that the backend
     * would reject.
     *
     * [mealType] is sent as a wire string ("BREAKFAST" / "LUNCH" / "DINNER")
     * — the presenter is responsible for resolving the enum to its
     * [ManualMealKind.wireValue] before calling.
     */
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

    /**
     * Persists the manually-built plan. The backend expands the request into
     * a [MealPlanDetail] tree and returns it directly so the caller can render
     * the saved plan without an extra round-trip.
     */
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
