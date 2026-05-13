package com.asc.gymgenie.navigation.tabs.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.asc.gymgenie.feature.create_workout.CreateWorkoutFlowScreen
import com.asc.gymgenie.feature.workouts.ExerciseDetailScreen
import com.asc.gymgenie.feature.workouts.WorkoutDetailScreen
import com.asc.gymgenie.feature.workouts.WorkoutsScreen
import com.asc.gymgenie.ui.theme.WarmOffWhite

@Composable
fun WorkoutsContent(
    component: WorkoutsComponent,
    onStartPlan: (planId: String, planName: String) -> Unit,
    modifier: Modifier = Modifier,
    isTabActive: Boolean = true,
) {
    val reloadKey by component.reloadKey.subscribeAsState()

    Box(modifier = modifier.fillMaxSize().background(WarmOffWhite)) {
        Children(
            stack = component.stack,
            animation = stackAnimation(slide()),
        ) { child ->
            when (val instance = child.instance) {
                WorkoutsComponent.Child.Main -> WorkoutsScreen(
                    onOpenExercise = { exercise ->
                        component.openExerciseDetail(exercise.id)
                    },
                    onCreateWorkout = component::openCreateWorkout,
                    onStartPlan = onStartPlan,
                    onViewPlan = component::openWorkoutDetail,
                    reloadKey = reloadKey,
                    isTabActive = isTabActive,
                )

                is WorkoutsComponent.Child.ExerciseDetail -> ExerciseDetailScreen(
                    exerciseId = instance.exerciseId,
                    onBack = component::pop,
                )

                is WorkoutsComponent.Child.WorkoutDetail -> WorkoutDetailScreen(
                    planId = instance.planId,
                    onBack = component::pop,
                    onStartPlan = onStartPlan,
                )

                is WorkoutsComponent.Child.CreateWorkout -> CreateWorkoutFlowScreen(
                    viewModel = instance.viewModel,
                    onDismiss = {
                        instance.viewModel.reset()
                        component.pop()
                    },
                    onSaved = component::onWorkoutCreated,
                )
            }
        }
    }
}
