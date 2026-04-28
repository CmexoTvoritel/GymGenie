package com.asc.gymgenie.presentation

import com.asc.gymgenie.exercise.ExerciseApi
import com.asc.gymgenie.exercise.ExerciseDetailResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseDetailUiState(
    val isLoading: Boolean = false,
    val exercise: ExerciseDetailResponse? = null,
    val errorMessage: String? = null,
)

class ExerciseDetailViewModel(
    private val exerciseApi: ExerciseApi,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(ExerciseDetailUiState())
    val state: StateFlow<ExerciseDetailUiState> = _state.asStateFlow()

    fun load(id: String) {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, errorMessage = null) }

        scope.launch {
            exerciseApi.getExerciseById(id).fold(
                onSuccess = { detail ->
                    _state.update {
                        it.copy(isLoading = false, exercise = detail, errorMessage = null)
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Ошибка загрузки: ${error.message}",
                        )
                    }
                },
            )
        }
    }

    fun retry(id: String) {
        load(id)
    }

    fun onCleared() {
        scope.cancel()
    }
}
