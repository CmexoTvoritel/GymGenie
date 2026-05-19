package com.asc.gymgenie.feature.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.activity.ActivityHistoryDayResponse
import com.asc.gymgenie.activity.ActivityLogResponse
import com.asc.gymgenie.activity.ActivityTodayResponse
import com.asc.gymgenie.feature.activities.components.ActivityDatePicker
import com.asc.gymgenie.feature.activities.components.ActivityTab
import com.asc.gymgenie.feature.activities.components.ActivityTabSelector
import com.asc.gymgenie.feature.home.components.ActivityRowsCard
import com.asc.gymgenie.nutrition.todayLocalDate
import com.asc.gymgenie.presentation.ActivitiesUiState
import com.asc.gymgenie.presentation.ActivitiesViewModel
import com.asc.gymgenie.ui.components.GymGenieToolbar
import com.asc.gymgenie.ui.components.ToolbarAction
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
) {
    val selectedDateKt = remember(state.selectedDate) {
        runCatching { kotlinx.datetime.LocalDate.parse(state.selectedDate) }
            .getOrDefault(todayLocalDate())
    }

    when {
        state.isLoading -> CenteredSpinner()
        state.error != null && state.todayActivities.isEmpty() -> ErrorState(
            message = state.error.toString(),
            onRetry = onRetry,
        )
        else -> LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
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
                        icon = "📋",
                        message = "Нет активностей на этот день",
                        hint = "Добавь активности через каталог →",
                    )
                }
            } else {
                item {
                    ActivityRowsCard(
                        activities = state.todayActivities,
                        onCheckIn = onCheckIn,
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
    if (history.isEmpty()) {
        EmptyState(
            icon = "⌛",
            message = "История пуста",
            hint = "Начни отмечать активности — они появятся здесь",
        )
        return
    }

    var selectedDate by remember(history) {
        mutableStateOf(history.lastOrNull()?.date)
    }

    val avgCompletion = remember(history) {
        if (history.isEmpty()) 0 else (history.map { it.completionPct }.average() * 100).toInt()
    }
    val totalLogs = remember(history) {
        history.sumOf { it.logs.size }
    }
    val perfectDays = remember(history) {
        history.count { it.completionPct >= 1.0 }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatCard(
                    value = "$avgCompletion%",
                    label = "Среднее",
                    emoji = "📊",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = totalLogs.toString(),
                    label = "Записей",
                    emoji = "✅",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = perfectDays.toString(),
                    label = "100% дней",
                    emoji = "🏆",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            DayStrip(
                history = history,
                selectedDate = selectedDate,
                onSelect = { selectedDate = it },
            )
        }

        val selectedDay = history.firstOrNull { it.date == selectedDate }
        if (selectedDay != null) {
            if (selectedDay.logs.isEmpty()) {
                item { DayLogsEmpty() }
            } else {
                items(selectedDay.logs) { log ->
                    LogRow(log = log, todayActivities = todayActivities)
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    emoji: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFEDEDEF), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = emoji, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DeepInk,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MutedText,
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
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        lastDays.forEach { day ->
            val isSelected = day.date == selectedDate
            val pct = (day.completionPct.coerceIn(0.0, 1.0) * 100).toInt()
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(day.date) }
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) RingMind else SoftCard),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$pct%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else DeepInk,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatStripLabel(day.date),
                    fontSize = 10.sp,
                    color = MutedText,
                )
            }
        }
    }
}

@Composable
private fun LogRow(log: ActivityLogResponse, todayActivities: List<ActivityTodayResponse>) {
    val activityName = remember(log.activityId, todayActivities) {
        todayActivities.firstOrNull { it.activityId == log.activityId }?.name ?: log.activityId
    }

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
                .background(RingMind.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "✓", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RingMind)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = activityName,
            fontSize = 12.sp,
            color = MutedText,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = log.value.toString(),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = DeepInk,
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
        Text(text = "Нет записей за этот день", fontSize = 14.sp, color = MutedText)
    }
}

@Composable
private fun CenteredSpinner() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AccentOrange)
    }
}

@Composable
private fun EmptyState(icon: String, message: String, hint: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SoftCard)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = icon, fontSize = 32.sp)
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
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SoftCard)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "⚠️", fontSize = 32.sp)
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
}

private fun formatStripLabel(isoDate: String): String =
    runCatching {
        LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("dd.MM"))
    }.getOrDefault("?")
