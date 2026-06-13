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

class MealPlansApi(
    private val client: HttpClient,
    private val baseUrl: String = AppConfig.BASE_URL,
) {
    private companion object {

        const val TODAY_PAGE_SIZE = 100
    }

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
