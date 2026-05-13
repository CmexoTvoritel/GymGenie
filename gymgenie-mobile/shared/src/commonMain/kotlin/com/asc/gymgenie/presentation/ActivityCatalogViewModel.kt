package com.asc.gymgenie.presentation

import com.asc.gymgenie.activity.ActivityApi
import com.asc.gymgenie.activity.ActivityCatalogResponse
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

/**
 * UI state of the Activity Catalog screen.
 *
 * [planIds] holds the ids of activities currently in the user's daily plan,
 * derived from the today endpoint. We expose it as a [Set] so the UI can
 * resolve the per-row toggle state in O(1) without iterating.
 */
data class ActivityCatalogUiState(
    val isLoading: Boolean = true,
    val catalog: List<ActivityCatalogResponse> = emptyList(),
    val planIds: Set<String> = emptySet(),
    val error: String? = null,
)

/**
 * Drives the Activity Catalog screen — the list of activities the user can
 * add to their daily plan.
 *
 * The plan-membership signal is recovered from `getTodayActivities()` because
 * the catalog endpoint itself is plan-agnostic. A failure on the today call is
 * non-fatal — the catalog still renders, just without the toggle indicator.
 */
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

            // Today's plan is best-effort; if it fails (and isn't a 401) we
            // still render the catalog with an empty plan set so the user
            // can keep browsing.
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

    /**
     * Toggles the plan membership of [activityId]. Updates local state
     * optimistically and rolls back if the server call fails.
     */
    fun togglePlan(activityId: String) {
        val isInPlan = activityId in _state.value.planIds
        applyPlanFlag(activityId, addToPlan = !isInPlan)

        scope.launch {
            val result = if (isInPlan) {
                activityApi.removeFromPlan(activityId)
            } else {
                activityApi.addToPlan(activityId)
            }
            result.onFailure { e ->
                if (isUnauthorized(e)) {
                    sessionManager.triggerLogout()
                    return@launch
                }
                // Revert the optimistic update.
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
