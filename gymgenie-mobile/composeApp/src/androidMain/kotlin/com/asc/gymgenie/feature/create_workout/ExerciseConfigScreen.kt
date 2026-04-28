package com.asc.gymgenie.feature.create_workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.exercise.ExerciseShortResponse
import com.asc.gymgenie.presentation.CreateWorkoutLimits
import com.asc.gymgenie.presentation.PendingExercise
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.WarmOffWhite

/**
 * Step 3 of the create-workout flow: configures sets/reps for the chosen exercise.
 *
 * This screen does not know about the rest of the flow — it just constructs a
 * [PendingExercise] and hands it upwards. Limit enforcement mirrors the values
 * declared in [CreateWorkoutLimits] so that the UI and view model agree.
 */
@Composable
fun ExerciseConfigScreen(
    exercise: ExerciseShortResponse,
    onBack: () -> Unit,
    onConfirm: (PendingExercise) -> Unit,
) {
    var sets by remember(exercise.id) { mutableIntStateOf(CreateWorkoutLimits.DEFAULT_SETS) }
    var reps by remember(exercise.id) { mutableIntStateOf(CreateWorkoutLimits.DEFAULT_REPS) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CreateWorkoutTopBar(
                title = exercise.nameRu,
                subtitle = "Шаг 3 из 4",
                onBack = onBack,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
            ) {
                ExerciseSummaryCard(exercise = exercise)

                Spacer(modifier = Modifier.height(20.dp))

                StepperCard(
                    label = "Подходы",
                    value = sets,
                    onDecrement = { sets = (sets - 1).coerceAtLeast(CreateWorkoutLimits.MIN_SETS) },
                    onIncrement = { sets = (sets + 1).coerceAtMost(CreateWorkoutLimits.MAX_SETS) },
                    canDecrement = sets > CreateWorkoutLimits.MIN_SETS,
                    canIncrement = sets < CreateWorkoutLimits.MAX_SETS,
                )

                Spacer(modifier = Modifier.height(12.dp))

                StepperCard(
                    label = "Повторений в подходе",
                    value = reps,
                    onDecrement = { reps = (reps - 1).coerceAtLeast(CreateWorkoutLimits.MIN_REPS) },
                    onIncrement = { reps = (reps + 1).coerceAtMost(CreateWorkoutLimits.MAX_REPS) },
                    canDecrement = reps > CreateWorkoutLimits.MIN_REPS,
                    canIncrement = reps < CreateWorkoutLimits.MAX_REPS,
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        onConfirm(
                            PendingExercise(
                                exerciseId = exercise.id,
                                exerciseNameRu = exercise.nameRu,
                                exerciseNameEn = exercise.nameEn,
                                muscleGroupKey = exercise.muscleGroup,
                                sets = sets,
                                reps = reps,
                            ),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentOrange,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = "Добавить упражнение",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ExerciseSummaryCard(exercise: ExerciseShortResponse) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = muscleGroupPickerEmoji(exercise.muscleGroup),
            fontSize = 40.sp,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.nameRu,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk,
                maxLines = 2,
            )
            if (exercise.muscleGroup.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = muscleGroupDisplayNameRu(exercise.muscleGroup),
                    fontSize = 13.sp,
                    color = MutedText,
                )
            }
        }
    }
}

@Composable
private fun StepperCard(
    label: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    canDecrement: Boolean,
    canIncrement: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepInk,
            modifier = Modifier.weight(1f),
        )

        StepperCircleButton(
            symbol = "−",
            enabled = canDecrement,
            onClick = onDecrement,
        )

        Box(
            modifier = Modifier
                .width(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk,
            )
        }

        StepperCircleButton(
            symbol = "+",
            enabled = canIncrement,
            onClick = onIncrement,
        )
    }
}

/**
 * Small local mapping — mirrors the ru labels from ExerciseDetailScreen without
 * introducing a cross-feature dependency.
 */
private fun muscleGroupDisplayNameRu(group: String): String = when (group.uppercase()) {
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
