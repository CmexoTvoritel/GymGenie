package com.asc.gymgenie.presentation

import com.asc.gymgenie.common.ApiException
import com.asc.gymgenie.common.SessionManager
import com.asc.gymgenie.workout.WorkoutApi
import com.asc.gymgenie.workout.WorkoutSessionHistoryItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class WorkoutHistoryState(
    val selectedDate: LocalDate = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val weekDates: List<LocalDate> = emptyList(),
    val sessions: List<WorkoutSessionHistoryItem> = emptyList(),
    val weekSessions: Map<String, List<WorkoutSessionHistoryItem>> = emptyMap(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

class WorkoutHistoryViewModel(
    private val workoutApi: WorkoutApi,
    private val sessionManager: SessionManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(WorkoutHistoryState())
    val state: StateFlow<WorkoutHistoryState> = _state.asStateFlow()
    private var loadDayJob: Job? = null

    init {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        val weekDates = computeWeekDates(today)
        _state.update { it.copy(selectedDate = today, weekDates = weekDates) }
        loadSelectedDay()
        loadWeekDots()
    }

    fun selectDate(date: LocalDate) {
        val oldWeekStart = _state.value.weekDates.firstOrNull()
        _state.update { it.copy(selectedDate = date, weekDates = computeWeekDates(date)) }
        loadSelectedDay()
        if (_state.value.weekDates.firstOrNull() != oldWeekStart) {
            loadWeekDots()
        }
    }

    fun shiftWeek(offset: Int) {
        val currentMonday = _state.value.weekDates.firstOrNull() ?: return
        val newMonday = currentMonday.plus(offset * 7, DateTimeUnit.DAY)
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        val newWeek = computeWeekDates(newMonday)
        val selectedDay = if (today in newWeek) today else newMonday
        selectDate(selectedDay)
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true) }
        loadSelectedDay()
        loadWeekDots()
    }

    private fun loadSelectedDay() {
        loadDayJob?.cancel()
        val date = _state.value.selectedDate
        loadDayJob = scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            workoutApi.getSessionsByDate(date.toString()).fold(
                onSuccess = { list ->
                    if (_state.value.selectedDate == date) {
                        _state.update { current ->
                            current.copy(
                                sessions = list,
                                isLoading = false,
                                isRefreshing = false,
                                weekSessions = current.weekSessions + (date.toString() to list),
                            )
                        }
                    }
                },
                onFailure = { e ->
                    if ((e as? ApiException)?.statusCode == 401) {
                        sessionManager.triggerLogout()
                        return@launch
                    }
                    if (_state.value.selectedDate == date) {
                        _state.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
                    }
                },
            )
        }
    }

    private fun loadWeekDots() {
        val dates = _state.value.weekDates
        val selected = _state.value.selectedDate.toString()
        for (date in dates) {
            val key = date.toString()
            if (key == selected) continue
            scope.launch {
                workoutApi.getSessionsByDate(key).onSuccess { list ->
                    _state.update { current ->
                        current.copy(weekSessions = current.weekSessions + (key to list))
                    }
                }
            }
        }
    }

    private fun computeWeekDates(date: LocalDate): List<LocalDate> {
        val mondayOffset = (date.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal + 7) % 7
        val monday = date.minus(mondayOffset, DateTimeUnit.DAY)
        return (0..6).map { monday.plus(it, DateTimeUnit.DAY) }
    }

    fun onCleared() {
        scope.cancel()
    }
}
