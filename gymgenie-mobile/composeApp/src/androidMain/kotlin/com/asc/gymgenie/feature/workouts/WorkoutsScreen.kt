package com.asc.gymgenie.feature.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.asc.gymgenie.exercise.ExerciseShortResponse
import com.asc.gymgenie.feature.workouts.components.CreateWorkoutFab
import com.asc.gymgenie.feature.workouts.components.ErrorContent
import com.asc.gymgenie.feature.workouts.components.ExerciseCard
import com.asc.gymgenie.feature.workouts.components.ExerciseFilterBottomSheet
import com.asc.gymgenie.feature.workouts.components.ExerciseSearchBar
import com.asc.gymgenie.feature.workouts.components.ExercisesEmptyState
import com.asc.gymgenie.feature.workouts.components.LoadingContent
import com.asc.gymgenie.feature.workouts.components.MuscleGroupFilterChips
import com.asc.gymgenie.feature.workouts.components.WorkoutTabSelector
import com.asc.gymgenie.feature.workouts.components.WorkoutsEmptyState
import com.asc.gymgenie.presentation.WorkoutsTab
import com.asc.gymgenie.presentation.WorkoutsUiState
import com.asc.gymgenie.ui.components.GymGenieToolbar
import com.asc.gymgenie.ui.components.WorkoutPlanCard
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.WarmOffWhite

private val TabOrder: List<WorkoutsTab> = listOf(WorkoutsTab.WORKOUTS, WorkoutsTab.EXERCISES)
private val ExercisesCollapsibleHeight = 110.dp

@Composable
fun WorkoutsScreen(
    onOpenExercise: (ExerciseShortResponse) -> Unit = {},
    onCreateWorkout: () -> Unit = {},
    onStartPlan: (planId: String, planName: String) -> Unit = { _, _ -> },
    onViewPlan: (planId: String) -> Unit = {},
    reloadKey: Int = 0,
    isTabActive: Boolean = true,
) {
    val viewModel = rememberWorkoutsViewModel()
    val state by viewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Hoisted to survive Exercises tab leaving composition when the user
    // switches to the Workouts tab. Owned here so scroll position and
    // collapsible-header offset are preserved across tab switches.
    val exercisesGridState = rememberLazyGridState()
    var exercisesScrollOffsetPx by remember { mutableFloatStateOf(0f) }

    // Filter bottom sheet visibility is screen-local UI concern; the underlying
    // filter values live in the shared ViewModel state so they survive
    // tab switches and process death-equivalent recompositions.
    var showFilterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(reloadKey) {
        viewModel.loadWorkoutPlans()
    }

    // Stable key — pagerState is never recreated due to tab changes.
    // This prevents the feedback loop where a 0-dp viewport (TabHost hiding
    // the composable) causes an incorrect page emission → ViewModel update →
    // wrong initial page on the next visit.
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { TabOrder.size },
    )

    // When the tab becomes visible: first snap the pager to the ViewModel's selected
    // tab (corrects any wrong page from the 0dp TabHost), then start listening for
    // user swipes. .drop(1) skips the initial snapshotFlow emission (the page we
    // just snapped to), so neither the 0dp-induced page nor the correction itself
    // can corrupt ViewModel state. The two concerns are sequential in one coroutine,
    // eliminating the race condition that existed when they were separate effects.
    LaunchedEffect(isTabActive) {
        if (!isTabActive) return@LaunchedEffect
        val targetPage = TabOrder.indexOf(state.selectedTab).coerceAtLeast(0)
        if (pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
        snapshotFlow { pagerState.currentPage }
            .drop(1)
            .collect { page ->
                val tab = TabOrder.getOrNull(page) ?: return@collect
                viewModel.selectTab(tab)
            }
    }

    val currentTab = TabOrder.getOrNull(pagerState.currentPage) ?: WorkoutsTab.WORKOUTS

    // Stable lambdas keyed on the ViewModel — without these, every recomposition
    // hands fresh function references to ExerciseSearchBar / MuscleGroupFilterChips
    // and they cannot skip recomposition, defeating the no-unmount goal when
    // filters or the query change.
    val onSearchQueryChanged: (String) -> Unit = remember(viewModel) { viewModel::onSearchQueryChanged }
    val onSearch: () -> Unit = remember(viewModel) { viewModel::searchExercises }
    val onClearSearch: () -> Unit = remember(viewModel) {
        {
            viewModel.onSearchQueryChanged("")
            viewModel.loadExercises(reset = true)
        }
    }
    val onFilterMuscleGroup: (String?) -> Unit = remember(viewModel) { viewModel::filterByMuscleGroup }
    val onLoadMore: () -> Unit = remember(viewModel) { viewModel::loadMoreExercises }
    val onShowFilters: () -> Unit = remember { { showFilterSheet = true } }
    val onDismissFilters: () -> Unit = remember { { showFilterSheet = false } }
    val onApplyFilters: (List<String>, Boolean?, String?, String?) -> Unit = remember(viewModel) {
        { difficulties, requiresEquipment, sortByDifficulty, sortByCalories ->
            viewModel.applyFilters(difficulties, requiresEquipment, sortByDifficulty, sortByCalories)
        }
    }

    // Count non-default filter dimensions for the active-state indicator on the
    // filter button. Plain derivation is fine because `state` is already a
    // snapshot-observed value read by collectAsState; recomputing on every
    // recomposition is cheap.
    val activeFiltersCount = run {
        var count = 0
        if (state.selectedDifficulties.isNotEmpty()) count++
        if (state.requiresEquipment != null) count++
        if (state.sortByDifficulty != null) count++
        if (state.sortByCalories != null) count++
        count
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GymGenieToolbar(title = "Тренировки")

            Spacer(modifier = Modifier.height(4.dp))

            WorkoutTabSelector(
                selectedTab = currentTab,
                onTabSelected = { tab ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(TabOrder.indexOf(tab))
                    }
                    viewModel.selectTab(tab)
                },
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (TabOrder[page]) {
                    WorkoutsTab.WORKOUTS -> WorkoutsPage(
                        state = state,
                        onStartPlan = onStartPlan,
                        onViewPlan = onViewPlan,
                        onCreateWorkout = onCreateWorkout,
                        onRetry = viewModel::retry,
                        onRefresh = viewModel::refresh,
                    )
                    WorkoutsTab.EXERCISES -> ExercisesPage(
                        state = state,
                        gridState = exercisesGridState,
                        scrollOffsetPx = exercisesScrollOffsetPx,
                        onScrollOffsetChange = { exercisesScrollOffsetPx = it },
                        onSearchQueryChanged = onSearchQueryChanged,
                        onSearch = onSearch,
                        onClearSearch = onClearSearch,
                        onLoadMore = onLoadMore,
                        onFilterMuscleGroup = onFilterMuscleGroup,
                        onExerciseClick = onOpenExercise,
                        onRetry = viewModel::retry,
                        onRefresh = viewModel::refresh,
                        onShowFilters = onShowFilters,
                        activeFiltersCount = activeFiltersCount,
                    )
                }
            }
        }

        if (currentTab == WorkoutsTab.WORKOUTS) {
            CreateWorkoutFab(
                onClick = onCreateWorkout,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp),
            )
        }
    }

    if (showFilterSheet) {
        ExerciseFilterBottomSheet(
            currentDifficulties = state.selectedDifficulties,
            currentRequiresEquipment = state.requiresEquipment,
            currentSortByDifficulty = state.sortByDifficulty,
            currentSortByCalories = state.sortByCalories,
            onApply = onApplyFilters,
            onDismiss = onDismissFilters,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutsPage(
    state: WorkoutsUiState,
    onStartPlan: (planId: String, planName: String) -> Unit,
    onViewPlan: (planId: String) -> Unit,
    onCreateWorkout: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
) {
    val tabIsEmpty = state.workoutPlans.isEmpty()
    val showLoading = state.isLoading && tabIsEmpty
    val showError = state.errorMessage != null && tabIsEmpty && !(state.workoutPlansLoaded && tabIsEmpty)

    when {
        showLoading -> LoadingContent()
        showError -> ErrorContent(message = state.errorMessage ?: "", onRetry = onRetry)
        else -> WorkoutsTabContent(
            state = state,
            onStartPlan = onStartPlan,
            onViewPlan = onViewPlan,
            onCreateWorkout = onCreateWorkout,
            onRefresh = onRefresh,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisesPage(
    state: WorkoutsUiState,
    gridState: LazyGridState,
    scrollOffsetPx: Float,
    onScrollOffsetChange: (Float) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onLoadMore: () -> Unit,
    onFilterMuscleGroup: (String?) -> Unit,
    onExerciseClick: (ExerciseShortResponse) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onShowFilters: () -> Unit,
    activeFiltersCount: Int,
) {
    val tabIsEmpty = state.exercises.isEmpty()
    val showLoading = state.isLoading && tabIsEmpty
    val expectedEmpty = state.exercisesLoaded && tabIsEmpty && state.errorMessage == null
    val showError = state.errorMessage != null && tabIsEmpty && !expectedEmpty

    when {
        showLoading -> LoadingContent()
        showError -> ErrorContent(message = state.errorMessage ?: "", onRetry = onRetry)
        else -> ExercisesTabContent(
            state = state,
            gridState = gridState,
            scrollOffsetPx = scrollOffsetPx,
            onScrollOffsetChange = onScrollOffsetChange,
            onSearchQueryChanged = onSearchQueryChanged,
            onSearch = onSearch,
            onClearSearch = onClearSearch,
            onLoadMore = onLoadMore,
            onFilterMuscleGroup = onFilterMuscleGroup,
            onExerciseClick = onExerciseClick,
            onRefresh = onRefresh,
            onShowFilters = onShowFilters,
            activeFiltersCount = activeFiltersCount,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutsTabContent(
    state: WorkoutsUiState,
    onStartPlan: (planId: String, planName: String) -> Unit,
    onViewPlan: (planId: String) -> Unit,
    onCreateWorkout: () -> Unit,
    onRefresh: () -> Unit,
) {
    val refreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        state = refreshState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = refreshState,
                isRefreshing = state.isRefreshing,
                containerColor = Color.White,
                color = Coral,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
    ) {
        if (state.workoutPlans.isEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    WorkoutsEmptyState(onCreateWorkout = onCreateWorkout)
                }
            }
            return@PullToRefreshBox
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.workoutPlans, key = { it.id }) { plan ->
                WorkoutPlanCard(
                    plan = plan,
                    onView = { onViewPlan(plan.id) },
                    onStart = { onStartPlan(plan.id, plan.name) },
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisesTabContent(
    state: WorkoutsUiState,
    gridState: LazyGridState,
    scrollOffsetPx: Float,
    onScrollOffsetChange: (Float) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onLoadMore: () -> Unit,
    onFilterMuscleGroup: (String?) -> Unit,
    onExerciseClick: (ExerciseShortResponse) -> Unit,
    onRefresh: () -> Unit,
    onShowFilters: () -> Unit,
    activeFiltersCount: Int,
) {
    val refreshState = rememberPullToRefreshState()
    val density = LocalDensity.current
    // headerHeightPx is derived from layout measurement (not user scroll),
    // so resetting it on recomposition is harmless — keep local.
    var headerHeightPx by remember { mutableFloatStateOf(with(density) { ExercisesCollapsibleHeight.toPx() }) }

    // Keep latest references so the remembered NestedScrollConnection always
    // reads the current hoisted offset and callback, without rebuilding.
    val currentScrollOffsetPx by rememberUpdatedState(scrollOffsetPx)
    val currentOnScrollOffsetChange by rememberUpdatedState(onScrollOffsetChange)

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val offset = currentScrollOffsetPx
                if (available.y > 0f && offset > 0f) {
                    val toExpand = minOf(available.y, offset)
                    currentOnScrollOffsetChange(offset - toExpand)
                    return Offset(0f, toExpand)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (consumed.y < 0f) {
                    currentOnScrollOffsetChange(
                        (currentScrollOffsetPx - consumed.y).coerceIn(0f, headerHeightPx),
                    )
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(gridState) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - 4
        }.collect { shouldLoadMore ->
            if (shouldLoadMore) onLoadMore()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
    ) {
        val visibleHeaderHeightDp = with(density) {
            (headerHeightPx - scrollOffsetPx).coerceAtLeast(0f).toDp()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(visibleHeaderHeightDp)
                .background(WarmOffWhite)
                .clipToBounds(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(align = Alignment.Top, unbounded = true)
                    .graphicsLayer { translationY = -scrollOffsetPx }
                    .onGloballyPositioned { headerHeightPx = it.size.height.toFloat() },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ExerciseSearchBar(
                        searchQuery = state.searchQuery,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onSearch = onSearch,
                        onClearSearch = onClearSearch,
                        modifier = Modifier.weight(1f),
                    )
                    FilterButton(
                        activeFiltersCount = activeFiltersCount,
                        onClick = onShowFilters,
                    )
                }
                if (state.searchQuery.isBlank()) {
                    MuscleGroupFilterChips(
                        selected = state.selectedMuscleGroup,
                        onSelected = onFilterMuscleGroup,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    )
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            state = refreshState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = refreshState,
                    isRefreshing = state.isRefreshing,
                    containerColor = Color.White,
                    color = Coral,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
        ) {
            val showEmpty = state.exercises.isEmpty() &&
                !state.isLoading &&
                !state.isLoadingMore &&
                !state.isRefreshing
            if (showEmpty) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(span = { GridItemSpan(2) }) {
                        ExercisesEmptyState(
                            hasFilter = state.searchQuery.isNotBlank() ||
                                state.selectedMuscleGroup != null ||
                                state.selectedDifficulties.isNotEmpty() ||
                                state.requiresEquipment != null ||
                                state.sortByDifficulty != null ||
                                state.sortByCalories != null,
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = 12.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.exercises, key = { it.id }) { exercise ->
                        ExerciseCard(
                            exercise = exercise,
                            onClick = { onExerciseClick(exercise) },
                        )
                    }

                    if (state.isLoadingMore) {
                        item(span = { GridItemSpan(2) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color = AccentOrange,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }

                    item(span = { GridItemSpan(2) }) {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterButton(
    activeFiltersCount: Int,
    onClick: () -> Unit,
) {
    val isActive = activeFiltersCount > 0
    // Solid orange fill is the active indicator. No corner badge: avoids the
    // clipping artefacts produced by a small chip overlapping the circle's edge
    // and keeps the affordance readable at the 44dp touch target size.
    val background = if (isActive) AccentOrange else Color.White
    val borderColor = if (isActive) AccentOrange else Color(0xFFEDEDEF)
    val iconTint = if (isActive) Color.White else Color(0xFF4C4C53)

    Box(
        modifier = Modifier
            .size(44.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(background)
            .border(1.5.dp, borderColor, RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Tune,
            contentDescription = "Фильтры",
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
    }
}
