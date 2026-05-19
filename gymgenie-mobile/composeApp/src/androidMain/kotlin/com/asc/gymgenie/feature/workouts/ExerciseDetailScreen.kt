package com.asc.gymgenie.feature.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Speed
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.exercise.ExerciseDetailResponse
import com.asc.gymgenie.feature.workouts.components.muscleGroupEmoji
import com.asc.gymgenie.presentation.ExerciseDetailViewModel
import androidx.compose.foundation.layout.statusBarsPadding
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.SoftCard
import com.asc.gymgenie.ui.theme.WarmOffWhite
import org.koin.core.context.GlobalContext

@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    onBack: () -> Unit = {},
    onAddToWorkout: (ExerciseDetailResponse) -> Unit = {},
) {
    val koin = remember { GlobalContext.get() }
    val viewModel = remember { koin.get<ExerciseDetailViewModel>() }
    DisposableEffect(Unit) {
        onDispose { viewModel.onCleared() }
    }

    LaunchedEffect(exerciseId) {
        viewModel.load(exerciseId)
    }

    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        when {
            state.isLoading && state.exercise == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = AccentOrange)
                }
            }

            state.errorMessage != null && state.exercise == null -> {
                ErrorState(
                    message = state.errorMessage ?: "",
                    onRetry = { viewModel.retry(exerciseId) },
                    onBack = onBack,
                )
            }

            state.exercise != null -> {
                ExerciseDetailContent(
                    exercise = state.exercise!!,
                    onBack = onBack,
                    onAddToWorkout = { onAddToWorkout(state.exercise!!) },
                )
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
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
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Назад",
            color = MutedText,
            modifier = Modifier
                .clickable { onBack() }
                .padding(8.dp),
        )
    }
}

@Composable
private fun ExerciseDetailContent(
    exercise: ExerciseDetailResponse,
    onBack: () -> Unit,
    onAddToWorkout: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
        ) {
            item {
                HeroSection(exercise = exercise)
            }

            item {
                ContentSheet(exercise = exercise)
            }
        }

        // Floating back button — always on top of scroll
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 16.dp)
                .size(40.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = DeepInk,
                modifier = Modifier.size(20.dp),
            )
        }

        // Sticky bottom "Add to workout" button.
        BottomAddButton(
            onClick = onAddToWorkout,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun HeroSection(exercise: ExerciseDetailResponse) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(SoftCard),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = muscleGroupEmoji(exercise.muscleGroup),
            fontSize = 72.sp,
        )
    }
}

@Composable
private fun ContentSheet(exercise: ExerciseDetailResponse) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-20).dp)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(WarmOffWhite)
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        HeaderRow(exercise = exercise)

        val statItems = buildList {
            if (exercise.muscleGroup.isNotEmpty()) {
                add(StatItem(Icons.Outlined.FitnessCenter, muscleGroupRu(exercise.muscleGroup), muscleGroupStatColor(exercise.muscleGroup)))
            }
            exercise.secondsPer10Reps?.let {
                add(StatItem(Icons.Outlined.Schedule, "$it сек/10 повт.", AccentOrange))
            }
            if (exercise.difficultyLevel.isNotEmpty()) {
                val diffColor = when (exercise.difficultyLevel.uppercase()) {
                    "BEGINNER" -> Color(0xFF4CAF50)
                    "INTERMEDIATE" -> AccentOrange
                    "ADVANCED" -> Color(0xFFE53935)
                    else -> MutedText
                }
                add(StatItem(Icons.Outlined.Speed, difficultyLabel(exercise.difficultyLevel), diffColor))
            }
            exercise.caloriesBurned?.let {
                add(StatItem(Icons.Outlined.LocalFireDepartment, "$it ккал", Color(0xFFE53935)))
            }
        }
        if (statItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            StatsRow(stats = statItems)
        }

        exercise.description?.takeIf { it.isNotBlank() }?.let { description ->
            Spacer(modifier = Modifier.height(20.dp))
            SectionTitle("Описание")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 17.sp,
                color = DeepInk,
                lineHeight = 22.sp,
            )
        }

        if (exercise.instructions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            SectionTitle("Техника выполнения")
            Spacer(modifier = Modifier.height(10.dp))
            exercise.instructions.forEachIndexed { index, step ->
                InstructionRow(index = index + 1, text = step)
                if (index < exercise.instructions.lastIndex) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        exercise.techniqueTip?.takeIf { it.isNotBlank() }?.let { tip ->
            Spacer(modifier = Modifier.height(20.dp))
            SectionTitle("Совет по технике")
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentOrange.copy(alpha = 0.08f))
                    .padding(14.dp),
            ) {
                Text(
                    text = tip,
                    fontSize = 17.sp,
                    color = DeepInk,
                    lineHeight = 22.sp,
                )
            }
        }

        if (exercise.equipment.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            SectionTitle("Оборудование")
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                exercise.equipment.forEach { item ->
                    Text(
                        text = item,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepInk,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, DeepInk.copy(alpha = 0.25f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        }

        if (exercise.secondaryMuscleGroups.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            SectionTitle("Вспомогательные мышцы")
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                exercise.secondaryMuscleGroups.forEach { group ->
                    Text(
                        text = secondaryMuscleGroupRu(group),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepInk,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, DeepInk.copy(alpha = 0.25f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(exercise: ExerciseDetailResponse) {
    Text(
        text = exercise.nameRu,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = DeepInk,
        lineHeight = 32.sp,
        modifier = Modifier.fillMaxWidth(),
    )
}


private data class StatItem(val icon: ImageVector, val text: String, val color: Color)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsRow(stats: List<StatItem>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        stats.forEach { item ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(item.color.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.color,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = item.color,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 21.sp,
        fontWeight = FontWeight.Bold,
        color = DeepInk,
    )
}

@Composable
private fun InstructionRow(index: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(AccentOrange),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = index.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 17.sp,
            color = DeepInk,
            lineHeight = 22.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BottomAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        WarmOffWhite.copy(alpha = 0f),
                        WarmOffWhite,
                    ),
                )
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentOrange,
                contentColor = Color.White,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = "Добавить в тренировку",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// -- Helpers --

internal fun muscleGroupRu(group: String): String = when (group.uppercase()) {
    "CHEST" -> "Грудь"
    "BACK" -> "Спина"
    "SHOULDERS" -> "Плечи"
    "BICEPS" -> "Бицепс"
    "TRICEPS" -> "Трицепс"
    "FOREARMS" -> "Предплечья"
    "ABS" -> "Пресс"
    "QUADRICEPS" -> "Квадрицепс"
    "HAMSTRINGS" -> "Бицепс бедра"
    "CALVES" -> "Икры"
    "GLUTES" -> "Ягодицы"
    "CARDIO" -> "Кардио"
    "FULL_BODY" -> "Всё тело"
    else -> group
}

internal fun secondaryMuscleGroupRu(group: String): String = muscleGroupRu(group)

internal fun categoryRu(category: String): String = when (category.uppercase()) {
    "STRENGTH" -> "Сила"
    "CARDIO" -> "Кардио"
    "FLEXIBILITY" -> "Гибкость"
    "BALANCE" -> "Баланс"
    "PLYOMETRIC" -> "Плиометрика"
    "FUNCTIONAL" -> "Функционал"
    else -> category
}

internal fun difficultyLabel(level: String): String = when (level.uppercase()) {
    "BEGINNER" -> "Легко"
    "INTERMEDIATE" -> "Средне"
    "ADVANCED" -> "Сложно"
    else -> level
}

private fun muscleGroupStatColor(group: String): Color = when (group.uppercase()) {
    "CHEST" -> Color(0xFFE94A2C)
    "BACK" -> Color(0xFF3B5BDB)
    "SHOULDERS" -> Color(0xFFE89B12)
    "BICEPS", "TRICEPS", "FOREARMS" -> Color(0xFFB8860B)
    "ABS" -> Color(0xFF6741D9)
    "QUADRICEPS", "HAMSTRINGS", "GLUTES", "CALVES" -> Color(0xFF2F9E44)
    "FULL_BODY" -> Color(0xFFFF5A3C)
    "CARDIO" -> Color(0xFFC2255C)
    else -> Color(0xFF76726A)
}
