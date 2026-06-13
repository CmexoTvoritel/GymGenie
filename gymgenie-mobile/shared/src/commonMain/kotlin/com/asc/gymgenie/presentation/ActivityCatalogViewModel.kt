package com.asc.gymgenie.presentation

import com.asc.gymgenie.activity.ActivityApi
import com.asc.gymgenie.activity.ActivityCatalogResponse
import com.asc.gymgenie.activity.AddActivityToPlanRequest
import com.asc.gymgenie.common.ApiException
import com.asc.gymgenie.common.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActivityCatalogUiState(
    val isLoading: Boolean = true,
    val catalog: List<ActivityCatalogResponse> = emptyList(),
    val planIds: Set<String> = emptySet(),
    val error: String? = null,
)

class ActivityCatalogViewModel(
    private val activityApi: ActivityApi,
    private val sessionManager: SessionManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(ActivityCatalogUiState())
    val state: StateFlow<ActivityCatalogUiState> = _state.asStateFlow()

    fun load() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val catalogResult = activityApi.getCatalog()
            val catalog = catalogResult.getOrElse { e ->
                if (isUnauthorized(e)) {
                    sessionManager.triggerLogout()
                    return@launch
                }
                _state.update { it.copy(isLoading = false, error = e.message) }
                return@launch
            }

            val planIds = activityApi.getTodayActivities().fold(
                onSuccess = { today -> today.map { it.activityId }.toSet() },
                onFailure = { e ->
                    if (isUnauthorized(e)) {
                        sessionManager.triggerLogout()
                        return@launch
                    }
                    emptySet()
                },
            )

            _state.update {
                it.copy(isLoading = false, catalog = catalog, planIds = planIds, error = null)
            }
        }
    }

    fun togglePlan(activityId: String) {
        togglePlanInternal(activityId, request = null)
    }

    fun addToPlanWithSchedule(
        activityId: String,
        scheduleType: String?,
        scheduleDays: List<String>?,
        oneOffDate: String?,
        goal: Int?,
    ) {
        val hasSchedule = scheduleType != null || scheduleDays != null
                || oneOffDate != null || goal != null
        val request = if (hasSchedule) {
            AddActivityToPlanRequest(
                scheduleType = scheduleType,
                scheduleDays = scheduleDays,
                oneOffDate = oneOffDate,
                goal = goal,
            )
        } else null
        togglePlanInternal(activityId, request)
    }

    private fun togglePlanInternal(
        activityId: String,
        request: AddActivityToPlanRequest?,
    ) {
        val isInPlan = activityId in _state.value.planIds
        applyPlanFlag(activityId, addToPlan = !isInPlan)

        scope.launch {
            val result = if (isInPlan) {
                activityApi.removeFromPlan(activityId)
            } else {
                activityApi.addToPlan(activityId, request)
            }
            result.onFailure { e ->
                if (isUnauthorized(e)) {
                    sessionManager.triggerLogout()
                    return@launch
                }

                applyPlanFlag(activityId, addToPlan = isInPlan)
            }
        }
    }

    private fun applyPlanFlag(activityId: String, addToPlan: Boolean) {
        _state.update { current ->
            val newIds = if (addToPlan) current.planIds + activityId
            else current.planIds - activityId
            current.copy(planIds = newIds)
        }
    }

    private fun isUnauthorized(error: Throwable): Boolean =
        (error as? ApiException)?.statusCode == 401

    fun onCleared() {
        scope.cancel()
    }
}
