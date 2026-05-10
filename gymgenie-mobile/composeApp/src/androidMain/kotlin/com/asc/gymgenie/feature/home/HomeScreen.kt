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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.activity.ActivityApi
import com.asc.gymgenie.activity.ActivityTodayResponse
import com.asc.gymgenie.feature.home.components.ActivityRingsCard
import com.asc.gymgenie.feature.home.components.ActivityRowsCard
import com.asc.gymgenie.feature.home.components.CoursesBlock
import com.asc.gymgenie.feature.home.components.HomeHeaderSection
import com.asc.gymgenie.feature.home.components.MealPlanSection
import com.asc.gymgenie.feature.home.components.NoWorkoutPlaceholder
import com.asc.gymgenie.feature.home.components.WorkoutTodayPager
import com.asc.gymgenie.feature.home.components.resolveTodaySlot
import com.asc.gymgenie.nutrition.MealPlansApi
import com.asc.gymgenie.nutrition.TodayMealPlanCard
import com.asc.gymgenie.presentation.HomeViewModel
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.ui.ScreenState
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.SoftCard
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.user.UserApi
import com.asc.gymgenie.user.UserProfileStore
import com.asc.gymgenie.workout.ActiveWorkoutSession
import com.asc.gymgenie.workout.WorkoutApi
import com.asc.gymgenie.workout.WorkoutPlanShortResponse
import org.koin.core.context.GlobalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home screen — premium redesign.
 *
 * Keeps the existing KMM [HomeViewModel] contract intact and only changes
 * the UI layer. Navigation to Activities / ActivityCatalog is hoisted to
 * the parent ([com.asc.gymgenie.feature.main.MainScreen]) via callbacks, so
 * this screen stays agnostic of the surrounding navigation container.
 */
@Composable
fun HomeScreen(
    tokenStorage: TokenStorage,
    userProfileStore: UserProfileStore,
    onLogout: () -> Unit,
    onOpenActivities: () -> Unit,
    onOpenCatalog: () -> Unit,
    onViewPlan: (WorkoutPlanShortResponse) -> Unit,
    onSessionReady: (ActiveWorkoutSession) -> Unit,
    onCreateMealPlan: () -> Unit,
    onViewMealPlan: (planId: String) -> Unit,
    /**
     * Bumped by the parent whenever a meal-plan was created or deleted in a
     * pushed surface (manual creation flow, AI coach, ...). Each new value
     * triggers a targeted reload of the meal-plan section without re-fetching
     * the rest of the dashboard.
     */
    mealPlansReloadKey: Int = 0,
    modifier: Modifier = Modifier,
) {
    val koin = remember { GlobalContext.get() }
    val viewModel = remember {
        HomeViewModel(
            userApi = koin.get<UserApi>(),
            workoutApi = koin.get<WorkoutApi>(),
            activityApi = koin.get<ActivityApi>(),
            mealPlansApi = koin.get<MealPlansApi>(),
            tokenStorage = tokenStorage,
            userProfileStore = userProfileStore,
            onLogout = onLogout,
        )
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.onCleared() }
    }

    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        if (!state.isContentLoaded) {
            viewModel.load()
        }
    }

    // Targeted refresh after a meal-plan mutation in a pushed surface. The
    // initial composition starts at `0` and the very first `load()` already
    // covers it, so we skip the leading edge to avoid a double fetch.
    LaunchedEffect(mealPlansReloadKey) {
        if (mealPlansReloadKey > 0) {
            viewModel.refreshMealPlans()
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
                    onRefresh = viewModel::refresh,
                    onViewPlan = onViewPlan,
                    onStartPlan = { plan -> viewModel.startWorkout(plan.id, plan.name) },
                    onOpenActivities = onOpenActivities,
                    onOpenCatalog = onOpenCatalog,
                    onViewMealPlan = onViewMealPlan,
                    // Meal plan creation is hoisted to the parent so the
                    // navigation tree owns the manual / AI flow surfaces.
                    onCreateMealPlan = onCreateMealPlan,
                    onCheckIn = viewModel::checkIn,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentWithPullToRefresh(
    state: com.asc.gymgenie.presentation.HomeUiState,
    onRefresh: () -> Unit,
    onViewPlan: (WorkoutPlanShortResponse) -> Unit,
    onStartPlan: (WorkoutPlanShortResponse) -> Unit,
    onOpenActivities: () -> Unit,
    onOpenCatalog: () -> Unit,
    onViewMealPlan: (planId: String) -> Unit,
    onCreateMealPlan: () -> Unit,
    onCheckIn: (String, Int) -> Unit,
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
            username = state.username.ifBlank { "друг" },
            streakDays = state.streakDays,
            activePlans = state.activeWorkoutPlans,
            todayActivities = state.todayActivities,
            todayMealPlans = state.todayMealPlans,
            onViewPlan = onViewPlan,
            onStartPlan = onStartPlan,
            onOpenActivities = onOpenActivities,
            onOpenCatalog = onOpenCatalog,
            onViewMealPlan = onViewMealPlan,
            onCreateMealPlan = onCreateMealPlan,
            onCheckIn = onCheckIn,
            isLoadingSession = state.isLoadingSession,
            sessionError = state.sessionError,
            transientError = state.errorMessage,
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
        Text(text = "⚠️", fontSize = 40.sp)
        Spacer(modifier = Modifier.height(12.dp))
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
    streakDays: Int,
    activePlans: List<WorkoutPlanShortResponse>,
    todayActivities: List<ActivityTodayResponse>,
    todayMealPlans: List<TodayMealPlanCard>,
    onViewPlan: (WorkoutPlanShortResponse) -> Unit,
    onStartPlan: (WorkoutPlanShortResponse) -> Unit,
    onOpenActivities: () -> Unit,
    onOpenCatalog: () -> Unit,
    onViewMealPlan: (planId: String) -> Unit,
    onCreateMealPlan: () -> Unit,
    onCheckIn: (String, Int) -> Unit,
    isLoadingSession: Boolean,
    sessionError: String?,
    transientError: String?,
) {
    val today = remember { currentDayOfWeek() }
    val todaySlot = remember(activePlans, today) {
        resolveTodaySlot(activePlans = activePlans, today = today)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            HomeHeaderSection(
                username = username,
                streakDays = streakDays,
                date = remember { currentDateLabel() },
            )
        }

        item {
            SectionHeaderPremium(
                title = todaySlot.title,
                subtitle = todaySlot.subtitle,
                actionTitle = if (todaySlot.showAll) "Все" else null,
                onAction = if (todaySlot.showAll) {
                    { /* TODO: navigate to plans list when route is available */ }
                } else null,
            )
        }

        item {
            if (todaySlot.plans.isEmpty()) {
                NoWorkoutPlaceholder(onCreate = { /* TODO: hook up creation route */ })
            } else {
                WorkoutTodayPager(
                    plans = todaySlot.plans,
                    isRecommended = todaySlot.isRecommended,
                    onView = onViewPlan,
                    onStart = onStartPlan,
                )
            }
        }

        if (isLoadingSession || sessionError != null || transientError != null) {
            item {
                SessionStatusBanner(
                    isLoading = isLoadingSession,
                    errorMessage = sessionError ?: transientError,
                )
            }
        }

        item {
            SectionHeaderPremium(
                title = "Активности",
                subtitle = "Отметить вручную — без умных часов",
                actionTitle = "Ещё",
                onAction = onOpenActivities,
            )
        }

        item { ActivityRingsCard(activities = todayActivities) }

        item { ActivityRowsCard(activities = todayActivities, onCheckIn = onCheckIn) }

        item {
            AddActivityButton(onClick = onOpenCatalog)
        }

        item {
            MealPlanSection(
                todayPlans = todayMealPlans,
                onPlanTap = onViewMealPlan,
                onCreatePlan = onCreateMealPlan,
            )
        }

        item {
            SectionHeaderPremium(
                title = "Курсы тренеров",
                subtitle = "От профи под твои цели",
                actionTitle = "Все",
                onAction = { /* TODO: hook up courses screen once backend exposes the list */ },
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
            Text(text = "⚠️", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = errorMessage,
                fontSize = 13.sp,
                color = MutedText,
            )
        }
    }
}

@Composable
fun SectionHeaderPremium(
    title: String,
    subtitle: String? = null,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.widthIn(max = 260.dp)) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepInk,
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (actionTitle != null && onAction != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onAction() }
                    .padding(6.dp),
            ) {
                Text(
                    text = actionTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentOrange,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "→",
                    fontSize = 14.sp,
                    color = AccentOrange,
                )
            }
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
        Text(text = "+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepInk)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Добавить активность",
            fontSize = 15.sp,
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

/**
 * Returns the current weekday in `DayOfWeek` enum-name form (e.g. `MONDAY`)
 * — matches the casing used by the backend / [WorkoutPlanShortResponse.scheduleDays].
 *
 * `java.util.Calendar` uses `SUNDAY = 1 … SATURDAY = 7`; we map it to the
 * Java/Kotlin `DayOfWeek` ordering so the comparison in `resolveTodaySlot`
 * is unambiguous.
 */
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
