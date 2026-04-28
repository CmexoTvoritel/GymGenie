package com.asc.gymgenie.feature.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.asc.gymgenie.db.DatabaseDriverFactory
import com.asc.gymgenie.feature.activities.ActivitiesScreen
import com.asc.gymgenie.feature.activities.ActivityCatalogScreen
import com.asc.gymgenie.feature.activities.ActivityGoalSettingsScreen
import com.asc.gymgenie.feature.activities.GoalCategory
import com.asc.gymgenie.feature.create_workout.CreateWorkoutFlowScreen
import com.asc.gymgenie.feature.workouts.ExerciseDetailScreen
import com.asc.gymgenie.feature.home.HomeScreen
import com.asc.gymgenie.feature.profile.ProfileScreen
import com.asc.gymgenie.feature.workout_session.WorkoutSessionScreen
import com.asc.gymgenie.feature.workouts.WorkoutsScreen
import com.asc.gymgenie.auth.AuthApi
import com.asc.gymgenie.common.createAuthenticatedClient
import com.asc.gymgenie.exercise.ExerciseApi
import com.asc.gymgenie.presentation.CreateWorkoutViewModel
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.workout.LocalWorkoutRepository
import com.asc.gymgenie.ui.theme.OnBackground
import com.asc.gymgenie.ui.theme.OnSurfaceVariant
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.workout.ActiveWorkoutSession
import com.asc.gymgenie.workout.WorkoutApi
import com.asc.gymgenie.workout.toActiveSession
import kotlinx.coroutines.launch

/**
 * Top-level bottom navigation tabs for the redesigned app.
 */
enum class MainTab(
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    HOME("Главная", Icons.Outlined.Home, Icons.Filled.Home),
    AI_COACH("ИИ", Icons.Outlined.AutoAwesome, Icons.Filled.AutoAwesome),
    WORKOUTS("Тренировки", Icons.Outlined.FitnessCenter, Icons.Filled.FitnessCenter),
    PROFILE("Профиль", Icons.Outlined.Person, Icons.Filled.Person),
}

/**
 * Secondary pages that can be pushed above the Home tab content.
 *
 * This is intentionally a lightweight stack-based navigation layer. The existing
 * app currently wires feature navigation via plain state hoisting (see [com.asc.gymgenie.App]);
 * using the same style keeps the new Activities/Catalog flow consistent with
 * the surrounding code until Decompose is introduced for in-tab navigation.
 */
sealed class HomeStackDestination {
    data object Activities : HomeStackDestination()
    data object ActivityCatalog : HomeStackDestination()
    data class ActivityGoalSettings(val category: GoalCategory) : HomeStackDestination()
}

@Composable
fun MainScreen(tokenStorage: TokenStorage, onLogout: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(MainTab.HOME) }
    val homeStack = remember { mutableStateListOf<HomeStackDestination>() }
    var selectedExerciseId by remember { mutableStateOf<String?>(null) }
    var showCreateWorkout by remember { mutableStateOf(false) }
    var activeWorkoutSession by remember { mutableStateOf<ActiveWorkoutSession?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val workoutApi = remember {
        val authApi = AuthApi()
        val client = createAuthenticatedClient(tokenStorage, authApi)
        WorkoutApi(client)
    }
    var isLoadingSession by remember { mutableStateOf(false) }
    val createWorkoutViewModel = remember {
        val authApi = AuthApi()
        val client = createAuthenticatedClient(tokenStorage, authApi)
        CreateWorkoutViewModel(
            exerciseApi = ExerciseApi(client),
            workoutApi = WorkoutApi(client),
        )
    }
    // The repository / driver factory are app-scoped: the underlying SQLite
    // connection is opened once for the lifetime of the surface so that the
    // workout-session feature can write through it on every set without
    // reopening the database.
    val localWorkoutRepository = remember(context) {
        LocalWorkoutRepository(DatabaseDriverFactory(context.applicationContext))
    }

    val isOverlayActive = selectedExerciseId != null || showCreateWorkout

    BackHandler(enabled = isOverlayActive || homeStack.isNotEmpty() || activeWorkoutSession != null) {
        when {
            activeWorkoutSession != null -> activeWorkoutSession = null
            showCreateWorkout -> showCreateWorkout = false
            selectedExerciseId != null -> selectedExerciseId = null
            homeStack.isNotEmpty() -> homeStack.removeAt(homeStack.lastIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(WarmOffWhite)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = 88.dp + WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding(),
                ),
        ) {
            renderContent(
                state = TabStackState(selectedTab, homeStack.toList()),
                tokenStorage = tokenStorage,
                onLogout = onLogout,
                onOpenActivities = { homeStack.add(HomeStackDestination.Activities) },
                onOpenCatalog = { homeStack.add(HomeStackDestination.ActivityCatalog) },
                onPop = {
                    if (homeStack.isNotEmpty()) homeStack.removeAt(homeStack.lastIndex)
                },
                onOpenGoalSettings = { category ->
                    homeStack.add(HomeStackDestination.ActivityGoalSettings(category))
                },
                onOpenExercise = { exerciseId -> selectedExerciseId = exerciseId },
                onCreateWorkout = { showCreateWorkout = true },
                onStartWorkout = { planId, planName ->
                    if (!isLoadingSession) {
                        isLoadingSession = true
                        coroutineScope.launch {
                            workoutApi.getPlanById(planId).fold(
                                onSuccess = { plan ->
                                    activeWorkoutSession = plan.toActiveSession()
                                },
                                onFailure = { /* session stays null, user can retry */ },
                            )
                            isLoadingSession = false
                        }
                    }
                },
                onSessionReady = { session -> activeWorkoutSession = session },
            )
        }

        selectedExerciseId?.let { exerciseId ->
            ExerciseDetailScreen(
                exerciseId = exerciseId,
                tokenStorage = tokenStorage,
                onBack = { selectedExerciseId = null },
            )
        }

        if (showCreateWorkout) {
            CreateWorkoutFlowScreen(
                viewModel = createWorkoutViewModel,
                tokenStorage = tokenStorage,
                onDismiss = {
                    showCreateWorkout = false
                    createWorkoutViewModel.reset()
                },
                onSaved = {
                    showCreateWorkout = false
                    createWorkoutViewModel.reset()
                },
            )
        }

        activeWorkoutSession?.let { session ->
            WorkoutSessionScreen(
                session = session,
                localRepository = localWorkoutRepository,
                workoutApi = workoutApi,
                onFinish = { activeWorkoutSession = null },
            )
        }

        if (!isOverlayActive && activeWorkoutSession == null) {
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
                selectedIndex = tabs.indexOf(selectedTab),
                onItemSelected = { index ->
                    val tab = tabs[index]
                    if (tab == selectedTab) {
                        if (tab == MainTab.HOME) homeStack.clear()
                    } else {
                        selectedTab = tab
                        if (tab == MainTab.HOME) homeStack.clear()
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private data class TabStackState(val tab: MainTab, val stack: List<HomeStackDestination>)

@Composable
private fun renderContent(
    state: TabStackState,
    tokenStorage: TokenStorage,
    onLogout: () -> Unit,
    onOpenActivities: () -> Unit,
    onOpenCatalog: () -> Unit,
    onPop: () -> Unit,
    onOpenGoalSettings: (GoalCategory) -> Unit = {},
    onOpenExercise: (String) -> Unit = {},
    onCreateWorkout: () -> Unit = {},
    onStartWorkout: (planId: String, planName: String) -> Unit = { _, _ -> },
    onSessionReady: (ActiveWorkoutSession) -> Unit = {},
) {
    if (state.tab == MainTab.HOME && state.stack.isNotEmpty()) {
        when (val dest = state.stack.last()) {
            HomeStackDestination.Activities ->
                ActivitiesScreen(
                    onBack = onPop,
                    onOpenCatalog = onOpenCatalog,
                )
            HomeStackDestination.ActivityCatalog ->
                ActivityCatalogScreen(
                    onBack = onPop,
                    onCategorySelected = onOpenGoalSettings,
                )
            is HomeStackDestination.ActivityGoalSettings ->
                ActivityGoalSettingsScreen(
                    category = dest.category,
                    onBack = onPop,
                )
        }
        return
    }

    when (state.tab) {
        MainTab.HOME -> HomeScreen(
            tokenStorage = tokenStorage,
            onLogout = onLogout,
            onOpenActivities = onOpenActivities,
            onOpenCatalog = onOpenCatalog,
            onSessionReady = onSessionReady,
        )
        MainTab.AI_COACH -> PlaceholderScreen("ИИ Тренер", "Скоро будет доступен")
        MainTab.WORKOUTS -> WorkoutsScreen(
            tokenStorage = tokenStorage,
            onLogout = onLogout,
            onOpenExercise = { exercise -> onOpenExercise(exercise.id) },
            onCreateWorkout = onCreateWorkout,
            onStartPlan = { planId, planName -> onStartWorkout(planId, planName) },
        )
        MainTab.PROFILE -> ProfileScreen(tokenStorage = tokenStorage, onLogout = onLogout)
    }
}

@Composable
fun PlaceholderScreen(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = OnBackground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
        )
    }
}

