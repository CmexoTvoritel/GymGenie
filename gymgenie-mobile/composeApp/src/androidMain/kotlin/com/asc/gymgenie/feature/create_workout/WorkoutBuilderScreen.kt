package com.asc.gymgenie.feature.create_workout

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.R
import com.asc.gymgenie.presentation.CreateWorkoutLimits
import com.asc.gymgenie.presentation.CreateWorkoutUiState
import com.asc.gymgenie.presentation.PendingExercise
import com.asc.gymgenie.ui.components.GymGenieToolbar
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.SoftCard
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.utils.WeekdayPairs
import com.asc.gymgenie.utils.formatRestDuration
import com.asc.gymgenie.workout.WorkoutScheduleType

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WorkoutBuilderScreen(
    state: CreateWorkoutUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onIncrementRest: () -> Unit,
    onDecrementRest: () -> Unit,
    onRemoveExerciseAt: (Int) -> Unit,
    onAddMoreExercises: () -> Unit,
    onSave: () -> Unit,
    onScheduleTypeChange: (WorkoutScheduleType) -> Unit = {},
    onToggleScheduleDay: (String) -> Unit = {},
    onEditExerciseAt: (Int) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .imePadding()
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            },
    ) {

        GymGenieToolbar(
            title = "Создание тренировки",
            showBackNavigation = true,
            showCloseIcon = true,
            onBackClick = onBack,
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
                WorkoutDescriptionField(
                    value = state.description,
                    onValueChange = { newValue ->
                        if (newValue.length <= WorkoutDescriptionMaxLength) {
                            onDescriptionChange(newValue)
                        }
                    },
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
                SectionHeader(text = "Упражнения", count = state.exercises.size)
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
                        onEdit = { onEditExerciseAt(index) },
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
    Column {
        Text(
            text = "Название",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MutedText,
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Название тренировки", color = MutedText) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            textStyle = TextStyle(fontSize = 16.sp, color = DeepInk),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentOrange,
                unfocusedBorderColor = SoftCard,
                cursorColor = AccentOrange,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
            ),
        )
    }
}

@Composable
private fun WorkoutDescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column {
        Text(
            text = "Описание",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MutedText,
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Короткое описание (необязательно)", color = MutedText) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = false,
            maxLines = 2,
            textStyle = TextStyle(fontSize = 16.sp, color = DeepInk),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentOrange,
                unfocusedBorderColor = SoftCard,
                cursorColor = AccentOrange,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
            ),
        )
    }
}

private const val WorkoutDescriptionMaxLength = 500

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
        Icon(
            imageVector = Icons.Outlined.Timer,
            contentDescription = null,
            tint = AccentOrange,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Отдых между подходами",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepInk,
            modifier = Modifier.weight(1f),
        )

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
private fun SectionHeader(text: String, count: Int = 0) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MutedText,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (count > 0) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AccentOrange),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
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
    onEdit: () -> Unit,
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
                .clip(RoundedCornerShape(12.dp))
                .background(AccentOrange.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = muscleGroupExerciseDrawable(exercise.muscleGroupKey)),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.exerciseNameRu,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk,
                maxLines = 2,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = buildExerciseRowSubtitle(exercise),
                fontSize = 14.sp,
                color = MutedText,
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SoftCard)
                .clickable { onEdit() },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = "Редактировать",
                modifier = Modifier.size(16.dp),
                colorFilter = ColorFilter.tint(DeepInk),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SoftCard)
                .clickable { onRemove() },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = "Удалить",
                modifier = Modifier.size(16.dp),
                colorFilter = ColorFilter.tint(DeepInk),
            )
        }
    }
}

private fun buildExerciseRowSubtitle(exercise: PendingExercise): String = buildString {
    append("${exercise.sets} подх • ${exercise.reps} пов")
    val weights = exercise.setWeightsKg
        ?.filterNotNull()
        ?.takeIf { it.isNotEmpty() }
        ?: return@buildString
    if (!exercise.requiresWeight) return@buildString
    val unique = weights.distinct()
    if (unique.size == 1) {
        append(" • ${formatRowWeight(unique.first())} кг")
    } else {
        append(" • ${formatRowWeight(weights.min())}-${formatRowWeight(weights.max())} кг")
    }
}

private fun formatRowWeight(kg: Double): String =
    if (kg % 1.0 == 0.0) kg.toInt().toString() else kg.toString()

@Composable
private fun BottomActionBar(
    isSaving: Boolean,
    canSave: Boolean,
    onAddMore: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WarmOffWhite)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = onAddMore,
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepInk),
        ) {
            Text(
                text = "Добавить",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
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
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ScheduleTypeSelector(
    selected: WorkoutScheduleType,
    onSelected: (WorkoutScheduleType) -> Unit,
) {
    Column {
        Text(
            text = "Тип тренировки",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MutedText,
        )
        Spacer(modifier = Modifier.height(6.dp))
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
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) Color.White else MutedText,
        )
    }
}

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

private val ScheduleDays = WeekdayPairs
