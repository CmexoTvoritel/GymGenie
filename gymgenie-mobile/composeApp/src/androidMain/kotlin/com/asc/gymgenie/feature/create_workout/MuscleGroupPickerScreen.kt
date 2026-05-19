package com.asc.gymgenie.feature.create_workout

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.R
import com.asc.gymgenie.exercise.MuscleGroupInfo
import com.asc.gymgenie.presentation.CreateWorkoutUiState
import com.asc.gymgenie.ui.components.GymGenieToolbar
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.WarmOffWhite

/**
 * Step 1 of the create-workout flow: picks the target muscle group.
 *
 * The list is server-driven via [CreateWorkoutUiState.muscleGroups]; the screen
 * itself is stateless and only forwards selections upwards.
 */
@Composable
fun MuscleGroupPickerScreen(
    state: CreateWorkoutUiState,
    onBack: () -> Unit,
    onGroupSelected: (MuscleGroupInfo) -> Unit,
    onRetry: () -> Unit,
) {
    val bottomSafeArea = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        GymGenieToolbar(
            title = "Выбери группу мышц",
            showBackNavigation = true,
            onBackClick = onBack,
        )

        WorkoutFlowStepHeader(currentStep = 1)

        when {
            state.isMuscleGroupsLoading && state.muscleGroups.isEmpty() -> {
                LoadingBlock()
            }

            state.errorMessage != null && state.muscleGroups.isEmpty() -> {
                ErrorBlock(message = state.errorMessage ?: "", onRetry = onRetry)
            }

            state.muscleGroups.isEmpty() -> {
                EmptyBlock()
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    // Top inset is small (the step header already adds breathing room).
                    // Bottom inset combines the navigation-bar safe area with a 16dp
                    // floor so the last row never sits flush against the gesture handle.
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 12.dp,
                        bottom = bottomSafeArea + 16.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.muscleGroups, key = { it.key }) { group ->
                        MuscleGroupTile(
                            info = group,
                            onClick = { onGroupSelected(group) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MuscleGroupTile(info: MuscleGroupInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
//            .height(130.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = muscleGroupDrawable(info.key)),
            contentDescription = info.nameRu,
            modifier = Modifier.fillMaxWidth().aspectRatio(ratio = 1f),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = info.nameRu,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepInk,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LoadingBlock() {
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
            text = "Группы мышц недоступны",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepInk,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Попробуйте позже",
            fontSize = 13.sp,
            color = MutedText,
        )
    }
}
