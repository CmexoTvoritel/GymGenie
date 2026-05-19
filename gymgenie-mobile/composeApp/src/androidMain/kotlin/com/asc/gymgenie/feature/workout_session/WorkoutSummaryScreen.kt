package com.asc.gymgenie.feature.workout_session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.ui.theme.CoralLight
import com.asc.gymgenie.ui.theme.OnBackground
import com.asc.gymgenie.ui.theme.OnSurfaceVariant
import com.asc.gymgenie.workout.ActiveExercise
import com.asc.gymgenie.workout.CompletedSet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutSummaryScreen(
    planName: String,
    durationSeconds: Int,
    exerciseCount: Int,
    completedSets: List<CompletedSet>,
    exercises: List<ActiveExercise> = emptyList(),
    isSubmitting: Boolean = false,
    isSubmitted: Boolean = false,
    submitError: String? = null,
    onRetrySubmit: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val durationMinutes = durationSeconds / 60
    val estimatedCalories = exerciseCount * 45 + durationMinutes * 4
    val totalVolumeKg = completedSets
        .asSequence()
        .filter { !it.isSkipped() }
        .sumOf { it.weightActual * it.repsActual }
        .toInt()
    val dateStr = SimpleDateFormat("d MMMM, HH:mm", Locale("ru")).format(Date())

    Box(modifier = Modifier.fillMaxSize().background(WarmOffWhite)) {
        Column(modifier = Modifier.fillMaxSize()) {
            GymGenieToolbar(
                title = "Результат тренировки",
                showBackNavigation = true,
                onBackClick = onDismiss,
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(CoralLight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🏆", fontSize = 36.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Отличная работа!", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = OnBackground)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Тренировка \"$planName\" завершена",
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
                        StatCard("⚖️", "$totalVolumeKg кг", "Общий объём", Modifier.weight(1f))
                        StatCard("🏋️", "$exerciseCount", "Упражнений", Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    SubmissionStatusBanner(
                        isSubmitting = isSubmitting,
                        isSubmitted = isSubmitted,
                        submitError = submitError,
                        onRetry = onRetrySubmit,
                    )
                }

                if (exercises.isNotEmpty()) {
                    item {
                        Text(
                            "Упражнения",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnBackground,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    items(exercises.withIndex().toList(), key = { it.index }) { (index, exercise) ->
                        val setsForExercise = completedSets.filter { it.exerciseIndex == index }
                        val nonSkippedSets = setsForExercise.filter { !it.isSkipped() }
                        val totalSetsCount = setsForExercise.size
                        val totalReps = nonSkippedSets.sumOf { it.repsActual }
                        val maxWeight = nonSkippedSets.maxOfOrNull { it.weightActual } ?: 0.0
                        ExerciseSummaryRow(
                            name = exercise.exerciseName,
                            setsCount = totalSetsCount,
                            repsCount = totalReps,
                            maxWeightKg = maxWeight,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .navigationBarsPadding()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Coral),
        ) {
            Text("На главную", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
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

@Composable
private fun ExerciseSummaryRow(
    name: String,
    setsCount: Int,
    repsCount: Int,
    maxWeightKg: Double,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CoralLight),
                contentAlignment = Alignment.Center,
            ) {
                Text("🏋️", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = OnBackground)
                val statsText = buildString {
                    append("$setsCount подходов · $repsCount повторений")
                    if (maxWeightKg > 0.0) {
                        val formatted = if (maxWeightKg % 1.0 == 0.0) {
                            maxWeightKg.toInt().toString()
                        } else {
                            String.format("%.1f", maxWeightKg)
                        }
                        append(" · $formatted кг")
                    }
                }
                Text(
                    statsText,
                    fontSize = 14.sp,
                    color = OnSurfaceVariant,
                )
            }
        }
    }
}

private fun CompletedSet.isSkipped(): Boolean = repsActual == 0 && weightActual == 0.0

@Composable
private fun SubmissionStatusBanner(
    isSubmitting: Boolean,
    isSubmitted: Boolean,
    submitError: String?,
    onRetry: () -> Unit,
) {
    when {
        isSubmitting -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(
                    color = Coral,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Сохраняем тренировку…",
                    fontSize = 13.sp,
                    color = OnSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        submitError != null -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = submitError,
                    color = Color(0xFFE53935),
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral),
                ) {
                    Text("Повторить", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        isSubmitted -> {}
    }
}
