package com.asc.gymgenie.feature.activities

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.activity.ActivityHistoryDayResponse
import com.asc.gymgenie.activity.ActivityTodayResponse
import com.asc.gymgenie.feature.activities.components.ActivityDatePicker
import com.asc.gymgenie.feature.activities.components.ActivityTab
import com.asc.gymgenie.feature.activities.components.ActivityTabSelector
import com.asc.gymgenie.feature.home.components.ActivityRingsCard
import com.asc.gymgenie.feature.home.components.ActivityRowsCard
import com.asc.gymgenie.nutrition.todayLocalDate
import com.asc.gymgenie.presentation.ActivitiesUiState
import com.asc.gymgenie.presentation.ActivitiesViewModel
import com.asc.gymgenie.ui.components.GymGenieToolbar
import com.asc.gymgenie.ui.components.ToolbarAction
import com.asc.gymgenie.R
import com.asc.gymgenie.activity.ActivityRing
import com.asc.gymgenie.feature.home.components.activities.ringColorFor
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.RingMind
import com.asc.gymgenie.ui.theme.SoftCard
import com.asc.gymgenie.ui.theme.WarmOffWhite
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val HISTORY_WINDOW_DAYS = 30

@Composable
fun ActivitiesScreen(
    onBack: () -> Unit,
    onOpenCatalog: () -> Unit,
    refreshSignal: Int = 0,
    onOpenScheduleSettings: ((
        activityId: String,
        name: String,
        scheduleType: String?,
        scheduleDays: List<String>,
        oneOffDate: String?,
    ) -> Unit)? = null,
) {
    val koin = remember { GlobalContext.get() }
    val viewModel = remember { koin.get<ActivitiesViewModel>() }
    val state by viewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val tabOrder = remember { listOf(ActivityTab.PLAN, ActivityTab.HISTORY) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabOrder.size })
    var selectedTab by remember { mutableStateOf(ActivityTab.PLAN) }

    LaunchedEffect(refreshSignal) { viewModel.load() }

    LaunchedEffect(Unit) {
        val today = LocalDate.now()
        val start = today.minusDays((HISTORY_WINDOW_DAYS - 1).toLong())
        viewModel.loadHistory(start.toString(), today.toString())
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            selectedTab = tabOrder.getOrElse(page) { ActivityTab.PLAN }
        }
    }

    DisposableEffect(viewModel) {
        onDispose { viewModel.onCleared() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        GymGenieToolbar(
            title = "Активности",
            showBackNavigation = true,
            onBackClick = onBack,
            actions = listOf(
                ToolbarAction(
                    content = {
                        Text(text = "+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    },
                    onClick = onOpenCatalog,
                ),
            ),
        )

        Spacer(modifier = Modifier.height(4.dp))

        ActivityTabSelector(
            selectedTab = selectedTab,
            onTabSelected = { tab ->
                selectedTab = tab
                coroutineScope.launch {
                    pagerState.animateScrollToPage(tabOrder.indexOf(tab))
                }
            },
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (tabOrder[page]) {
                ActivityTab.PLAN -> PlanTab(
                    state = state,
                    onCheckIn = viewModel::checkIn,
                    onRetry = viewModel::load,
                    onDateSelected = viewModel::selectDate,
                    onOpenScheduleSettings = onOpenScheduleSettings,
                    onRemoveFromPlan = viewModel::removeFromPlan,
                )
                ActivityTab.HISTORY -> HistoryTab(
                    history = state.history,
                    isLoading = state.isHistoryLoading,
                    todayActivities = state.todayActivities,
                )
            }
        }
    }
}

@Composable
private fun PlanTab(
    state: ActivitiesUiState,
    onCheckIn: (activityId: String, value: Int) -> Unit,
    onRetry: () -> Unit,
    onDateSelected: (String) -> Unit,
    onOpenScheduleSettings: ((
        activityId: String,
        name: String,
        scheduleType: String?,
        scheduleDays: List<String>,
        oneOffDate: String?,
    ) -> Unit)? = null,
    onRemoveFromPlan: ((String) -> Unit)? = null,
) {
    val selectedDateKt = remember(state.selectedDate) {
        runCatching { kotlinx.datetime.LocalDate.parse(state.selectedDate) }
            .getOrDefault(todayLocalDate())
    }

    when {
        state.isLoading -> CenteredSpinner()
        state.error != null && state.todayActivities.isEmpty() -> Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            ErrorState(
                message = state.error.toString(),
                onRetry = onRetry,
            )
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ActivityDatePicker(
                    selectedDate = selectedDateKt,
                    onDateSelected = { date -> onDateSelected(date.toString()) },
                )
            }

            if (state.todayActivities.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Assignment,
                        message = "Нет активностей на этот день",
                        hint = "Добавь активности через каталог",
                    )
                }
            } else {
                item {
                    ActivityRowsCard(
                        activities = state.todayActivities,
                        onCheckIn = onCheckIn,
                        onRemoveFromPlan = onRemoveFromPlan,
                        onOpenScheduleSettings = if (onOpenScheduleSettings != null) {
                            { activity ->
                                onOpenScheduleSettings(
                                    activity.activityId,
                                    activity.name,
                                    activity.scheduleType,
                                    activity.scheduleDays,
                                    activity.oneOffDate,
                                )
                            }
                        } else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(
    history: List<ActivityHistoryDayResponse>,
    isLoading: Boolean,
    todayActivities: List<ActivityTodayResponse>,
) {
    if (isLoading && history.isEmpty()) {
        CenteredSpinner()
        return
    }

    val displayDays = remember(history) {
        val last7 = history.takeLast(7)
        if (last7.isNotEmpty()) {
            last7
        } else {
            val today = LocalDate.now()
            (6 downTo 0).map { offset ->
                ActivityHistoryDayResponse(
                    date = today.minusDays(offset.toLong()).toString(),
                    completionPct = 0.0,
                    logs = emptyList(),
                )
            }
        }
    }

    var selectedDate by remember(history) {
        mutableStateOf(history.lastOrNull()?.date ?: LocalDate.now().toString())
    }

    val selectedDay = remember(history, selectedDate) {
        history.firstOrNull { it.date == selectedDate }
    }

    val targetPct = (selectedDay?.completionPct?.coerceIn(0.0, 1.0) ?: 0.0).toFloat()
    val animatedPct by animateFloatAsState(
        targetValue = targetPct,
        animationSpec = tween(durationMillis = 400),
        label = "progress",
    )

    val historyActivities = remember(selectedDay, todayActivities) {
        todayActivities.map { activity ->
            val historyLog = selectedDay?.logs?.find { it.activityId == activity.activityId }
            if (historyLog != null) {
                activity.copy(logValue = historyLog.value)
            } else {
                activity.copy(logValue = 0)
            }
        }
    }

    val completionRows = remember(selectedDay, todayActivities) {
        if (selectedDay != null) buildCompletionRows(selectedDay, todayActivities)
        else emptyList()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 16.dp,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ActivityRingsCard(activities = historyActivities)
        }

        item {
            DayStrip(
                history = displayDays,
                selectedDate = selectedDate,
                onSelect = { selectedDate = it },
            )
        }

        item {
            HistoryProgressBar(fraction = animatedPct)
        }

        if (selectedDay != null && completionRows.isNotEmpty()) {
            items(completionRows, key = { it.activityId }) { row ->
                ActivityCompletionRow(row = row)
            }
        } else {
            item { DayLogsEmpty() }
        }
    }
}

private data class CompletionRow(
    val activityId: String,
    val name: String,
    val ring: String,
    val isDone: Boolean,
    val achieved: Int,
    val goal: Int,
    val unit: String,
)

private fun buildCompletionRows(
    day: ActivityHistoryDayResponse,
    allActivities: List<ActivityTodayResponse>,
): List<CompletionRow> {
    val logsByActivity = day.logs.associateBy { it.activityId }

    return allActivities.map { activity ->
        val log = logsByActivity[activity.activityId]
        val achieved = log?.value ?: 0
        val goal = activity.goal ?: 1
        val isDone = achieved >= goal
        CompletionRow(
            activityId = activity.activityId,
            name = activity.name,
            ring = activity.ring,
            isDone = isDone,
            achieved = achieved,
            goal = goal,
            unit = activity.unit.orEmpty(),
        )
    }
}

@Composable
private fun DayStrip(
    history: List<ActivityHistoryDayResponse>,
    selectedDate: String?,
    onSelect: (String) -> Unit,
) {
    val lastDays = remember(history) { history.takeLast(7) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        lastDays.forEach { day ->
            val isSelected = day.date == selectedDate
            val pct = (day.completionPct.coerceIn(0.0, 1.0) * 100).toInt()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(day.date) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(if (isSelected) RingMind else SoftCard),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$pct%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else DeepInk,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatStripLabel(day.date),
                    fontSize = 12.sp,
                    color = DeepInk,
                )
            }
        }
    }
}

@Composable
private fun HistoryProgressBar(fraction: Float) {
    val safe = fraction.coerceIn(0f, 1f)
    val pctText = "${(safe * 100).toInt()}%"

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Выполнение",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MutedText,
            )
            Text(
                text = pctText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFEEEEF1)),
        ) {
            if (safe > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = safe)
                        .height(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(AccentOrange),
                )
            }
        }
    }
}

@Composable
private fun ActivityCompletionRow(row: CompletionRow) {
    val rowRingColor = ringColorFor(row.ring)
    val unitSuffix = if (row.unit.isNotBlank()) " ${row.unit}" else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (row.isDone) rowRingColor.copy(alpha = 0.15f)
                    else Color(0xFFD94444).copy(alpha = 0.12f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(
                    when (row.ring) {
                        ActivityRing.MOVE.name -> R.drawable.ic_activity_run
                        ActivityRing.MIND.name -> R.drawable.ic_activity_mind
                        else -> R.drawable.ic_activity_schedule
                    }
                ),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = row.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = DeepInk,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${row.achieved}/${row.goal}$unitSuffix",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (row.isDone) RingMind else MutedText,
        )
    }
}

@Composable
private fun DayLogsEmpty() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SoftCard)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Здесь пока ничего нет", fontSize = 17.sp, color = MutedText)
    }
}

@Composable
private fun CenteredSpinner() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AccentOrange)
    }
}

@Composable
private fun EmptyState(icon: ImageVector, message: String, hint: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SoftCard)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MutedText,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepInk,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = hint, fontSize = 13.sp, color = MutedText)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SoftCard)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Warning,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = com.asc.gymgenie.ui.theme.AccentOrange,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Не удалось загрузить",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepInk,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = message, fontSize = 13.sp, color = MutedText)
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(AccentOrange)
                .clickable { onRetry() }
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Text(
                text = "Повторить",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

private fun formatStripLabel(isoDate: String): String =
    runCatching {
        LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("dd.MM"))
    }.getOrDefault("?")
