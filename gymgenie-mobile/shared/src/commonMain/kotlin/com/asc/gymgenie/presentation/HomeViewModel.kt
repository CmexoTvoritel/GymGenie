package com.asc.gymgenie.presentation

import com.asc.gymgenie.activity.ActivityApi
import com.asc.gymgenie.activity.ActivityCheckinRequest
import com.asc.gymgenie.activity.ActivityTodayResponse
import com.asc.gymgenie.common.ApiException
import com.asc.gymgenie.common.SessionManager
import com.asc.gymgenie.nutrition.MealPlansApi
import com.asc.gymgenie.nutrition.TodayMealPlanCard
import com.asc.gymgenie.nutrition.toTodayCards
import com.asc.gymgenie.nutrition.todayLocalDate
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.ui.ScreenState
import com.asc.gymgenie.user.UserApi
import com.asc.gymgenie.user.UserProfileResponse
import com.asc.gymgenie.user.UserProfileStore
import com.asc.gymgenie.workout.ActiveWorkoutSession
import com.asc.gymgenie.workout.PendingSessionUploader
import com.asc.gymgenie.workout.WorkoutApi
import com.asc.gymgenie.workout.WorkoutPlanShortResponse
import com.asc.gymgenie.workout.toActiveSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class HomeUiState(
    val screenState: ScreenState = ScreenState.Loading,
    val isContentLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val username: String = "",
    val subscriptionType: String = "FREE",
    val streakDays: Int = 0,
    val activeWorkoutPlans: List<WorkoutPlanShortResponse> = emptyList(),
    val todayActivities: List<ActivityTodayResponse> = emptyList(),
    val todayMealPlans: List<TodayMealPlanCard> = emptyList(),
    val selectedMealDate: LocalDate = todayLocalDate(),
    val isLoadingMealPlans: Boolean = false,
    val userProfile: UserProfileResponse? = null,
    val pendingSession: ActiveWorkoutSession? = null,
    val isLoadingSession: Boolean = false,
    val sessionError: String? = null,
    val activityError: String? = null,
)

class HomeViewModel(
    private val userApi: UserApi,
    private val workoutApi: WorkoutApi,
    private val activityApi: ActivityApi,
    private val mealPlansApi: MealPlansApi,
    private val tokenStorage: TokenStorage,
    private val userProfileStore: UserProfileStore,
    private val sessionManager: SessionManager,
    private val pendingSessionUploader: PendingSessionUploader,
) {
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        pendingSessionUploader.tryUploadPending()
    }

    private var loadJob: Job? = null

    fun load() {
        if (_state.value.isContentLoaded) return
        if (loadJob?.isActive == true) return

        _state.update { it.copy(screenState = ScreenState.Loading, errorMessage = null) }

        loadJob = scope.launch {
            try {
                if (tokenStorage.getAccessToken() == null) {
                    sessionManager.triggerLogout()
                    return@launch
                }

                val profileOk = fetchProfile(fatalOnFailure = true) ?: return@launch
                if (!profileOk) {
                    return@launch
                }

                awaitAll(
                    async { fetchActivePlans() },
                    async { fetchTodayActivities() },
                    async { fetchMealPlansForDate(_state.value.selectedMealDate) },
                )

                _state.update {
                    it.copy(
                        screenState = ScreenState.Content,
                        isContentLoaded = true,
                    )
                }
            } finally {
                loadJob = null
            }
        }
    }

    fun retry() {
        loadJob?.cancel()
        loadJob = null
        _state.update {
            it.copy(
                screenState = ScreenState.Loading,
                isContentLoaded = false,
                errorMessage = null,
            )
        }
        load()
    }

    fun refresh() {
        if (_state.value.isRefreshing) return
        _state.update { it.copy(isRefreshing = true, errorMessage = null) }

        scope.launch {
            if (tokenStorage.getAccessToken() == null) {
                _state.update { it.copy(isRefreshing = false) }
                sessionManager.triggerLogout()
                return@launch
            }

            awaitAll(
                async { fetchProfile(fatalOnFailure = false) },
                async { fetchActivePlans() },
                async { fetchTodayActivities() },
                async { fetchMealPlansForDate(_state.value.selectedMealDate) },
            )

            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun refreshMealPlans() {
        if (_state.value.isLoadingMealPlans) return
        _state.update { it.copy(errorMessage = null) }
        scope.launch {
            if (tokenStorage.getAccessToken() == null) {
                sessionManager.triggerLogout()
                return@launch
            }
            fetchMealPlansForDate(_state.value.selectedMealDate)
        }
    }

    fun selectMealDate(date: LocalDate) {
        if (date == _state.value.selectedMealDate) return
        _state.update { it.copy(selectedMealDate = date, todayMealPlans = emptyList()) }
        scope.launch {
            if (tokenStorage.getAccessToken() == null) {
                sessionManager.triggerLogout()
                return@launch
            }
            fetchMealPlansForDate(date)
        }
    }

    fun clearTransientError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun reset() {
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        loadJob = null
        _state.value = HomeUiState()
    }

    fun onCleared() {
        scope.cancel()
    }

    fun startWorkout(planId: String, planName: String) {
        if (_state.value.isLoadingSession) return
        _state.update { it.copy(isLoadingSession = true, sessionError = null) }
        scope.launch {
            if (tokenStorage.getAccessToken() == null) {
                _state.update { it.copy(isLoadingSession = false) }
                sessionManager.triggerLogout()
                return@launch
            }
            workoutApi.getPlanById(planId).fold(
                onSuccess = { plan ->
                    _state.update {
                        it.copy(
                            isLoadingSession = false,
                            pendingSession = plan.toActiveSession(),
                        )
                    }
                },
                onFailure = { error ->
                    if (shouldLogOut(error)) {
                        tokenStorage.clearTokens()
                        _state.update { it.copy(isLoadingSession = false) }
                        sessionManager.triggerLogout()
                        return@fold
                    }
                    _state.update {
                        it.copy(
                            isLoadingSession = false,
                            sessionError = "Не удалось загрузить тренировку: ${error.message}",
                        )
                    }
                },
            )
        }
    }

    fun clearPendingSession() {
        _state.update { it.copy(pendingSession = null, sessionError = null) }
    }

    fun checkIn(activityId: String, value: Int) {
        val previousActivities = _state.value.todayActivities
        val previousValue = previousActivities
            .firstOrNull { it.activityId == activityId }
            ?.logValue
            ?: return

        if (previousValue == value) return

        _state.update { current ->
            val updated = current.todayActivities.map { activity ->
                if (activity.activityId == activityId) activity.copy(logValue = value) else activity
            }
            current.copy(todayActivities = updated, activityError = null)
        }

        scope.launch {
            val today = todayIsoDate()
            activityApi.checkin(
                activityId = activityId,
                request = ActivityCheckinRequest(date = today, value = value),
            ).onFailure { error ->
                if (shouldLogOut(error)) {
                    tokenStorage.clearTokens()
                    sessionManager.triggerLogout()
                    return@onFailure
                }
                _state.update { it.copy(todayActivities = previousActivities, activityError = "Не удалось сохранить") }
            }
        }
    }

    fun clearActivityError() {
        _state.update { it.copy(activityError = null) }
    }

    private suspend fun fetchProfile(fatalOnFailure: Boolean): Boolean? {
        var abort = false
        var ok = false
        userApi.getProfile().fold(
            onSuccess = { profile ->
                userProfileStore.update(profile)
                _state.update {
                    it.copy(
                        userProfile = profile,
                        username = profile.username,
                        subscriptionType = profile.subscriptionType,
                    )
                }
                ok = true
            },
            onFailure = { error ->
                if (shouldLogOut(error)) {
                    tokenStorage.clearTokens()
                    sessionManager.triggerLogout()
                    abort = true
                    return@fold
                }
                if (fatalOnFailure) {
                    _state.update {
                        it.copy(
                            screenState = ScreenState.Error(
                                "Ошибка загрузки профиля: ${error.message}",
                            ),
                        )
                    }
                    abort = true
                } else {
                    _state.update {
                        it.copy(errorMessage = "Ошибка загрузки профиля: ${error.message}")
                    }
                }
            },
        )
        return when {
            abort -> null
            else -> ok
        }
    }

    private suspend fun fetchActivePlans() {
        workoutApi.getActivePlans().fold(
            onSuccess = { plans ->
                _state.update { it.copy(activeWorkoutPlans = plans) }
            },
            onFailure = { error ->
                if (shouldLogOut(error)) {
                    tokenStorage.clearTokens()
                    sessionManager.triggerLogout()
                    return@fold
                }
                if (_state.value.errorMessage == null) {
                    _state.update {
                        it.copy(errorMessage = "Ошибка загрузки планов: ${error.message}")
                    }
                }
            },
        )
    }

    private suspend fun fetchTodayActivities() {
        activityApi.getTodayActivities().fold(
            onSuccess = { activities ->
                _state.update { it.copy(todayActivities = activities) }
            },
            onFailure = { error ->
                if (shouldLogOut(error)) {
                    tokenStorage.clearTokens()
                    sessionManager.triggerLogout()
                    return@fold
                }
                _state.update {
                    val current = it
                    current.copy(
                        todayActivities = emptyList(),
                        errorMessage = current.errorMessage
                            ?: "Не удалось загрузить активности: ${error.message}",
                    )
                }
            },
        )
    }

    private suspend fun fetchMealPlansForDate(date: LocalDate) {
        _state.update { it.copy(isLoadingMealPlans = true) }
        mealPlansApi.getTodayPlans(today = date).fold(
            onSuccess = { plans ->
                val cards = plans
                    .flatMap { it.toTodayCards() }
                    .sortedWith(compareBy({ mealTypeOrder(it.mealType) }, { it.planName }))
                _state.update {
                    it.copy(
                        todayMealPlans = cards,
                        isLoadingMealPlans = false,
                    )
                }
            },
            onFailure = { error ->
                if (shouldLogOut(error)) {
                    tokenStorage.clearTokens()
                    _state.update { it.copy(isLoadingMealPlans = false) }
                    sessionManager.triggerLogout()
                    return@fold
                }
                _state.update {
                    it.copy(
                        isLoadingMealPlans = false,
                        errorMessage = it.errorMessage
                            ?: "Не удалось загрузить план питания: ${error.message}",
                    )
                }
            },
        )
    }

    private fun mealTypeOrder(mealType: String): Int = when (mealType.uppercase()) {
        "BREAKFAST" -> 0
        "LUNCH" -> 1
        "DINNER" -> 2
        "SNACK" -> 3
        else -> Int.MAX_VALUE
    }

    private suspend fun shouldLogOut(error: Throwable): Boolean {
        val is401 = (error as? ApiException)?.statusCode == 401
        return is401 || tokenStorage.getAccessToken() == null
    }

    private fun todayIsoDate(): String =
        Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()
}
