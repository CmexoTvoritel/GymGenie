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
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.RingActivity
import com.asc.gymgenie.ui.theme.RingMovement
import com.asc.gymgenie.ui.theme.RingWarmups
import com.asc.gymgenie.ui.theme.SoftCard

/**
 * Apple Fitness style concentric-ring card.
 *
 * All progress values are hardcoded — the backend does not yet expose a
 * dedicated Activity feed. Replace with real data once available.
 */
@Composable
fun ActivityRingsCard() {
    // TODO: hook up to an ActivityFeed endpoint when the backend exposes one.
    val movementProgress = 420f / 600f
    val activityProgress = 28f / 45f
    val warmupsProgress = 6f / 12f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SoftCard)
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConcentricRings(
            movementProgress = movementProgress,
            activityProgress = activityProgress,
            warmupsProgress = warmupsProgress,
            modifier = Modifier.size(132.dp),
        )

        Spacer(modifier = Modifier.width(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendRow(color = RingMovement, title = "ДВИЖЕНИЕ", value = "420/600 ккал")
            LegendRow(color = RingActivity, title = "АКТИВНОСТЬ", value = "28/45 мин")
            LegendRow(color = RingWarmups, title = "РАЗМИНКИ", value = "6/12 раз")
        }
    }
}

@Composable
private fun ConcentricRings(
    movementProgress: Float,
    activityProgress: Float,
    warmupsProgress: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 12.dp.toPx()
        val gap = 4.dp.toPx()

        drawRing(
            color = RingMovement,
            progress = movementProgress,
            inset = 0f,
            strokeWidth = strokeWidth,
        )
        drawRing(
            color = RingActivity,
            progress = activityProgress,
            inset = strokeWidth + gap,
            strokeWidth = strokeWidth,
        )
        drawRing(
            color = RingWarmups,
            progress = warmupsProgress,
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
        color = color.copy(alpha = 0.18f),
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )
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
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MutedText,
                letterSpacing = 0.4.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepInk,
            )
        }
    }
}
