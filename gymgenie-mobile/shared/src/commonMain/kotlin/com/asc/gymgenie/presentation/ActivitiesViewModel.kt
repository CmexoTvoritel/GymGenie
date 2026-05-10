package com.asc.gymgenie.presentation

import com.asc.gymgenie.activity.ActivityApi
import com.asc.gymgenie.activity.ActivityCheckinRequest
import com.asc.gymgenie.activity.ActivityHistoryDayResponse
import com.asc.gymgenie.activity.ActivityTodayResponse
import com.asc.gymgenie.common.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * UI state of the Activities screen.
 *
 * The "Today" and "History" tabs share the same loading/error contract so the
 * UI layer can drive both with a single observed state. [isHistoryLoading] is
 * deliberately separate from the screen-wide [isLoading] flag — the history
 * range refreshes independently from the today list, and we don't want a
 * history reload to collapse the today view back to a spinner.
 */
data class ActivitiesUiState(
    val isLoading: Boolean = true,
    val todayActivities: List<ActivityTodayResponse> = emptyList(),
    val history: List<ActivityHistoryDayResponse> = emptyList(),
    val isHistoryLoading: Boolean = false,
    val error: String? = null,
)

/**
 * Drives the Activities screen.
 *
 * Mirrors the [HomeViewModel] check-in semantics: optimistic update first,
 * server call second, automatic rollback on failure. Auth failures (401) are
 * forwarded to [onLogout] so the host can drop the user back to the login
 * surface — the screen itself never owns navigation state.
 */
class ActivitiesViewModel(
    private val activityApi: ActivityApi,
    private val onLogout: () -> Unit = {},
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(ActivitiesUiState())
    val state: StateFlow<ActivitiesUiState> = _state.asStateFlow()

    /**
     * Loads today's activities. Safe to invoke multiple times — every call
     * resets the error banner and reissues the request, which makes it a fine
     * fit for `LaunchedEffect(refreshSignal)` triggers from the UI.
     */
    fun load() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            activityApi.getTodayActivities()
                .onSuccess { list ->
                    _state.update { it.copy(isLoading = false, todayActivities = list) }
                }
                .onFailure { e ->
                    if (isUnauthorized(e)) {
                        onLogout()
                        return@launch
                    }
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    /**
     * Loads the per-day history for the inclusive `[startDate, endDate]`
     * range. Both arguments are ISO-8601 strings (`YYYY-MM-DD`) — see
     * [ActivityApi.getHistory] for the contract.
     *
     * History failures intentionally do not surface in [ActivitiesUiState.error]:
     * the today tab must remain usable even if the history endpoint is down.
     */
    fun loadHistory(startDate: String, endDate: String) {
        scope.launch {
            _state.update { it.copy(isHistoryLoading = true) }
            activityApi.getHistory(startDate, endDate)
                .onSuccess { days ->
                    _state.update { it.copy(isHistoryLoading = false, history = days) }
                }
                .onFailure { e ->
                    if (isUnauthorized(e)) {
                        onLogout()
                        return@launch
                    }
                    _state.update { it.copy(isHistoryLoading = false) }
                }
        }
    }

    /**
     * Optimistically writes [value] for [activityId] and persists it via the
     * check-in endpoint. On failure the local snapshot is rolled back to the
     * previous value so state stays in sync with the server.
     *
     * Value semantics depend on the activity kind — see
     * [HomeViewModel.checkIn] for the canonical doc.
     */
    fun checkIn(activityId: String, value: Int) {
        val previousValue = _state.value.todayActivities
            .firstOrNull { it.activityId == activityId }
            ?.logValue
            ?: return

        if (previousValue == value) return

        applyLogValue(activityId, value)

        val today = todayIsoDate()
        scope.launch {
            activityApi.checkin(
                activityId = activityId,
                request = ActivityCheckinRequest(date = today, value = value),
            ).onFailure { e ->
                if (isUnauthorized(e)) {
                    onLogout()
                    return@launch
                }
                applyLogValue(activityId, previousValue)
            }
        }
    }

    private fun applyLogValue(activityId: String, value: Int) {
        _state.update { current ->
            val updated = current.todayActivities.map { activity ->
                if (activity.activityId == activityId) activity.copy(logValue = value) else activity
            }
            current.copy(todayActivities = updated)
        }
    }

    private fun todayIsoDate(): String =
        Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()

    private fun isUnauthorized(error: Throwable): Boolean =
        (error as? ApiException)?.statusCode == 401

    fun onCleared() {
        scope.cancel()
    }
}
