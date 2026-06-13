package com.asc.gymgenie.feature.create_workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.exercise.ExerciseShortResponse
import com.asc.gymgenie.feature.workouts.components.ExerciseCard
import com.asc.gymgenie.feature.workout_session.components.ExerciseDetailSheetContent
import com.asc.gymgenie.presentation.WorkoutsViewModel
import com.asc.gymgenie.ui.components.GymGenieToolbar
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.WarmOffWhite
import org.koin.core.context.GlobalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerScreen(
    muscleGroupKey: String,
    muscleGroupNameRu: String,
    onBack: () -> Unit,
    onExerciseSelected: (ExerciseShortResponse) -> Unit,
) {
    val koin = remember { GlobalContext.get() }
    val listViewModel = remember { koin.get<WorkoutsViewModel>() }
    DisposableEffect(Unit) { onDispose { listViewModel.onCleared() } }

    LaunchedEffect(muscleGroupKey) {
        listViewModel.filterByMuscleGroup(muscleGroupKey)
    }

    val state by listViewModel.state.collectAsState()

    var detailExercise by remember { mutableStateOf<ExerciseShortResponse?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        GymGenieToolbar(
            title = muscleGroupNameRu,
            showBackNavigation = true,
            onBackClick = onBack,
        )

        WorkoutFlowStepHeader(currentStep = 2)

        when {
            state.isLoading && state.exercises.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentOrange)
                }
            }

            state.errorMessage != null && state.exercises.isEmpty() -> {
                ErrorBlock(
                    message = state.errorMessage ?: "",
                    onRetry = { listViewModel.loadExercises(reset = true) },
                )
            }

            state.exercises.isEmpty() -> {
                EmptyBlock()
            }

            else -> {
                ExercisesGrid(
                    exercises = state.exercises,
                    isLoadingMore = state.isLoadingMore,
                    hasMore = state.hasMoreExercises,
                    onExerciseSelected = onExerciseSelected,
                    onOpenDetail = { detailExercise = it },
                    onLoadMore = listViewModel::loadMoreExercises,
                )
            }
        }
    }

    if (detailExercise != null) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { detailExercise = null },
            containerColor = WarmOffWhite,
        ) {
            ExerciseDetailSheetContent(
                exerciseId = detailExercise!!.id,
            )

            Button(
                onClick = {
                    val exercise = detailExercise
                    detailExercise = null
                    if (exercise != null) onExerciseSelected(exercise)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
            ) {
                Text(
                    "Добавить в тренировку",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ExercisesGrid(
    exercises: List<ExerciseShortResponse>,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onExerciseSelected: (ExerciseShortResponse) -> Unit,
    onOpenDetail: (ExerciseShortResponse) -> Unit,
    onLoadMore: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val bottomSafeArea = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LaunchedEffect(gridState, hasMore) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= total - 4
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && hasMore) onLoadMore()
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),

        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = bottomSafeArea + 16.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(exercises, key = { it.id }) { exercise ->

            ExerciseCard(
                exercise = exercise,
                onClick = { onExerciseSelected(exercise) },
                onLongClick = { onOpenDetail(exercise) },
                onInfoClick = { onOpenDetail(exercise) },
            )
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
                        color = AccentOrange,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Warning,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = AccentOrange,
        )
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
private fun EmptyBlock() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Inbox,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = MutedText,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Упражнений пока нет",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepInk,
        )
    }
}
