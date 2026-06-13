package com.asc.gymgenie.feature.create_workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.exercise.ExerciseShortResponse
import com.asc.gymgenie.presentation.CreateWorkoutViewModel
import com.asc.gymgenie.ui.theme.WarmOffWhite

sealed class CreateWorkoutStep {
    data object MuscleGroupPicker : CreateWorkoutStep()
    data class ExercisePicker(val muscleGroupKey: String, val nameRu: String) : CreateWorkoutStep()
    data class ExerciseConfig(val exercise: ExerciseShortResponse) : CreateWorkoutStep()

    data class ExerciseEdit(
        val exercise: ExerciseShortResponse,
        val editingIndex: Int,
    ) : CreateWorkoutStep()

    data object WorkoutBuilder : CreateWorkoutStep()
}

@Composable
fun CreateWorkoutFlowScreen(
    viewModel: CreateWorkoutViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    initialExercise: ExerciseShortResponse? = null,
) {
    val state by viewModel.state.collectAsState()

    val stack = remember {
        val startStep: CreateWorkoutStep = if (initialExercise != null) {
            CreateWorkoutStep.ExerciseConfig(initialExercise)
        } else {
            CreateWorkoutStep.MuscleGroupPicker
        }
        mutableStateListOf(startStep)
    }

    var showDismissDialog by remember { mutableStateOf(false) }

    fun hasUnsavedData(): Boolean =
        state.workoutName.isNotBlank() || state.exercises.isNotEmpty()

    fun requestDismiss() {
        if (hasUnsavedData()) {
            showDismissDialog = true
        } else {
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadMuscleGroups()
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }

    BackHandler {
        val currentStep = stack.lastOrNull()
        when {
            currentStep is CreateWorkoutStep.WorkoutBuilder -> requestDismiss()
            stack.size > 1 -> stack.removeAt(stack.lastIndex)
            else -> requestDismiss()
        }
    }

    DisposableEffect(stack.isEmpty()) {
        if (stack.isEmpty()) onDismiss()
        onDispose { }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        when (val step = stack.last()) {
            CreateWorkoutStep.MuscleGroupPicker -> MuscleGroupPickerScreen(
                state = state,
                onBack = {
                    if (stack.size > 1) {
                        stack.removeAt(stack.lastIndex)
                    } else {
                        requestDismiss()
                    }
                },
                onGroupSelected = { group ->
                    stack.add(
                        CreateWorkoutStep.ExercisePicker(
                            muscleGroupKey = group.key,
                            nameRu = group.nameRu,
                        ),
                    )
                },
                onRetry = { viewModel.loadMuscleGroups(forceReload = true) },
            )

            is CreateWorkoutStep.ExercisePicker -> ExercisePickerScreen(
                muscleGroupKey = step.muscleGroupKey,
                muscleGroupNameRu = step.nameRu,
                onBack = { stack.removeAt(stack.lastIndex) },
                onExerciseSelected = { exercise ->
                    stack.add(CreateWorkoutStep.ExerciseConfig(exercise))
                },
            )

            is CreateWorkoutStep.ExerciseConfig -> ExerciseConfigScreen(
                exercise = step.exercise,
                onBack = {
                    if (stack.size <= 1) requestDismiss()
                    else stack.removeAt(stack.lastIndex)
                },
                onConfirm = { pending ->
                    viewModel.addExercise(pending)
                    advanceToBuilder(stack)
                },
                showStepHeader = initialExercise == null || stack.size > 1,
            )

            is CreateWorkoutStep.ExerciseEdit -> {

                val current = state.exercises.getOrNull(step.editingIndex)
                if (current == null) {
                    LaunchedEffect(step.editingIndex) {
                        if (stack.lastOrNull() is CreateWorkoutStep.ExerciseEdit) {
                            stack.removeAt(stack.lastIndex)
                        }
                    }
                } else {
                    ExerciseConfigScreen(
                        exercise = step.exercise,
                        onBack = { stack.removeAt(stack.lastIndex) },
                        onConfirm = { updated ->
                            viewModel.updateExerciseAt(step.editingIndex, updated)
                            stack.removeAt(stack.lastIndex)
                        },
                        prefillFrom = current,
                        showStepHeader = false,
                    )
                }
            }

            CreateWorkoutStep.WorkoutBuilder -> WorkoutBuilderScreen(
                state = state,
                onBack = { requestDismiss() },
                onNameChange = viewModel::setWorkoutName,
                onDescriptionChange = viewModel::setDescription,
                onIncrementRest = viewModel::incrementRestSeconds,
                onDecrementRest = viewModel::decrementRestSeconds,
                onRemoveExerciseAt = viewModel::removeExerciseAt,
                onAddMoreExercises = {
                    stack.add(CreateWorkoutStep.MuscleGroupPicker)
                },
                onSave = viewModel::saveWorkout,
                onScheduleTypeChange = viewModel::setScheduleType,
                onToggleScheduleDay = viewModel::toggleScheduleDay,
                onEditExerciseAt = { index ->
                    val pending = viewModel.state.value.exercises.getOrNull(index)
                        ?: return@WorkoutBuilderScreen

                    val exerciseShort = ExerciseShortResponse(
                        id = pending.exerciseId,
                        nameRu = pending.exerciseNameRu,
                        nameEn = pending.exerciseNameEn,
                        muscleGroup = pending.muscleGroupKey,
                        requiresWeight = pending.requiresWeight,
                    )
                    stack.add(
                        CreateWorkoutStep.ExerciseEdit(
                            exercise = exerciseShort,
                            editingIndex = index,
                        ),
                    )
                },
            )
        }
    }

    if (showDismissDialog) {
        DismissCreateWorkoutDialog(
            onConfirm = {
                showDismissDialog = false
                onDismiss()
            },
            onCancel = { showDismissDialog = false },
        )
    }
}

@Composable
private fun DismissCreateWorkoutDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = "Завершить создание?",
                color = Color(0xFF0A0A0A),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = "Вы уверены, что хотите закончить создание тренировки?",
                color = Color(0xFF8B8B92),
                fontSize = 16.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Да",
                    color = Color(0xFFE5484D),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = "Нет", color = Color(0xFF0A0A0A), fontSize = 16.sp)
            }
        },
        containerColor = Color.White,
    )
}

private fun advanceToBuilder(stack: androidx.compose.runtime.snapshots.SnapshotStateList<CreateWorkoutStep>) {
    val builderIndex = stack.indexOfFirst { it is CreateWorkoutStep.WorkoutBuilder }
    if (builderIndex >= 0) {
        while (stack.lastIndex > builderIndex) {
            stack.removeAt(stack.lastIndex)
        }
    } else {
        stack.clear()
        stack.add(CreateWorkoutStep.WorkoutBuilder)
    }
}
