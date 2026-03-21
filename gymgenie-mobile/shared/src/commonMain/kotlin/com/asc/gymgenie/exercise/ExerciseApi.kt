package com.asc.gymgenie.exercise

import com.asc.gymgenie.auth.NetworkException
import com.asc.gymgenie.common.ApiException
import com.asc.gymgenie.common.PagedResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ExerciseApi(
    private val baseUrl: String,
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun getExercises(
        muscleGroup: String? = null,
        category: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): Result<PagedResponse<ExerciseShortResponse>> {
        return try {
            val response = client.get("$baseUrl/exercises") {
                muscleGroup?.let { parameter("muscleGroup", it) }
                category?.let { parameter("category", it) }
                parameter("page", page)
                parameter("size", size)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<PagedResponse<ExerciseShortResponse>>())
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(ApiException(response.status.value, errorBody))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun searchExercises(
        query: String,
        page: Int = 0,
        size: Int = 20,
    ): Result<PagedResponse<ExerciseShortResponse>> {
        return try {
            val response = client.get("$baseUrl/exercises/search") {
                parameter("query", query)
                parameter("page", page)
                parameter("size", size)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<PagedResponse<ExerciseShortResponse>>())
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(ApiException(response.status.value, errorBody))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }
}
