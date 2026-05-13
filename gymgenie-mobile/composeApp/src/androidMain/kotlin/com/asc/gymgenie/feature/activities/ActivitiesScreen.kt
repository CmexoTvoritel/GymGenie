package com.asc.gymgenie.feature.activities

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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.activity.ActivityHistoryDayResponse
import com.asc.gymgenie.activity.ActivityLogResponse
import com.asc.gymgenie.feature.home.components.ActivityRowsCard
import com.asc.gymgenie.presentation.ActivitiesViewModel
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.RingMind
import com.asc.gymgenie.ui.theme.SoftCard
import com.asc.gymgenie.ui.theme.WarmOffWhite
import org.koin.core.context.GlobalContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class ActivityTab { TODAY, HISTORY }

/**
 * Window of days the History tab loads in a single fetch. Kept as a constant
 * here so the start/end date math has one source of truth — and so the strip
 * UI can size itself with confidence.
 */
private const val HISTORY_WINDOW_DAYS = 7

/**
 * Activities screen. Hosts two tabs:
 *  - "Сегодня" — today's plan with the standard check-in interactions.
 *  - "История" — last [HISTORY_WINDOW_DAYS] days, ring-style completion
 *    indicator per day plus the raw log list for the selected day.
 *
 * @param refreshSignal increments whenever the parent wants the today list
 *   re-fetched (e.g. after returning from the catalog where the plan may have
 *   changed). The screen re-loads on every distinct value, including the
 *   initial composition.
 */
@Composable
fun ActivitiesScreen(
    onBack: () -> Unit,
    onOpenCatalog: () -> Unit,
    refreshSignal: Int = 0,
) {
    val koin = remember { GlobalContext.get() }
    val viewModel = remember { koin.get<ActivitiesViewModel>() }
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableStateOf(ActivityTab.TODAY) }

    LaunchedEffect(refreshSignal) { viewModel.load() }

    LaunchedEffect(Unit) {
        val today = LocalDate.now()
        val start = today.minusDays((HISTORY_WINDOW_DAYS - 1).toLong())
        viewModel.loadHistory(start.toString(), today.toString())
    }

    DisposableEffect(viewModel) {
        onDispose { viewModel.onCleared() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .statusBarsPadding(),
    ) {
        TopBar(onBack = onBack, onAdd = onOpenCatalog)
        TabBar(selected = selectedTab, onSelect = { selectedTab = it })

        when (selectedTab) {
            ActivityTab.TODAY -> TodayTab(
                isLoading = state.isLoading,
                activities = state.todayActivities,
                error = state.error,
                onCheckIn = viewModel::checkIn,
                onRetry = viewModel::load,
            )
            ActivityTab.HISTORY -> HistoryTab(
                history = state.history,
                isLoading = state.isHistoryLoading,
            )
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SoftCard)
                .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "‹", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DeepInk)
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Активности",
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DeepInk,
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AccentOrange)
                .clickable { onAdd() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun TabBar(selected: ActivityTab, onSelect: (ActivityTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        TabButton(title = "Сегодня", isSelected = selected == ActivityTab.TODAY) {
            onSelect(ActivityTab.TODAY)
        }
        TabButton(title = "История", isSelected = selected == ActivityTab.HISTORY) {
            onSelect(ActivityTab.HISTORY)
        }
    }
}

@Composable
private fun TabButton(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) DeepInk else MutedText,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (isSelected) AccentOrange else Color.Transparent),
        )
    }
}

@Composable
private fun TodayTab(
    isLoading: Boolean,
    activities: List<com.asc.gymgenie.activity.ActivityTodayResponse>,
    error: String?,
    onCheckIn: (activityId: String, value: Int) -> Unit,
    onRetry: () -> Unit,
) {
    when {
        isLoading -> CenteredSpinner()
        error != null && activities.isEmpty() -> ErrorState(message = error, onRetry = onRetry)
        activities.isEmpty() -> EmptyState(
            icon = "📋",
            message = "Нет активностей на сегодня",
            hint = "Добавь активности через каталог →",
        )
        else -> LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ActivityRowsCard already owns its filter chips + preset sheet,
            // so we plug it in as a single item and let it manage its own
            // internal interaction state.
            item {
                ActivityRowsCard(
                    activities = activities,
                    onCheckIn = onCheckIn,
                )
            }
        }
    }
}

@Composable
private fun HistoryTab(history: List<ActivityHistoryDayResponse>, isLoading: Boolean) {
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

    // Default the strip selection to the most recent day in the response so
    // the user sees today's logs (if any) instead of an empty bucket.
    var selectedDate by remember(history) {
        mutableStateOf(history.lastOrNull()?.date)
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
                items(selectedDay.logs) { log -> LogRow(log = log) }
            }
        }
    }
}

@Composable
private fun DayStrip(
    history: List<ActivityHistoryDayResponse>,
    selectedDate: String?,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        history.forEach { day ->
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
private fun LogRow(log: ActivityLogResponse) {
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
        // Without a join from ids to names on the client side we keep this
        // as a stable ID label; the value column is the actionable signal.
        Text(
            text = log.activityId,
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
