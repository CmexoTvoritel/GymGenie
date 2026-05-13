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

/**
 * UI state for the Home screen.
 *
 * The state intentionally separates two notions of "loading":
 *  - [screenState] drives the full-screen phase (skeleton / error / content).
 *    It only transitions back to [ScreenState.Loading] on the very first load
 *    or when an explicit retry is invoked.
 *  - [isRefreshing] is the pull-to-refresh badge that overlays existing content
 *    so the user keeps seeing the previous data while a background refresh runs.
 *
 * [errorMessage] is the *transient* error banner shown together with content
 * (e.g. "Activities failed to load" while the rest of the dashboard is fine).
 * It is unrelated to [ScreenState.Error], which is the blocking phase.
 */
data class HomeUiState(
    val screenState: ScreenState = ScreenState.Loading,
    val isContentLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val username: String = "",
    val subscriptionType: String = "FREE",
    // Placeholder — the backend profile endpoint does not yet expose a streak
    // counter. Kept in state so the UI contract is stable; wired to 0 until
    // a dedicated streak endpoint is added.
    val streakDays: Int = 0,
    val activeWorkoutPlans: List<WorkoutPlanShortResponse> = emptyList(),
    val todayActivities: List<ActivityTodayResponse> = emptyList(),
    /**
     * Manually-created and AI-generated meal plans whose schedule (RECURRING
     * weekday list or ONE_TIME date) matches today. One entry per meal — a
     * single day may legitimately carry several cards (breakfast / lunch /
     * dinner) coming from different plans.
     */
    val todayMealPlans: List<TodayMealPlanCard> = emptyList(),
    val selectedMealDate: LocalDate = todayLocalDate(),
    val isLoadingMealPlans: Boolean = false,
    val userProfile: UserProfileResponse? = null,
    val pendingSession: ActiveWorkoutSession? = null,
    val isLoadingSession: Boolean = false,
    val sessionError: String? = null,
)

/**
 * Presenter for the Home screen.
 *
 * Composes the backend reads required to render the dashboard (profile,
 * active workout plans, today's activities, today's meal plans) and exposes
 * the result as a single [HomeUiState] flow.
 *
 * Meal plans are sourced from [MealPlansApi] (the new manual + AI
 * meal-plan surface). The legacy weekly `MealPlan` shape served by
 * `NutritionApi.getActivePlan()` is no longer consumed here — its endpoint
 * has been retired and the home dashboard renders today's manually-created
 * (or AI-generated) plans directly via [TodayMealPlanCard].
 *
 * Concurrency model:
 *  - The presenter owns a [SupervisorJob]-scoped [CoroutineScope] on
 *    [Dispatchers.Main]. A failure in one of the parallel loads cannot
 *    cancel its siblings.
 *
 * Error policy:
 *  - 401 on any endpoint -> forced logout via [shouldLogOut].
 *  - During the first [load], a profile failure stops the whole load (we
 *    cannot render anything meaningful without it). All other endpoints are
 *    non-fatal and degrade gracefully into transient banners.
 *  - During [refresh], every endpoint is non-fatal: the user already sees
 *    valid content from the previous successful load.
 */
class HomeViewModel(
    private val userApi: UserApi,
    private val workoutApi: WorkoutApi,
    private val activityApi: ActivityApi,
    private val mealPlansApi: MealPlansApi,
    private val tokenStorage: TokenStorage,
    private val userProfileStore: UserProfileStore,
    private val sessionManager: SessionManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /**
     * Tracks the currently-running [load] coroutine. Used for de-duplication
     * (the screen calls [load] on every composition that has not yet seen
     * content) without conflating "in-flight" with the [ScreenState.Loading]
     * UI phase. Cleared in a `finally` so it is robust against cancellation.
     */
    private var loadJob: Job? = null

    // -----------------------------------------------------------------------
    // Lifecycle entry points
    // -----------------------------------------------------------------------

    /**
     * First-time load of the dashboard. Idempotent: subsequent calls are
     * ignored once content has been shown or while a load is still in flight
     * (the screen calls this on every recomposition that has not yet rendered
     * content).
     */
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

                // Profile is the only fatal-on-first-load endpoint: without a
                // username/subscription we cannot meaningfully render the header.
                val profileOk = fetchProfile(fatalOnFailure = true) ?: return@launch
                if (!profileOk) {
                    // Non-401 failure already surfaced as a blocking error.
                    return@launch
                }

                // Remaining loads run in parallel and are individually non-fatal.
                // awaitAll() joins them; Result-typed APIs never throw, so a
                // failure here only sets the transient error banner.
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

    /**
     * Re-runs [load] after a blocking error. Equivalent to "first load" from
     * the user's perspective: the screen will see [ScreenState.Loading] again,
     * not a pull-to-refresh badge.
     */
    fun retry() {
        // Cancel any zombie load job first (defensive — `loadJob` should
        // already be null on the error path, but a retry while still loading
        // would otherwise be a silent no-op).
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

    /**
     * Pull-to-refresh entry point. Never transitions back to
     * [ScreenState.Loading]: the user keeps seeing the previous content while
     * the four endpoints reload. Failures are surfaced as a transient banner.
     */
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

    /**
     * Targeted refresh of the meal-plan section only.
     *
     * Used by the parent surface after a meal-plan creation flow completes
     * successfully — we do not want to re-fetch profile / workouts / activities
     * for that mutation, just the section that actually changed. Safe to call
     * before the first [load] has completed; in that case the next [load] /
     * [refresh] will overwrite the result.
     */
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

    /**
     * Switches the meal-plan section to a different calendar day. Idempotent
     * for the same value. Clears the currently-displayed cards immediately so
     * the previous date's slots do not flash while the new fetch is in flight.
     */
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

    /**
     * Dismiss the transient error banner shown alongside content. The screen
     * calls this after a delay; clearing on a stale state is a no-op.
     */
    fun clearTransientError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun onCleared() {
        scope.cancel()
    }

    // -----------------------------------------------------------------------
    // Workout session
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // Activity check-in
    // -----------------------------------------------------------------------

    /**
     * Persists a check-in for [activityId] for *today*, then refetches the
     * day's activities so ring/row UI stays consistent with the server's
     * recomputed log values.
     */
    /**
     * [value] semantics depend on the activity kind — callers are responsible
     * for passing the correct integer: `0`/`1` for BINARY, count increment for
     * COUNTER, preset selection for PRESET. Matches [ActivityCheckinRequest.value].
     */
    fun checkIn(activityId: String, value: Int) {
        scope.launch {
            val today = todayIsoDate()
            activityApi.checkin(
                activityId = activityId,
                request = ActivityCheckinRequest(date = today, value = value),
            ).fold(
                onSuccess = {
                    // Refetch — backend may roll up streaks / daily totals.
                    fetchTodayActivities()
                },
                onFailure = { error ->
                    if (shouldLogOut(error)) {
                        tokenStorage.clearTokens()
                        sessionManager.triggerLogout()
                        return@fold
                    }
                    _state.update {
                        it.copy(errorMessage = "Не удалось сохранить активность: ${error.message}")
                    }
                },
            )
        }
    }

    // -----------------------------------------------------------------------
    // Internal fetch helpers
    // -----------------------------------------------------------------------

    /**
     * Fetches the user profile. Returns:
     *  - `true` if the fetch succeeded (state updated, store synced),
     *  - `false` if it failed in a non-fatal-but-loggable way,
     *  - `null` if the caller must abort the whole pipeline (401 -> logout
     *    triggered, or [fatalOnFailure] forced a blocking error).
     */
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

    /**
     * Non-fatal: a failed plans fetch leaves the previously-loaded list in
     * place and only surfaces a transient banner. The full-screen loading
     * phase never blocks on this.
     */
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

    /**
     * Loads meal-plan cards for [date] via [MealPlansApi.getTodayPlans] and
     * flattens each detail into the presentation-ready [TodayMealPlanCard]
     * shape. Cards are ordered by meal type (BREAKFAST → LUNCH → DINNER →
     * SNACK → other) so the UI renders a stable, chronological strip even
     * when the underlying plans were created in arbitrary order.
     *
     * Failures are non-fatal: the section keeps the previous list and the
     * transient banner is set unless one is already present (we never
     * overwrite a more important error from a sibling fetch).
     */
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

    /**
     * Stable display order for meal types on the home strip. Unknown types
     * fall to the end so a future server-side addition does not break the
     * sort.
     */
    private fun mealTypeOrder(mealType: String): Int = when (mealType.uppercase()) {
        "BREAKFAST" -> 0
        "LUNCH" -> 1
        "DINNER" -> 2
        "SNACK" -> 3
        else -> Int.MAX_VALUE
    }

    /**
     * Auth failures fall into two cases that both demand a forced logout:
     *  1. The Ktor bearer plugin already cleared tokens after a failed refresh.
     *  2. The endpoint itself returned 401 for any other reason (e.g. revoked
     *     session) and we must drop the user back to login instead of showing
     *     a misleading error banner.
     */
    private suspend fun shouldLogOut(error: Throwable): Boolean {
        val is401 = (error as? ApiException)?.statusCode == 401
        return is401 || tokenStorage.getAccessToken() == null
    }

    /**
     * Today's date in `YYYY-MM-DD` form using the device's current timezone —
     * the format the backend's `@DateTimeFormat` parser expects on every
     * activity endpoint that takes a date parameter.
     */
    private fun todayIsoDate(): String =
        Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()
}
