package com.asc.gymgenie.nutrition

import com.asc.gymgenie.auth.NetworkException
import com.asc.gymgenie.common.ApiException
import com.asc.gymgenie.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * Backend client for the AI chat / meal-plan-generation feature.
 *
 * Mirrors the contract pattern used by [com.asc.gymgenie.ai.AiApi]:
 * authentication is delegated to the injected [HttpClient] (the bearer plugin
 * configured in `createAuthenticatedClient` handles 401 + refresh), so this
 * class focuses purely on transport concerns.
 *
 * Wiring `Result<T>` back to the presenter keeps error semantics explicit at
 * the call-site: HTTP errors surface as [ApiException] (with the raw body so
 * the UI can show backend-provided messages where useful), and connectivity
 * problems surface as [NetworkException].
 */
class AiMealApi(
    private val client: HttpClient,
    private val baseUrl: String = AppConfig.BASE_URL,
) {

    /**
     * Sends a free-form user message to the AI nutrition coach. The backend
     * keeps a server-side session keyed by the authenticated user, so the
     * client only needs to ship the latest message plus the static profile +
     * dietary context. Resending the static fields on every request keeps the
     * wire contract idempotent and lets the backend rebuild context if its
     * session expires.
     */
    suspend fun chat(request: AiMealChatRequest): Result<AiMealChatResponse> {
        return try {
            val response = client.post("$baseUrl/api/v1/ai/meal/chat") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<AiMealChatResponse>())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    /**
     * Persists a previously-returned meal plan as a real user-owned plan, so
     * the user can later view / track it from the Nutrition tab.
     */
    suspend fun saveMealPlan(request: SaveMealPlanRequest): Result<SaveMealPlanResponse> {
        return try {
            val response = client.post("$baseUrl/api/v1/ai/meal/save") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<SaveMealPlanResponse>())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    /**
     * Overwrites a previously-saved meal plan with the freshly-refined version
     * returned by the AI. Used when the user iterates on the same generated
     * plan and wants the persisted entry to reflect the new contents instead
     * of stacking up duplicates.
     */
    suspend fun replaceMealPlan(
        planId: String,
        request: SaveMealPlanRequest,
    ): Result<SaveMealPlanResponse> {
        return try {
            val response = client.put("$baseUrl/api/v1/ai/meal/save/$planId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<SaveMealPlanResponse>())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    /**
     * Drops the server-side meal-chat session. Called when the user explicitly
     * resets the flow (back-to-start or finishes the feature) so the AI does
     * not carry stale context across unrelated meal-plan requests.
     */
    suspend fun clearSession(): Result<Unit> {
        return try {
            val response = client.delete("$baseUrl/api/v1/ai/meal/session")
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
