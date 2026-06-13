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
            createWorkoutViewModelProvider = createWorkoutViewModelProvider,
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

    private val paywallNavigation = SlotNavigation<PaywallConfig>()

    override val paywallSlot: Value<ChildSlot<*, MainComponent.PaywallChild>> =
        childSlot(
            source = paywallNavigation,
            key = "paywall_slot",
            serializer = null,
            handleBackButton = true,
            childFactory = { _, _ ->
                MainComponent.PaywallChild.Active
            },
        )

    override fun selectTab(tab: MainTab) {
        if (_activeTab.value == tab) {
            when (tab) {
                MainTab.HOME -> homeComponent.resetToMain()
                MainTab.WORKOUTS -> workoutsComponent.resetToMain()
                MainTab.PROFILE -> profileComponent.resetToMain()
                MainTab.AI_COACH -> {}
            }
        }
        _activeTab.value = tab
    }

    override fun openPaywall() {
        paywallNavigation.activate(PaywallConfig)
    }

    override fun closePaywall() {
        paywallNavigation.dismiss()
    }

    override fun startWorkoutSession(session: ActiveWorkoutSession) {
        sessionNavigation.activate(WorkoutSessionConfig(session))
    }

    override fun closeWorkoutSession() {
        sessionNavigation.dismiss()
    }

    private data class WorkoutSessionConfig(val session: ActiveWorkoutSession)

    private data object PaywallConfig
}
