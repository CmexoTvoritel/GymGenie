package com.asc.gymgenie.feature.workouts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.exercise.ExerciseShortResponse
import com.asc.gymgenie.feature.create_workout.ExerciseConfigScreen
import com.asc.gymgenie.feature.create_workout.ExercisePickerScreen
import com.asc.gymgenie.feature.create_workout.MuscleGroupPickerScreen
import com.asc.gymgenie.ui.components.MuscleGroupColors
import com.asc.gymgenie.ui.components.formatRecurringDays
import com.asc.gymgenie.ui.components.muscleGroupCardEmoji
import com.asc.gymgenie.ui.components.muscleGroupColors
import com.asc.gymgenie.presentation.CreateWorkoutLimits
import com.asc.gymgenie.presentation.CreateWorkoutViewModel
import com.asc.gymgenie.presentation.PendingExercise
import com.asc.gymgenie.presentation.WorkoutDetailUiState
import com.asc.gymgenie.presentation.WorkoutDetailViewModel
import com.asc.gymgenie.workout.WorkoutPlanResponse
import com.asc.gymgenie.workout.WorkoutScheduleType
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

private val Coral = Color(0xFFFF5A3C)
private val CoralDark = Color(0xFFE94A2C)
private val CoralTint = Color(0xFFFFF4F0)
private val InkBlack = Color(0xFF0A0A0A)
private val InkMuted = Color(0xFF8B8B92)
private val BorderGray = Color(0xFFEDEDEF)
private val SoftGray = Color(0xFFF4F4F6)
private val DangerRed = Color(0xFFE5484D)
private val WhiteBg = Color(0xFFFFFFFF)

private val ScheduleDayLabels = listOf(
    "MONDAY" to "Пн",
    "TUESDAY" to "Вт",
    "WEDNESDAY" to "Ср",
    "THURSDAY" to "Чт",
    "FRIDAY" to "Пт",
    "SATURDAY" to "Сб",
    "SUNDAY" to "Вс",
)

/**
 * View / edit screen for a single workout plan.
 *
 * The composable owns its own [WorkoutDetailViewModel] keyed by [planId] so
 * that re-entering the screen for a different plan starts from a clean slate.
 * The exercise picker (used in edit mode) is rendered as a full-screen overlay
 * driven by an internal stack — this mirrors the create-workout flow without
 * reusing its top-level navigation, since the parent tab structure differs.
 */
@Composable
fun WorkoutDetailScreen(
    planId: String,
    onBack: () -> Unit,
    onStartPlan: (planId: String, planName: String) -> Unit = { _, _ -> },
) {
    val koin = remember { GlobalContext.get() }
    val viewModel = remember(planId) {
        koin.get<WorkoutDetailViewModel> { parametersOf(planId) }
    }
    DisposableEffect(planId) {
        onDispose { viewModel.onCleared() }
    }

    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) {
            onBack()
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            snackbarHostState.showSnackbar("Тренировка сохранена")
            viewModel.consumeSavedFlag()
        }
    }

    BackHandler(enabled = true) {
        when {
            state.isEditing -> viewModel.cancelEditing()
            else -> onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBg),
    ) {
        when {
            state.isLoading && state.plan == null -> LoadingState()
            state.errorMessage != null && state.plan == null -> ErrorState(
                message = state.errorMessage ?: "",
                onRetry = viewModel::retry,
                onBack = onBack,
            )
            state.plan != null -> Content(
                state = state,
                viewModel = viewModel,
                onBack = onBack,
                onStartPlan = onStartPlan,
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
            snackbar = { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = InkBlack,
                    contentColor = Color.White,
                )
            },
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Coral)
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "⚠️", fontSize = 36.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = InkMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        PrimaryButton(label = "Повторить", onClick = onRetry)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Назад",
            color = InkMuted,
            modifier = Modifier
                .clickable { onBack() }
                .padding(8.dp),
        )
    }
}

@Composable
private fun Content(
    state: WorkoutDetailUiState,
    viewModel: WorkoutDetailViewModel,
    onBack: () -> Unit,
    onStartPlan: (String, String) -> Unit,
) {
    val plan = state.plan ?: return

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExercisePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBg)
            .imePadding(),
    ) {
        Header(
            title = if (state.isEditing) "Редактирование" else "Просмотр",
            planName = plan.name,
            onBack = {
                if (state.isEditing) viewModel.cancelEditing() else onBack()
            },
            trailing = if (state.isEditing) null else {
                {
                    DeleteIconButton(
                        isDeleting = state.isDeleting,
                        onClick = { showDeleteDialog = true },
                    )
                }
            },
        )

        if (state.isEditing) {
            EditModeBody(
                state = state,
                viewModel = viewModel,
                onAddExercise = { showExercisePicker = true },
            )
        } else {
            ViewModeBody(
                plan = plan,
                onEdit = viewModel::startEditing,
                onStart = { onStartPlan(plan.id, plan.name) },
            )
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            onConfirm = {
                showDeleteDialog = false
                viewModel.deletePlan()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    if (showExercisePicker) {
        ExercisePickerOverlay(
            onDismiss = { showExercisePicker = false },
            onConfirmed = { pending ->
                viewModel.addPendingExercise(pending)
                showExercisePicker = false
            },
        )
    }
}

@Composable
private fun Header(
    title: String,
    planName: String,
    onBack: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(WhiteBg)
                .border(1.5.dp, BorderGray, CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = InkBlack,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                color = InkMuted,
            )
            Text(
                text = planName,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = InkBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
private fun DeleteIconButton(
    isDeleting: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(WhiteBg)
            .border(1.5.dp, BorderGray, CircleShape)
            .clickable(enabled = !isDeleting) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (isDeleting) {
            CircularProgressIndicator(
                color = DangerRed,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Удалить",
                tint = DangerRed,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// -- View mode --

@Composable
private fun ViewModeBody(
    plan: WorkoutPlanResponse,
    onEdit: () -> Unit,
    onStart: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { HeroCard(plan = plan) }
            item { ScheduleCard(plan = plan) }
            item { RestTimeCard(restSeconds = restSecondsOf(plan)) }
            item {
                SectionHeader(
                    text = "УПРАЖНЕНИЯ · ${exerciseCountOf(plan)}",
                )
            }
            val exercises = collectExercises(plan)
            if (exercises.isEmpty()) {
                item { EmptyExercisesPlaceholder() }
            } else {
                itemsIndexed(exercises, key = { i, ex -> "${ex.exerciseId}#$i" }) { index, exercise ->
                    ExerciseRow(
                        index = index + 1,
                        exercise = exercise,
                        onRemove = null,
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        ViewModeBottomBar(onEdit = onEdit, onStart = onStart)
    }
}

@Composable
private fun HeroCard(plan: WorkoutPlanResponse) {
    val muscle = primaryMuscleGroup(plan)
    val colors = muscleGroupColors(muscle)
    val isAi = plan.createdBy.equals("AI", ignoreCase = true)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(colors.background, WhiteBg),
                ),
            )
            .border(1.5.dp, BorderGray, RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(WhiteBg)
                        .border(1.dp, BorderGray, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = muscleGroupCardEmoji(muscle),
                        fontSize = 22.sp,
                    )
                }

                MuscleGroupChip(label = muscleGroupRu(muscle), colors = colors)

                if (isAi) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "✦ AI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Coral)
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = plan.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = InkBlack,
            )

            plan.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = desc,
                    fontSize = 14.sp,
                    color = InkMuted,
                    lineHeight = 19.sp,
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            HeroStatsRow(plan = plan)
        }
    }
}

@Composable
private fun MuscleGroupChip(label: String, colors: MuscleGroupColors) {
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = colors.foreground,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(WhiteBg)
            .border(1.dp, colors.foreground.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun HeroStatsRow(plan: WorkoutPlanResponse) {
    val rest = restSecondsOf(plan)
    val exerciseCount = exerciseCountOf(plan)
    val totalSets = totalSetsOf(plan)
    val approxMinutes = estimatedMinutes(totalSets = totalSets, restSeconds = rest)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WhiteBg)
            .border(1.dp, BorderGray, RoundedCornerShape(16.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatCell(
            modifier = Modifier.weight(1f),
            value = "~$approxMinutes",
            unit = "мин",
            caption = "Время",
        )
        StatDivider()
        StatCell(
            modifier = Modifier.weight(1f),
            value = exerciseCount.toString(),
            unit = "",
            caption = "Упражнений",
        )
        StatDivider()
        StatCell(
            modifier = Modifier.weight(1f),
            value = totalSets.toString(),
            unit = "",
            caption = "Подходов",
        )
    }
}

@Composable
private fun StatCell(
    modifier: Modifier,
    value: String,
    unit: String,
    caption: String,
) {
    Column(
        modifier = modifier.padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = InkBlack,
            )
            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkMuted,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = caption,
            fontSize = 11.sp,
            color = InkMuted,
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(BorderGray),
    )
}

@Composable
private fun ScheduleCard(plan: WorkoutPlanResponse) {
    val isRecurring = plan.scheduleType.equals(WorkoutScheduleType.RECURRING.name, ignoreCase = true)
    val activeDays = plan.days.map { it.dayOfWeek.uppercase() }.toSet()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(WhiteBg)
            .border(1.dp, BorderGray, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CoralTint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isRecurring) Icons.Filled.Repeat else Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = CoralDark,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Расписание",
                    fontSize = 13.sp,
                    color = InkMuted,
                )
                Text(
                    text = if (isRecurring) "Постоянная" else "Разовая тренировка",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkBlack,
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isRecurring) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ScheduleDayLabels.forEach { (key, label) ->
                    val active = key in activeDays
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(if (active) Coral else SoftGray),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.White else InkMuted,
                        )
                    }
                }
            }
            if (activeDays.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = formatRecurringDays(activeDays.toList()),
                    fontSize = 12.sp,
                    color = InkMuted,
                )
            }
        } else {
            Text(
                text = "Можно выполнить в любой день",
                fontSize = 13.sp,
                color = InkMuted,
            )
        }
    }
}

@Composable
private fun RestTimeCard(restSeconds: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(WhiteBg)
            .border(1.dp, BorderGray, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CoralTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = null,
                tint = CoralDark,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Отдых между подходами",
                fontSize = 13.sp,
                color = InkMuted,
            )
            Text(
                text = formatRestDurationLong(restSeconds),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = InkBlack,
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = InkMuted,
        letterSpacing = 0.7.sp,
    )
}

@Composable
private fun ExerciseRow(
    index: Int,
    exercise: ExerciseLineItem,
    onRemove: (() -> Unit)?,
) {
    val colors = muscleGroupColors(exercise.muscleGroup)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WhiteBg)
            .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.background),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = muscleGroupCardEmoji(exercise.muscleGroup),
                fontSize = 18.sp,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.name,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = InkBlack,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${exercise.sets} × ${exercise.reps}",
                fontSize = 12.5.sp,
                color = InkMuted,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (onRemove != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SoftGray)
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Удалить",
                    tint = DangerRed,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            Text(
                text = "#$index",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = InkMuted,
            )
        }
    }
}

@Composable
private fun EmptyExercisesPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.5.dp,
                color = BorderGray,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(vertical = 28.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Упражнений пока нет",
            fontSize = 13.sp,
            color = InkMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ViewModeBottomBar(
    onEdit: () -> Unit,
    onStart: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WhiteBg)
            .border(1.dp, BorderGray, RoundedCornerShape(0.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(WhiteBg)
                .border(1.5.dp, BorderGray, RoundedCornerShape(14.dp))
                .clickable { onEdit() }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                tint = InkBlack,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Редактировать",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkBlack,
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Coral)
                .clickable { onStart() }
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Начать",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

// -- Edit mode --

@Composable
private fun EditModeBody(
    state: WorkoutDetailUiState,
    viewModel: WorkoutDetailViewModel,
    onAddExercise: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                EditTextField(
                    label = "Название",
                    value = state.editName,
                    onValueChange = viewModel::setEditName,
                    placeholder = "Название тренировки",
                )
            }
            item {
                EditTextField(
                    label = "Описание",
                    value = state.editDescription,
                    onValueChange = viewModel::setEditDescription,
                    placeholder = "Краткое описание",
                    singleLine = false,
                )
            }
            item {
                ScheduleTypeToggle(
                    selected = state.editScheduleType,
                    onSelected = viewModel::setEditScheduleType,
                )
            }
            if (state.editScheduleType == WorkoutScheduleType.RECURRING) {
                item {
                    EditDayPicker(
                        selectedDays = state.editScheduleDays,
                        onToggle = viewModel::toggleEditScheduleDay,
                    )
                }
            }
            item {
                EditRestStepper(
                    restSeconds = state.editRestSeconds,
                    onIncrement = viewModel::incrementEditRestSeconds,
                    onDecrement = viewModel::decrementEditRestSeconds,
                )
            }
            item {
                SectionHeader(text = "УПРАЖНЕНИЯ · ${state.editExercises.size}")
            }

            if (state.editExercises.isEmpty()) {
                item { EmptyExercisesPlaceholder() }
            } else {
                itemsIndexed(
                    state.editExercises,
                    key = { i, ex -> "${ex.exerciseId}#$i" },
                ) { index, exercise ->
                    ExerciseRow(
                        index = index + 1,
                        exercise = ExerciseLineItem(
                            exerciseId = exercise.exerciseId,
                            name = exercise.exerciseNameRu,
                            muscleGroup = exercise.muscleGroupKey,
                            sets = exercise.sets,
                            reps = exercise.reps,
                        ),
                        onRemove = { viewModel.removeExercise(index) },
                    )
                }
            }

            if (state.errorMessage != null) {
                item {
                    Text(
                        text = state.errorMessage ?: "",
                        color = DangerRed,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        EditBottomBar(
            isSaving = state.isSaving,
            canSave = state.editName.trim().isNotEmpty() && state.editExercises.isNotEmpty() && !state.isSaving,
            onAddExercise = onAddExercise,
            onSave = viewModel::saveEdit,
        )
    }
}

@Composable
private fun EditTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = InkMuted,
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = InkMuted) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = singleLine,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Coral,
                unfocusedBorderColor = BorderGray,
                cursorColor = Coral,
                focusedContainerColor = WhiteBg,
                unfocusedContainerColor = WhiteBg,
            ),
        )
    }
}

@Composable
private fun ScheduleTypeToggle(
    selected: WorkoutScheduleType,
    onSelected: (WorkoutScheduleType) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ScheduleTypePill(
            label = "Разовая",
            isSelected = selected == WorkoutScheduleType.ONE_TIME,
            onClick = { onSelected(WorkoutScheduleType.ONE_TIME) },
            modifier = Modifier.weight(1f),
        )
        ScheduleTypePill(
            label = "Постоянная",
            isSelected = selected == WorkoutScheduleType.RECURRING,
            onClick = { onSelected(WorkoutScheduleType.RECURRING) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ScheduleTypePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (isSelected) Coral else WhiteBg
    val borderColor = if (isSelected) Coral else BorderGray
    val textColor = if (isSelected) Color.White else InkBlack
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
    }
}

@Composable
private fun EditDayPicker(
    selectedDays: Set<String>,
    onToggle: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ScheduleDayLabels.forEach { (key, label) ->
            val isSelected = key in selectedDays
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(if (isSelected) Coral else SoftGray)
                    .clickable { onToggle(key) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else InkMuted,
                )
            }
        }
    }
}

@Composable
private fun EditRestStepper(
    restSeconds: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WhiteBg)
            .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Отдых между подходами",
                fontSize = 13.sp,
                color = InkMuted,
            )
            Text(
                text = formatRestDurationLong(restSeconds),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = InkBlack,
            )
        }
        StepperButton(
            symbol = "−",
            enabled = restSeconds > CreateWorkoutLimits.MIN_REST_SECONDS,
            onClick = onDecrement,
        )
        Spacer(modifier = Modifier.width(8.dp))
        StepperButton(
            symbol = "+",
            enabled = restSeconds < CreateWorkoutLimits.MAX_REST_SECONDS,
            onClick = onIncrement,
        )
    }
}

@Composable
private fun StepperButton(
    symbol: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val background = if (enabled) Coral else InkMuted.copy(alpha = 0.35f)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun EditBottomBar(
    isSaving: Boolean,
    canSave: Boolean,
    onAddExercise: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WhiteBg)
            .border(1.dp, BorderGray, RoundedCornerShape(0.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(WhiteBg)
                .border(1.5.dp, BorderGray, RoundedCornerShape(14.dp))
                .clickable(enabled = !isSaving) { onAddExercise() }
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = InkBlack,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Добавить",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkBlack,
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (canSave) Coral else Coral.copy(alpha = 0.5f))
                .clickable(enabled = canSave) { onSave() }
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Сохранить",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

// -- Delete confirm --

@Composable
private fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Удалить план?", color = InkBlack, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "План тренировок будет удалён без возможности восстановления.",
                color = InkMuted,
                fontSize = 14.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Удалить", color = DangerRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Отмена", color = InkBlack)
            }
        },
        containerColor = WhiteBg,
    )
}

// -- Exercise picker overlay --

/**
 * Reuses the create-workout flow (muscle group → exercise → sets/reps) as a
 * scoped picker. The overlay drives a private [CreateWorkoutViewModel] purely
 * to carry the muscle-groups list — adding an exercise is intercepted by the
 * caller's [onConfirmed], so the local view model never enters a save path.
 */
@Composable
private fun ExercisePickerOverlay(
    onDismiss: () -> Unit,
    onConfirmed: (PendingExercise) -> Unit,
) {
    val koin = remember { GlobalContext.get() }
    val pickerVm = remember { koin.get<CreateWorkoutViewModel>() }
    DisposableEffect(Unit) {
        pickerVm.loadMuscleGroups()
        onDispose { pickerVm.onCleared() }
    }
    val pickerState by pickerVm.state.collectAsState()

    var step by remember { mutableStateOf<PickerStep>(PickerStep.MuscleGroup) }

    BackHandler(enabled = true) {
        when (val current = step) {
            PickerStep.MuscleGroup -> onDismiss()
            is PickerStep.Exercise -> step = PickerStep.MuscleGroup
            is PickerStep.Config -> step = PickerStep.Exercise(current.muscleKey, current.muscleNameRu)
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFFFAF9F7)),
    ) {
        when (val current = step) {
            PickerStep.MuscleGroup -> MuscleGroupPickerScreen(
                state = pickerState,
                onBack = onDismiss,
                onGroupSelected = { group ->
                    step = PickerStep.Exercise(group.key, group.nameRu)
                },
                onRetry = { pickerVm.loadMuscleGroups(forceReload = true) },
            )
            is PickerStep.Exercise -> ExercisePickerScreen(
                muscleGroupKey = current.muscleKey,
                muscleGroupNameRu = current.muscleNameRu,
                onBack = { step = PickerStep.MuscleGroup },
                onExerciseSelected = { exercise ->
                    step = PickerStep.Config(
                        muscleKey = current.muscleKey,
                        muscleNameRu = current.muscleNameRu,
                        exercise = exercise,
                    )
                },
            )
            is PickerStep.Config -> ExerciseConfigScreen(
                exercise = current.exercise,
                onBack = {
                    step = PickerStep.Exercise(current.muscleKey, current.muscleNameRu)
                },
                onConfirm = { pending ->
                    onConfirmed(pending)
                },
            )
        }
    }
}

private sealed interface PickerStep {
    data object MuscleGroup : PickerStep
    data class Exercise(val muscleKey: String, val muscleNameRu: String) : PickerStep
    data class Config(
        val muscleKey: String,
        val muscleNameRu: String,
        val exercise: ExerciseShortResponse,
    ) : PickerStep
}

// -- Helpers --

private data class ExerciseLineItem(
    val exerciseId: String,
    val name: String,
    val muscleGroup: String,
    val sets: Int,
    val reps: Int,
)

private fun collectExercises(plan: WorkoutPlanResponse): List<ExerciseLineItem> {
    return plan.days
        .sortedBy { it.orderIndex }
        .flatMap { day ->
            day.exercises
                .sortedBy { it.orderIndex }
                .map { ex ->
                    ExerciseLineItem(
                        exerciseId = ex.exerciseId,
                        name = ex.exerciseNameRu,
                        muscleGroup = ex.muscleGroup,
                        sets = ex.sets,
                        reps = ex.reps,
                    )
                }
        }
}

private fun primaryMuscleGroup(plan: WorkoutPlanResponse): String? {
    return plan.days
        .flatMap { it.exercises }
        .map { it.muscleGroup }
        .filter { it.isNotBlank() }
        .groupingBy { it.uppercase() }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
}

private fun restSecondsOf(plan: WorkoutPlanResponse): Int {
    val perExercise = plan.days.flatMap { it.exercises }.map { it.restSeconds }
    if (perExercise.isEmpty()) return 60
    return perExercise.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: perExercise.first()
}

private fun exerciseCountOf(plan: WorkoutPlanResponse): Int {
    return plan.days.sumOf { it.exercises.size }
}

private fun totalSetsOf(plan: WorkoutPlanResponse): Int {
    return plan.days.flatMap { it.exercises }.sumOf { it.sets }
}

private fun estimatedMinutes(totalSets: Int, restSeconds: Int): Int {
    if (totalSets <= 0) return 0
    val perSetSeconds = 30 + restSeconds.coerceAtLeast(0)
    val total = totalSets * perSetSeconds
    return (total / 60).coerceAtLeast(1)
}

private fun formatRestDurationLong(seconds: Int): String {
    if (seconds < 60) return "$seconds сек"
    val minutes = seconds / 60
    val remainder = seconds % 60
    return if (remainder == 0) "$minutes мин" else "$minutes мин $remainder сек"
}

private fun muscleGroupRu(group: String?): String = when (group?.uppercase()) {
    "CHEST" -> "Грудь"
    "BACK" -> "Спина"
    "SHOULDERS", "SHOULDER" -> "Плечи"
    "BICEPS" -> "Бицепс"
    "TRICEPS" -> "Трицепс"
    "FOREARMS" -> "Предплечья"
    "ABS", "CORE" -> "Пресс"
    "QUADRICEPS" -> "Квадрицепс"
    "HAMSTRINGS" -> "Бицепс бедра"
    "CALVES" -> "Икры"
    "GLUTES" -> "Ягодицы"
    "CARDIO" -> "Кардио"
    "FULL_BODY" -> "Всё тело"
    "ARMS" -> "Руки"
    "LEGS" -> "Ноги"
    null, "" -> "План"
    else -> group
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Coral)
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
