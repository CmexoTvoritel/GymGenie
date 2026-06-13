package com.asc.gymgenie.presentation

import com.asc.gymgenie.activity.ActivityApi
import com.asc.gymgenie.activity.ActivityCheckinRequest
import com.asc.gymgenie.activity.ActivityHistoryDayResponse
import com.asc.gymgenie.activity.ActivityTodayResponse
import com.asc.gymgenie.activity.UpdateActivityScheduleRequest
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
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class ActivitiesUiState(
    val isLoading: Boolean = true,
    val todayActivities: List<ActivityTodayResponse> = emptyList(),
    val history: List<ActivityHistoryDayResponse> = emptyList(),
    val isHistoryLoading: Boolean = false,
    val error: String? = null,
    val selectedDate: String = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString(),
    val isScheduleUpdating: Boolean = false,
    val scheduleUpdateError: String? = null,
)

class ActivitiesViewModel(
    private val activityApi: ActivityApi,
    private val sessionManager: SessionManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(ActivitiesUiState())
    val state: StateFlow<ActivitiesUiState> = _state.asStateFlow()

    fun load() {
        loadForDate(_state.value.selectedDate)
    }

    fun selectDate(date: String) {
        if (date == _state.value.selectedDate) return
        _state.update { it.copy(selectedDate = date) }
        loadForDate(date)
    }

    private fun loadForDate(date: String) {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            activityApi.getTodayActivities(date = date)
                .onSuccess { list ->
                    _state.update { it.copy(isLoading = false, todayActivities = list) }
                }
                .onFailure { e ->
                    if (isUnauthorized(e)) {
                        sessionManager.triggerLogout()
                        return@launch
                    }
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun loadHistory(startDate: String, endDate: String) {
        scope.launch {
            _state.update { it.copy(isHistoryLoading = true) }
            activityApi.getHistory(startDate, endDate)
                .onSuccess { days ->
                    _state.update { it.copy(isHistoryLoading = false, history = days) }
                }
                .onFailure { e ->
                    if (isUnauthorized(e)) {
                        sessionManager.triggerLogout()
                        return@launch
                    }
                    _state.update { it.copy(isHistoryLoading = false) }
                }
        }
    }

    fun checkIn(activityId: String, value: Int) {
        val previousValue = _state.value.todayActivities
            .firstOrNull { it.activityId == activityId }
            ?.logValue
            ?: return

        if (previousValue == value) return

        applyLogValue(activityId, value)

        val checkinDate = _state.value.selectedDate
        scope.launch {
            activityApi.checkin(
                activityId = activityId,
                request = ActivityCheckinRequest(date = checkinDate, value = value),
            ).onFailure { e ->
                if (isUnauthorized(e)) {
                    sessionManager.triggerLogout()
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

    fun updateSchedule(
        activityId: String,
        scheduleType: String?,
        scheduleDays: List<String>,
        oneOffDate: String?,
    ) {
        val current = _state.value.todayActivities.firstOrNull {
            it.activityId == activityId
        } ?: return

        applySchedule(activityId, scheduleType, scheduleDays, oneOffDate)
        _state.update { it.copy(isScheduleUpdating = true, scheduleUpdateError = null) }

        scope.launch {
            val request = UpdateActivityScheduleRequest(
                scheduleType = scheduleType,
                scheduleDays = scheduleDays.ifEmpty { null },
                oneOffDate = oneOffDate,
            )
            activityApi.updateSchedule(activityId, request)
                .onSuccess {
                    _state.update { it.copy(isScheduleUpdating = false) }
                }
                .onFailure { e ->
                    if (isUnauthorized(e)) {
                        sessionManager.triggerLogout()
                        return@launch
                    }

                    applySchedule(
                        activityId,
                        current.scheduleType,
                        current.scheduleDays,
                        current.oneOffDate,
                    )
                    _state.update {
                        it.copy(
                            isScheduleUpdating = false,
                            scheduleUpdateError = e.message,
                        )
                    }
                }
        }
    }

    fun removeFromPlan(activityId: String) {
        val removed = _state.value.todayActivities.firstOrNull {
            it.activityId == activityId
        } ?: return

        _state.update { current ->
            current.copy(todayActivities = current.todayActivities.filter { it.activityId != activityId })
        }

        scope.launch {
            activityApi.removeFromPlan(activityId).onFailure { e ->
                if (isUnauthorized(e)) {
                    sessionManager.triggerLogout()
                    return@launch
                }
                _state.update { current ->
                    current.copy(
                        todayActivities = current.todayActivities + removed,
                        error = "Не удалось удалить активность",
                    )
                }
            }
        }
    }

    fun clearScheduleUpdateError() {
        _state.update { it.copy(scheduleUpdateError = null) }
    }

    private fun applySchedule(
        activityId: String,
        scheduleType: String?,
        scheduleDays: List<String>,
        oneOffDate: String?,
    ) {
        _state.update { current ->
            val updated = current.todayActivities.map { activity ->
                if (activity.activityId == activityId) {
                    activity.copy(
                        scheduleType = scheduleType,
                        scheduleDays = scheduleDays,
                        oneOffDate = oneOffDate,
                    )
                } else {
                    activity
                }
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
