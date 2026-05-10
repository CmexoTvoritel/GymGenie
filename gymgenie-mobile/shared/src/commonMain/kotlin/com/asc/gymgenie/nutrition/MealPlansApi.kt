package com.asc.gymgenie.nutrition

import com.asc.gymgenie.auth.NetworkException
import com.asc.gymgenie.common.ApiException
import com.asc.gymgenie.common.PagedResponse
import com.asc.gymgenie.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.LocalDate

/**
 * CRUD client for AI-saved meal plans.
 *
 * Lives next to [AiMealApi] — the AI flow writes meal plans, this client
 * reads/lists/deletes them. Keeping read and write surfaces separated avoids
 * a single bloated class and matches the pattern already used between
 * [com.asc.gymgenie.workout.WorkoutApi] (CRUD) and
 * [com.asc.gymgenie.ai.AiApi] (chat).
 *
 * Authentication is delegated to the injected [HttpClient]; the bearer-token
 * lifecycle and 401-driven refresh are handled by the Ktor `Auth` plugin
 * configured in `createAuthenticatedClient`.
 */
class MealPlansApi(
    private val client: HttpClient,
    private val baseUrl: String = AppConfig.BASE_URL,
) {
    private companion object {
        /**
         * Cap on how many plans the home today-fetch will inspect. A typical
         * user has well under this number of saved plans; the cap exists as a
         * defensive bound so a misconfigured account cannot trigger an
         * unbounded number of detail calls on every home load.
         */
        const val TODAY_PAGE_SIZE = 100
    }

    /**
     * Returns a single page of saved meal plans, newest-first.
     *
     * Uses [PagedResponse] for parity with [com.asc.gymgenie.workout.WorkoutApi.getPlans]
     * so the iOS list view can reuse the same scrolling/loading patterns.
     */
    suspend fun getMealPlans(
        page: Int = 0,
        size: Int = 20,
    ): Result<PagedResponse<MealPlanShortInfo>> {
        return try {
            val response = client.get("$baseUrl/api/v1/meal-plans") {
                parameter("page", page)
                parameter("size", size)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<PagedResponse<MealPlanShortInfo>>())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun getMealPlanById(id: String): Result<MealPlanDetail> {
        return try {
            val response = client.get("$baseUrl/api/v1/meal-plans/$id")
            if (response.status.isSuccess()) {
                Result.success(response.body<MealPlanDetail>())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    /**
     * Returns the saved meal plans whose schedule includes [today].
     *
     * Implementation notes:
     *  - The backend list endpoint returns short-form summaries that do not
     *    yet expose enough data (meals, scheduling) to render the home card,
     *    so we fetch the first page of summaries and then expand each into a
     *    [MealPlanDetail] in parallel.
     *  - Filtering happens client-side to avoid a backend change. The expected
     *    list size for one user is small (a handful of plans), so the cost is
     *    acceptable. Should the average grow, this can become a dedicated
     *    `/api/v1/meal-plans/today` endpoint without the home presenter
     *    changing.
     *  - We intentionally cap the page size at [TODAY_PAGE_SIZE]. A user with
     *    more saved plans than that hits the cap; the home screen is a
     *    summary surface, so rendering at most this many today-cards is
     *    acceptable. The list view is the surface that handles full
     *    pagination.
     *  - A failure in *one* per-plan detail fetch is treated as "skip this
     *    plan" rather than failing the whole call: the home screen prefers a
     *    partial render to a blocking error banner. The list-level fetch
     *    failure is still propagated so the caller can surface it.
     */
    suspend fun getTodayPlans(today: LocalDate): Result<List<MealPlanDetail>> = coroutineScope {
        val listResult = getMealPlans(page = 0, size = TODAY_PAGE_SIZE)
        listResult.fold(
            onSuccess = { paged ->
                val details = paged.content
                    .map { short -> async { getMealPlanById(short.id).getOrNull() } }
                    .awaitAll()
                    .filterNotNull()
                Result.success(details.filter { it.appliesOn(today) })
            },
            onFailure = { Result.failure(it) },
        )
    }

    /**
     * Permanently deletes a meal plan. The backend responds with 204 on
     * success — we accept any 2xx and treat the body as empty.
     */
    suspend fun deleteMealPlan(id: String): Result<Unit> {
        return try {
            val response = client.delete("$baseUrl/api/v1/meal-plans/$id")
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
