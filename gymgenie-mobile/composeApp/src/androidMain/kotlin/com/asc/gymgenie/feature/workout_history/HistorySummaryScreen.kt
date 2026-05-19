package com.asc.gymgenie.feature.workout_history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.components.GymGenieToolbar
import com.asc.gymgenie.ui.theme.CoralLight
import com.asc.gymgenie.ui.theme.OnBackground
import com.asc.gymgenie.ui.theme.OnSurfaceVariant
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.workout.WorkoutSessionHistoryItem
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val MonthNamesGenitive = listOf(
    "", "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

@Composable
fun HistorySummaryScreen(
    session: WorkoutSessionHistoryItem,
    onBack: () -> Unit,
) {
    val isCompleted = session.status == "COMPLETED"
    val durationMinutes = session.durationMinutes ?: 0
    val estimatedCalories = session.totalExercises * 45 + durationMinutes * 4

    val dateStr = run {
        val instant = Instant.fromEpochMilliseconds(session.startedAt.toLong())
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val month = MonthNamesGenitive.getOrElse(local.monthNumber) { "" }
        "${local.dayOfMonth} $month, ${String.format("%02d:%02d", local.hour, local.minute)}"
    }

    if (session.name.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(WarmOffWhite)) {
            Column(modifier = Modifier.fillMaxSize()) {
                GymGenieToolbar(
                    title = "Результат тренировки",
                    showBackNavigation = true,
                    onBackClick = onBack,
                )
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Данные недоступны",
                        fontSize = 17.sp,
                        color = Color(0xFF8B8B92),
                    )
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(WarmOffWhite)) {
        GymGenieToolbar(
            title = "Результат тренировки",
            showBackNavigation = true,
            onBackClick = onBack,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(CoralLight),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (isCompleted) "🏆" else "⚠️", fontSize = 36.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isCompleted) "Отличная работа!" else "Тренировка не завершена",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = OnBackground,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isCompleted) "Тренировка \"${session.name}\" завершена"
                else "Тренировка \"${session.name}\"",
                fontSize = 17.sp,
                color = OnBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Text(dateStr, fontSize = 17.sp, color = OnBackground)

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                "Результаты",
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                color = OnBackground,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard("⏱", "$durationMinutes мин", "Общее время", Modifier.weight(1f))
                StatCard("🔥", "$estimatedCalories ккал", "Сожжено", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard("🏋️", "${session.totalExercises}", "Упражнений", Modifier.weight(1f))
                StatCard("✅", "${session.completedSets}", "Подходов", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatCard(
    icon: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CoralLight),
                contentAlignment = Alignment.Center,
            ) {
                Text(icon, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = OnBackground)
            Text(label, fontSize = 14.sp, color = OnSurfaceVariant)
        }
    }
}
