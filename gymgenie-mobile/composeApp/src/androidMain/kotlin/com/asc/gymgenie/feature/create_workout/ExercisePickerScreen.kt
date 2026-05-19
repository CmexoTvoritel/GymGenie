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
import androidx.compose.material3.CircularProgressIndicator
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
import com.asc.gymgenie.presentation.ExerciseDetailViewModel
import com.asc.gymgenie.presentation.WorkoutsViewModel
import com.asc.gymgenie.ui.components.GymGenieToolbar
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.WarmOffWhite
import org.koin.core.context.GlobalContext

/**
 * Step 2 of the create-workout flow: picks the exercise.
 *
 * The screen reuses the shared [WorkoutsViewModel] strictly as a paged
 * data source — all UI state (search, filters) are disabled because this step
 * is scoped to a single pre-selected muscle group.
 *
 * Tapping the card navigates forward; tapping the small info button opens a
 * [ModalBottomSheet] with the exercise detail, without leaving this step.
 */
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

    var detailExerciseId by remember { mutableStateOf<String?>(null) }
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
                    onOpenDetail = { detailExerciseId = it.id },
                    onLoadMore = listViewModel::loadMoreExercises,
                )
            }
        }
    }

    if (detailExerciseId != null) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { detailExerciseId = null },
            containerColor = WarmOffWhite,
        ) {
            ExerciseDetailBottomSheetContent(
                exerciseId = detailExerciseId!!,
            )
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
        // Bottom inset adds the gesture/navigation safe area on top of a 16dp
        // floor so the last row is always reachable, independent of the device.
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
            // ExerciseCard now hosts the info badge inside the image area and
            // owns the long-press gesture, so the picker no longer needs to
            // overlay a separate Box for the badge.
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
private fun ExerciseDetailBottomSheetContent(
    exerciseId: String,
) {
    val koin = remember { GlobalContext.get() }
    val viewModel = remember(exerciseId) { koin.get<ExerciseDetailViewModel>() }
    DisposableEffect(exerciseId) { onDispose { viewModel.onCleared() } }

    LaunchedEffect(exerciseId) { viewModel.load(exerciseId) }

    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 24.dp),
    ) {
        when {
            state.isLoading && state.exercise == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = AccentOrange)
                }
            }

            state.errorMessage != null && state.exercise == null -> {
                Text(
                    text = state.errorMessage ?: "Ошибка загрузки",
                    color = MutedText,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 32.dp),
                    textAlign = TextAlign.Center,
                )
            }

            state.exercise != null -> {
                val exercise = state.exercise!!
                Text(
                    text = exercise.nameRu,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepInk,
                )
                if (exercise.nameEn.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = exercise.nameEn,
                        fontSize = 14.sp,
                        color = MutedText,
                    )
                }

                exercise.description?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(14.dp))
                    SectionTitle("Описание")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        color = DeepInk,
                        lineHeight = 20.sp,
                    )
                }

                if (exercise.equipment.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    SectionTitle("Оборудование")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = exercise.equipment.joinToString(", "),
                        fontSize = 14.sp,
                        color = DeepInk,
                    )
                }

                if (exercise.instructions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    SectionTitle("Техника выполнения")
                    Spacer(modifier = Modifier.height(8.dp))
                    exercise.instructions.forEachIndexed { index, step ->
                        Text(
                            text = "${index + 1}. $step",
                            fontSize = 14.sp,
                            color = DeepInk,
                            lineHeight = 20.sp,
                        )
                        if (index < exercise.instructions.lastIndex) {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = DeepInk,
    )
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
private fun EmptyBlock() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "📦", fontSize = 44.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Упражнений пока нет",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepInk,
        )
    }
}
