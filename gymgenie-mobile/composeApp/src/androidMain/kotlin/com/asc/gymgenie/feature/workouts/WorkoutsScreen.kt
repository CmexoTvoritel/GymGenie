package com.asc.gymgenie.feature.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.exercise.ExerciseShortResponse
import com.asc.gymgenie.feature.workouts.components.ExerciseCard
import com.asc.gymgenie.feature.workouts.components.ExerciseSearchBar
import com.asc.gymgenie.feature.workouts.components.FeaturedWorkoutCard
import com.asc.gymgenie.feature.workouts.components.MuscleGroupFilterChips
import com.asc.gymgenie.feature.workouts.components.WorkoutCardSmall
import com.asc.gymgenie.feature.workouts.components.WorkoutTabSelector
import com.asc.gymgenie.auth.AuthApi
import com.asc.gymgenie.common.createAuthenticatedClient
import com.asc.gymgenie.exercise.ExerciseApi
import com.asc.gymgenie.presentation.WorkoutsTab
import com.asc.gymgenie.presentation.WorkoutsUiState
import com.asc.gymgenie.presentation.WorkoutsViewModel
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.workout.WorkoutApi
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.WarmOffWhite

@Composable
fun WorkoutsScreen(
    tokenStorage: TokenStorage,
    onLogout: () -> Unit = {},
    onOpenExercise: (ExerciseShortResponse) -> Unit = {},
    onCreateWorkout: () -> Unit = {},
    onStartPlan: (planId: String, planName: String) -> Unit = { _, _ -> },
    reloadKey: Int = 0,
) {
    val viewModel = remember {
        val authApi = AuthApi()
        val client = createAuthenticatedClient(tokenStorage, authApi)
        WorkoutsViewModel(
            workoutApi = WorkoutApi(client),
            exerciseApi = ExerciseApi(client),
            tokenStorage = tokenStorage,
            onLogout = onLogout,
        )
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.onCleared() }
    }

    val state by viewModel.state.collectAsState()

    // Reload the plan list whenever `reloadKey` changes. This is how the
    // create-workout flow tells the screen that a new plan was saved.
    LaunchedEffect(reloadKey) {
        viewModel.loadWorkoutPlans()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HeaderSection()

            Spacer(modifier = Modifier.height(4.dp))

            WorkoutTabSelector(
                selectedTab = state.selectedTab,
                onTabSelected = viewModel::selectTab,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            when {
                state.isLoading && contentIsEmpty(state) -> {
                    LoadingContent()
                }

                state.errorMessage != null && contentIsEmpty(state) && !isExpectedEmptyState(state) -> {
                    ErrorContent(
                        message = state.errorMessage ?: "",
                        onRetry = viewModel::retry,
                    )
                }

                else -> {
                    when (state.selectedTab) {
                        WorkoutsTab.WORKOUTS -> WorkoutsTabContent(
                            state = state,
                            onStartPlan = onStartPlan,
                            onCreateWorkout = onCreateWorkout,
                        )
                        WorkoutsTab.EXERCISES -> ExercisesTabContent(
                            state = state,
                            onSearchQueryChanged = viewModel::onSearchQueryChanged,
                            onSearch = viewModel::searchExercises,
                            onClearSearch = {
                                viewModel.onSearchQueryChanged("")
                                viewModel.loadExercises(reset = true)
                            },
                            onLoadMore = viewModel::loadMoreExercises,
                            onFilterMuscleGroup = viewModel::filterByMuscleGroup,
                            onExerciseClick = onOpenExercise,
                        )
                    }
                }
            }
        }

        CreateWorkoutFab(
            onClick = onCreateWorkout,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
        )
    }
}

@Composable
private fun HeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = "Тренировки",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DeepInk,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Твои планы и каталог упражнений",
            fontSize = 13.sp,
            color = MutedText,
        )
    }
}

@Composable
private fun CreateWorkoutFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(elevation = 8.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(AccentOrange)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Создать план",
            tint = Color.White,
            modifier = Modifier.size(28.dp),
        )
    }
}

private fun contentIsEmpty(state: WorkoutsUiState): Boolean {
    return when (state.selectedTab) {
        WorkoutsTab.WORKOUTS -> state.workoutPlans.isEmpty()
        WorkoutsTab.EXERCISES -> state.exercises.isEmpty()
    }
}

/**
 * When the server successfully returned an empty page we should render the empty state,
 * not the generic error view. This avoids the bug where an empty plans list was shown as
 * an error simply because a prior errorMessage lingered in state.
 */
private fun isExpectedEmptyState(state: WorkoutsUiState): Boolean {
    return when (state.selectedTab) {
        WorkoutsTab.WORKOUTS -> state.workoutPlansLoaded && state.workoutPlans.isEmpty()
        WorkoutsTab.EXERCISES -> state.exercisesLoaded && state.exercises.isEmpty() && state.errorMessage == null
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = AccentOrange)
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "⚠️", fontSize = 36.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = MutedText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
            shape = RoundedCornerShape(50),
        ) {
            Text("Повторить")
        }
    }
}

@Composable
private fun WorkoutsTabContent(
    state: WorkoutsUiState,
    onStartPlan: (planId: String, planName: String) -> Unit,
    onCreateWorkout: () -> Unit,
) {
    if (state.workoutPlans.isEmpty()) {
        WorkoutsEmptyState(onCreateWorkout = onCreateWorkout)
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.workoutPlans.firstOrNull()?.let { featured ->
            item(span = { GridItemSpan(2) }) {
                FeaturedWorkoutCard(
                    plan = featured,
                    onStart = { onStartPlan(featured.id, featured.name) },
                )
            }
        }

        val otherPlans = state.workoutPlans.drop(1)
        items(otherPlans, key = { it.id }) { plan ->
            WorkoutCardSmall(
                plan = plan,
                onStart = { onStartPlan(plan.id, plan.name) },
            )
        }

        // Reserve space at the bottom so the FAB does not cover last cards.
        item(span = { GridItemSpan(2) }) {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun WorkoutsEmptyState(onCreateWorkout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "🏋", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Нет тренировочных планов",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepInk,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Создайте свой первый план тренировок",
            fontSize = 13.sp,
            color = MutedText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onCreateWorkout,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentOrange,
                contentColor = Color.White,
            ),
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Создать первый план",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ExercisesTabContent(
    state: WorkoutsUiState,
    onSearchQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onLoadMore: () -> Unit,
    onFilterMuscleGroup: (String?) -> Unit,
    onExerciseClick: (ExerciseShortResponse) -> Unit,
) {
    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - 4
        }.collect { shouldLoadMore ->
            if (shouldLoadMore) {
                onLoadMore()
            }
        }
    }

    Column {
        ExerciseSearchBar(
            searchQuery = state.searchQuery,
            onSearchQueryChanged = onSearchQueryChanged,
            onSearch = onSearch,
            onClearSearch = onClearSearch,
        )

        // Show muscle group filter chips only when not searching.
        if (state.searchQuery.isBlank()) {
            MuscleGroupFilterChips(
                selected = state.selectedMuscleGroup,
                onSelected = onFilterMuscleGroup,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        val showEmpty = state.exercises.isEmpty() && !state.isLoading && !state.isLoadingMore
        if (showEmpty) {
            ExercisesEmptyState(
                hasFilter = state.searchQuery.isNotBlank() || state.selectedMuscleGroup != null,
            )
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
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

@Composable
private fun ExercisesEmptyState(hasFilter: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "📦", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasFilter) "Ничего не найдено" else "Каталог пуст",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepInk,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (hasFilter) "Попробуйте изменить запрос или фильтр" else "Упражнения скоро появятся",
            fontSize = 13.sp,
            color = MutedText,
            textAlign = TextAlign.Center,
        )
    }
}
