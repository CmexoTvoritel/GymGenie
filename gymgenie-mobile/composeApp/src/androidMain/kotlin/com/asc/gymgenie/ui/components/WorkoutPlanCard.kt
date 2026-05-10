package com.asc.gymgenie.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.workout.WorkoutPlanShortResponse
import com.asc.gymgenie.workout.WorkoutScheduleType

private val Coral = Color(0xFFFF5A3C)
private val CoralDark = Color(0xFFE94A2C)
private val CoralTint = Color(0xFFFFF4F0)
private val BorderGray = Color(0xFFEDEDEF)
private val SoftGray = Color(0xFFF4F4F6)
private val ChipText = Color(0xFF3A3A40)
private val MutedManualText = Color(0xFF5C5C63)
private val InkBlack = Color(0xFF0A0A0A)
private val InkMuted = Color(0xFF8B8B92)

/**
 * Shared card representing a single workout plan.
 *
 * Used in two contexts:
 *  - The workouts catalog list, where the eye/start action pair is shown via a
 *    non-null [onView] handler.
 *  - The home tab pager, where [onView] is `null` and the card collapses to a
 *    single full-width "Начать тренировку" CTA on a deep-ink background.
 *
 * Active plans get a coral border + a 3dp top stripe so they read at a glance
 * without forcing the layout to grow taller. The body is wrapped in a coral →
 * white vertical gradient that signals the action color without overpowering
 * the content.
 */
@Composable
fun WorkoutPlanCard(
    plan: WorkoutPlanShortResponse,
    onView: (() -> Unit)? = null,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (plan.isActive) Coral else BorderGray
    val borderWidth = if (plan.isActive) 2.dp else 1.5.dp
    val isAi = plan.createdBy.equals("AI", ignoreCase = true)
    val muscleColors = muscleGroupColors(plan.primaryMuscleGroup)
    val isRecurring = plan.scheduleType.equals(WorkoutScheduleType.RECURRING.name, ignoreCase = true)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Coral.copy(alpha = 0.1f), Color.White),
                ),
            )
            .border(borderWidth, borderColor, RoundedCornerShape(20.dp)),
    ) {
        if (plan.isActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Coral),
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            HeaderRow(
                plan = plan,
                muscleColors = muscleColors,
                isAi = isAi,
            )

            Spacer(modifier = Modifier.height(14.dp))

            ChipsRow(
                plan = plan,
                isRecurring = isRecurring,
            )

            Spacer(modifier = Modifier.height(14.dp))

            FooterActions(
                onView = onView,
                onStart = onStart,
            )
        }
    }
}

@Composable
private fun HeaderRow(
    plan: WorkoutPlanShortResponse,
    muscleColors: MuscleGroupColors,
    isAi: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(muscleColors.background),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = muscleGroupCardEmoji(plan.primaryMuscleGroup),
                fontSize = 22.sp,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = plan.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = InkBlack,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            plan.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    color = InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        SourceBadge(isAi = isAi)
    }
}

@Composable
private fun SourceBadge(isAi: Boolean) {
    if (isAi) {
        Text(
            text = "✦ AI",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Coral)
                .padding(horizontal = 9.dp, vertical = 5.dp),
        )
    } else {
        Text(
            text = "Ручная",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MutedManualText,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(SoftGray)
                .padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun ChipsRow(
    plan: WorkoutPlanShortResponse,
    isRecurring: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InfoChip(
            icon = Icons.Filled.Schedule,
            text = "~${estimatedMinutes(plan)} мин",
        )
        InfoChip(
            icon = Icons.AutoMirrored.Filled.List,
            text = "${plan.exercisesCount} упр.",
        )
        ScheduleChip(plan = plan, isRecurring = isRecurring)
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SoftGray)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ChipText,
            modifier = Modifier.size(13.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = ChipText,
        )
    }
}

@Composable
private fun ScheduleChip(
    plan: WorkoutPlanShortResponse,
    isRecurring: Boolean,
) {
    val (background, foreground, label, icon) = if (isRecurring) {
        ScheduleChipStyle(
            background = CoralTint,
            foreground = CoralDark,
            label = formatRecurringDays(plan.scheduleDays),
            icon = Icons.Filled.Repeat,
        )
    } else {
        ScheduleChipStyle(
            background = SoftGray,
            foreground = ChipText,
            label = "Разовая",
            icon = Icons.Filled.Schedule,
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(13.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = foreground,
        )
    }
}

@Composable
private fun FooterActions(
    onView: (() -> Unit)?,
    onStart: () -> Unit,
) {
    if (onView != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.5.dp, BorderGray, RoundedCornerShape(14.dp))
                    .clickable { onView() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = "Просмотр",
                    tint = InkBlack,
                    modifier = Modifier.size(20.dp),
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Coral)
                    .clickable { onStart() }
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Начать",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(InkBlack)
                .clickable { onStart() }
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Начать тренировку",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

private data class ScheduleChipStyle(
    val background: Color,
    val foreground: Color,
    val label: String,
    val icon: ImageVector,
)

internal data class MuscleGroupColors(
    val background: Color,
    val foreground: Color,
)

internal fun muscleGroupColors(group: String?): MuscleGroupColors {
    return when (group?.uppercase()) {
        "CHEST" -> MuscleGroupColors(Color(0xFFFFE8E2), Color(0xFFE94A2C))
        "BACK" -> MuscleGroupColors(Color(0xFFE6EEFF), Color(0xFF3B5BDB))
        "QUADRICEPS", "HAMSTRINGS", "GLUTES", "CALVES", "LEGS" ->
            MuscleGroupColors(Color(0xFFE8F7E8), Color(0xFF2F9E44))
        "BICEPS", "TRICEPS", "FOREARMS", "ARMS" ->
            MuscleGroupColors(Color(0xFFFFF4D6), Color(0xFFB8860B))
        "CARDIO" -> MuscleGroupColors(Color(0xFFFCE8F2), Color(0xFFC2255C))
        "ABS", "CORE" -> MuscleGroupColors(Color(0xFFEAE6FF), Color(0xFF6741D9))
        "SHOULDERS", "SHOULDER" -> MuscleGroupColors(Color(0xFFE6EEFF), Color(0xFF3B5BDB))
        else -> MuscleGroupColors(Color(0xFFFFE8E2), Color(0xFFE94A2C))
    }
}

internal fun muscleGroupCardEmoji(group: String?): String = when (group?.uppercase()) {
    "CHEST" -> "🫁"
    "BACK" -> "🦾"
    "SHOULDERS", "SHOULDER" -> "💪"
    "BICEPS", "TRICEPS", "FOREARMS", "ARMS" -> "💪"
    "ABS", "CORE" -> "🔥"
    "QUADRICEPS", "HAMSTRINGS", "GLUTES", "CALVES", "LEGS" -> "🦵"
    "CARDIO" -> "❤️"
    "FULL_BODY" -> "⭐"
    else -> "🏋"
}

/**
 * Lightweight time estimate that doesn't pretend to be precise: each set is
 * assumed to take ~1 min including the configured rest interval. This keeps
 * the chip useful even for plans that haven't been executed yet (so we can't
 * pull a real average from session history).
 */
private fun estimatedMinutes(plan: WorkoutPlanShortResponse): Int {
    if (plan.totalSets <= 0) return 0
    val perSetSeconds = 30 + plan.restSeconds.coerceAtLeast(0)
    val seconds = plan.totalSets * perSetSeconds
    return (seconds / 60).coerceAtLeast(1)
}

private val DayAbbreviations = listOf(
    "MONDAY" to "Пн",
    "TUESDAY" to "Вт",
    "WEDNESDAY" to "Ср",
    "THURSDAY" to "Чт",
    "FRIDAY" to "Пт",
    "SATURDAY" to "Сб",
    "SUNDAY" to "Вс",
)

internal fun formatRecurringDays(days: List<String>): String {
    if (days.isEmpty()) return "Постоянная"
    val normalized = days.map { it.uppercase() }.toSet()
    val ordered = DayAbbreviations.filter { it.first in normalized }.map { it.second }
    return if (ordered.isEmpty()) "Постоянная" else ordered.joinToString(" · ")
}
