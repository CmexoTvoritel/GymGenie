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

/**
 * Internal navigation targets for the create-workout flow.
 *
 * The add-exercise sub-flow is a 3-step wizard (group → exercise → config);
 * the builder is the surrounding hub and is intentionally not numbered.
 *
 * This is intentionally simpler than Decompose: the flow is bounded, the
 * "navigate" verbs only include push/pop/replace semantics and every step's
 * inputs are either carried in the view model or the destination parameters.
 * When Decompose is introduced for the app at large, the entry point can be
 * swapped without changing any of the individual step screens.
 */
sealed class CreateWorkoutStep {
    data object MuscleGroupPicker : CreateWorkoutStep()
    data class ExercisePicker(val muscleGroupKey: String, val nameRu: String) : CreateWorkoutStep()
    data class ExerciseConfig(val exercise: ExerciseShortResponse) : CreateWorkoutStep()

    /**
     * Reuses the config screen as an "edit existing row" surface. The
     * surrounding flow swaps the wizard semantics off: no step header, the
     * confirm callback patches the row at [editingIndex] instead of appending,
     * and the CTA copy reads "Сохранить изменения".
     *
     * [exercise] is rebuilt from the already-added [PendingExercise] so the
     * config screen still gets a uniform [ExerciseShortResponse] input — we
     * deliberately do not refetch the exercise details over the network just
     * to edit sets/weights, since none of the extra fields are read here.
     */
    data class ExerciseEdit(
        val exercise: ExerciseShortResponse,
        val editingIndex: Int,
    ) : CreateWorkoutStep()

    data object WorkoutBuilder : CreateWorkoutStep()
}

/**
 * Orchestrator composable for the create-workout flow.
 *
 * Holds a lightweight [mutableStateListOf] stack of [CreateWorkoutStep]s and
 * routes each step's forward/back intents through this stack. The view model
 * is injected from the caller so the flow can be re-entered without losing
 * the "Добавить упражнение" context.
 */
@Composable
fun CreateWorkoutFlowScreen(
    viewModel: CreateWorkoutViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    val stack = remember { mutableStateListOf<CreateWorkoutStep>(CreateWorkoutStep.MuscleGroupPicker) }

    var showDismissDialog by remember { mutableStateOf(false) }

    // Returns true when the user has entered any meaningful data that would
    // be lost if the flow were dismissed without saving.
    fun hasUnsavedData(): Boolean =
        state.workoutName.isNotBlank() || state.exercises.isNotEmpty()

    // Dismisses the flow if there's nothing to lose, otherwise shows the
    // confirmation dialog.
    fun requestDismiss() {
        if (hasUnsavedData()) {
            showDismissDialog = true
        } else {
            onDismiss()
        }
    }

    // Load muscle groups the first time the flow is opened.
    LaunchedEffect(Unit) {
        viewModel.loadMuscleGroups()
    }

    // Whenever the view model reports a successful save, notify the caller.
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }

    // Hardware back handling: pop the internal stack first, dismiss the flow
    // only when the stack is down to its root and the user presses back again.
    // On the builder step, show a confirmation dialog if there is unsaved data.
    BackHandler {
        val currentStep = stack.lastOrNull()
        when {
            currentStep is CreateWorkoutStep.WorkoutBuilder -> requestDismiss()
            stack.size > 1 -> stack.removeAt(stack.lastIndex)
            else -> requestDismiss()
        }
    }

    // Safety net — if the list somehow becomes empty we treat it as a dismiss.
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
                onBack = { stack.removeAt(stack.lastIndex) },
                onConfirm = { pending ->
                    viewModel.addExercise(pending)
                    advanceToBuilder(stack)
                },
            )

            is CreateWorkoutStep.ExerciseEdit -> {
                // Snapshot the row by index from the latest state so any
                // upstream updates (e.g. a future "reorder") are reflected
                // when the user re-opens edit. Falls back to a pop when the
                // row is no longer present (stale index).
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
                    // Re-derive a minimal [ExerciseShortResponse] from the
                    // already-stored [PendingExercise]. The edit screen only
                    // reads id / nameRu / nameEn / muscleGroup / requiresWeight
                    // from this object, so the remaining fields are safe to
                    // leave at their data-class defaults.
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

/**
 * Collapses the forward-navigation stack so that the user lands back on the
 * workout builder after finishing the add-exercise sub-flow. This keeps the
 * user's scroll position and entered name intact.
 *
 * If the builder is already present somewhere in the stack, pop everything
 * above it; otherwise replace the entire stack with just the builder.
 */
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
