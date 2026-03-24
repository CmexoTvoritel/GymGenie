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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.feature.workouts.components.ExerciseCard
import com.asc.gymgenie.feature.workouts.components.ExerciseSearchBar
import com.asc.gymgenie.feature.workouts.components.FeaturedWorkoutCard
import com.asc.gymgenie.feature.workouts.components.WorkoutCardSmall
import com.asc.gymgenie.feature.workouts.components.WorkoutTabSelector
import com.asc.gymgenie.presentation.WorkoutsTab
import com.asc.gymgenie.presentation.WorkoutsUiState
import com.asc.gymgenie.presentation.WorkoutsViewModel
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.ui.theme.OnBackground
import com.asc.gymgenie.ui.theme.OnSurfaceVariant
import com.asc.gymgenie.ui.theme.Primary

@Composable
fun WorkoutsScreen(tokenStorage: TokenStorage) {
    val viewModel = remember {
        WorkoutsViewModel(tokenStorage = tokenStorage)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.onCleared() }
    }

    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadWorkoutPlans()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Мои тренировки",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OnBackground,
            )

            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary.copy(alpha = 0.1f),
                    contentColor = Primary,
                ),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Добавить",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Tab row
        WorkoutTabSelector(
            selectedTab = state.selectedTab,
            onTabSelected = viewModel::selectTab,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        // Content
        when {
            state.isLoading && contentIsEmpty(state) -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }

            state.errorMessage != null && contentIsEmpty(state) -> {
                ErrorContent(
                    message = state.errorMessage ?: "",
                    onRetry = viewModel::retry,
                )
            }

            else -> {
                when (state.selectedTab) {
                    WorkoutsTab.WORKOUTS -> WorkoutsTabContent(state = state)
                    WorkoutsTab.EXERCISES -> ExercisesTabContent(
                        state = state,
                        onSearchQueryChanged = viewModel::onSearchQueryChanged,
                        onSearch = viewModel::searchExercises,
                        onClearSearch = {
                            viewModel.onSearchQueryChanged("")
                            viewModel.loadExercises(reset = true)
                        },
                        onLoadMore = viewModel::loadMoreExercises,
                    )
                }
            }
        }
    }
}

private fun contentIsEmpty(state: WorkoutsUiState): Boolean {
    return when (state.selectedTab) {
        WorkoutsTab.WORKOUTS -> state.workoutPlans.isEmpty()
        WorkoutsTab.EXERCISES -> state.exercises.isEmpty()
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
        Text(text = "\u26A0\uFE0F", fontSize = 36.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(50),
        ) {
            Text("Повторить")
        }
    }
}

@Composable
private fun WorkoutsTabContent(state: WorkoutsUiState) {
    if (state.workoutPlans.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "\uD83D\uDCE6", fontSize = 36.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Нет тренировочных планов",
                    fontSize = 15.sp,
                    color = OnSurfaceVariant,
                )
            }
        }
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
                FeaturedWorkoutCard(plan = featured)
            }
        }

        val otherPlans = state.workoutPlans.drop(1)
        items(otherPlans, key = { it.id }) { plan ->
            WorkoutCardSmall(plan = plan)
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

        if (state.exercises.isEmpty() && !state.isLoadingMore) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "\uD83D\uDCE6", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Упражнения не найдены",
                        fontSize = 15.sp,
                        color = OnSurfaceVariant,
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.exercises, key = { it.id }) { exercise ->
                    ExerciseCard(exercise = exercise)
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
                                color = Primary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
