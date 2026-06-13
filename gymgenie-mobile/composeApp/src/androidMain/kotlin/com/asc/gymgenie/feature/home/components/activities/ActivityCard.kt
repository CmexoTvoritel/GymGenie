package com.asc.gymgenie.feature.home.components.activities

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import kotlin.math.min
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.R
import com.asc.gymgenie.activity.ActivityKind
import com.asc.gymgenie.activity.ActivityRing
import com.asc.gymgenie.activity.ActivityTodayResponse
import com.asc.gymgenie.activity.toProgress
import com.asc.gymgenie.ui.theme.ActivityCardBorder
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.RingLife
import com.asc.gymgenie.ui.theme.RingMind
import com.asc.gymgenie.ui.theme.RingMove
import com.asc.gymgenie.ui.theme.SoftCard
import com.asc.gymgenie.utils.WeekdayOrder
import com.asc.gymgenie.utils.weekdayShortRu
import kotlin.math.max

internal fun ringColorFor(ring: String): Color = when (ring) {
    ActivityRing.MOVE.name -> RingMove
    ActivityRing.MIND.name -> RingMind
    ActivityRing.LIFE.name -> RingLife
    else -> DeepInk
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ActivityCard(
    activity: ActivityTodayResponse,
    onCheckIn: (String, Int) -> Unit,
    onOpenPreset: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    onOpenScheduleSettings: (() -> Unit)? = null,
) {
    val progress = remember(activity) { activity.toProgress() }
    val ringColor = ringColorFor(activity.ring)
    val kind = remember(activity.kind) {
        runCatching { ActivityKind.valueOf(activity.kind) }.getOrDefault(ActivityKind.BINARY)
    }
    val isPartial = !progress.isDone && progress.fraction > 0f
    val borderColor = if (progress.isDone) ringColor else ActivityCardBorder
    val titleColor = if (progress.isDone || isPartial) DeepInk else Color(0xFF5A5A62)

    val scheduleLabel = remember(activity.scheduleType, activity.scheduleDays, activity.oneOffDate) {
        formatScheduleLabel(activity)
    }

    val cardShape = RoundedCornerShape(18.dp)
    val cardModifier = Modifier
        .fillMaxWidth()
        .clip(cardShape)
        .background(Color.White)
        .border(1.5.dp, borderColor, cardShape)
        .combinedClickable(
            onClick = { if (kind == ActivityKind.BINARY) toggleBinary(activity, onCheckIn) },
            onLongClick = { onLongPress?.invoke() },
        )
        .padding(horizontal = 14.dp, vertical = 12.dp)

    Row(
        modifier = cardModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActivityEmojiBadge(
            ring = activity.ring,
            ringColor = ringColor,
            isDone = progress.isDone,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
            )
            Spacer(modifier = Modifier.height(4.dp))
            ActivitySubtitle(
                activity = activity,
                kind = kind,
                progressFraction = progress.fraction,
                isDone = progress.isDone,
                ringColor = ringColor,
            )
            if (scheduleLabel != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = scheduleLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MutedText.copy(alpha = 0.7f),
                    modifier = if (onOpenScheduleSettings != null) {
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onOpenScheduleSettings() }
                            .padding(vertical = 2.dp)
                    } else {
                        Modifier.padding(vertical = 2.dp)
                    },
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        ActionButton(
            activity = activity,
            kind = kind,
            isDone = progress.isDone,
            ringColor = ringColor,
            onCheckIn = onCheckIn,
            onOpenPreset = onOpenPreset,
        )
    }
}

@Composable
private fun ActivityEmojiBadge(ring: String, ringColor: Color, isDone: Boolean) {
    val background = if (isDone) ringColor else SoftCard
    val ringIcon = when (ring) {
        ActivityRing.MOVE.name -> R.drawable.ic_activity_run
        ActivityRing.MIND.name -> R.drawable.ic_activity_mind
        else -> R.drawable.ic_activity_schedule
    }
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(ringIcon),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ActivitySubtitle(
    activity: ActivityTodayResponse,
    kind: ActivityKind,
    progressFraction: Float,
    isDone: Boolean,
    ringColor: Color,
) {
    when (kind) {
        ActivityKind.BINARY -> {
            val text = when {
                activity.inverse && isDone -> "Сегодня без алкоголя ✓"
                activity.inverse && !isDone -> "Снято — сегодня выпил"
                isDone -> "Выполнено ✓"
                else -> "Тапни, чтобы отметить"
            }
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDone) ringColor else MutedText,
            )
        }

        ActivityKind.COUNTER, ActivityKind.PRESET -> {
            val unit = activity.unit.orEmpty()
            val goal = activity.goal ?: 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${activity.logValue} / $goal $unit".trim(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDone) ringColor else MutedText,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            ProgressBar(progress = progressFraction, color = ringColor)
        }
    }
}

@Composable
private fun ProgressBar(progress: Float, color: Color) {
    val safe = progress.coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFEEEEF1)),
    ) {
        if (safe > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = safe)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
        }
    }
}

@Composable
private fun ActionButton(
    activity: ActivityTodayResponse,
    kind: ActivityKind,
    isDone: Boolean,
    ringColor: Color,
    onCheckIn: (String, Int) -> Unit,
    onOpenPreset: () -> Unit,
) {
    val shape = CircleShape
    when (kind) {
        ActivityKind.BINARY -> {
            val background = if (isDone) ringColor else Color.White
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(shape)
                    .background(background)
                    .let { if (!isDone) it.border(1.5.dp, ActivityCardBorder, shape) else it }
                    .clickable { toggleBinary(activity, onCheckIn) },
                contentAlignment = Alignment.Center,
            ) {
                if (isDone) {
                    Text(text = "✓", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Text(text = "○", fontSize = 16.sp, color = MutedText)
                }
            }
        }

        ActivityKind.COUNTER -> {
            val maxVal = activity.goal ?: Int.MAX_VALUE
            val minusEnabled = activity.logValue > 0
            val plusEnabled = activity.logValue < maxVal

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(shape)
                        .background(
                            if (minusEnabled) {
                                if (isDone) ringColor else DeepInk
                            } else {
                                (if (isDone) ringColor else DeepInk).copy(alpha = 0.3f)
                            },
                        )
                        .then(
                            if (minusEnabled) Modifier.clickable {
                                onCheckIn(activity.activityId, max(0, activity.logValue - 1))
                            } else Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "−",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(shape)
                        .background(
                            if (plusEnabled) {
                                if (isDone) ringColor else DeepInk
                            } else {
                                (if (isDone) ringColor else DeepInk).copy(alpha = 0.3f)
                            },
                        )
                        .then(
                            if (plusEnabled) Modifier.clickable {
                                onCheckIn(
                                    activity.activityId,
                                    min(maxVal, activity.logValue + 1),
                                )
                            } else Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }

        ActivityKind.PRESET -> {
            val background = if (isDone) ringColor else DeepInk
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(shape)
                    .background(background)
                    .clickable { onOpenPreset() },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

internal fun toggleBinary(
    activity: ActivityTodayResponse,
    onCheckIn: (String, Int) -> Unit,
) {
    val currentlyDone = if (activity.inverse) activity.logValue == 0 else activity.logValue > 0
    val next = if (currentlyDone) {
        if (activity.inverse) 1 else 0
    } else {
        if (activity.inverse) 0 else 1
    }
    onCheckIn(activity.activityId, next)
}

private fun formatScheduleLabel(activity: ActivityTodayResponse): String? {
    return when (activity.scheduleType) {
        "RECURRING" -> {
            if (activity.scheduleDays.isEmpty()) return null
            val sorted = activity.scheduleDays.sortedBy { WeekdayOrder.indexOf(it) }
            sorted.map { weekdayShortRu(it) }.joinToString(" ")
        }
        "ONE_TIME" -> {
            val raw = activity.oneOffDate ?: return null
            runCatching {
                val ld = java.time.LocalDate.parse(raw)
                val fmt = java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale("ru"))
                ld.format(fmt)
            }.getOrDefault(raw)
        }
        else -> null
    }
}
