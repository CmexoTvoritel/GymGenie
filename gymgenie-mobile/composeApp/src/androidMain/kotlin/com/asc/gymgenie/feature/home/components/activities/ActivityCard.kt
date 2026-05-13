package com.asc.gymgenie.feature.home.components.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlin.math.max

internal fun ringColorFor(ring: String): Color = when (ring) {
    ActivityRing.MOVE.name -> RingMove
    ActivityRing.MIND.name -> RingMind
    ActivityRing.LIFE.name -> RingLife
    else -> DeepInk
}

@Composable
internal fun ActivityCard(
    activity: ActivityTodayResponse,
    onCheckIn: (String, Int) -> Unit,
    onOpenPreset: () -> Unit,
) {
    val progress = remember(activity) { activity.toProgress() }
    val ringColor = ringColorFor(activity.ring)
    val kind = remember(activity.kind) {
        runCatching { ActivityKind.valueOf(activity.kind) }.getOrDefault(ActivityKind.BINARY)
    }
    val isPartial = !progress.isDone && progress.fraction > 0f
    val borderColor = if (progress.isDone) ringColor else ActivityCardBorder
    val titleColor = if (progress.isDone || isPartial) DeepInk else Color(0xFF5A5A62)

    val cardShape = RoundedCornerShape(18.dp)
    val cardModifier = Modifier
        .fillMaxWidth()
        .clip(cardShape)
        .background(Color.White)
        .border(1.5.dp, borderColor, cardShape)
        .let { base ->
            if (kind == ActivityKind.BINARY) {
                base.clickable { toggleBinary(activity, onCheckIn) }
            } else {
                base
            }
        }
        .padding(horizontal = 14.dp, vertical = 12.dp)

    Row(
        modifier = cardModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActivityEmojiBadge(
            name = activity.name,
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
private fun ActivityEmojiBadge(name: String, ringColor: Color, isDone: Boolean) {
    val background = if (isDone) ringColor else SoftCard
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.take(1).uppercase(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDone) Color.White else ringColor,
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
            val background = if (isDone) ringColor else DeepInk
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(shape)
                    .background(background)
                    .pointerInput(activity.activityId, activity.logValue) {
                        detectTapGestures(
                            onTap = { onCheckIn(activity.activityId, activity.logValue + 1) },
                            onLongPress = {
                                onCheckIn(activity.activityId, max(0, activity.logValue - 1))
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
