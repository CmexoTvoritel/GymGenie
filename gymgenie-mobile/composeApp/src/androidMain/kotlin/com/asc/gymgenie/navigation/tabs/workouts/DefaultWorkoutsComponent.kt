// Required because StackNavigator.push (used by openExerciseDetail /
// openWorkoutDetail / openCreateWorkout) is @DelicateDecomposeApi. Pushes
// here are user-initiated and configurations carry distinct ids
// (exerciseId / planId), so duplicate-configuration crashes are not a
// realistic risk.
@file:OptIn(com.arkivanov.decompose.DelicateDecomposeApi::class)

package com.asc.gymgenie.navigation.tabs.workouts

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.asc.gymgenie.presentation.CreateWorkoutViewModel

class DefaultWorkoutsComponent(
    componentContext: ComponentContext,
    private val createWorkoutViewModelProvider: () -> CreateWorkoutViewModel,
) : WorkoutsComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<WorkoutsConfig>()

    private val createWorkoutViewModel: CreateWorkoutViewModel by lazy {
        createWorkoutViewModelProvider().also { vm ->
            lifecycle.doOnDestroy { vm.onCleared() }
        }
    }

    override val stack: Value<ChildStack<*, WorkoutsComponent.Child>> =
        childStack(
            source = navigation,
            serializer = WorkoutsConfig.serializer(),
            initialConfiguration = WorkoutsConfig.Main,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    private val _reloadKey = MutableValue(0)
    override val reloadKey: Value<Int> = _reloadKey

    private fun createChild(
        config: WorkoutsConfig,
        @Suppress("UNUSED_PARAMETER") childContext: ComponentContext,
    ): WorkoutsComponent.Child = when (config) {
        WorkoutsConfig.Main -> WorkoutsComponent.Child.Main
        is WorkoutsConfig.ExerciseDetail -> WorkoutsComponent.Child.ExerciseDetail(config.exerciseId)
        is WorkoutsConfig.WorkoutDetail -> WorkoutsComponent.Child.WorkoutDetail(config.planId)
        WorkoutsConfig.CreateWorkout -> WorkoutsComponent.Child.CreateWorkout(createWorkoutViewModel)
    }

    override fun openExerciseDetail(exerciseId: String) {
        navigation.push(WorkoutsConfig.ExerciseDetail(exerciseId))
    }

    override fun openWorkoutDetail(planId: String) {
        navigation.push(WorkoutsConfig.WorkoutDetail(planId))
    }

    override fun openCreateWorkout() {
        navigation.push(WorkoutsConfig.CreateWorkout)
    }

    override fun pop() {
        val current = stack.value.active.configuration
        navigation.pop()
        if (current is WorkoutsConfig.WorkoutDetail) {
            _reloadKey.value = _reloadKey.value + 1
        }
    }

    override fun resetToMain() {
        navigation.replaceAll(WorkoutsConfig.Main)
    }

    override fun onWorkoutCreated() {
        createWorkoutViewModel.reset()
        _reloadKey.value = _reloadKey.value + 1
        navigation.pop()
    }
}
