package com.asc.gymgenie.feature.workout_history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.components.muscleGroupCardEmoji
import com.asc.gymgenie.ui.components.muscleGroupColors
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.workout.WorkoutSessionHistoryItem
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val InkBlack = Color(0xFF0A0A0A)
private val BorderGray = Color(0xFFEDEDEF)
private val MutedGray = Color(0xFF8B8B92)
private val SoftGray = Color(0xFFF4F4F6)

private val CompletedBg = Color(0xFFE6F6EC)
private val CompletedFg = Color(0xFF1E8E4F)
private val CompletedAccent = Color(0xFF22A06B)

private val IncompleteBg = Color(0xFFFFF4DA)
private val IncompleteFg = Color(0xFF8A5A00)
private val IncompleteAccent = Color(0xFFE89B12)

@Composable
fun HistoryCard(
    session: WorkoutSessionHistoryItem,
    onClick: () -> Unit,
) {
    val isCompleted = session.status == "COMPLETED"
    val statusBg = if (isCompleted) CompletedBg else IncompleteBg
    val statusFg = if (isCompleted) CompletedFg else IncompleteFg
    val statusText = if (isCompleted) "Выполнено" else "Не закончено"
    val muscleColors = muscleGroupColors(session.primaryMuscleGroup)
    val muscleEmoji = muscleGroupCardEmoji(session.primaryMuscleGroup)

    val progress = if (session.totalSets > 0) {
        session.completedSets.toFloat() / session.totalSets
    } else if (isCompleted) 1f else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, BorderGray, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(muscleColors.background),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = muscleEmoji, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkBlack,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (session.primaryMuscleGroup != null) {
                        Text(
                            text = muscleGroupLabel(session.primaryMuscleGroup),
                            fontSize = 15.sp,
                            color = MutedGray,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = statusText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusFg,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val startTime = formatEpochTime(session.startedAt)
                val endTime = session.finishedAt?.let { formatEpochTime(it) }
                if (endTime != null) {
                    InfoChip(text = "$startTime – $endTime")
                }
                session.durationMinutes?.let { InfoChip(text = "${it} мин") }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (progress > 0f) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Coral,
                        trackColor = SoftGray,
                        strokeCap = StrokeCap.Round,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isCompleted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    StatItem(label = "Упражнения", value = "${session.totalExercises}")
                    StatItem(label = "Подходы", value = "${session.completedSets}")
                }
            } else if (session.totalSets > 0) {
                Text(
                    text = "Сделано ${session.completedSets} из ${session.totalSets} подходов",
                    fontSize = 13.sp,
                    color = IncompleteFg,
                )
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MutedGray,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SoftGray)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun StatItem(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = InkBlack)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 15.sp, color = MutedGray)
    }
}

private fun formatEpochTime(epochMillis: Double): String {
    val instant = Instant.fromEpochMilliseconds(epochMillis.toLong())
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return String.format("%02d:%02d", local.hour, local.minute)
}

private fun muscleGroupLabel(group: String?): String = when (group?.uppercase()) {
    "CHEST" -> "Грудные мышцы"
    "BACK" -> "Спина"
    "SHOULDERS", "SHOULDER" -> "Плечи"
    "BICEPS" -> "Бицепс"
    "TRICEPS" -> "Трицепс"
    "FOREARMS" -> "Предплечья"
    "ARMS" -> "Руки"
    "ABS", "CORE" -> "Пресс"
    "QUADRICEPS" -> "Квадрицепсы"
    "HAMSTRINGS" -> "Задняя поверхность бедра"
    "GLUTES" -> "Ягодицы"
    "CALVES" -> "Икроножные"
    "LEGS" -> "Ноги"
    "CARDIO" -> "Кардио"
    "FULL_BODY" -> "Все тело"
    else -> "Смешанная"
}
