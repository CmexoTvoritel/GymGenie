package com.asc.gymgenie.feature.workout_history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.workout.WorkoutSessionHistoryItem

private val InkBlack = Color(0xFF0A0A0A)
private val BorderGray = Color(0xFFEDEDEF)
private val MutedGray = Color(0xFF8B8B92)

@Composable
fun DaySummary(completedSessions: List<WorkoutSessionHistoryItem>) {
    val totalMinutes = completedSessions.sumOf { it.durationMinutes ?: 0 }
    val totalExercises = completedSessions.sumOf { it.totalExercises }
    val estimatedCalories = totalExercises * 45 + totalMinutes * 4

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        SummaryColumn(icon = "⏱", label = "Время", value = "${totalMinutes} мин")
        SummaryColumn(icon = "🔁", label = "Подходы", value = "${completedSessions.sumOf { it.completedSets }}")
        SummaryColumn(icon = "🔥", label = "ккал", value = "$estimatedCalories")
    }
}

@Composable
private fun SummaryColumn(icon: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = icon, fontSize = 36.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = InkBlack)
        Text(text = label, fontSize = 16.sp, color = MutedGray)
    }
}
