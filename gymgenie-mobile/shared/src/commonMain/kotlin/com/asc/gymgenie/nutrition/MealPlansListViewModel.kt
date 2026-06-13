package com.asc.gymgenie.nutrition

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MealPlansListUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val plans: List<MealPlanShortInfo> = emptyList(),
    val plansLoaded: Boolean = false,
    val hasMore: Boolean = true,
    val currentPage: Int = 0,
    val errorMessage: String? = null,
    val deletingPlanId: String? = null,
)

class MealPlansListViewModel(
    private val mealPlansApi: MealPlansApi,
    private val pageSize: Int = 20,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(MealPlansListUiState())
    val state: StateFlow<MealPlansListUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    fun load() {
        if (_state.value.isRefreshing || _state.value.isLoading) return
        _state.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                currentPage = 0,
                hasMore = true,
            )
        }
        loadJob = scope.launch { runLoad(page = 0, isRefresh = false) }
    }

    fun loadMore() {
        val state = _state.value
        if (state.isLoading || state.isLoadingMore || state.isRefreshing || !state.hasMore) return
        _state.update { it.copy(isLoadingMore = true) }
        loadJob = scope.launch { runLoad(page = state.currentPage + 1, isRefresh = false) }
    }

    fun refresh() {
        if (loadJob?.isActive == true) return
        _state.update {
            it.copy(
                isRefreshing = true,
                errorMessage = null,
                currentPage = 0,
                hasMore = true,
            )
        }
        loadJob = scope.launch { runLoad(page = 0, isRefresh = true) }
    }

    fun retry() {
        _state.update { it.copy(errorMessage = null) }
        load()
    }

    fun deletePlan(planId: String) {
        if (_state.value.deletingPlanId != null) return
        _state.update { it.copy(deletingPlanId = planId, errorMessage = null) }
        scope.launch {
            mealPlansApi.deleteMealPlan(planId).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            deletingPlanId = null,
                            plans = it.plans.filterNot { plan -> plan.id == planId },
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            deletingPlanId = null,
                            errorMessage = "Не удалось удалить рацион: ${error.message}",
                        )
                    }
                },
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun onCleared() {
        scope.cancel()
    }

    private suspend fun runLoad(page: Int, isRefresh: Boolean) {
        mealPlansApi.getMealPlans(page = page, size = pageSize).fold(
            onSuccess = { paged ->
                val merged = if (page == 0) paged.content else _state.value.plans + paged.content
                val hasMore = (paged.last == false) ||
                    (paged.totalPages > 0 && page + 1 < paged.totalPages)
                _state.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        isRefreshing = if (isRefresh) false else it.isRefreshing,
                        plans = merged,
                        plansLoaded = true,
                        currentPage = page,
                        hasMore = hasMore,
                        errorMessage = null,
                    )
                }
            },
            onFailure = { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        isRefreshing = if (isRefresh) false else it.isRefreshing,
                        errorMessage = "Ошибка загрузки: ${error.message}",
                    )
                }
            },
        )
    }
}
