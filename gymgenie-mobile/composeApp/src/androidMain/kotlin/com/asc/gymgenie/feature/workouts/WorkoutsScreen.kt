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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.gymgenie.exercise.ExerciseShortResponse
import com.asc.gymgenie.ui.theme.OnBackground
import com.asc.gymgenie.ui.theme.OnSurfaceVariant
import com.asc.gymgenie.ui.theme.Primary
import com.asc.gymgenie.workout.WorkoutPlanShortResponse

@Composable
fun WorkoutsScreen(
    viewModel: WorkoutsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

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
        TabRow(
            selectedTabIndex = if (state.selectedTab == WorkoutsTab.WORKOUTS) 0 else 1,
            containerColor = Color.Transparent,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(
                        tabPositions[if (state.selectedTab == WorkoutsTab.WORKOUTS) 0 else 1],
                    ),
                    color = Primary,
                )
            },
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            Tab(
                selected = state.selectedTab == WorkoutsTab.WORKOUTS,
                onClick = { viewModel.selectTab(WorkoutsTab.WORKOUTS) },
                text = {
                    Text(
                        text = "Тренировки",
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.selectedTab == WorkoutsTab.WORKOUTS) Primary else OnSurfaceVariant,
                    )
                },
            )
            Tab(
                selected = state.selectedTab == WorkoutsTab.EXERCISES,
                onClick = { viewModel.selectTab(WorkoutsTab.EXERCISES) },
                text = {
                    Text(
                        text = "Упражнения",
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.selectedTab == WorkoutsTab.EXERCISES) Primary else OnSurfaceVariant,
                    )
                },
            )
        }

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
                    WorkoutsTab.WORKOUTS -> WorkoutsTabContent(plans = state.workoutPlans)
                    WorkoutsTab.EXERCISES -> ExercisesTabContent(
                        exercises = state.exercises,
                        searchQuery = state.searchQuery,
                        isLoadingMore = state.isLoadingMore,
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
private fun WorkoutsTabContent(plans: List<WorkoutPlanShortResponse>) {
    if (plans.isEmpty()) {
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
        // Featured plan (full width)
        plans.firstOrNull()?.let { featured ->
            item(span = { GridItemSpan(2) }) {
                FeaturedPlanCard(plan = featured)
            }
        }

        // Other plans
        val otherPlans = plans.drop(1)
        items(otherPlans, key = { it.id }) { plan ->
            SmallPlanCard(plan = plan)
        }
    }
}

@Composable
private fun FeaturedPlanCard(plan: WorkoutPlanShortResponse) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Primary, Primary.copy(alpha = 0.7f)),
                ),
            )
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Today plan",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.3f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )

            Text(
                text = "09:00",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = plan.name,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )

        plan.description?.let { desc ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${plan.daysCount} дн.",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {},
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text("Детали", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Primary,
                ),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text("Начать", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SmallPlanCard(plan: WorkoutPlanShortResponse) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(12.dp),
    ) {
        Text(
            text = "План",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Primary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Primary.copy(alpha = 0.1f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = plan.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = OnBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${plan.daysCount} дн.",
            fontSize = 12.sp,
            color = OnSurfaceVariant,
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Детали",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary,
            )

            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text("Начать", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ExercisesTabContent(
    exercises: List<ExerciseShortResponse>,
    searchQuery: String,
    isLoadingMore: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val gridState = rememberLazyGridState()

    // Detect scroll to end
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
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Поиск упражнений...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Поиск",
                    tint = OnSurfaceVariant,
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Очистить",
                            tint = OnSurfaceVariant,
                        )
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Primary,
                unfocusedBorderColor = Color.Transparent,
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        )

        if (exercises.isEmpty() && !isLoadingMore) {
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
                items(exercises, key = { it.id }) { exercise ->
                    ExerciseCard(exercise = exercise)
                }

                if (isLoadingMore) {
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

@Composable
private fun ExerciseCard(exercise: ExerciseShortResponse) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(10.dp),
    ) {
        // Image placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Gray.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "\uD83C\uDFCB", fontSize = 28.sp)

            if (exercise.muscleGroup.isNotEmpty()) {
                Text(
                    text = exercise.muscleGroup,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Primary)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = exercise.nameRu,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = OnBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        exercise.durationMinutes?.let { duration ->
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "\u23F0", fontSize = 10.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$duration мин",
                    fontSize = 11.sp,
                    color = OnSurfaceVariant,
                )
            }
        }

        if (exercise.difficultyLevel.isNotEmpty()) {
            Text(
                text = exercise.difficultyLevel,
                fontSize = 10.sp,
                color = OnSurfaceVariant,
            )
        }
    }
}
