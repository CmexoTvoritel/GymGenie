package com.asc.gymgenie.feature.workout_session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.feature.create_workout.muscleGroupExerciseDrawable
import com.asc.gymgenie.feature.workout_session.components.ExerciseInfoSheet
import com.asc.gymgenie.ui.components.GymGenieToolbar
import com.asc.gymgenie.presentation.WorkoutSessionViewModel
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.OnBackground
import com.asc.gymgenie.ui.theme.OnSurfaceVariant
import com.asc.gymgenie.ui.theme.WarmOffWhite

@Composable
fun RestTimerScreen(
    viewModel: WorkoutSessionViewModel,
    state: WorkoutSessionViewModel.State,
    sessionStartMillis: Long,
    onBack: () -> Unit,
) {
    var showExitDialog by remember { mutableStateOf(false) }
    var detailExerciseId by remember { mutableStateOf<String?>(null) }

    BackHandler { showExitDialog = true }

    var restRemaining by remember { mutableIntStateOf(state.restDurationSeconds) }
    var circleMax by remember { mutableIntStateOf(state.restDurationSeconds) }

    LaunchedEffect(state.currentExerciseIndex, state.currentSetIndex) {
        restRemaining = state.restDurationSeconds
        circleMax = state.restDurationSeconds
    }

    var prevDuration by remember { mutableIntStateOf(state.restDurationSeconds) }
    LaunchedEffect(state.restDurationSeconds) {
        if (state.restDurationSeconds != prevDuration) {
            val delta = state.restDurationSeconds - prevDuration
            restRemaining = (restRemaining + delta).coerceAtLeast(1)
            if (restRemaining > circleMax) circleMax = restRemaining
            prevDuration = state.restDurationSeconds
        }
    }

    LaunchedEffect(state.currentExerciseIndex, state.currentSetIndex) {
        while (restRemaining > 0) {
            delay(1_000)
            restRemaining--
        }
        viewModel.restComplete()
    }

    val remaining = restRemaining
    val minutes = remaining / 60
    val seconds = remaining % 60
    val totalRest = circleMax.coerceAtLeast(1)
    val progress = if (totalRest > 0) remaining.toFloat() / totalRest.toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GymGenieToolbar(
            title = "Тренировка",
            showBackNavigation = true,
            showCloseIcon = true,
            onBackClick = { showExitDialog = true },
        )

        Spacer(modifier = Modifier.weight(1f))

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
                Text("отдых", fontSize = 18.sp, color = OnSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

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
                border = BorderStroke(1.5.dp, Coral.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = OnBackground,
                    containerColor = Coral.copy(alpha = 0.05f),
                ),
            ) {
                Text("- 10 сек", fontSize = 19.sp, fontWeight = FontWeight.Medium)
            }
            OutlinedButton(
                onClick = { viewModel.adjustRest(10) },
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, Coral.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = OnBackground,
                    containerColor = Coral.copy(alpha = 0.05f),
                ),
            ) {
                Text("+ 10 сек", fontSize = 19.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                Image(
                    painter = painterResource(
                        id = muscleGroupExerciseDrawable(nextMuscleGroup(state)),
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Далее", fontSize = 15.sp, color = OnSurfaceVariant)
                    Text(
                        nextExerciseName(state),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = OnBackground,
                    )
                    Text(
                        nextSetLabel(state),
                        fontSize = 15.sp,
                        color = OnSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Coral.copy(alpha = 0.12f))
                        .clickable {
                            val nextId = nextExerciseId(state)
                            if (nextId != null) detailExerciseId = nextId
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Информация",
                        tint = Coral,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
            Text("Пропустить подход", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(8.dp))

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
            Text("Пропустить отдых", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    ExerciseInfoSheet(
        exerciseId = detailExerciseId,
        onDismiss = { detailExerciseId = null },
    )

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = "Завершить тренировку?",
                    color = Color(0xFF0A0A0A),
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "Прогресс будет сохранён как незавершённая тренировка.",
                    color = Color(0xFF8B8B92),
                    fontSize = 16.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    viewModel.cancelWorkout(
                        ((System.currentTimeMillis() - sessionStartMillis) / 1000).toInt(),
                    )
                }) {
                    Text(
                        text = "Завершить",
                        color = Color(0xFFE5484D),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(text = "Продолжить", color = Color(0xFF0A0A0A), fontSize = 16.sp)
                }
            },
            containerColor = Color.White,
        )
    }
}

private fun nextExerciseName(state: WorkoutSessionViewModel.State): String {
    val current = state.currentExercise ?: return "Финиш"
    return if (state.currentSetIndex + 2 <= current.sets) {
        current.exerciseName
    } else {
        state.nextExercise?.exerciseName ?: "Финиш"
    }
}

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

private fun nextExerciseId(state: WorkoutSessionViewModel.State): String? {
    val current = state.currentExercise ?: return null
    return if (state.currentSetIndex + 2 <= current.sets) {
        current.exerciseId
    } else {
        state.nextExercise?.exerciseId
    }
}

private fun nextMuscleGroup(state: WorkoutSessionViewModel.State): String {
    val current = state.currentExercise ?: return ""
    return if (state.currentSetIndex + 2 <= current.sets) {
        current.muscleGroupLabel
    } else {
        state.nextExercise?.muscleGroupLabel ?: ""
    }
}
