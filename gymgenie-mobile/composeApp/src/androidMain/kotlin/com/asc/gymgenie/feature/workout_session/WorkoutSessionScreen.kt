package com.asc.gymgenie.feature.workout_session

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.feature.workout_session.components.CircularIconButton
import com.asc.gymgenie.feature.workout_session.components.ControlCard
import com.asc.gymgenie.feature.workout_session.components.ElapsedTimer
import com.asc.gymgenie.feature.workout_session.components.ExerciseHero
import com.asc.gymgenie.feature.workout_session.components.ExerciseInfoSheet
import com.asc.gymgenie.presentation.WorkoutSessionViewModel
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.OnBackground
import com.asc.gymgenie.ui.theme.OnSurfaceVariant
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.workout.ActiveWorkoutSession
import com.asc.gymgenie.workout.LocalWorkoutRepository
import com.asc.gymgenie.workout.WorkoutApi
import kotlinx.coroutines.delay

@Composable
fun WorkoutSessionScreen(
    session: ActiveWorkoutSession,
    localRepository: LocalWorkoutRepository,
    workoutApi: WorkoutApi,
    onFinish: () -> Unit,
) {
    val viewModel = remember(session) {
        WorkoutSessionViewModel(
            session = session,
            localRepository = localRepository,
            workoutApi = workoutApi,
        )
    }
    val state by viewModel.state.collectAsState()
    val sessionStartMillis = remember { System.currentTimeMillis() }

    DisposableEffect(viewModel) {
        onDispose { viewModel.dispose() }
    }

    when {
        state.isFinished -> {
            val durationSeconds = remember {
                ((System.currentTimeMillis() - sessionStartMillis) / 1000).toInt()
            }
            WorkoutSummaryScreen(
                planName = state.session.planName,
                durationSeconds = durationSeconds,
                exerciseCount = state.totalExercises,
                completedSets = state.completedSets,
                exercises = state.session.exercises,
                isSubmitting = state.isSubmitting,
                isSubmitted = state.isSubmitted,
                submitError = state.submitError,
                onRetrySubmit = viewModel::retrySubmit,
                onDismiss = onFinish,
            )
        }

        state.phase == WorkoutSessionViewModel.Phase.REST -> RestTimerScreen(
            viewModel = viewModel,
            state = state,
            sessionStartMillis = sessionStartMillis,
            onBack = onFinish,
        )

        else -> ExerciseScreen(
            viewModel = viewModel,
            state = state,
            sessionStartMillis = sessionStartMillis,
            onBack = onFinish,
        )
    }
}

@Composable
private fun ExerciseScreen(
    viewModel: WorkoutSessionViewModel,
    state: WorkoutSessionViewModel.State,
    sessionStartMillis: Long,
    onBack: () -> Unit,
) {
    var elapsed by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var detailExerciseId by remember { mutableStateOf<String?>(null) }

    BackHandler { showExitDialog = true }

    LaunchedEffect(state.currentExerciseIndex, state.currentSetIndex) {
        elapsed = 0
        paused = false
    }

    LaunchedEffect(state.currentExerciseIndex, state.currentSetIndex, paused) {
        if (paused) return@LaunchedEffect
        while (true) {
            delay(1_000)
            if (!paused) elapsed += 1
        }
    }

    val exercise = state.currentExercise

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularIconButton(onClick = { showExitDialog = true }) {
                Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = OnBackground)
            }
            Text(
                "Тренировка",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = OnBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.size(40.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ExerciseHero(
                techniqueTip = exercise?.techniqueTip,
                onInfoClick = { exercise?.exerciseId?.let { detailExerciseId = it } },
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                exercise?.exerciseName.orEmpty(),
                fontSize = 20.sp,
                fontWeight = FontWeight.W600,
                color = OnBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Основная группа мышц: ${exercise?.muscleGroupLabel.orEmpty()}",
                fontSize = 18.sp,
                fontWeight = FontWeight.W500,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Подход ${state.displaySetNumber} из ${state.totalSets}",
                fontWeight = FontWeight.W500,
                fontSize = 16.sp,
                color = OnBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(state.totalSets) { i ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (i < state.displaySetNumber) Coral
                                else Color.Gray.copy(alpha = 0.3f),
                            ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            ElapsedTimer(
                elapsedSeconds = elapsed,
                paused = paused,
                onTogglePause = { paused = !paused },
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (state.requiresWeight) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ControlCard(
                        label = "Вес (кг)",
                        value = String.format("%.1f", state.currentWeight).removeSuffix(".0"),
                        modifier = Modifier.weight(1f),
                        onMinus = { viewModel.adjustWeight(-2.5) },
                        onPlus = { viewModel.adjustWeight(2.5) },
                    )
                    ControlCard(
                        label = "Повторы",
                        value = state.currentReps.toString(),
                        modifier = Modifier.weight(1f),
                        onMinus = { viewModel.adjustReps(-1) },
                        onPlus = { viewModel.adjustReps(1) },
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    ControlCard(
                        label = "Повторы",
                        value = state.currentReps.toString(),
                        modifier = Modifier.width(180.dp),
                        onMinus = { viewModel.adjustReps(-1) },
                        onPlus = { viewModel.adjustReps(1) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        Button(
            onClick = { viewModel.completeSet(elapsed) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .navigationBarsPadding()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Coral),
        ) {
            Text("Завершить подход", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
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
                    onBack()
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
