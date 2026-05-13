package com.asc.gymgenie.navigation.tabs.workouts

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.asc.gymgenie.presentation.CreateWorkoutViewModel

interface WorkoutsComponent {

    val stack: Value<ChildStack<*, Child>>
    val reloadKey: Value<Int>

    fun openExerciseDetail(exerciseId: String)
    fun openWorkoutDetail(planId: String)
    fun openCreateWorkout()
    fun pop()
    fun resetToMain()
    fun onWorkoutCreated()

    sealed class Child {
        data object Main : Child()
        data class ExerciseDetail(val exerciseId: String) : Child()
        data class WorkoutDetail(val planId: String) : Child()
        data class CreateWorkout(val viewModel: CreateWorkoutViewModel) : Child()
    }
}
