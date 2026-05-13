package com.asc.gymgenie.exercise

import com.asc.gymgenie.auth.NetworkException
import com.asc.gymgenie.config.AppConfig
import com.asc.gymgenie.common.ApiException
import com.asc.gymgenie.common.PagedResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

class ExerciseApi(
    private val client: HttpClient,
    private val baseUrl: String = AppConfig.BASE_URL,
) {
    suspend fun getExercises(
        muscleGroup: String? = null,
        category: String? = null,
        difficultyLevels: List<String> = emptyList(),
        requiresEquipment: Boolean? = null,
        sortByDifficulty: String? = null,
        sortByCalories: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): Result<PagedResponse<ExerciseShortResponse>> {
        return try {
            val response = client.get("$baseUrl/api/v1/exercises") {
                muscleGroup?.let { parameter("muscleGroup", it) }
                category?.let { parameter("category", it) }
                difficultyLevels.forEach { parameter("difficultyLevels", it) }
                requiresEquipment?.let { parameter("requiresEquipment", it) }
                sortByDifficulty?.let { parameter("sortByDifficulty", it) }
                sortByCalories?.let { parameter("sortByCalories", it) }
                parameter("page", page)
                parameter("size", size)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun searchExercises(
        query: String,
        difficultyLevels: List<String> = emptyList(),
        requiresEquipment: Boolean? = null,
        sortByDifficulty: String? = null,
        sortByCalories: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): Result<PagedResponse<ExerciseShortResponse>> {
        return try {
            val response = client.get("$baseUrl/api/v1/exercises/search") {
                parameter("query", query)
                difficultyLevels.forEach { parameter("difficultyLevels", it) }
                requiresEquipment?.let { parameter("requiresEquipment", it) }
                sortByDifficulty?.let { parameter("sortByDifficulty", it) }
                sortByCalories?.let { parameter("sortByCalories", it) }
                parameter("page", page)
                parameter("size", size)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun getExerciseById(id: String): Result<ExerciseDetailResponse> {
        return try {
            val response = client.get("$baseUrl/api/v1/exercises/$id")
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }

    suspend fun getMuscleGroups(): Result<List<MuscleGroupInfo>> {
        return try {
            val response = client.get("$baseUrl/api/v1/exercises/muscle-groups")
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(ApiException(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException(e.message ?: "Ошибка сети"))
        }
    }
}
