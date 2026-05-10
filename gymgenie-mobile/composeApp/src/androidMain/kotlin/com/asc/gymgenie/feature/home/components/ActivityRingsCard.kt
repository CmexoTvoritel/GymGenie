package com.asc.gymgenie.feature.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.activity.ActivityRing
import com.asc.gymgenie.activity.ActivityTodayResponse
import com.asc.gymgenie.activity.toProgress
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.RingLife
import com.asc.gymgenie.ui.theme.RingMind
import com.asc.gymgenie.ui.theme.RingMove
import com.asc.gymgenie.ui.theme.SoftCard

/**
 * Triple-ring summary of today's activity progress.
 *
 * Receives the raw KMM payloads and computes the per-ring averages on the
 * fly, which keeps the card stateless and allows the parent screen to
 * recompose freely without re-fetching anything.
 */
@Composable
fun ActivityRingsCard(activities: List<ActivityTodayResponse>) {
    val (move, mind, life) = remember(activities) { computeRingProgress(activities) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SoftCard)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConcentricRings(
            moveProgress = move ?: 0f,
            mindProgress = mind ?: 0f,
            lifeProgress = life ?: 0f,
            modifier = Modifier.size(140.dp),
        )

        Spacer(modifier = Modifier.width(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendRow(color = RingMove, title = "ДВИЖЕНИЕ", value = formatPct(move))
            LegendRow(color = RingMind, title = "РАЗУМ", value = formatPct(mind))
            LegendRow(color = RingLife, title = "РЕЖИМ", value = formatPct(life))
        }
    }
}

/**
 * Per-ring progress as a fraction in `[0f, 1f]` or `null` if the ring has no
 * activities planned for today. Keeping `null` separate from `0f` lets the
 * legend distinguish "no progress yet" from "nothing to do" — the latter
 * renders as `–` instead of `0%`.
 */
private data class RingTotals(val move: Float?, val mind: Float?, val life: Float?)

private operator fun RingTotals.component1() = move
private operator fun RingTotals.component2() = mind
private operator fun RingTotals.component3() = life

/**
 * Aggregates the per-activity progress fractions into one number per ring by
 * averaging across the activities that belong to that ring. An empty ring
 * yields `null` so the UI can render a neutral placeholder.
 *
 * Activities whose `ring` field does not match any [ActivityRing] entry are
 * silently ignored — they cannot influence a ring they don't belong to.
 */
private fun computeRingProgress(activities: List<ActivityTodayResponse>): RingTotals {
    fun average(target: ActivityRing): Float? {
        val group = activities.filter {
            runCatching { ActivityRing.valueOf(it.ring) }.getOrNull() == target
        }
        if (group.isEmpty()) return null
        return group.fold(0f) { acc, a -> acc + a.toProgress().fraction } / group.size
    }
    return RingTotals(
        move = average(ActivityRing.MOVE),
        mind = average(ActivityRing.MIND),
        life = average(ActivityRing.LIFE),
    )
}

private fun formatPct(value: Float?): String =
    if (value == null) "–" else "${(value.coerceIn(0f, 1f) * 100f).toInt()}%"

@Composable
private fun ConcentricRings(
    moveProgress: Float,
    mindProgress: Float,
    lifeProgress: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 13.dp.toPx()
        val gap = 3.dp.toPx()

        drawRing(
            color = RingMove,
            progress = moveProgress,
            inset = 0f,
            strokeWidth = strokeWidth,
        )
        drawRing(
            color = RingMind,
            progress = mindProgress,
            inset = strokeWidth + gap,
            strokeWidth = strokeWidth,
        )
        drawRing(
            color = RingLife,
            progress = lifeProgress,
            inset = 2 * (strokeWidth + gap),
            strokeWidth = strokeWidth,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRing(
    color: Color,
    progress: Float,
    inset: Float,
    strokeWidth: Float,
) {
    val diameter = size.minDimension - (inset * 2) - strokeWidth
    if (diameter <= 0f) return

    val topLeft = Offset(
        x = inset + strokeWidth / 2f,
        y = inset + strokeWidth / 2f,
    )
    val arcSize = Size(diameter, diameter)
    val safeProgress = progress.coerceIn(0f, 1f)

    drawArc(
        color = color.copy(alpha = 0.15f),
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )
    if (safeProgress > 0f) {
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * safeProgress,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun LegendRow(color: Color, title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MutedText,
                letterSpacing = 0.6.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepInk,
            )
        }
    }
}
