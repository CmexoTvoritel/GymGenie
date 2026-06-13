package com.asc.gymgenie.feature.workouts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Warning
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.asc.gymgenie.exercise.ExerciseShortResponse
import com.asc.gymgenie.feature.create_workout.ExerciseConfigScreen
import com.asc.gymgenie.feature.create_workout.ExercisePickerScreen
import com.asc.gymgenie.feature.create_workout.MuscleGroupPickerScreen
import com.asc.gymgenie.feature.create_workout.muscleGroupExerciseDrawable
import com.asc.gymgenie.ui.components.GymGenieToolbar
import com.asc.gymgenie.ui.components.ToolbarAction
import com.asc.gymgenie.ui.components.formatRecurringDays
import com.asc.gymgenie.ui.components.muscleGroupColors
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.presentation.CreateWorkoutLimits
import com.asc.gymgenie.presentation.CreateWorkoutViewModel
import com.asc.gymgenie.presentation.PendingExercise
import com.asc.gymgenie.presentation.WorkoutDetailUiState
import com.asc.gymgenie.presentation.WorkoutDetailViewModel
import com.asc.gymgenie.workout.WorkoutPlanResponse
import com.asc.gymgenie.utils.WeekdayPairs
import com.asc.gymgenie.utils.formatRestDurationLong
import com.asc.gymgenie.workout.WorkoutScheduleType
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt

private val Coral = Color(0xFFFF5A3C)
private val CoralDark = Color(0xFFE94A2C)
private val CoralTint = Color(0xFFFFF4F0)
private val InkBlack = Color(0xFF0A0A0A)
private val InkMuted = Color(0xFF8B8B92)
private val BorderGray = Color(0xFFEDEDEF)
private val SoftGray = Color(0xFFF4F4F6)
private val DangerRed = Color(0xFFE5484D)
private val WhiteBg = Color(0xFFFFFFFF)

private val ScheduleDayLabels = WeekdayPairs

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

    BackHandler(enabled = !state.isEditing) {
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite),
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
        Icon(
            imageVector = Icons.Outlined.Warning,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = InkMuted,
        )
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
    var showEditDismissDialog by remember { mutableStateOf(false) }
    var showExercisePicker by remember { mutableStateOf(false) }

    var editingExerciseIndex by remember { mutableStateOf<Int?>(null) }

    BackHandler(enabled = state.isEditing) {
        if (hasUnsavedEditChanges(state)) {
            showEditDismissDialog = true
        } else {
            viewModel.cancelEditing()
        }
    }

    if (showEditDismissDialog) {
        EditDismissConfirmDialog(
            onConfirm = {
                showEditDismissDialog = false
                viewModel.cancelEditing()
            },
            onCancel = { showEditDismissDialog = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .imePadding(),
    ) {
        GymGenieToolbar(
            title = if (state.isEditing) "Редактирование тренировки" else "Просмотр",
            showBackNavigation = true,
            showCloseIcon = state.isEditing,
            onBackClick = {
                if (state.isEditing) {
                    if (hasUnsavedEditChanges(state)) {
                        showEditDismissDialog = true
                    } else {
                        viewModel.cancelEditing()
                    }
                } else {
                    onBack()
                }
            },
            actions = if (state.isEditing) emptyList() else listOf(
                ToolbarAction(
                    content = {
                        if (state.isDeleting) {
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
                    },
                    onClick = { if (!state.isDeleting) showDeleteDialog = true },
                )
            ),
        )

        if (state.isEditing) {
            EditModeBody(
                state = state,
                viewModel = viewModel,
                onAddExercise = { showExercisePicker = true },
                onEditExercise = { index -> editingExerciseIndex = index },
                onRemoveExercise = { index ->
                    val editing = editingExerciseIndex
                    if (editing != null && editing >= index) editingExerciseIndex = null
                    viewModel.removeExercise(index)
                },
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

    editingExerciseIndex?.let { idx ->
        if (!state.isEditing || idx !in state.editExercises.indices) {

            LaunchedEffect(idx, state.isEditing) { editingExerciseIndex = null }
        } else {
            val pending = state.editExercises[idx]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WarmOffWhite),
            ) {
                ExerciseConfigScreen(
                    exercise = ExerciseShortResponse(
                        id = pending.exerciseId,
                        nameRu = pending.exerciseNameRu,
                        nameEn = pending.exerciseNameEn,
                        muscleGroup = pending.muscleGroupKey,
                        requiresWeight = pending.requiresWeight,
                    ),
                    onBack = { editingExerciseIndex = null },
                    onConfirm = { updated ->
                        viewModel.updatePendingExerciseAt(idx, updated)
                        editingExerciseIndex = null
                    },
                    prefillFrom = pending,
                    showStepHeader = false,
                )
            }
            BackHandler(enabled = true) { editingExerciseIndex = null }
        }
    }
}

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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SoftGray)
            .border(1.5.dp, BorderGray, RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = plan.name,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                color = InkBlack,
            )

            plan.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = desc,
                    fontSize = 16.sp,
                    color = Color(0xFF555560),
                    lineHeight = 21.sp,
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            HeroStatsRow(plan = plan)
        }
    }
}

@Composable
private fun HeroStatsRow(plan: WorkoutPlanResponse) {
    val exerciseCount = exerciseCountOf(plan)
    val totalSets = totalSetsOf(plan)
    val approxMinutes = estimatedMinutesFromExercises(plan)

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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkMuted,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = caption,
            fontSize = 13.sp,
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
                    fontSize = 15.sp,
                    color = InkMuted,
                )
                Text(
                    text = if (isRecurring) "Постоянная" else "Разовая тренировка",
                    fontSize = 17.sp,
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
                fontSize = 17.sp,
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
                fontSize = 15.sp,
                color = InkMuted,
            )
            Text(
                text = formatRestDurationLong(restSeconds),
                fontSize = 17.sp,
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
        fontSize = 15.sp,
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
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
) {
    val colors = muscleGroupColors(exercise.muscleGroup)
    Row(
        modifier = modifier
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
            Image(
                painter = painterResource(id = muscleGroupExerciseDrawable(exercise.muscleGroup)),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.name,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Bold,
                color = InkBlack,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = exercise.subtitle,
                fontSize = 14.5.sp,
                color = InkMuted,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (onRemove != null) {
            if (onEdit != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SoftGray)
                        .clickable { onEdit() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Редактировать",
                        tint = InkBlack,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
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
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Coral),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = index.toString(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun DraggableExerciseList(
    exercises: List<PendingExercise>,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (Int) -> Unit,
    onEdit: (Int) -> Unit,
) {
    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var itemHeightPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val gapPx = remember(density) { with(density) { 10.dp.toPx() } }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        exercises.forEachIndexed { index, exercise ->
            val isDragging = dragStartIndex == index

            val targetIndex = if (dragStartIndex >= 0 && itemHeightPx > 0) {
                val moves = (dragOffsetY / (itemHeightPx + gapPx)).roundToInt()
                (dragStartIndex + moves).coerceIn(0, exercises.lastIndex)
            } else -1

            val visualOffsetPx = when {
                isDragging -> dragOffsetY
                dragStartIndex >= 0 && targetIndex >= 0 -> {
                    val slotSize = itemHeightPx + gapPx
                    when {
                        dragStartIndex < targetIndex && index in (dragStartIndex + 1)..targetIndex -> -slotSize
                        dragStartIndex > targetIndex && index in targetIndex until dragStartIndex -> slotSize
                        else -> 0f
                    }
                }
                else -> 0f
            }

            ExerciseRow(
                index = index + 1,
                exercise = ExerciseLineItem(
                    exerciseId = exercise.exerciseId,
                    name = exercise.exerciseNameRu,
                    muscleGroup = exercise.muscleGroupKey,
                    sets = exercise.sets,
                    reps = exercise.reps,
                    subtitle = buildEditExerciseSubtitle(exercise),
                ),
                onRemove = { onRemove(index) },
                onEdit = { onEdit(index) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size -> if (size.height > 0) itemHeightPx = size.height.toFloat() }
                    .zIndex(if (isDragging) 1f else 0f)
                    .offset { IntOffset(0, visualOffsetPx.roundToInt()) }
                    .graphicsLayer {
                        alpha = if (isDragging) 0.92f else 1f
                        shadowElevation = if (isDragging) 16f else 0f
                    }
                    .pointerInput(exercise.exerciseId, index) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                dragStartIndex = index
                                dragOffsetY = 0f
                            },
                            onDrag = { _, delta ->
                                dragOffsetY += delta.y
                            },
                            onDragEnd = {
                                val from = dragStartIndex
                                val captured = dragOffsetY
                                dragStartIndex = -1
                                dragOffsetY = 0f
                                if (from >= 0 && itemHeightPx > 0) {
                                    val moves = (captured / (itemHeightPx + gapPx)).roundToInt()
                                    val target = (from + moves).coerceIn(0, exercises.lastIndex)
                                    if (target != from) onMove(from, target)
                                }
                            },
                            onDragCancel = {
                                dragStartIndex = -1
                                dragOffsetY = 0f
                            },
                        )
                    },
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
                fontSize = 16.sp,
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
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun EditModeBody(
    state: WorkoutDetailUiState,
    viewModel: WorkoutDetailViewModel,
    onAddExercise: () -> Unit,
    onEditExercise: (Int) -> Unit,
    onRemoveExercise: (Int) -> Unit,
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
                item {
                    DraggableExerciseList(
                        exercises = state.editExercises,
                        onMove = { from, to -> viewModel.moveExercise(from, to) },
                        onRemove = onRemoveExercise,
                        onEdit = onEditExercise,
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
            fontSize = 16.sp,
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
            minLines = if (!singleLine) 3 else 1,
            textStyle = TextStyle(fontSize = 16.sp, color = InkBlack),
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
            fontSize = 16.sp,
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
                fontSize = 15.sp,
                color = InkMuted,
            )
            Text(
                text = formatRestDurationLong(restSeconds),
                fontSize = 17.sp,
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
                fontSize = 16.sp,
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
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

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

private data class ExerciseLineItem(
    val exerciseId: String,
    val name: String,
    val muscleGroup: String,
    val sets: Int,
    val reps: Int,
    val subtitle: String = "",
)

private fun collectExercises(plan: WorkoutPlanResponse): List<ExerciseLineItem> {
    return plan.days
        .sortedBy { it.orderIndex }
        .flatMap { day ->
            day.exercises
                .sortedBy { it.orderIndex }
                .map { ex ->
                    val subtitle = buildString {
                        append("${ex.sets} × ${ex.reps}")
                        val weights = ex.setWeightsKg?.filterNotNull()?.takeIf { it.isNotEmpty() }
                        if (weights != null) {
                            val unique = weights.distinct()
                            if (unique.size == 1) {
                                val w = if (unique[0] % 1.0 == 0.0) unique[0].toInt().toString() else unique[0].toString()
                                append(" • $w кг")
                            } else {
                                val minW = weights.min()
                                val maxW = weights.max()
                                val minStr = if (minW % 1.0 == 0.0) minW.toInt().toString() else minW.toString()
                                val maxStr = if (maxW % 1.0 == 0.0) maxW.toInt().toString() else maxW.toString()
                                append(" • $minStr-$maxStr кг")
                            }
                        }
                    }
                    ExerciseLineItem(
                        exerciseId = ex.exerciseId,
                        name = ex.exerciseNameRu,
                        muscleGroup = ex.muscleGroup,
                        sets = ex.sets,
                        reps = ex.reps,
                        subtitle = subtitle,
                    )
                }
        }
}

private fun buildEditExerciseSubtitle(exercise: PendingExercise): String = buildString {
    append("${exercise.sets} × ${exercise.reps}")
    if (!exercise.requiresWeight) return@buildString
    val weights = exercise.setWeightsKg?.filterNotNull()?.takeIf { it.isNotEmpty() } ?: return@buildString
    val unique = weights.distinct()
    if (unique.size == 1) {
        val w = if (unique[0] % 1.0 == 0.0) unique[0].toInt().toString() else unique[0].toString()
        append(" • $w кг")
    } else {
        val minW = weights.min()
        val maxW = weights.max()
        val minStr = if (minW % 1.0 == 0.0) minW.toInt().toString() else minW.toString()
        val maxStr = if (maxW % 1.0 == 0.0) maxW.toInt().toString() else maxW.toString()
        append(" • $minStr-$maxStr кг")
    }
}

private fun restSecondsOf(plan: WorkoutPlanResponse): Int {
    val perExercise = plan.days.flatMap { it.exercises }.map { it.restSeconds }
    if (perExercise.isEmpty()) return 60
    return perExercise.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: perExercise.first()
}

private fun exerciseCountOf(plan: WorkoutPlanResponse): Int {
    val day = plan.days.minByOrNull { it.orderIndex } ?: return 0
    return day.exercises.size
}

private fun totalSetsOf(plan: WorkoutPlanResponse): Int {
    val day = plan.days.minByOrNull { it.orderIndex } ?: return 0
    return day.exercises.sumOf { it.sets }
}

private fun estimatedMinutesFromExercises(plan: WorkoutPlanResponse): Int {
    val day = plan.days.minByOrNull { it.orderIndex } ?: return 0
    val exercises = day.exercises.sortedBy { it.orderIndex }
    if (exercises.isEmpty()) return 0

    var totalSeconds = 0.0
    for (ex in exercises) {
        val secPer10 = (ex.secondsPer10Reps ?: 30).toDouble()
        val workSeconds = (secPer10 / 10.0) * ex.reps * ex.sets
        val restTotal = ex.restSeconds.toDouble() * (ex.sets - 1).coerceAtLeast(0)
        totalSeconds += workSeconds + restTotal
    }
    return (totalSeconds / 60.0).toInt().coerceAtLeast(1)
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

private fun hasUnsavedEditChanges(state: WorkoutDetailUiState): Boolean {
    val plan = state.plan ?: return false

    if (state.editName != plan.name) return true
    if (state.editDescription != (plan.description ?: "")) return true

    val originalScheduleType = try {
        WorkoutScheduleType.valueOf(plan.scheduleType.uppercase())
    } catch (_: IllegalArgumentException) {
        WorkoutScheduleType.ONE_TIME
    }
    if (state.editScheduleType != originalScheduleType) return true

    val originalDays = plan.days.map { it.dayOfWeek.uppercase() }.toSet()
    if (state.editScheduleDays != originalDays) return true

    val originalRest = restSecondsOf(plan)
    if (state.editRestSeconds != originalRest) return true

    val originalExercises = collectExerciseSummaries(plan)
    val editSummaries = state.editExercises.map { ex ->
        ExerciseEditSummary(ex.exerciseId, ex.sets, ex.reps, ex.setWeightsKg)
    }
    if (editSummaries != originalExercises) return true

    return false
}

private data class ExerciseEditSummary(
    val exerciseId: String,
    val sets: Int,
    val reps: Int,
    val setWeightsKg: List<Double?>?,
)

private fun collectExerciseSummaries(plan: WorkoutPlanResponse): List<ExerciseEditSummary> {
    return plan.days
        .sortedBy { it.orderIndex }
        .flatMap { day ->
            day.exercises
                .sortedBy { it.orderIndex }
                .map { ex ->
                    ExerciseEditSummary(
                        exerciseId = ex.exerciseId,
                        sets = ex.sets,
                        reps = ex.reps,
                        setWeightsKg = ex.setWeightsKg,
                    )
                }
        }
}

@Composable
private fun EditDismissConfirmDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = "Завершить редактирование?",
                color = InkBlack,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = "Вы уверены, что хотите закончить редактирование тренировки? Несохранённые изменения будут потеряны.",
                color = InkMuted,
                fontSize = 16.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Да", color = DangerRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = "Нет", color = InkBlack, fontSize = 16.sp)
            }
        },
        containerColor = WhiteBg,
    )
}
