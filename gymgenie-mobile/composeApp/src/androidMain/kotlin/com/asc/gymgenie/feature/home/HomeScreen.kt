package com.asc.gymgenie.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.activity.ActivityTodayResponse
import com.asc.gymgenie.feature.home.components.ActivityRingsCard
import com.asc.gymgenie.feature.home.components.ActivityRowsCard
import com.asc.gymgenie.feature.home.components.CoursesBlock
import com.asc.gymgenie.feature.home.components.HomeHeaderSection
import com.asc.gymgenie.feature.home.components.MealPlanSection
import com.asc.gymgenie.feature.home.components.NoWorkoutPlaceholder
import com.asc.gymgenie.feature.home.components.SectionHeaderPremium
import com.asc.gymgenie.feature.home.components.WorkoutTodayPager
import com.asc.gymgenie.feature.home.components.resolveTodaySlot
import com.asc.gymgenie.nutrition.TodayMealPlanCard
import com.asc.gymgenie.presentation.HomeViewModel
import com.asc.gymgenie.ui.ScreenState
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.SoftCard
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.user.UserProfileResponse
import com.asc.gymgenie.user.UserProfileStore
import com.asc.gymgenie.workout.ActiveWorkoutSession
import com.asc.gymgenie.workout.WorkoutPlanShortResponse
import kotlinx.datetime.LocalDate
import org.koin.core.context.GlobalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onOpenActivities: () -> Unit,
    onOpenCatalog: () -> Unit,
    onViewPlan: (WorkoutPlanShortResponse) -> Unit,
    onSessionReady: (ActiveWorkoutSession) -> Unit,
    onCreateMealPlan: (mealType: String?, date: String?) -> Unit,
    onViewMealPlan: (planId: String) -> Unit,
    onCreateWorkout: () -> Unit = {},
    mealPlansReloadKey: Int = 0,
    activitiesRefreshSignal: Int = 0,
    onOpenPaywall: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onSwitchToProfile: () -> Unit = {},
    onSwitchToWorkouts: () -> Unit = {},
    onOpenActivityScheduleSettings: (activityId: String, name: String, scheduleType: String?, scheduleDays: List<String>, oneOffDate: String?) -> Unit = { _, _, _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    val koin = remember { GlobalContext.get() }
    val viewModel = remember { koin.get<HomeViewModel>() }
    val userProfileStore = remember { koin.get<UserProfileStore>() }

    val state by viewModel.state.collectAsState()
    val profile by userProfileStore.profile.collectAsState()
    val displayName = remember(profile) { buildDisplayName(profile) }

    LaunchedEffect(Unit) {
        if (!state.isContentLoaded) {
            viewModel.load()
        } else {
            viewModel.refresh()
        }
    }

    LaunchedEffect(mealPlansReloadKey) {
        if (mealPlansReloadKey > 0) {
            viewModel.refreshMealPlans()
        }
    }

    LaunchedEffect(activitiesRefreshSignal) {
        if (activitiesRefreshSignal > 0) {
            viewModel.refreshTodayActivities()
        }
    }

    LaunchedEffect(state.pendingSession) {
        state.pendingSession?.let { session ->
            onSessionReady(session)
            viewModel.clearPendingSession()
        }
    }

    LaunchedEffect(state.errorMessage) {
        if (state.errorMessage != null && state.isContentLoaded) {
            delay(4_000)
            viewModel.clearTransientError()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.activityError) {
        state.activityError?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActivityError()
        }
    }

    val onPastDateBlocked: () -> Unit = {
        scope.launch { snackbarHostState.showSnackbar("Создание на вчерашний день недоступно") }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            when (val screen = state.screenState) {
                ScreenState.Loading -> LoadingState()
                is ScreenState.Error -> ErrorState(
                    message = screen.message,
                    onRetry = viewModel::retry,
                )
                ScreenState.Content -> ContentWithPullToRefresh(
                    state = state,
                    displayName = displayName,
                    onRefresh = viewModel::refresh,
                    onViewPlan = onViewPlan,
                    onStartPlan = { plan -> viewModel.startWorkout(plan.id, plan.name) },
                    onOpenActivities = onOpenActivities,
                    onOpenCatalog = onOpenCatalog,
                    onViewMealPlan = onViewMealPlan,
                    onCreateMealPlan = onCreateMealPlan,
                    onCreateWorkout = onCreateWorkout,
                    onPastDateBlocked = onPastDateBlocked,
                    onCheckIn = viewModel::checkIn,
                    onRemoveFromPlan = viewModel::removeFromPlan,
                    selectedMealDate = state.selectedMealDate,
                    onMealDateSelected = viewModel::selectMealDate,
                    isPremium = state.subscriptionType != "FREE",
                    onOpenPaywall = onOpenPaywall,
                    onNotificationsClick = onNotificationsClick,
                    onSwitchToProfile = onSwitchToProfile,
                    onSwitchToWorkouts = onSwitchToWorkouts,
                    onOpenActivityScheduleSettings = onOpenActivityScheduleSettings,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentWithPullToRefresh(
    state: com.asc.gymgenie.presentation.HomeUiState,
    displayName: String,
    onRefresh: () -> Unit,
    onViewPlan: (WorkoutPlanShortResponse) -> Unit,
    onStartPlan: (WorkoutPlanShortResponse) -> Unit,
    onOpenActivities: () -> Unit,
    onOpenCatalog: () -> Unit,
    onViewMealPlan: (planId: String) -> Unit,
    onCreateMealPlan: (mealType: String?, date: String?) -> Unit,
    onCreateWorkout: () -> Unit,
    onPastDateBlocked: () -> Unit,
    onCheckIn: (String, Int) -> Unit,
    onRemoveFromPlan: (String) -> Unit,
    selectedMealDate: LocalDate,
    onMealDateSelected: (LocalDate) -> Unit,
    isPremium: Boolean,
    onOpenPaywall: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSwitchToProfile: () -> Unit,
    onSwitchToWorkouts: () -> Unit,
    onOpenActivityScheduleSettings: (activityId: String, name: String, scheduleType: String?, scheduleDays: List<String>, oneOffDate: String?) -> Unit,
) {
    val refreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        state = refreshState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = refreshState,
                isRefreshing = state.isRefreshing,
                containerColor = Color.White,
                color = Coral,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
    ) {
        HomeContent(
            username = displayName,
            activePlans = state.activeWorkoutPlans,
            todayActivities = state.todayActivities,
            todayMealPlans = state.todayMealPlans,
            isLoadingMealPlans = state.isLoadingMealPlans,
            selectedMealDate = selectedMealDate,
            onMealDateSelected = onMealDateSelected,
            onViewPlan = onViewPlan,
            onStartPlan = onStartPlan,
            onOpenActivities = onOpenActivities,
            onOpenCatalog = onOpenCatalog,
            onViewMealPlan = onViewMealPlan,
            onCreateMealPlan = onCreateMealPlan,
            onCreateWorkout = onCreateWorkout,
            onPastDateBlocked = onPastDateBlocked,
            onCheckIn = onCheckIn,
            onRemoveFromPlan = onRemoveFromPlan,
            isLoadingSession = state.isLoadingSession,
            sessionError = state.sessionError,
            isPremium = isPremium,
            onOpenPaywall = onOpenPaywall,
            onNotificationsClick = onNotificationsClick,
            onSwitchToProfile = onSwitchToProfile,
            onSwitchToWorkouts = onSwitchToWorkouts,
            onOpenActivityScheduleSettings = onOpenActivityScheduleSettings,
        )
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = AccentOrange)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Загрузка...", fontSize = 14.sp, color = MutedText)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            fontSize = 14.sp,
            color = MutedText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Повторить", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HomeContent(
    username: String,
    activePlans: List<WorkoutPlanShortResponse>,
    todayActivities: List<ActivityTodayResponse>,
    todayMealPlans: List<TodayMealPlanCard>,
    isLoadingMealPlans: Boolean,
    selectedMealDate: LocalDate,
    onMealDateSelected: (LocalDate) -> Unit,
    onViewPlan: (WorkoutPlanShortResponse) -> Unit,
    onStartPlan: (WorkoutPlanShortResponse) -> Unit,
    onOpenActivities: () -> Unit,
    onOpenCatalog: () -> Unit,
    onViewMealPlan: (planId: String) -> Unit,
    onCreateMealPlan: (mealType: String?, date: String?) -> Unit,
    onCreateWorkout: () -> Unit,
    onPastDateBlocked: () -> Unit,
    onCheckIn: (String, Int) -> Unit,
    onRemoveFromPlan: (String) -> Unit,
    isLoadingSession: Boolean,
    sessionError: String?,
    isPremium: Boolean,
    onOpenPaywall: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSwitchToProfile: () -> Unit,
    onSwitchToWorkouts: () -> Unit,
    onOpenActivityScheduleSettings: (activityId: String, name: String, scheduleType: String?, scheduleDays: List<String>, oneOffDate: String?) -> Unit,
) {
    val today = remember { currentDayOfWeek() }
    val todaySlot = remember(activePlans, today) {
        resolveTodaySlot(activePlans = activePlans, today = today)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HomeHeaderSection(
                username = username,
                date = remember { currentDateLabel() },
                onNotificationsClick = onNotificationsClick,
                onProfileClick = onSwitchToProfile,
            )
        }

        item {
            SectionHeaderPremium(
                title = todaySlot.title,
                subtitle = todaySlot.subtitle,
                actionTitle = if (todaySlot.showAll) "Все" else null,
                onAction = if (todaySlot.showAll) {
                    { onSwitchToWorkouts() }
                } else null,
                modifier = Modifier.padding(top = 24.dp),
            )
        }

        item {
            if (todaySlot.plans.isEmpty()) {
                NoWorkoutPlaceholder(onCreate = onCreateWorkout)
            } else {
                WorkoutTodayPager(
                    plans = todaySlot.plans,
                    isRecommended = todaySlot.isRecommended,
                    onView = onViewPlan,
                    onStart = onStartPlan,
                )
            }
        }

        if (isLoadingSession || sessionError != null) {
            item {
                SessionStatusBanner(
                    isLoading = isLoadingSession,
                    errorMessage = sessionError,
                )
            }
        }

        item {
            SectionHeaderPremium(
                title = "Активности",
                subtitle = "Отметить вручную — без умных часов",
                actionTitle = "Ещё",
                onAction = onOpenActivities,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        item { ActivityRingsCard(activities = todayActivities) }

        item {
            ActivityRowsCard(
                activities = todayActivities,
                onCheckIn = onCheckIn,
                onRemoveFromPlan = onRemoveFromPlan,
                onOpenScheduleSettings = { activity ->
                    onOpenActivityScheduleSettings(
                        activity.activityId,
                        activity.name,
                        activity.scheduleType,
                        activity.scheduleDays,
                        activity.oneOffDate,
                    )
                },
            )
        }

        item {
            AddActivityButton(onClick = onOpenCatalog)
        }

        item {
            MealPlanSection(
                todayPlans = todayMealPlans,
                isLoading = isLoadingMealPlans,
                selectedDate = selectedMealDate,
                onDateSelected = onMealDateSelected,
                onPlanTap = onViewMealPlan,
                onCreatePlan = onCreateMealPlan,
                onPastDateBlocked = onPastDateBlocked,
                isPremium = isPremium,
                onOpenPaywall = onOpenPaywall,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        item {
            SectionHeaderPremium(
                title = "Курсы тренеров",
                subtitle = "От профи под твои цели",
                actionTitle = "Все",
                onAction = { },
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        item { CoursesBlock() }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun SessionStatusBanner(isLoading: Boolean, errorMessage: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SoftCard)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = AccentOrange,
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Загружаем тренировку...",
                fontSize = 14.sp,
                color = DeepInk,
            )
        } else if (errorMessage != null) {
            Text(
                text = errorMessage,
                fontSize = 13.sp,
                color = MutedText,
            )
        }
    }
}

@Composable
private fun AddActivityButton(onClick: () -> Unit) {
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .drawBehind { drawDashedBorder(dashEffect) }
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(text = "+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepInk)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Добавить активность",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepInk,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDashedBorder(effect: PathEffect) {
    val stroke = Stroke(width = 3f, pathEffect = effect)
    drawRoundRectBorder(
        color = DeepInk.copy(alpha = 0.35f),
        size = Size(size.width, size.height),
        stroke = stroke,
        cornerPx = 16.dp.toPx(),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundRectBorder(
    color: Color,
    size: Size,
    stroke: Stroke,
    cornerPx: Float,
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(0f, 0f),
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerPx, cornerPx),
        style = stroke,
    )
}

private fun currentDateLabel(): String {
    val formatter = SimpleDateFormat("EEEE, d MMMM", Locale("ru", "RU"))
    return formatter.format(Date())
}

private fun buildDisplayName(profile: UserProfileResponse?): String {
    if (profile == null) return "друг"
    val firstName = profile.firstName
    if (!firstName.isNullOrBlank()) return firstName
    return "друг"
}

private fun currentDayOfWeek(): String {
    val calendar = java.util.Calendar.getInstance()
    return when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
        java.util.Calendar.MONDAY -> "MONDAY"
        java.util.Calendar.TUESDAY -> "TUESDAY"
        java.util.Calendar.WEDNESDAY -> "WEDNESDAY"
        java.util.Calendar.THURSDAY -> "THURSDAY"
        java.util.Calendar.FRIDAY -> "FRIDAY"
        java.util.Calendar.SATURDAY -> "SATURDAY"
        java.util.Calendar.SUNDAY -> "SUNDAY"
        else -> "MONDAY"
    }
}
