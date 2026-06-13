package com.asc.gymgenie.navigation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.asc.gymgenie.feature.main.BottomNavBar
import com.asc.gymgenie.feature.main.BottomNavItem
import com.asc.gymgenie.feature.paywall.PaywallScreen
import com.asc.gymgenie.feature.paywall.PurchaseSuccessScreen
import com.asc.gymgenie.feature.workout_session.WorkoutSessionScreen
import com.asc.gymgenie.navigation.tabs.ai.AiContent
import com.asc.gymgenie.navigation.tabs.home.HomeContent
import com.asc.gymgenie.navigation.tabs.profile.ProfileContent
import com.asc.gymgenie.navigation.tabs.workouts.WorkoutsContent
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.workout.LocalWorkoutRepository
import com.asc.gymgenie.workout.WorkoutApi
import com.asc.gymgenie.workout.toActiveSession
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

@Composable
fun MainContent(
    component: MainComponent,
    modifier: Modifier = Modifier,
) {
    val activeTab by component.activeTab.subscribeAsState()
    val sessionSlot by component.workoutSessionSlot.subscribeAsState()
    val paywallSlotState by component.paywallSlot.subscribeAsState()

    val koin = remember { GlobalContext.get() }
    val workoutApi = remember { koin.get<WorkoutApi>() }
    val localWorkoutRepository = remember { koin.get<LocalWorkoutRepository>() }
    val coroutineScope = rememberCoroutineScope()
    var isLoadingSession by remember { mutableStateOf(false) }
    var showingPurchaseSuccess by rememberSaveable { mutableStateOf(false) }

    val onStartPlan: (String, String) -> Unit = { planId, _ ->
        if (!isLoadingSession) {
            isLoadingSession = true
            coroutineScope.launch {
                workoutApi.getPlanById(planId).fold(
                    onSuccess = { plan -> component.startWorkoutSession(plan.toActiveSession()) },
                    onFailure = { },
                )
                isLoadingSession = false
            }
        }
    }

    val activeSession = (sessionSlot.child?.instance as? MainComponent.WorkoutSessionChild.Active)
    val isSessionActive = activeSession != null
    val isPaywallActive = paywallSlotState.child?.instance is MainComponent.PaywallChild.Active
    val showBottomBar = !isSessionActive && !isPaywallActive && shouldShowBottomBar(component, activeTab)

    Box(modifier = modifier.fillMaxSize().background(WarmOffWhite)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = if (showBottomBar) {
                        88.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    } else 0.dp,
                ),
        ) {
            TabHost(visible = activeTab == MainTab.HOME) {
                HomeContent(
                    component = component.homeComponent,
                    onOpenWorkoutPlan = { plan -> component.homeComponent.openWorkoutDetail(plan.id) },
                    onStartPlan = onStartPlan,
                    onSessionReady = component::startWorkoutSession,
                    onOpenPaywall = component::openPaywall,
                    onSwitchToProfile = { component.selectTab(MainTab.PROFILE) },
                    onSwitchToWorkouts = { component.selectTab(MainTab.WORKOUTS) },
                )
            }
            TabHost(visible = activeTab == MainTab.AI_COACH) {
                AiContent(
                    component = component.aiComponent,
                    onOpenPaywall = component::openPaywall,
                )
            }
            TabHost(visible = activeTab == MainTab.WORKOUTS) {
                WorkoutsContent(
                    component = component.workoutsComponent,
                    onStartPlan = onStartPlan,
                    isTabActive = activeTab == MainTab.WORKOUTS,
                )
            }
            TabHost(visible = activeTab == MainTab.PROFILE) {
                ProfileContent(component = component.profileComponent)
            }
        }

        if (activeSession != null) {
            WorkoutSessionScreen(
                session = activeSession.session,
                localRepository = localWorkoutRepository,
                workoutApi = workoutApi,
                onFinish = component::closeWorkoutSession,
            )
        }

        if (isPaywallActive) {
            if (showingPurchaseSuccess) {
                PurchaseSuccessScreen(
                    onContinue = {
                        showingPurchaseSuccess = false
                        component.closePaywall()
                    },
                )
            } else {
                PaywallScreen(
                    onPurchaseSuccess = { showingPurchaseSuccess = true },
                    onSkip = component::closePaywall,
                )
            }
        }

        if (showBottomBar) {
            val tabs = remember { MainTab.entries.toList() }
            val items = remember(tabs) {
                tabs.map { tab ->
                    BottomNavItem(
                        title = tab.title,
                        icon = tab.icon,
                        selectedIcon = tab.selectedIcon,
                    )
                }
            }
            BottomNavBar(
                items = items,
                selectedIndex = tabs.indexOf(activeTab),
                onItemSelected = { index -> component.selectTab(tabs[index]) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun shouldShowBottomBar(component: MainComponent, tab: MainTab): Boolean {
    return when (tab) {
        MainTab.AI_COACH -> {
            val aiShow by component.aiComponent.showBottomBar.subscribeAsState()
            aiShow
        }
        MainTab.HOME -> {
            val home by component.homeComponent.stack.subscribeAsState()
            home.items.size == 1
        }
        MainTab.WORKOUTS -> {
            val workouts by component.workoutsComponent.stack.subscribeAsState()
            workouts.items.size == 1
        }
        MainTab.PROFILE -> {
            val profile by component.profileComponent.stack.subscribeAsState()
            profile.items.size == 1
        }
    }
}

@Composable
private fun TabHost(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = if (visible) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .size(0.dp)
                .clip(RoundedCornerShape(0.dp))
                .clipToBounds()
                .pointerInput(Unit) { }
        },
    ) {
        content()
    }
}
