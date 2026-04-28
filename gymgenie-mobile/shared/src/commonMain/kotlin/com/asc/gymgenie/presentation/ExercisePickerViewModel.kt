package com.asc.gymgenie.presentation

import com.asc.gymgenie.exercise.ExerciseApi
import com.asc.gymgenie.exercise.ExerciseShortResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExercisePickerUiState(
    val muscleGroupKey: String = "",
    val exercises: List<ExerciseShortResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val errorMessage: String? = null,
    val currentPage: Int = 0,
)

class ExercisePickerViewModel(
    private val exerciseApi: ExerciseApi,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(ExercisePickerUiState())
    val state: StateFlow<ExercisePickerUiState> = _state.asStateFlow()

    companion object {
        private const val PAGE_SIZE = 20
    }

    fun load(muscleGroup: String) {
        val current = _state.value
        if (current.isLoading && current.muscleGroupKey == muscleGroup) return

        _state.value = ExercisePickerUiState(
            muscleGroupKey = muscleGroup,
            isLoading = true,
        )
        fetchPage(muscleGroup = muscleGroup, page = 0, append = false)
    }

    fun loadMore() {
        val current = _state.value
        if (current.muscleGroupKey.isEmpty()) return
        if (current.isLoading || current.isLoadingMore) return
        if (!current.hasMore) return

        _state.update { it.copy(isLoadingMore = true, errorMessage = null) }
        fetchPage(
            muscleGroup = current.muscleGroupKey,
            page = current.currentPage + 1,
            append = true,
        )
    }

    fun retry() {
        val current = _state.value
        if (current.muscleGroupKey.isEmpty()) return
        load(current.muscleGroupKey)
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun onCleared() {
        scope.cancel()
    }

    private fun fetchPage(muscleGroup: String, page: Int, append: Boolean) {
        scope.launch {
            val result = exerciseApi.getExercises(
                muscleGroup = muscleGroup,
                page = page,
                size = PAGE_SIZE,
            )
            result.fold(
                onSuccess = { paged ->
                    _state.update { current ->
                        val merged = if (append) current.exercises + paged.content else paged.content
                        val isLast = paged.last ?: (paged.content.size < PAGE_SIZE)
                        current.copy(
                            exercises = merged,
                            isLoading = false,
                            isLoadingMore = false,
                            hasMore = !isLast,
                            currentPage = page,
                            errorMessage = null,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            errorMessage = "Ошибка загрузки: ${error.message}",
                        )
                    }
                },
            )
        }
    }
}
