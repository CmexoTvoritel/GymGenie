package com.asc.gymgenie.feature.exercise_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.exercise.ExerciseApi
import com.asc.gymgenie.exercise.ExerciseDetailResponse
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.ui.theme.AccentGreen
import com.asc.gymgenie.ui.theme.Background
import com.asc.gymgenie.ui.theme.OnBackground
import com.asc.gymgenie.ui.theme.OnSurfaceVariant
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    tokenStorage: TokenStorage,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var exercise by remember { mutableStateOf<ExerciseDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val koin = remember { GlobalContext.get() }
    val api = remember { koin.get<ExerciseApi>() }

    fun loadExercise() {
        isLoading = true
        errorMessage = null
        scope.launch {
            api.getExerciseById(exerciseId)
                .onSuccess { exercise = it }
                .onFailure { errorMessage = it.message ?: "Не удалось загрузить упражнение" }
            isLoading = false
        }
    }

    LaunchedEffect(exerciseId) { loadExercise() }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AccentGreen)
        } else if (errorMessage != null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(errorMessage ?: "Ошибка", color = OnSurfaceVariant, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { loadExercise() }, colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)) {
                    Text("Повторить")
                }
            }
        } else {
            exercise?.let { ex ->
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        // Hero image placeholder
                        Box(
                            modifier = Modifier.fillMaxWidth().height(260.dp).background(Color.Gray.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("🏋️", fontSize = 80.sp)
                        }

                        // Content
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-20).dp)
                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                .background(Background)
                                .padding(20.dp),
                        ) {
                            // Title row
                            Row(verticalAlignment = Alignment.Top) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ex.nameRu, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = OnBackground)
                                    if (ex.nameEn.isNotEmpty()) {
                                        Text("(${ex.nameEn})", fontSize = 14.sp, color = OnSurfaceVariant)
                                    }
                                    Text(muscleLabel(ex), fontSize = 13.sp, color = OnSurfaceVariant)
                                }
                                ex.rating?.let { rating ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⭐", fontSize = 14.sp)
                                        Text(String.format("%.1f", rating), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = OnBackground)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Stat chips
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ex.secondsPer10Reps?.let { secPer10 ->
                                    StatChip("⏱", "Темп", "${secPer10} сек/10", Modifier.weight(1f))
                                }
                                StatChip("🏋️", "Уровень", difficultyLabel(ex.difficultyLevel), Modifier.weight(1f))
                                ex.caloriesBurned?.let { cal ->
                                    StatChip("🔥", "Ккал", "~$cal", Modifier.weight(1f))
                                }
                            }

                            // Instructions
                            if (ex.instructions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Text("Инструкция", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = OnBackground)
                                Spacer(modifier = Modifier.height(12.dp))
                                ex.instructions.forEachIndexed { index, step ->
                                    Row(modifier = Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.Top) {
                                        Box(
                                            modifier = Modifier.size(24.dp).clip(CircleShape).background(AccentGreen.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text("${index + 1}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(step, fontSize = 14.sp, color = OnBackground, modifier = Modifier.weight(1f))
                                    }
                                }
                            }

                            // Equipment
                            if (ex.equipment.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Text("Необходимый инвентарь", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = OnBackground)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    ex.equipment.forEach { item ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(AccentGreen.copy(alpha = 0.1f))
                                                .padding(horizontal = 12.dp, vertical = 7.dp),
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("✅", fontSize = 12.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(item, fontSize = 13.sp, color = OnBackground)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }

        // Back button overlay
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .align(Alignment.TopStart),
        ) {
            Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
        }
    }
}

@Composable
private fun StatChip(icon: String, label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = OnBackground)
            Text(label, fontSize = 11.sp, color = OnSurfaceVariant)
        }
    }
}

private fun muscleLabel(ex: ExerciseDetailResponse): String {
    val muscle = muscleGroupRu(ex.muscleGroup)
    val cat = categoryRu(ex.category)
    return listOf(muscle, cat).filter { it.isNotEmpty() }.joinToString(" · ")
}

private fun muscleGroupRu(raw: String) = when (raw) {
    "CHEST" -> "Грудные"
    "BACK" -> "Спина"
    "SHOULDERS" -> "Плечи"
    "BICEPS" -> "Бицепс"
    "TRICEPS" -> "Трицепс"
    "ABS" -> "Пресс"
    "QUADRICEPS" -> "Квадрицепс"
    "HAMSTRINGS" -> "Бицепс бедра"
    "GLUTES" -> "Ягодицы"
    "CALVES" -> "Икры"
    "FULL_BODY" -> "Всё тело"
    else -> raw
}

private fun categoryRu(raw: String) = when (raw) {
    "STRENGTH" -> "Силовая"
    "CARDIO" -> "Кардио"
    "FLEXIBILITY" -> "Гибкость"
    "PLYOMETRIC" -> "Плиометрика"
    "CALISTHENICS" -> "С весом тела"
    else -> raw
}

private fun difficultyLabel(raw: String) = when (raw) {
    "BEGINNER" -> "Начальный"
    "INTERMEDIATE" -> "Средний"
    "ADVANCED" -> "Продвинутый"
    else -> raw
}
