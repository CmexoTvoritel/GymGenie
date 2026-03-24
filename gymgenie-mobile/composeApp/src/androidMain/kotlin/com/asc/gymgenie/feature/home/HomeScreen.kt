package com.asc.gymgenie.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.feature.home.components.ActivityTimeline
import com.asc.gymgenie.feature.home.components.DailyChallengeCard
import com.asc.gymgenie.feature.home.components.HomeHeader
import com.asc.gymgenie.feature.home.components.MealCard
import com.asc.gymgenie.feature.home.components.SectionHeader
import com.asc.gymgenie.feature.home.components.WorkoutPlanCard
import com.asc.gymgenie.presentation.HomeViewModel
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.ui.theme.AccentGreen
import com.asc.gymgenie.ui.theme.OnSurfaceVariant
import com.asc.gymgenie.ui.theme.Primary
import com.asc.gymgenie.workout.WorkoutPlanShortResponse

@Composable
fun HomeScreen(tokenStorage: TokenStorage) {
    val viewModel = remember {
        HomeViewModel(tokenStorage = tokenStorage)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.onCleared() }
    }

    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        if (state.userProfile == null && !state.isLoading) {
            viewModel.load()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            state.isLoading && state.userProfile == null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = Primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Загрузка...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                    )
                }
            }

            state.errorMessage != null && state.userProfile == null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = "\u26A0\uFE0F", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 40.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = viewModel::retry,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text("Повторить")
                    }
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    item {
                        HomeHeader(
                            username = state.username,
                            streakDays = state.streakDays,
                        )
                    }

                    item { DailyChallengeCard() }

                    item {
                        SectionHeader(title = "Тренировочный план")
                    }

                    if (state.activeWorkoutPlans.isEmpty()) {
                        item { EmptyPlanCard() }
                    } else {
                        items(state.activeWorkoutPlans, key = { it.id }) { plan ->
                            WorkoutPlanCard(plan = plan)
                        }
                    }

                    item {
                        SectionHeader(
                            title = "Перейти к тренировкам",
                            actionTitle = "\u2192",
                        )
                    }

                    item {
                        SectionHeader(title = "Активности")
                    }

                    item { ActivityTimeline() }

                    item {
                        SectionHeader(title = "План питания")
                    }

                    item { MealPlanList() }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun EmptyPlanCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "\uD83D\uDCC5", fontSize = 28.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Нет активных планов",
            fontSize = 14.sp,
            color = OnSurfaceVariant,
        )
    }
}

@Composable
private fun MealPlanList() {
    val meals = listOf(
        Triple("Завтрак", "\uD83C\uDF05", Color(0xFFFF9800)),
        Triple("Обед", "\u2600\uFE0F", Primary),
        Triple("Ужин", "\uD83C\uDF19", Color(0xFF9C27B0)),
        Triple("Перекус", "\uD83C\uDF3F", AccentGreen),
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        meals.forEach { (title, emoji, color) ->
            MealCard(title = title, emoji = emoji, color = color)
        }
    }
}
