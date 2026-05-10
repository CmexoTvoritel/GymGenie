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
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
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
import com.asc.gymgenie.feature.workouts.WorkoutDetailScreen
import com.asc.gymgenie.feature.home.HomeScreen
import com.asc.gymgenie.feature.nutrition.CreateMealPlanFlowScreen
import com.asc.gymgenie.feature.nutrition.NutritionScreen
import com.asc.gymgenie.feature.profile.ProfileScreen
import com.asc.gymgenie.feature.workout_session.WorkoutSessionScreen
import com.asc.gymgenie.feature.workouts.WorkoutsScreen
import com.asc.gymgenie.feature.ai.AiFlowScreen
import com.asc.gymgenie.exercise.ExerciseApi
import com.asc.gymgenie.presentation.CreateWorkoutViewModel
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.user.UserProfileStore
import com.asc.gymgenie.workout.LocalWorkoutRepository
import com.asc.gymgenie.ui.theme.OnBackground
import com.asc.gymgenie.ui.theme.OnSurfaceVariant
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.workout.ActiveWorkoutSession
import com.asc.gymgenie.workout.WorkoutApi
import com.asc.gymgenie.workout.toActiveSession
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

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
    data object Nutrition : HomeStackDestination()
    /**
     * Manual meal-plan creation flow (4 in-feature steps + grams sheet).
     *
     * Pushed from the home empty-state "Создать план" CTA. Pops on dismiss
     * AND on a successful save — `CreateMealPlanFlowScreen` does not own
     * any navigation, the parent observes its terminal callbacks.
     */
    data object CreateMealPlan : HomeStackDestination()
}

@Composable
fun MainScreen(
    tokenStorage: TokenStorage,
    userProfileStore: UserProfileStore,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(MainTab.HOME) }
    val homeStack = remember { mutableStateListOf<HomeStackDestination>() }
    // Bumped whenever the secondary stack collapses past `Activities`, so the
    // activities screen knows to re-fetch the today list (the user may have
    // added or removed a planned activity in the catalog).
    var activitiesRefreshSignal by remember { mutableStateOf(0) }
    var selectedExerciseId by remember { mutableStateOf<String?>(null) }
    var selectedWorkoutPlanId by remember { mutableStateOf<String?>(null) }
    var showCreateWorkout by remember { mutableStateOf(false) }
    var activeWorkoutSession by remember { mutableStateOf<ActiveWorkoutSession?>(null) }
    var workoutsReloadKey by remember { mutableStateOf(0) }
    // Bumped after a meal-plan creation flow saves successfully so the Home
    // screen can refetch its today-meal section without rerunning the whole
    // dashboard load.
    var mealPlansReloadKey by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val koin = remember { GlobalContext.get() }
    val workoutApi = remember { koin.get<WorkoutApi>() }
    var isLoadingSession by remember { mutableStateOf(false) }
    val createWorkoutViewModel = remember {
        // Stateful create-workout view model — kept per-MainScreen via
        // remember{}, but its API dependencies are the shared Koin
        // singletons so token-refresh races cannot occur.
        CreateWorkoutViewModel(
            exerciseApi = koin.get<ExerciseApi>(),
            workoutApi = workoutApi,
        )
    }
    // The repository / driver factory are app-scoped: the underlying SQLite
    // connection is opened once for the lifetime of the surface so that the
    // workout-session feature can write through it on every set without
    // reopening the database.
    val localWorkoutRepository = remember(context) {
        LocalWorkoutRepository(DatabaseDriverFactory(context.applicationContext))
    }

    val isOverlayActive = selectedExerciseId != null || selectedWorkoutPlanId != null || showCreateWorkout

    BackHandler(enabled = isOverlayActive || homeStack.isNotEmpty() || activeWorkoutSession != null) {
        when {
            activeWorkoutSession != null -> activeWorkoutSession = null
            showCreateWorkout -> showCreateWorkout = false
            selectedWorkoutPlanId != null -> selectedWorkoutPlanId = null
            selectedExerciseId != null -> selectedExerciseId = null
            homeStack.isNotEmpty() -> {
                val popped = homeStack.removeAt(homeStack.lastIndex)
                if (popped is HomeStackDestination.ActivityCatalog &&
                    homeStack.lastOrNull() is HomeStackDestination.Activities
                ) {
                    activitiesRefreshSignal += 1
                }
            }
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
            // All four tab composables stay in the composition simultaneously
            // so each tab's `remember { }` ViewModel and scroll state survive
            // tab switches. Only the active tab is visible / interactive.
            val homeOverlayActive = selectedTab == MainTab.HOME && homeStack.isNotEmpty()

            TabContent(visible = selectedTab == MainTab.HOME && !homeOverlayActive) {
                HomeScreen(
                    tokenStorage = tokenStorage,
                    userProfileStore = userProfileStore,
                    onLogout = onLogout,
                    onOpenActivities = { homeStack.add(HomeStackDestination.Activities) },
                    onOpenCatalog = { homeStack.add(HomeStackDestination.ActivityCatalog) },
                    onViewPlan = { plan -> selectedWorkoutPlanId = plan.id },
                    onSessionReady = { session -> activeWorkoutSession = session },
                    // The "Create plan" CTA on the home empty-state opens
                    // the manual creation flow directly. The legacy AI-only
                    // surface (saved plans + AI coach FAB) is reachable from
                    // its own routes; keeping that one available avoids
                    // breaking the deep-link contract used elsewhere.
                    onCreateMealPlan = { homeStack.add(HomeStackDestination.CreateMealPlan) },
                    // Tapping a meal-plan card on the home screen opens the
                    // saved-plans surface — there is no per-plan detail
                    // screen on Android yet, but the user can find their
                    // plan in the list and inspect it from there. Once a
                    // dedicated detail screen exists this should switch
                    // to a `MealPlanDetail(planId)` push.
                    onViewMealPlan = { _ -> homeStack.add(HomeStackDestination.Nutrition) },
                    mealPlansReloadKey = mealPlansReloadKey,
                )
            }

            TabContent(visible = selectedTab == MainTab.AI_COACH) {
                AiFlowScreen(tokenStorage = tokenStorage, userProfileStore = userProfileStore)
            }

            TabContent(visible = selectedTab == MainTab.WORKOUTS) {
                WorkoutsScreen(
                    tokenStorage = tokenStorage,
                    onLogout = onLogout,
                    onOpenExercise = { exercise -> selectedExerciseId = exercise.id },
                    onCreateWorkout = { showCreateWorkout = true },
                    onStartPlan = { planId, planName ->
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
                    onViewPlan = { id -> selectedWorkoutPlanId = id },
                    reloadKey = workoutsReloadKey,
                )
            }

            TabContent(visible = selectedTab == MainTab.PROFILE) {
                ProfileScreen(
                    tokenStorage = tokenStorage,
                    userProfileStore = userProfileStore,
                    onLogout = onLogout,
                )
            }

            // Home secondary stack (Activities / Catalog / Goal settings) is
            // a transient overlay that lives above the Home tab content. It
            // is intentionally _not_ kept alive when the user navigates away
            // because its content is fed exclusively from the Home flow.
            if (homeOverlayActive) {
                HomeSecondaryStack(
                    stack = homeStack.toList(),
                    activitiesRefreshSignal = activitiesRefreshSignal,
                    tokenStorage = tokenStorage,
                    userProfileStore = userProfileStore,
                    onPop = {
                        if (homeStack.isNotEmpty()) {
                            // When we collapse past the catalog back onto the
                            // Activities screen, signal it to re-fetch its
                            // today list so plan edits become visible.
                            val popped = homeStack.removeAt(homeStack.lastIndex)
                            if (popped is HomeStackDestination.ActivityCatalog &&
                                homeStack.lastOrNull() is HomeStackDestination.Activities
                            ) {
                                activitiesRefreshSignal += 1
                            }
                        }
                    },
                    onOpenCatalog = { homeStack.add(HomeStackDestination.ActivityCatalog) },
                    onOpenGoalSettings = { category ->
                        homeStack.add(HomeStackDestination.ActivityGoalSettings(category))
                    },
                    // A successful save in the manual creation flow pops the
                    // overlay AND bumps the reload signal so the home meal
                    // section refetches and the new plan appears immediately.
                    // Bumping happens before the pop so the LaunchedEffect on
                    // the home key fires while the screen is already visible.
                    onMealPlanSaved = {
                        mealPlansReloadKey += 1
                        if (homeStack.isNotEmpty()) {
                            homeStack.removeAt(homeStack.lastIndex)
                        }
                    },
                )
            }
        }

        selectedExerciseId?.let { exerciseId ->
            ExerciseDetailScreen(
                exerciseId = exerciseId,
                tokenStorage = tokenStorage,
                onBack = { selectedExerciseId = null },
            )
        }

        selectedWorkoutPlanId?.let { planId ->
            WorkoutDetailScreen(
                planId = planId,
                tokenStorage = tokenStorage,
                onBack = {
                    selectedWorkoutPlanId = null
                    // Any view → edit → save round-trip on the detail screen
                    // affects what the catalog should display, so we always
                    // bump the reload key when leaving.
                    workoutsReloadKey += 1
                },
                onLogout = onLogout,
                onStartPlan = { id, _ ->
                    if (!isLoadingSession) {
                        isLoadingSession = true
                        coroutineScope.launch {
                            workoutApi.getPlanById(id).fold(
                                onSuccess = { plan ->
                                    selectedWorkoutPlanId = null
                                    workoutsReloadKey += 1
                                    activeWorkoutSession = plan.toActiveSession()
                                },
                                onFailure = { /* keep detail screen, user can retry */ },
                            )
                            isLoadingSession = false
                        }
                    }
                },
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
                    workoutsReloadKey += 1
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

/**
 * Wraps a tab's content so it can stay in the composition tree while not
 * visible. Hidden tabs collapse to zero size and swallow pointer input — the
 * goal is to keep their `remember { }` state (ViewModels, scroll position,
 * `LaunchedEffect` jobs) alive across tab switches without allowing the
 * inactive tree to receive gestures or affect parent layout.
 */
@Composable
private fun TabContent(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = if (visible) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .size(0.dp)
                .clipToBounds()
                .pointerInput(Unit) { /* swallow gestures while hidden */ }
        },
    ) {
        content()
    }
}

@Composable
private fun HomeSecondaryStack(
    stack: List<HomeStackDestination>,
    activitiesRefreshSignal: Int,
    tokenStorage: TokenStorage,
    userProfileStore: UserProfileStore,
    onPop: () -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenGoalSettings: (GoalCategory) -> Unit,
    onMealPlanSaved: () -> Unit,
) {
    val top = stack.lastOrNull() ?: return
    Box(modifier = Modifier.fillMaxSize().background(WarmOffWhite)) {
        when (top) {
            HomeStackDestination.Activities ->
                ActivitiesScreen(
                    onBack = onPop,
                    onOpenCatalog = onOpenCatalog,
                    refreshSignal = activitiesRefreshSignal,
                )
            HomeStackDestination.ActivityCatalog ->
                ActivityCatalogScreen(onBack = onPop)
            is HomeStackDestination.ActivityGoalSettings ->
                ActivityGoalSettingsScreen(
                    category = top.category,
                    onBack = onPop,
                )
            HomeStackDestination.Nutrition ->
                NutritionScreen(
                    tokenStorage = tokenStorage,
                    userProfileStore = userProfileStore,
                    onBack = onPop,
                )
            HomeStackDestination.CreateMealPlan ->
                CreateMealPlanFlowScreen(
                    onDismiss = onPop,
                    // `onSaved` pops the flow AND signals the home tab to
                    // refresh its meal-plan section so the new plan appears
                    // without a manual pull-to-refresh.
                    onSaved = onMealPlanSaved,
                )
        }
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

