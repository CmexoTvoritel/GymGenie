package com.asc.gymgenie.feature.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.gymgenie.ui.theme.AccentGreen
import com.asc.gymgenie.ui.theme.OnBackground
import com.asc.gymgenie.ui.theme.OnSurfaceVariant
import com.asc.gymgenie.ui.theme.Primary
import com.asc.gymgenie.workout.WorkoutPlanShortResponse

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (state.userProfile == null && !state.isLoading) {
            viewModel.loadData()
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
                    Text(
                        text = "\u26A0\uFE0F",
                        fontSize = 40.sp,
                    )
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
                    // Header
                    item {
                        HeaderSection(
                            username = state.userProfile?.username ?: "",
                        )
                    }

                    // Daily challenge
                    item { DailyChallengeCard() }

                    // Workout plan section
                    item {
                        WorkoutPlanSection(plans = state.activeWorkoutPlans)
                    }

                    // Activities
                    item { ActivitiesSection() }

                    // Meal plan
                    item { MealPlanSection() }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(username: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "Привет, $username!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OnBackground,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "\uD83D\uDD25", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "5 дней подряд",
                    fontSize = 13.sp,
                    color = OnSurfaceVariant,
                )
            }
        }

        IconButton(
            onClick = {},
            modifier = Modifier
                .size(40.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Уведомления",
                tint = OnBackground,
            )
        }
    }
}

@Composable
private fun DailyChallengeCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Primary, Primary.copy(alpha = 0.7f)),
                ),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Ежедневный челлендж",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Выполните задание дня и получите бонус",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Primary,
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "Начать \u2192",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
            }

            Text(
                text = "\uD83D\uDD25",
                fontSize = 48.sp,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
}

@Composable
private fun WorkoutPlanSection(plans: List<WorkoutPlanShortResponse>) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Тренировочный план",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = OnBackground,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (plans.isEmpty()) {
            // Empty state
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
        } else {
            plans.forEach { plan ->
                WorkoutPlanCard(plan = plan)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Перейти к тренировкам",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "\u2192",
                fontSize = 12.sp,
                color = Primary,
            )
        }
    }
}

@Composable
private fun WorkoutPlanCard(plan: WorkoutPlanShortResponse) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp),
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
                    .background(Primary)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )

            Text(
                text = "09:00",
                fontSize = 13.sp,
                color = OnSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = plan.name,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = OnBackground,
        )

        plan.description?.let { description ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = OnSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {},
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Детали",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary,
                )
            }

            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Начать",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ActivitiesSection() {
    Column {
        Text(
            text = "Активности",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OnBackground,
        )

        Spacer(modifier = Modifier.height(12.dp))

        val activities = listOf(
            Triple("Бег", "\uD83C\uDFC3", Color(0xFFFF9800)),
            Triple("Йога", "\uD83E\uDDD8", Color(0xFF9C27B0)),
            Triple("Силовая", "\uD83C\uDFCB", Primary),
            Triple("Кардио", "\u2764\uFE0F", Color(0xFFF44336)),
            Triple("Растяжка", "\uD83E\uDD38", AccentGreen),
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(activities) { (title, emoji, color) ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(color.copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = emoji, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnBackground,
                    )
                }
            }
        }
    }
}

@Composable
private fun MealPlanSection() {
    Column {
        Text(
            text = "План питания",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OnBackground,
        )

        Spacer(modifier = Modifier.height(12.dp))

        val meals = listOf(
            Triple("Завтрак", "\uD83C\uDF05", Color(0xFFFF9800)),
            Triple("Обед", "\u2600\uFE0F", Primary),
            Triple("Ужин", "\uD83C\uDF19", Color(0xFF9C27B0)),
            Triple("Перекус", "\uD83C\uDF3F", AccentGreen),
        )

        meals.forEach { (title, emoji, color) ->
            MealCard(title = title, emoji = emoji, color = color)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MealCard(title: String, emoji: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnBackground,
            )
            Text(
                text = "Нажмите для деталей",
                fontSize = 12.sp,
                color = OnSurfaceVariant,
            )
        }

        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary.copy(alpha = 0.1f),
                contentColor = Primary,
            ),
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(
                text = "Детали",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
