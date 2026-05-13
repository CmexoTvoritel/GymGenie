package com.asc.gymgenie.navigation.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.asc.gymgenie.navigation.tabs.ai.AiComponent
import com.asc.gymgenie.navigation.tabs.ai.DefaultAiComponent
import com.asc.gymgenie.navigation.tabs.home.DefaultHomeComponent
import com.asc.gymgenie.navigation.tabs.home.HomeComponent
import com.asc.gymgenie.navigation.tabs.profile.DefaultProfileComponent
import com.asc.gymgenie.navigation.tabs.profile.ProfileComponent
import com.asc.gymgenie.navigation.tabs.workouts.DefaultWorkoutsComponent
import com.asc.gymgenie.navigation.tabs.workouts.WorkoutsComponent
import com.asc.gymgenie.presentation.CreateWorkoutViewModel
import com.asc.gymgenie.presentation.ProfileViewModel
import com.asc.gymgenie.workout.ActiveWorkoutSession

class DefaultMainComponent(
    componentContext: ComponentContext,
    private val createWorkoutViewModelProvider: () -> CreateWorkoutViewModel,
    private val profileViewModelProvider: () -> ProfileViewModel,
) : MainComponent, ComponentContext by componentContext {

    private val _activeTab = MutableValue(MainTab.HOME)
    override val activeTab: Value<MainTab> = _activeTab

    override val homeComponent: HomeComponent =
        DefaultHomeComponent(
            componentContext = childContext(key = "tab_home"),
        )

    override val aiComponent: AiComponent =
        DefaultAiComponent(
            componentContext = childContext(key = "tab_ai"),
        )

    override val workoutsComponent: WorkoutsComponent =
        DefaultWorkoutsComponent(
            componentContext = childContext(key = "tab_workouts"),
            createWorkoutViewModelProvider = createWorkoutViewModelProvider,
        )

    override val profileComponent: ProfileComponent =
        DefaultProfileComponent(
            componentContext = childContext(key = "tab_profile"),
            profileViewModelProvider = profileViewModelProvider,
        )

    // Session is intentionally not persisted (serializer = null): on process
    // death the active workout overlay is silently dropped rather than
    // restored into a stale state. The user resumes from the workout list.
    private val sessionNavigation = SlotNavigation<WorkoutSessionConfig>()

    override val workoutSessionSlot: Value<ChildSlot<*, MainComponent.WorkoutSessionChild>> =
        childSlot(
            source = sessionNavigation,
            serializer = null,
            handleBackButton = true,
            childFactory = { config, _ ->
                MainComponent.WorkoutSessionChild.Active(config.session)
            },
        )

    override fun selectTab(tab: MainTab) {
        // Standard "pop to root" on tab reselect — only reset the stack of the
        // currently active tab when the user taps it again. Switching to a
        // different tab preserves its existing back stack.
        if (_activeTab.value == tab) {
            when (tab) {
                MainTab.HOME -> homeComponent.resetToMain()
                MainTab.WORKOUTS -> workoutsComponent.resetToMain()
                MainTab.PROFILE -> profileComponent.resetToMain()
                MainTab.AI_COACH -> { /* leaf tab — no inner stack to reset */ }
            }
        }
        _activeTab.value = tab
    }

    override fun startWorkoutSession(session: ActiveWorkoutSession) {
        sessionNavigation.activate(WorkoutSessionConfig(session))
    }

    override fun closeWorkoutSession() {
        sessionNavigation.dismiss()
    }

    private data class WorkoutSessionConfig(val session: ActiveWorkoutSession)
}
