package com.asc.gymgenie.navigation.main

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.Value
import com.asc.gymgenie.navigation.tabs.ai.AiComponent
import com.asc.gymgenie.navigation.tabs.home.HomeComponent
import com.asc.gymgenie.navigation.tabs.profile.ProfileComponent
import com.asc.gymgenie.navigation.tabs.workouts.WorkoutsComponent
import com.asc.gymgenie.workout.ActiveWorkoutSession

interface MainComponent {

    val activeTab: Value<MainTab>

    val homeComponent: HomeComponent
    val aiComponent: AiComponent
    val workoutsComponent: WorkoutsComponent
    val profileComponent: ProfileComponent

    val workoutSessionSlot: Value<ChildSlot<*, WorkoutSessionChild>>
    val paywallSlot: Value<ChildSlot<*, PaywallChild>>

    fun selectTab(tab: MainTab)
    fun openPaywall()
    fun closePaywall()
    fun startWorkoutSession(session: ActiveWorkoutSession)
    fun closeWorkoutSession()

    sealed class WorkoutSessionChild {
        data class Active(val session: ActiveWorkoutSession) : WorkoutSessionChild()
    }

    sealed class PaywallChild {
        data object Active : PaywallChild()
    }
}
