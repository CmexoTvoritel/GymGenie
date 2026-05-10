package com.asc.gymgenie.feature.create_workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.asc.gymgenie.exercise.ExerciseShortResponse
import com.asc.gymgenie.presentation.CreateWorkoutViewModel
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.ui.theme.WarmOffWhite

/**
 * Internal navigation targets for the 4-step create-workout flow.
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
    data object WorkoutBuilder : CreateWorkoutStep()
}

/**
 * Orchestrator composable for the 4-step create-workout flow.
 *
 * Holds a lightweight [mutableStateListOf] stack of [CreateWorkoutStep]s and
 * routes each step's forward/back intents through this stack. The view model
 * is injected from the caller so the flow can be re-entered without losing
 * the "Добавить упражнение" context.
 */
@Composable
fun CreateWorkoutFlowScreen(
    viewModel: CreateWorkoutViewModel,
    tokenStorage: TokenStorage,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    val stack = remember { mutableStateListOf<CreateWorkoutStep>(CreateWorkoutStep.MuscleGroupPicker) }

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
    BackHandler {
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
        } else {
            onDismiss()
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
                onBack = { if (stack.size > 1) stack.removeAt(stack.lastIndex) else onDismiss() },
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
                tokenStorage = tokenStorage,
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

            CreateWorkoutStep.WorkoutBuilder -> WorkoutBuilderScreen(
                state = state,
                onBack = {
                    if (stack.size > 1) stack.removeAt(stack.lastIndex) else onDismiss()
                },
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
            )
        }
    }
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
