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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.auth.AuthApi
import com.asc.gymgenie.common.createAuthenticatedClient
import com.asc.gymgenie.exercise.ExerciseApi
import com.asc.gymgenie.exercise.ExerciseDetailResponse
import com.asc.gymgenie.feature.workouts.components.muscleGroupEmoji
import com.asc.gymgenie.presentation.ExerciseDetailViewModel
import com.asc.gymgenie.storage.TokenStorage
import androidx.compose.foundation.layout.statusBarsPadding
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.SoftCard
import com.asc.gymgenie.ui.theme.WarmOffWhite

@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    tokenStorage: TokenStorage,
    onBack: () -> Unit = {},
    onAddToWorkout: (ExerciseDetailResponse) -> Unit = {},
) {
    val viewModel = remember {
        val authApi = AuthApi()
        val client = createAuthenticatedClient(tokenStorage, authApi)
        ExerciseDetailViewModel(exerciseApi = ExerciseApi(client))
    }
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
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                HeroSection(
                    exercise = exercise,
                    onBack = onBack,
                )
            }

            item {
                ContentSheet(exercise = exercise)
            }
        }

        // Sticky bottom "Add to workout" button.
        BottomAddButton(
            onClick = onAddToWorkout,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun HeroSection(
    exercise: ExerciseDetailResponse,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(SoftCard),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = muscleGroupEmoji(exercise.muscleGroup),
            fontSize = 72.sp,
        )

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

        Spacer(modifier = Modifier.height(12.dp))

        TagsRow(exercise = exercise)

        val stats = buildList {
            exercise.durationMinutes?.let { add("⏱ $it мин" to AccentOrange) }
            if (exercise.difficultyLevel.isNotEmpty()) {
                add("📊 ${difficultyLabel(exercise.difficultyLevel)}" to Color(0xFF4CAF50))
            }
            exercise.caloriesBurned?.let { add("🔥 $it ккал" to Color(0xFFE53935)) }
        }
        if (stats.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            StatsRow(stats = stats)
        }

        exercise.description?.takeIf { it.isNotBlank() }?.let { description ->
            Spacer(modifier = Modifier.height(20.dp))
            SectionTitle("Описание")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = DeepInk,
                lineHeight = 20.sp,
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
                    fontSize = 14.sp,
                    color = DeepInk,
                    lineHeight = 20.sp,
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
                        fontSize = 12.sp,
                        color = DeepInk,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, DeepInk.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
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
                        fontSize = 12.sp,
                        color = DeepInk,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(SoftCard)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(exercise: ExerciseDetailResponse) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.nameRu,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk,
                lineHeight = 28.sp,
            )
            if (exercise.nameEn.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = exercise.nameEn,
                    fontSize = 14.sp,
                    color = MutedText,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            val categoryText = buildString {
                append(muscleGroupRu(exercise.muscleGroup))
                if (exercise.category.isNotEmpty()) {
                    append(" • ")
                    append(categoryRu(exercise.category))
                }
            }
            Text(
                text = categoryText,
                fontSize = 13.sp,
                color = MutedText,
            )
        }

        exercise.rating?.let { rating ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⭐", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatRating(rating),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepInk,
                )
            }
        }
    }
}

@Composable
private fun TagsRow(exercise: ExerciseDetailResponse) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (exercise.muscleGroup.isNotEmpty()) {
            Text(
                text = muscleGroupRu(exercise.muscleGroup),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AccentOrange)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        exercise.equipment.firstOrNull()?.let { equipment ->
            Text(
                text = equipment,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepInk,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, DeepInk.copy(alpha = 0.25f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        if (exercise.difficultyLevel.isNotEmpty()) {
            val (label, color) = when (exercise.difficultyLevel.uppercase()) {
                "BEGINNER" -> "Легко" to Color(0xFF4CAF50)
                "INTERMEDIATE" -> "Средне" to AccentOrange
                "ADVANCED" -> "Сложно" to Color(0xFFE53935)
                else -> exercise.difficultyLevel to MutedText
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(color)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun StatsRow(stats: List<Pair<String, Color>>) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        stats.forEach { (label, color) ->
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = color,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 17.sp,
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
            fontSize = 14.sp,
            color = DeepInk,
            lineHeight = 20.sp,
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
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
                text = "🏋 Добавить в тренировку",
                fontSize = 16.sp,
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

private fun formatRating(rating: Double): String {
    val oneDecimal = (kotlin.math.round(rating * 10) / 10.0)
    return oneDecimal.toString()
}
