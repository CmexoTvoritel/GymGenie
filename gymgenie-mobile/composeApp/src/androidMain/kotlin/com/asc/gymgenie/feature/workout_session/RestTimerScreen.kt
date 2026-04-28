package com.asc.gymgenie.feature.workout_session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.presentation.WorkoutSessionViewModel
import com.asc.gymgenie.ui.theme.Background
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.CoralLight
import com.asc.gymgenie.ui.theme.OnBackground
import com.asc.gymgenie.ui.theme.OnSurfaceVariant

/**
 * Rest-between-sets screen.
 *
 * Renders the circular countdown, ±10s adjustments, the "next exercise" preview,
 * a "skip set" outlined action (records the current set as skipped and advances),
 * and a "skip rest" coral primary action (advances without recording).
 */
@Composable
fun RestTimerScreen(
    viewModel: WorkoutSessionViewModel,
    state: WorkoutSessionViewModel.State,
) {
    val remaining = state.restSecondsRemaining
    val minutes = remaining / 60
    val seconds = remaining % 60
    val totalRest = state.session.restSeconds.coerceAtLeast(remaining)
    val progress = if (totalRest > 0) remaining.toFloat() / totalRest.toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                state.currentExercise?.exerciseName ?: "Отдых",
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = OnBackground,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Circular timer
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
            Canvas(modifier = Modifier.size(240.dp)) {
                val strokeWidth = 12.dp.toPx()
                drawArc(
                    color = Color.Gray.copy(alpha = 0.15f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                )
                drawArc(
                    color = Coral,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    String.format("%02d:%02d", minutes, seconds),
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground,
                )
                Text("отдых", fontSize = 16.sp, color = OnSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ± 10s buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { viewModel.adjustRest(-10) },
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, Color.Gray.copy(alpha = 0.3f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OnBackground),
            ) {
                Text("- 10 сек", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            OutlinedButton(
                onClick = { viewModel.adjustRest(10) },
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, Color.Gray.copy(alpha = 0.3f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OnBackground),
            ) {
                Text("+ 10 сек", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Next-exercise card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CoralLight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🏋️", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Далее", fontSize = 12.sp, color = OnSurfaceVariant)
                    Text(
                        nextExerciseName(state),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = OnBackground,
                    )
                    Text(
                        nextSetLabel(state),
                        fontSize = 12.sp,
                        color = OnSurfaceVariant,
                    )
                }
                Text("›", fontSize = 20.sp, color = OnSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Skip current set (outlined / muted) — advances and marks the set skipped.
        OutlinedButton(
            onClick = { viewModel.skipSet() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, Color.Gray.copy(alpha = 0.3f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = OnBackground),
        ) {
            Text("Пропустить подход", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Skip rest (coral primary) — fast-forward to next set.
        Button(
            onClick = { viewModel.skipRest() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Coral),
        ) {
            Text("Пропустить отдых ⏭", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Returns the name to show in the "Далее" card.
 * If the next set is still within the current exercise — show the current exercise name.
 * Only when all sets of the current exercise are done — show the next exercise name.
 */
private fun nextExerciseName(state: WorkoutSessionViewModel.State): String {
    val current = state.currentExercise ?: return "Финиш"
    return if (state.currentSetIndex + 2 <= current.sets) {
        current.exerciseName
    } else {
        state.nextExercise?.exerciseName ?: "Финиш"
    }
}

/**
 * Compute the human-readable set label for the "Далее" card.
 * currentSetIndex is 0-based index of the set just finished, so next = currentSetIndex + 2.
 */
private fun nextSetLabel(state: WorkoutSessionViewModel.State): String {
    val current = state.currentExercise ?: return "Финиш"
    val nextSetWithinExercise = state.currentSetIndex + 2
    return if (nextSetWithinExercise <= current.sets) {
        "Подход $nextSetWithinExercise из ${current.sets}"
    } else {
        val next = state.nextExercise ?: return "Финиш"
        "Подход 1 из ${next.sets}"
    }
}
