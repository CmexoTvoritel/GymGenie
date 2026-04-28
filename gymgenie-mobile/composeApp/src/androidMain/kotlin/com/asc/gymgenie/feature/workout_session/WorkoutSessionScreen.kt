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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.presentation.WorkoutSessionViewModel
import com.asc.gymgenie.ui.theme.Background
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.CoralLight
import com.asc.gymgenie.ui.theme.OnBackground
import com.asc.gymgenie.ui.theme.OnSurfaceVariant
import com.asc.gymgenie.workout.ActiveWorkoutSession
import com.asc.gymgenie.workout.LocalWorkoutRepository
import com.asc.gymgenie.workout.WorkoutApi
import kotlinx.coroutines.delay

/**
 * Top-level workout session entry point.
 *
 * Drives three discrete sub-screens based on the shared [WorkoutSessionViewModel.State]:
 * - [ExerciseScreen]   — active set logging
 * - [RestTimerScreen]  — between-set rest countdown
 * - [WorkoutSummaryScreen] — final results
 *
 * IMPORTANT: When the session is finished, the summary is rendered INLINE rather than
 * dismissed via [onFinish]. The previous behavior auto-dismissed the screen on completion
 * which skipped the summary entirely. The summary owns the "go home" action that calls
 * [onFinish].
 */
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

    DisposableEffect(viewModel) {
        onDispose { viewModel.dispose() }
    }

    when {
        state.isFinished -> WorkoutSummaryScreen(
            planName = state.session.planName,
            durationSeconds = state.sessionDurationSeconds,
            exerciseCount = state.totalExercises,
            completedSets = state.completedSets,
            exercises = state.session.exercises,
            isSubmitting = state.isSubmitting,
            isSubmitted = state.isSubmitted,
            submitError = state.submitError,
            onRetrySubmit = viewModel::retrySubmit,
            onDismiss = onFinish,
        )

        state.phase == WorkoutSessionViewModel.Phase.REST -> RestTimerScreen(
            viewModel = viewModel,
            state = state,
        )

        else -> ExerciseScreen(
            viewModel = viewModel,
            state = state,
            onBack = onFinish,
        )
    }
}

@Composable
private fun ExerciseScreen(
    viewModel: WorkoutSessionViewModel,
    state: WorkoutSessionViewModel.State,
    onBack: () -> Unit,
) {
    // Per-set elapsed timer. This is intentionally local UI state — it counts up to show
    // how long the user has been working on the current set, and resets when the
    // exercise/set advances. Session-wide duration is tracked by the shared VM.
    var elapsed by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }

    // Reset elapsed timer whenever exercise or set changes.
    LaunchedEffect(state.currentExerciseIndex, state.currentSetIndex) {
        elapsed = 0
        paused = false
    }

    // Tick the elapsed timer once per second when not paused.
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
            .background(Background)
            .statusBarsPadding(),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularIconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Назад", tint = OnBackground)
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    exercise?.exerciseName.orEmpty(),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = OnBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    exercise?.muscleGroupLabel.orEmpty(),
                    fontSize = 12.sp,
                    color = OnSurfaceVariant,
                )
            }
            CircularIconButton(onClick = { /* future menu */ }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Меню", tint = OnBackground)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Exercise hero (image or placeholder).
            ExerciseHero(
                techniqueTip = exercise?.techniqueTip,
                recommendedKg = state.currentWeight.toInt(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Set progress
            Text(
                "Подход ${state.displaySetNumber} из ${state.totalSets}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = OnBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(state.totalSets) { i ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (i < state.displaySetNumber) Coral
                                else Color.Gray.copy(alpha = 0.3f),
                            ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Elapsed timer (per-set, counts up)
            ElapsedTimer(
                elapsedSeconds = elapsed,
                paused = paused,
                onTogglePause = { paused = !paused },
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Weight + reps controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ControlCard(
                    label = "Вес (кг)",
                    value = state.currentWeight.toInt().toString(),
                    modifier = Modifier.weight(1f),
                    onMinus = { viewModel.adjustWeight(-1.0) },
                    onPlus = { viewModel.adjustWeight(1.0) },
                )
                ControlCard(
                    label = "Повторы",
                    value = state.currentReps.toString(),
                    modifier = Modifier.weight(1f),
                    onMinus = { viewModel.adjustReps(-1) },
                    onPlus = { viewModel.adjustReps(1) },
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Complete-set primary action.
        Button(
            onClick = { viewModel.completeSet() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .navigationBarsPadding()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Coral),
        ) {
            Text("Завершить подход ✓", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ExerciseHero(
    techniqueTip: String?,
    recommendedKg: Int,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1A1A2E), Color(0xFF2D2D44)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text("🏋️", fontSize = 64.sp)

        // Recommended weight badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF2ECC71).copy(alpha = 0.9f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(
                "Рек: $recommendedKg кг",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }

        // Technique tip overlay (bottom).
        if (!techniqueTip.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                        ),
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    "Техника",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Text(
                    techniqueTip,
                    fontSize = 11.sp,
                    color = Color.White,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun ElapsedTimer(
    elapsedSeconds: Int,
    paused: Boolean,
    onTogglePause: () -> Unit,
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimerBox(text = String.format("%02d", minutes), modifier = Modifier.weight(1f))
            Text(":", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Coral)
            TimerBox(text = String.format("%02d", seconds), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pause / resume button — outlined coral circle.
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = onTogglePause,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
            ) {
                Icon(
                    imageVector = if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (paused) "Возобновить" else "Пауза",
                    tint = Coral,
                )
            }
        }
    }
}

@Composable
private fun TimerBox(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(88.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CoralLight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = OnBackground,
        )
    }
}

@Composable
private fun ControlCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                label,
                fontSize = 13.sp,
                color = OnSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Minus — outlined / neutral.
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(onClick = onMinus, modifier = Modifier.size(36.dp)) {
                        Text("−", fontSize = 20.sp, color = Coral, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    value,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground,
                )
                // Plus — coral filled.
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Coral),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(onClick = onPlus, modifier = Modifier.size(36.dp)) {
                        Text("+", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
            content()
        }
    }
}

