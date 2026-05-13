package com.asc.gymgenie.navigation.tabs.workouts

import kotlinx.serialization.Serializable

@Serializable
sealed class WorkoutsConfig {

    @Serializable
    data object Main : WorkoutsConfig()

    @Serializable
    data class ExerciseDetail(val exerciseId: String) : WorkoutsConfig()

    @Serializable
    data class WorkoutDetail(val planId: String) : WorkoutsConfig()

    @Serializable
    data object CreateWorkout : WorkoutsConfig()
}
