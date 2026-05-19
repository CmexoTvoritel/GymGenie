package com.asc.gymgenie.nutrition

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MealPlanDetailUiState(
    val isLoading: Boolean = false,
    val plan: MealPlanDetail? = null,
    val errorMessage: String? = null,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
)

class MealPlanDetailViewModel(
    private val mealPlansApi: MealPlansApi,
    private val planId: String,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(MealPlanDetailUiState())
    val state: StateFlow<MealPlanDetailUiState> = _state.asStateFlow()

    fun load() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, errorMessage = null) }

        scope.launch {
            mealPlansApi.getMealPlanById(planId).fold(
                onSuccess = { detail ->
                    _state.update { it.copy(isLoading = false, plan = detail) }
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

    fun retry() {
        _state.update { it.copy(errorMessage = null) }
        load()
    }

    fun delete() {
        if (_state.value.isDeleting) return
        _state.update { it.copy(isDeleting = true, errorMessage = null) }
        scope.launch {
            mealPlansApi.deleteMealPlan(planId).fold(
                onSuccess = {
                    _state.update { it.copy(isDeleting = false, isDeleted = true) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = "Не удалось удалить: ${error.message}",
                        )
                    }
                },
            )
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}
