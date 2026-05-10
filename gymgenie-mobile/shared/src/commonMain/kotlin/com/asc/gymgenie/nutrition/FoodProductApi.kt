package com.asc.gymgenie.nutrition

import com.asc.gymgenie.auth.NetworkException
import com.asc.gymgenie.common.ApiException
import com.asc.gymgenie.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

/**
 * Backend client for the food products catalog.
 *
 * Authentication is delegated to the injected [HttpClient] (the shared
 * authenticated client wired by `createAuthenticatedClient`). All endpoints
 * map the wire-level [FoodProductResponse] DTO to the domain [FoodProduct]
 * before returning, so callers never observe transport-layer types.
 */
class FoodProductApi(
    private val client: HttpClient,
    private val baseUrl: String = AppConfig.BASE_URL,
) {
    /**
     * Searches the catalog. Both filters are optional so a no-arg call returns
     * the full catalog — small enough that we filter further client-side.
     *
     * Blank queries are dropped before being sent: passing an empty
     * `?search=` would force a server-side string match against an empty
     * substring on every backend, which is wasteful and easy to avoid here.
     */
    suspend fun searchProducts(
        query: String? = null,
        category: FoodCategory? = null,
    ): Result<List<FoodProduct>> {
        return try {
            val response = client.get("$baseUrl/api/v1/products") {
                query?.takeIf { it.isNotBlank() }?.let { parameter("search", it) }
                category?.let { parameter("category", it.name) }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<List<FoodProductResponse>>().map { it.toDomain() })
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun getProduct(id: String): Result<FoodProduct> {
        return try {
            val response = client.get("$baseUrl/api/v1/products/$id")
            if (response.status.isSuccess()) {
                Result.success(response.body<FoodProductResponse>().toDomain())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }
}
