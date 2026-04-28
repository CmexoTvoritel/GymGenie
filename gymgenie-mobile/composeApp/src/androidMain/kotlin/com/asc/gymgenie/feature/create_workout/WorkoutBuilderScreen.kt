package com.asc.gymgenie.feature.create_workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.presentation.CreateWorkoutLimits
import com.asc.gymgenie.presentation.CreateWorkoutUiState
import com.asc.gymgenie.presentation.PendingExercise
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.SoftCard
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.workout.WorkoutScheduleType

/**
 * Step 4 of the create-workout flow: review, edit, save.
 *
 * Reads directly from the shared [CreateWorkoutUiState] so the list of already
 * configured exercises survives back-and-forth navigation to add more items.
 */
@Composable
fun WorkoutBuilderScreen(
    state: CreateWorkoutUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onIncrementRest: () -> Unit,
    onDecrementRest: () -> Unit,
    onRemoveExerciseAt: (Int) -> Unit,
    onAddMoreExercises: () -> Unit,
    onSave: () -> Unit,
    onScheduleTypeChange: (WorkoutScheduleType) -> Unit = {},
    onToggleScheduleDay: (String) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .imePadding(),
    ) {
        CreateWorkoutTopBar(
            title = state.workoutName.ifBlank { "Моя тренировка" },
            subtitle = "Шаг 4 из 4",
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                WorkoutNameField(
                    value = state.workoutName,
                    onValueChange = onNameChange,
                )
            }

            item {
                ScheduleTypeSelector(
                    selected = state.scheduleType,
                    onSelected = onScheduleTypeChange,
                )
            }

            if (state.scheduleType == WorkoutScheduleType.RECURRING) {
                item {
                    ScheduleDayPicker(
                        selectedDays = state.scheduleDays,
                        onToggle = onToggleScheduleDay,
                    )
                }
            }

            item {
                RestTimeCard(
                    restSeconds = state.restSeconds,
                    onDecrement = onDecrementRest,
                    onIncrement = onIncrementRest,
                )
            }

            item {
                SectionHeader(text = "Упражнения · ${state.exercises.size}")
            }

            if (state.exercises.isEmpty()) {
                item { EmptyExercisesHint() }
            } else {
                itemsIndexed(
                    state.exercises,
                    key = { index, item -> "${item.exerciseId}#$index" },
                ) { index, exercise ->
                    ExerciseRow(
                        exercise = exercise,
                        onRemove = { onRemoveExerciseAt(index) },
                    )
                }
            }

            if (state.errorMessage != null) {
                item {
                    Text(
                        text = state.errorMessage ?: "",
                        color = Color(0xFFE53935),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        BottomActionBar(
            isSaving = state.isSaving,
            canSave = state.workoutName.isNotBlank() && state.exercises.isNotEmpty() && !state.isSaving,
            onAddMore = onAddMoreExercises,
            onSave = onSave,
        )
    }
}

@Composable
private fun WorkoutNameField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Название тренировки", color = MutedText) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentOrange,
            unfocusedBorderColor = SoftCard,
            cursorColor = AccentOrange,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
        ),
    )
}

@Composable
private fun RestTimeCard(
    restSeconds: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Отдых между подходами",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepInk,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatRestDuration(restSeconds),
                fontSize = 13.sp,
                color = MutedText,
            )
        }

        StepperCircleButton(
            symbol = "−",
            enabled = restSeconds > CreateWorkoutLimits.MIN_REST_SECONDS,
            onClick = onDecrement,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier.width(64.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = formatRestDuration(restSeconds),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        StepperCircleButton(
            symbol = "+",
            enabled = restSeconds < CreateWorkoutLimits.MAX_REST_SECONDS,
            onClick = onIncrement,
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MutedText,
    )
}

@Composable
private fun EmptyExercisesHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SoftCard)
            .padding(vertical = 28.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Добавьте хотя бы одно упражнение",
            fontSize = 14.sp,
            color = MutedText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ExerciseRow(
    exercise: PendingExercise,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AccentOrange.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = muscleGroupPickerEmoji(exercise.muscleGroupKey),
                fontSize = 18.sp,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.exerciseNameRu,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk,
                maxLines = 2,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${exercise.sets} подх • ${exercise.reps} пов",
                fontSize = 12.sp,
                color = MutedText,
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SoftCard)
                .clickable { onRemove() },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "🗑", fontSize = 16.sp)
        }
    }
}

@Composable
private fun BottomActionBar(
    isSaving: Boolean,
    canSave: Boolean,
    onAddMore: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WarmOffWhite)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onAddMore,
            enabled = !isSaving,
            modifier = Modifier
                .weight(1f)
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepInk),
        ) {
            Text(
                text = "Добавить",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier
                .weight(1f)
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentOrange,
                contentColor = Color.White,
                disabledContainerColor = AccentOrange.copy(alpha = 0.5f),
                disabledContentColor = Color.White,
            ),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(
                    text = "Сохранить",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * Two-state segmented control: one-time vs recurring schedule. Pure UI — the
 * lookup table and click handler are kept tiny so the row stays readable.
 */
@Composable
private fun ScheduleTypeSelector(
    selected: WorkoutScheduleType,
    onSelected: (WorkoutScheduleType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SoftCard)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ScheduleTypeOption(
            label = "Разовая",
            isSelected = selected == WorkoutScheduleType.ONE_TIME,
            onClick = { onSelected(WorkoutScheduleType.ONE_TIME) },
            modifier = Modifier.weight(1f),
        )
        ScheduleTypeOption(
            label = "Постоянная",
            isSelected = selected == WorkoutScheduleType.RECURRING,
            onClick = { onSelected(WorkoutScheduleType.RECURRING) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ScheduleTypeOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) AccentOrange else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) Color.White else MutedText,
        )
    }
}

/**
 * Day-of-week picker shown only for the recurring schedule type. Day keys are
 * the `DayOfWeek` enum names so the value is API-ready and shared with iOS.
 */
@Composable
private fun ScheduleDayPicker(
    selectedDays: Set<String>,
    onToggle: (String) -> Unit,
) {
    val days = ScheduleDays

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        days.forEach { (key, label) ->
            val isSelected = key in selectedDays
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(if (isSelected) AccentOrange else SoftCard)
                    .clickable { onToggle(key) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else MutedText,
                )
            }
        }
    }
}

private val ScheduleDays = listOf(
    "MONDAY" to "Пн",
    "TUESDAY" to "Вт",
    "WEDNESDAY" to "Ср",
    "THURSDAY" to "Чт",
    "FRIDAY" to "Пт",
    "SATURDAY" to "Сб",
    "SUNDAY" to "Вс",
)

/**
 * Formats rest seconds according to the spec:
 * - < 60 → "Xс"
 * - >= 60 with no remainder → "Xм"
 * - >= 60 with remainder → "Xм Yс"
 */
internal fun formatRestDuration(seconds: Int): String {
    if (seconds < 60) return "${seconds}с"
    val minutes = seconds / 60
    val remainder = seconds % 60
    return if (remainder == 0) "${minutes}м" else "${minutes}м ${remainder}с"
}
