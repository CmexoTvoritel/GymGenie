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

/**
 * UI state for the saved meal-plan detail screen.
 *
 * `plan == null && !isLoading && errorMessage == null` is the initial /
 * disposed state — the view should kick a load on first appear and render
 * a neutral placeholder until either the plan or an error arrives.
 */
data class MealPlanDetailUiState(
    val isLoading: Boolean = false,
    val plan: MealPlanDetail? = null,
    val errorMessage: String? = null,
)

/**
 * Presenter for the saved meal-plan detail screen.
 *
 * A thin loader on top of [MealPlansApi.getMealPlanById]: holds the result
 * (or the error) on a single [StateFlow], retries on demand, and supports
 * cancelling the in-flight call from `onCleared`.
 *
 * Lifetime: callers must invoke [onCleared] when the surface is disposed so
 * in-flight coroutines are cancelled.
 */
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

    fun onCleared() {
        scope.cancel()
    }
}
