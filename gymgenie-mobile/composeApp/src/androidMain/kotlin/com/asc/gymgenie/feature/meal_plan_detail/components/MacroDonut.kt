package com.asc.gymgenie.feature.meal_plan_detail.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.theme.DeepInk

val ProteinColor = Color(0xFF0A84FF)
val FatColor = Color(0xFFFFB020)
val CarbsColor = Color(0xFF34C759)

@Composable
fun MacroDonut(
    proteinG: Int,
    fatG: Int,
    carbsG: Int,
    totalKcal: Int,
    modifier: Modifier = Modifier,
) {
    val proteinKcal = proteinG * 4f
    val fatKcal = fatG * 9f
    val carbsKcal = carbsG * 4f
    val macroTotal = proteinKcal + fatKcal + carbsKcal

    val proteinFraction = if (macroTotal > 0f) proteinKcal / macroTotal else 0f
    val fatFraction = if (macroTotal > 0f) fatKcal / macroTotal else 0f
    val carbsFraction = if (macroTotal > 0f) carbsKcal / macroTotal else 0f

    Box(modifier = modifier.size(108.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(108.dp)) {
            val strokeWidth = 13.dp.toPx()
            val style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            val gapDeg = if (macroTotal > 0f) 3f else 0f
            val totalGap = gapDeg * 3f
            val available = 360f - totalGap

            if (macroTotal <= 0f) {
                drawArc(
                    color = DeepInk.copy(alpha = 0.1f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = style,
                )
                return@Canvas
            }

            var currentAngle = -90f

            val proteinSweep = available * proteinFraction
            drawArc(
                color = ProteinColor,
                startAngle = currentAngle,
                sweepAngle = proteinSweep,
                useCenter = false,
                style = style,
            )
            currentAngle += proteinSweep + gapDeg

            val fatSweep = available * fatFraction
            drawArc(
                color = FatColor,
                startAngle = currentAngle,
                sweepAngle = fatSweep,
                useCenter = false,
                style = style,
            )
            currentAngle += fatSweep + gapDeg

            val carbsSweep = available * carbsFraction
            drawArc(
                color = CarbsColor,
                startAngle = currentAngle,
                sweepAngle = carbsSweep,
                useCenter = false,
                style = style,
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = totalKcal.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk,
            )
            Text(
                text = "ККАЛ",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk.copy(alpha = 0.55f),
                letterSpacing = 0.5.sp,
            )
        }
    }
}
