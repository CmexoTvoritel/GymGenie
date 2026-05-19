package com.asc.gymgenie.feature.workout_history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.feature.workout_history.components.DateSelector
import com.asc.gymgenie.feature.workout_history.components.DaySummary
import com.asc.gymgenie.feature.workout_history.components.EmptyState
import com.asc.gymgenie.feature.workout_history.components.HistoryCard
import com.asc.gymgenie.ui.components.GymGenieToolbar
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.workout.WorkoutSessionHistoryItem
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val InkBlack = Color(0xFF0A0A0A)
private val MutedGray = Color(0xFF8B8B92)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    onBack: () -> Unit,
    onSessionClick: (WorkoutSessionHistoryItem) -> Unit = {},
) {
    val viewModel = rememberWorkoutHistoryViewModel()
    val state by viewModel.state.collectAsState()

    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        GymGenieToolbar(
            title = "Статистика",
            showBackNavigation = true,
            onBackClick = onBack,
        )

        val refreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh() },
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Аналитика твоих тренировок",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkBlack,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                item {
                    DateSelector(
                        selectedDate = state.selectedDate,
                        today = today,
                        weekDates = state.weekDates,
                        weekSessions = state.weekSessions,
                        onDateSelected = viewModel::selectDate,
                        onShiftWeek = viewModel::shiftWeek,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                val completedSessions = state.sessions.filter { it.status == "COMPLETED" }
                if (completedSessions.isNotEmpty()) {
                    item {
                        DaySummary(completedSessions)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                if (state.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = Coral, modifier = Modifier.size(32.dp))
                        }
                    }
                } else if (state.error != null) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Ошибка загрузки", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = InkBlack)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(state.error ?: "", fontSize = 14.sp, color = MutedGray, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Повторить",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Coral,
                                modifier = Modifier.clickable { viewModel.refresh() },
                            )
                        }
                    }
                } else if (state.sessions.isEmpty()) {
                    item { EmptyState() }
                } else {
                    items(state.sessions, key = { it.id }) { session ->
                        HistoryCard(
                            session = session,
                            onClick = { onSessionClick(session) },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}
