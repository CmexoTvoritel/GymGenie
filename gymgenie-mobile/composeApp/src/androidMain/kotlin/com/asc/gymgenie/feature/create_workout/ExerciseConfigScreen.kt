package com.asc.gymgenie.feature.create_workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.exercise.ExerciseShortResponse
import com.asc.gymgenie.presentation.CreateWorkoutLimits
import com.asc.gymgenie.presentation.ExerciseDetailViewModel
import com.asc.gymgenie.presentation.PendingExercise
import com.asc.gymgenie.ui.components.GymGenieToolbar
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.SoftCard
import com.asc.gymgenie.ui.theme.WarmOffWhite
import org.koin.core.context.GlobalContext
import kotlin.math.round

/**
 * Step 3 of the create-workout flow: configures sets/reps (and optionally
 * per-set weights) for the chosen exercise.
 *
 * This screen does not know about the rest of the flow — it just constructs a
 * [PendingExercise] and hands it upwards. Limit enforcement mirrors the values
 * declared in [CreateWorkoutLimits] so that the UI and view model agree.
 *
 * The weight configuration card is only rendered when
 * [ExerciseShortResponse.requiresWeight] is `true`, keeping bodyweight-only
 * exercises uncluttered.
 *
 * When [prefillFrom] is non-null the screen acts as the edit surface for an
 * already-added row: stepper state seeds from the supplied [PendingExercise],
 * the wizard's step indicator is hidden via [showStepHeader], and the bottom
 * CTA switches to "Сохранить изменения". The "add new exercise" path keeps
 * the legacy defaults and unchanged copy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseConfigScreen(
    exercise: ExerciseShortResponse,
    onBack: () -> Unit,
    onConfirm: (PendingExercise) -> Unit,
    prefillFrom: PendingExercise? = null,
    showStepHeader: Boolean = true,
) {
    val isEditMode = prefillFrom != null

    var sets by remember(exercise.id) {
        mutableIntStateOf(prefillFrom?.sets ?: CreateWorkoutLimits.DEFAULT_SETS)
    }
    var reps by remember(exercise.id) {
        mutableIntStateOf(prefillFrom?.reps ?: CreateWorkoutLimits.DEFAULT_REPS)
    }

    // Initial weight mode mirrors the variance of the prefilled weights: if all
    // sets share a value (or the row never had weights), keep the simpler
    // "одинаковый вес" stepper; otherwise drop straight into per-set mode so
    // the user sees the actual pyramid configuration on first render.
    var weightMode by remember(exercise.id) {
        val initialMode = if (
            prefillFrom?.setWeightsKg != null &&
            prefillFrom.setWeightsKg!!.filterNotNull().distinct().size > 1
        ) WeightMode.PER_SET else WeightMode.UNIFORM
        mutableStateOf(initialMode)
    }
    var uniformWeightKg by remember(exercise.id) {
        mutableStateOf(
            prefillFrom?.setWeightsKg?.firstOrNull { it != null }
                ?: CreateWorkoutLimits.DEFAULT_WEIGHT_KG,
        )
    }
    var perSetWeightsKg by remember(exercise.id) {
        val seededSize = prefillFrom?.sets ?: CreateWorkoutLimits.DEFAULT_SETS
        val fallback = prefillFrom?.setWeightsKg?.firstOrNull { it != null }
            ?: CreateWorkoutLimits.DEFAULT_WEIGHT_KG
        mutableStateOf(
            prefillFrom?.setWeightsKg
                ?.map { it ?: fallback }
                ?.take(seededSize)
                ?.let { picked ->
                    if (picked.size < seededSize) {
                        picked + List(seededSize - picked.size) { fallback }
                    } else picked
                }
                ?: List(seededSize) { CreateWorkoutLimits.DEFAULT_WEIGHT_KG },
        )
    }

    // Resize the per-set list when the number of sets changes. Growing keeps
    // the previously-entered values and seeds new sets with the last weight
    // the user picked; shrinking drops the tail. Keeping the legacy values in
    // place is important so a user moving sets 3→4→3 sees the same weights.
    LaunchedEffect(sets) {
        if (perSetWeightsKg.size != sets) {
            perSetWeightsKg = if (sets > perSetWeightsKg.size) {
                val tailSeed = perSetWeightsKg.lastOrNull() ?: uniformWeightKg
                perSetWeightsKg + List(sets - perSetWeightsKg.size) { tailSeed }
            } else {
                perSetWeightsKg.take(sets)
            }
        }
    }

    var detailExerciseId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val bottomSafeArea = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GymGenieToolbar(
                title = exercise.nameRu,
                showBackNavigation = true,
                onBackClick = onBack,
            )

            if (showStepHeader) {
                WorkoutFlowStepHeader(currentStep = 3)
            }

            // LazyColumn so the weight-per-set section stays scrollable when the
            // user picks the maximum number of sets — otherwise long lists could
            // push the confirm button off-screen on small devices.
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    ExerciseSummaryCard(
                        exercise = exercise,
                        onInfoClick = { detailExerciseId = exercise.id },
                    )
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    StepperCard(
                        label = "Подходы",
                        value = sets,
                        onDecrement = {
                            sets = (sets - 1).coerceAtLeast(CreateWorkoutLimits.MIN_SETS)
                        },
                        onIncrement = {
                            sets = (sets + 1).coerceAtMost(CreateWorkoutLimits.MAX_SETS)
                        },
                        canDecrement = sets > CreateWorkoutLimits.MIN_SETS,
                        canIncrement = sets < CreateWorkoutLimits.MAX_SETS,
                    )
                }

                item {
                    StepperCard(
                        label = "Повторений в подходе",
                        value = reps,
                        onDecrement = {
                            reps = (reps - 1).coerceAtLeast(CreateWorkoutLimits.MIN_REPS)
                        },
                        onIncrement = {
                            reps = (reps + 1).coerceAtMost(CreateWorkoutLimits.MAX_REPS)
                        },
                        canDecrement = reps > CreateWorkoutLimits.MIN_REPS,
                        canIncrement = reps < CreateWorkoutLimits.MAX_REPS,
                    )
                }

                if (exercise.requiresWeight) {
                    item {
                        WeightSection(
                            mode = weightMode,
                            onModeChange = { weightMode = it },
                            uniformWeightKg = uniformWeightKg,
                            onUniformWeightChange = { uniformWeightKg = it },
                            perSetWeightsKg = perSetWeightsKg,
                            onPerSetWeightChange = { index, value ->
                                if (index in perSetWeightsKg.indices) {
                                    perSetWeightsKg = perSetWeightsKg
                                        .toMutableList()
                                        .also { it[index] = value }
                                }
                            },
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WarmOffWhite)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = bottomSafeArea + 16.dp, top = 8.dp),
            ) {
                Button(
                    onClick = {
                        val weightsForPayload: List<Double?>? = if (!exercise.requiresWeight) {
                            null
                        } else {
                            when (weightMode) {
                                WeightMode.UNIFORM -> List(sets) { uniformWeightKg }
                                WeightMode.PER_SET -> perSetWeightsKg.take(sets)
                                    .let { taken ->
                                        if (taken.size < sets) {
                                            taken + List(sets - taken.size) {
                                                taken.lastOrNull() ?: uniformWeightKg
                                            }
                                        } else taken
                                    }
                            }
                        }
                        onConfirm(
                            PendingExercise(
                                exerciseId = exercise.id,
                                exerciseNameRu = exercise.nameRu,
                                exerciseNameEn = exercise.nameEn,
                                muscleGroupKey = exercise.muscleGroup,
                                sets = sets,
                                reps = reps,
                                requiresWeight = exercise.requiresWeight,
                                setWeightsKg = weightsForPayload,
                            ),
                        )
                    },
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
                        text = if (isEditMode) "Сохранить изменения" else "Добавить упражнение",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }

    if (detailExerciseId != null) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { detailExerciseId = null },
            containerColor = WarmOffWhite,
        ) {
            ExerciseDetailSheetContent(exerciseId = detailExerciseId!!)
        }
    }
}

/**
 * Two-way toggle that drives the weight configuration UI on the config screen.
 *
 * `UNIFORM` shows a single stepper applied to every set; `PER_SET` reveals one
 * stepper per set so the user can pyramid up/down. Kept as a screen-local enum
 * because no other surface needs to express this concept yet.
 */
internal enum class WeightMode { UNIFORM, PER_SET }

@Composable
private fun ExerciseSummaryCard(
    exercise: ExerciseShortResponse,
    onInfoClick: () -> Unit,
) {
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
                fontSize = 18.sp, // bumped +2sp per spec
                fontWeight = FontWeight.Bold,
                color = DeepInk,
                maxLines = 2,
            )
            if (exercise.muscleGroup.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = muscleGroupDisplayNameRu(exercise.muscleGroup),
                    fontSize = 15.sp, // bumped +2sp per spec
                    color = MutedText,
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Info entry-point lives at the end of the row so the touch target is
        // distinct from the title area; tap opens the detail bottom sheet.
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AccentOrange.copy(alpha = 0.12f))
                .clickable(onClick = onInfoClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Подробнее об упражнении",
                tint = AccentOrange,
                modifier = Modifier.size(22.dp),
            )
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
            fontSize = 17.sp, // bumped +2sp per spec
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
            modifier = Modifier.width(56.dp),
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

@Composable
private fun WeightSection(
    mode: WeightMode,
    onModeChange: (WeightMode) -> Unit,
    uniformWeightKg: Double,
    onUniformWeightChange: (Double) -> Unit,
    perSetWeightsKg: List<Double>,
    onPerSetWeightChange: (Int, Double) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Вес",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepInk,
        )

        WeightModeSelector(mode = mode, onModeChange = onModeChange)

        when (mode) {
            WeightMode.UNIFORM -> {
                WeightStepperRow(
                    label = "Вес (кг)",
                    valueKg = uniformWeightKg,
                    onChange = onUniformWeightChange,
                )
            }

            WeightMode.PER_SET -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    perSetWeightsKg.forEachIndexed { index, valueKg ->
                        WeightStepperRow(
                            label = "Подход ${index + 1}",
                            valueKg = valueKg,
                            onChange = { onPerSetWeightChange(index, it) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightModeSelector(
    mode: WeightMode,
    onModeChange: (WeightMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SoftCard)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        WeightModeOption(
            label = "Одинаковый вес",
            selected = mode == WeightMode.UNIFORM,
            onClick = { onModeChange(WeightMode.UNIFORM) },
            modifier = Modifier.weight(1f),
        )
        WeightModeOption(
            label = "Разный вес",
            selected = mode == WeightMode.PER_SET,
            onClick = { onModeChange(WeightMode.PER_SET) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WeightModeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) AccentOrange else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Color.White else MutedText,
        )
    }
}

@Composable
private fun WeightStepperRow(
    label: String,
    valueKg: Double,
    onChange: (Double) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SoftCard)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepInk,
            modifier = Modifier.weight(1f),
        )

        val step = CreateWorkoutLimits.WEIGHT_STEP_KG
        val canDecrement = valueKg > CreateWorkoutLimits.MIN_WEIGHT_KG
        val canIncrement = valueKg < CreateWorkoutLimits.MAX_WEIGHT_KG

        StepperCircleButton(
            symbol = "−",
            enabled = canDecrement,
            onClick = {
                onChange(
                    (valueKg - step).coerceAtLeast(CreateWorkoutLimits.MIN_WEIGHT_KG),
                )
            },
        )

        Box(
            modifier = Modifier.width(72.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = formatWeightKg(valueKg),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk,
            )
        }

        StepperCircleButton(
            symbol = "+",
            enabled = canIncrement,
            onClick = {
                onChange(
                    (valueKg + step).coerceAtMost(CreateWorkoutLimits.MAX_WEIGHT_KG),
                )
            },
        )
    }
}

@Composable
private fun ExerciseDetailSheetContent(exerciseId: String) {
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
                    modifier = Modifier.padding(vertical = 32.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }

            state.exercise != null -> {
                val ex = state.exercise!!
                Text(
                    text = ex.nameRu,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepInk,
                )
                if (ex.nameEn.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ex.nameEn,
                        fontSize = 14.sp,
                        color = MutedText,
                    )
                }

                ex.description?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "Описание", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepInk)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = it, fontSize = 14.sp, color = DeepInk, lineHeight = 20.sp)
                }

                if (ex.equipment.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "Оборудование", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepInk)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = ex.equipment.joinToString(", "), fontSize = 14.sp, color = DeepInk)
                }

                if (ex.instructions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "Техника выполнения", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepInk)
                    Spacer(modifier = Modifier.height(8.dp))
                    ex.instructions.forEachIndexed { index, step ->
                        Text(
                            text = "${index + 1}. $step",
                            fontSize = 14.sp,
                            color = DeepInk,
                            lineHeight = 20.sp,
                        )
                        if (index < ex.instructions.lastIndex) {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders kilograms using a single decimal when needed so the user sees `2.5`
 * for half-step values but the cleaner `20` for whole numbers.
 */
private fun formatWeightKg(value: Double): String {
    val rounded = round(value * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) {
        "${rounded.toInt()} кг"
    } else {
        // Avoid locale-dependent string formatters in shared/common Compose
        // paths — explicit string concatenation keeps the output deterministic.
        val whole = rounded.toInt()
        val tenths = ((rounded - whole) * 10).toInt()
        "$whole.$tenths кг"
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
