package com.asc.gymgenie.feature.nutrition

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.asc.gymgenie.nutrition.AiMealChatMessage
import com.asc.gymgenie.nutrition.AiMealConflictPlan
import com.asc.gymgenie.nutrition.AiMealFlowStep
import com.asc.gymgenie.nutrition.AiMealProfileData
import com.asc.gymgenie.nutrition.AiMealType
import com.asc.gymgenie.nutrition.AiMealViewModel
import com.asc.gymgenie.nutrition.MealGoal
import com.asc.gymgenie.ui.components.GymGenieToolbar
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.user.UserProfileStore
import com.asc.gymgenie.R
import com.asc.gymgenie.utils.WeekdayPairs
import com.asc.gymgenie.utils.weekdayShortFromDayOfWeek
import kotlin.math.roundToInt
import org.koin.core.context.GlobalContext

private val MealOffWhite = WarmOffWhite
private val MealCardBg = Color(0xFFFFFFFF)
private val MealBorderGray = Color(0xFFE0E0E0)
private val MealMutedGray = Color(0xFF888888)
private val MealGreenSave = Color(0xFF22C55E)
private val MealGreenLight = Color(0xFFF0FDF4)
private val MealGreenText = Color(0xFF16A34A)

@Composable
fun AiMealFlowScreen(
    onDismiss: () -> Unit,
) {
    val koin = remember { GlobalContext.get() }
    val viewModel = remember { koin.get<AiMealViewModel>() }
    val userProfileStore = remember { koin.get<UserProfileStore>() }
    DisposableEffect(Unit) { onDispose { viewModel.onCleared() } }

    val state by viewModel.state.collectAsState()

    val mealProfileSnapshot by userProfileStore.profile.collectAsState()
    LaunchedEffect(mealProfileSnapshot) {
        viewModel.refreshProfileFromStore()
    }

    var showChatExitDialog by remember { mutableStateOf(false) }

    val needsChatExitConfirmation = state.step == AiMealFlowStep.CHAT
            && !state.isSaved
            && (state.messages.isNotEmpty() || state.isTyping)

    BackHandler {
        when {
            state.step == AiMealFlowStep.CHOOSE -> onDismiss()
            state.step == AiMealFlowStep.CHAT -> {
                if (needsChatExitConfirmation) showChatExitDialog = true else onDismiss()
            }
            else -> viewModel.goBack()
        }
    }

    if (showChatExitDialog) {
        AlertDialog(
            onDismissRequest = { showChatExitDialog = false },
            title = { Text("Выйти из чата?") },
            text = { Text("Весь прогресс будет потерян. Убедитесь, что план сохранён.") },
            confirmButton = {
                TextButton(onClick = {
                    showChatExitDialog = false
                    onDismiss()
                }) {
                    Text("Выйти", color = Color(0xFFDC2626))
                }
            },
            dismissButton = {
                TextButton(onClick = { showChatExitDialog = false }) {
                    Text("Остаться")
                }
            },
        )
    }

    AnimatedContent(
        targetState = state.step,
        transitionSpec = {
            val forward = targetState.index > initialState.index
            slideInHorizontally(
                initialOffsetX = { if (forward) it else -it },
                animationSpec = tween(300),
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { if (forward) -it else it },
                animationSpec = tween(300),
            )
        },
        label = "ai_meal_step",
    ) { step ->
        when (step) {
            AiMealFlowStep.CHOOSE -> MealTypeChooseScreen(
                selectedType = state.selectedMealType,
                onClose = onDismiss,
                onSelectType = { viewModel.setMealType(it) },
                onContinue = { viewModel.goTo(AiMealFlowStep.PROFILE) },
            )
            AiMealFlowStep.PROFILE -> ProfileScreen(
                profile = state.profile,
                onBack = { viewModel.goBack() },
                onNext = { viewModel.goTo(AiMealFlowStep.GOAL) },
                onAge = { viewModel.setAge(it) },
                onHeight = { viewModel.setHeight(it) },
                onWeight = { viewModel.setWeight(it) },
            )
            AiMealFlowStep.GOAL -> GoalScreen(
                selectedGoal = state.goal,
                onBack = { viewModel.goBack() },
                onNext = { viewModel.goTo(AiMealFlowStep.RESTRICTIONS) },
                onSelect = { viewModel.setGoal(it) },
            )
            AiMealFlowStep.RESTRICTIONS -> RestrictionsScreen(
                dietaryRestrictions = state.dietaryRestrictions,
                allergies = state.allergies,
                onBack = { viewModel.goBack() },
                onNext = { viewModel.goTo(AiMealFlowStep.CHAT) },
                onDietaryChange = { viewModel.setDietaryRestrictions(it) },
                onAllergiesChange = { viewModel.setAllergies(it) },
            )
            AiMealFlowStep.CHAT -> ChatScreen(
                messages = state.messages,
                isTyping = state.isTyping,
                hasMealPlan = state.lastMealPlan != null,
                savedPlanId = state.savedPlanId,
                isSaving = state.isSaving,
                isSaved = state.isSaved,
                errorMessage = state.errorMessage,
                showSchedulePicker = state.showSchedulePicker,
                scheduleMode = state.scheduleMode,
                selectedDate = state.selectedDate,
                selectedWeekdays = state.selectedWeekdays,
                bookedOneOffDates = state.bookedOneOffDates,
                bookedRecurringDays = state.bookedRecurringDays,
                showConflictDialog = state.showConflictDialog,
                conflicts = state.conflicts,
                onBack = {
                    if (needsChatExitConfirmation) showChatExitDialog = true else onDismiss()
                },
                onSend = { viewModel.sendMessage(it) },
                onAddPlanTapped = { viewModel.onAddPlanTapped() },
                onSetScheduleMode = { viewModel.setScheduleMode(it) },
                onSelectDate = { viewModel.setSelectedDate(it) },
                onToggleWeekday = { viewModel.toggleWeekday(it) },
                onSaveWithSchedule = { viewModel.saveWithSchedule() },
                onDismissSchedulePicker = { viewModel.dismissSchedulePicker() },
                onConfirmReplace = { viewModel.confirmReplace() },
                onDismissConflict = { viewModel.dismissConflictDialog() },
            )
        }
    }
}

@Composable
private fun MealTypeChooseScreen(
    selectedType: AiMealType?,
    onClose: () -> Unit,
    onSelectType: (AiMealType) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MealOffWhite)
            .navigationBarsPadding(),
    ) {
        GymGenieToolbar(
            title = "План питания",
            showBackNavigation = true,
            onBackClick = onClose,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Какой приём пищи составить?",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(32.dp))
            AiMealType.entries.forEach { type ->
                MealTypeCard(
                    type = type,
                    isSelected = selectedType == type,
                    onClick = { onSelectType(type) },
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        PrimaryButton(
            text = "Продолжить",
            enabled = selectedType != null,
            onClick = onContinue,
        )
    }
}

@Composable
private fun MealTypeCard(
    type: AiMealType,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MealCardBg)
            .border(
                width = if (isSelected) 2.dp else 1.5.dp,
                color = if (isSelected) Coral else Color(0xFFEEEEEE),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(
                id = when (type) {
                    AiMealType.BREAKFAST -> R.drawable.ic_breakfast
                    AiMealType.LUNCH -> R.drawable.ic_lunch
                    AiMealType.DINNER -> R.drawable.ic_dinner
                },
            ),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                type.displayName,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepInk,
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Coral),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    profile: AiMealProfileData,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onAge: (Int) -> Unit,
    onHeight: (Int) -> Unit,
    onWeight: (Int) -> Unit,
) {
    val canProceed = profile.isProfileFilled
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MealOffWhite)
            .navigationBarsPadding(),
    ) {
        GymGenieToolbar(
            title = "План питания",
            showBackNavigation = true,
            onBackClick = onBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text("Ваши параметры", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DeepInk)
            Spacer(Modifier.height(24.dp))
            SliderField(label = "Возраст", value = profile.age, min = 15, max = 80, onChange = onAge)
            SliderField(label = "Рост (см)", value = profile.height, min = 140, max = 220, onChange = onHeight)
            SliderField(label = "Вес (кг)", value = profile.weight, min = 40, max = 180, onChange = onWeight)
        }
        PrimaryButton(text = "Далее", enabled = canProceed, onClick = onNext)
    }
}

@Composable
private fun SliderField(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit,
) {
    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    var accumulatedOffset by remember { mutableFloatStateOf(0f) }
    val pct = if (max > min) (value - min).toFloat() / (max - min) else 0f

    var isEditing by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    fun commitInput() {
        val digits = inputText.filter { it.isDigit() }
        val parsed = digits.toIntOrNull()
        if (parsed != null) {
            onChange(parsed.coerceIn(min, max))
        }
        isEditing = false
    }

    Column(modifier = Modifier.padding(bottom = 28.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF555555))
            if (isEditing) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { new -> inputText = new.filter { it.isDigit() }.take(4) },
                    textStyle = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepInk,
                        textAlign = TextAlign.End,
                    ),
                    cursorBrush = SolidColor(Coral),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { commitInput() }),
                    modifier = Modifier
                        .widthIn(min = 60.dp, max = 120.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused && isEditing) {
                                commitInput()
                            }
                        },
                )
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            } else {
                Text(
                    if (value == 0) "—" else value.toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepInk,
                    modifier = Modifier.clickable {
                        inputText = if (value == 0) "" else value.toString()
                        isEditing = true
                    },
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(min, max) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            accumulatedOffset = offset.x.coerceIn(0f, trackWidthPx)
                            val ratio = (accumulatedOffset / trackWidthPx).coerceIn(0f, 1f)
                            onChange((min + ratio * (max - min)).toInt().coerceIn(min, max))
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            accumulatedOffset = (accumulatedOffset + dragAmount).coerceIn(0f, trackWidthPx)
                            val ratio = (accumulatedOffset / trackWidthPx).coerceIn(0f, 1f)
                            onChange((min + ratio * (max - min)).toInt().coerceIn(min, max))
                        },
                    )
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MealBorderGray),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .height(6.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Coral),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset {
                        val thumbPx = 24.dp.toPx()
                        IntOffset(
                            x = (pct * (trackWidthPx - thumbPx)).roundToInt().coerceAtLeast(0),
                            y = 0,
                        )
                    }
                    .size(24.dp)
                    .border(2.5.dp, Color.White, CircleShape)
                    .clip(CircleShape)
                    .background(Coral),
            )
        }

        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(min.toString(), fontSize = 13.sp, color = Color(0xFFAAAAAA))
            Text(max.toString(), fontSize = 13.sp, color = Color(0xFFAAAAAA))
        }
    }
}

private data class GoalOption(
    val wireValue: String,
    val iconRes: Int,
    val title: String,
    val subtitle: String,
)

private val goalOptions = listOf(
    GoalOption(
        wireValue = MealGoal.LOSE_WEIGHT.wireValue,
        iconRes = R.drawable.ic_ai_weight_loss,
        title = MealGoal.LOSE_WEIGHT.displayName,
        subtitle = "Дефицит калорий, лёгкий рацион",
    ),
    GoalOption(
        wireValue = MealGoal.MAINTAIN.wireValue,
        iconRes = R.drawable.ic_ai_keeping_fit,
        title = MealGoal.MAINTAIN.displayName,
        subtitle = "Сбалансированное питание",
    ),
    GoalOption(
        wireValue = MealGoal.GAIN_MUSCLE.wireValue,
        iconRes = R.drawable.ic_ai_muscles,
        title = MealGoal.GAIN_MUSCLE.displayName,
        subtitle = "Профицит и больше белка",
    ),
)

@Composable
private fun GoalScreen(
    selectedGoal: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val canProceed = selectedGoal.isNotBlank()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MealOffWhite)
            .navigationBarsPadding(),
    ) {
        GymGenieToolbar(
            title = "План питания",
            showBackNavigation = true,
            onBackClick = onBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text("Ваша цель", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DeepInk)
            Spacer(Modifier.height(24.dp))
            goalOptions.forEach { option ->
                GoalCard(
                    option = option,
                    isSelected = option.wireValue == selectedGoal,
                    onClick = { onSelect(option.wireValue) },
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        PrimaryButton(text = "Далее", enabled = canProceed, onClick = onNext)
    }
}

@Composable
private fun GoalCard(
    option: GoalOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MealCardBg)
            .border(
                width = if (isSelected) 2.dp else 1.5.dp,
                color = if (isSelected) Coral else Color(0xFFEEEEEE),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(option.iconRes),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(option.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = DeepInk)
            Spacer(Modifier.height(4.dp))
            Text(option.subtitle, fontSize = 14.sp, color = MealMutedGray)
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Coral),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun RestrictionsScreen(
    dietaryRestrictions: String,
    allergies: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onDietaryChange: (String) -> Unit,
    onAllergiesChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MealOffWhite)
            .navigationBarsPadding()
            .imePadding()

            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            },
    ) {
        GymGenieToolbar(
            title = "План питания",
            showBackNavigation = true,
            onBackClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                onBack()
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Ограничения и аллергии",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Можно оставить пустым",
                fontSize = 15.sp,
                color = MealMutedGray,
            )
            Spacer(Modifier.height(24.dp))

            TextEditorField(
                label = "Ограничения в питании",
                placeholder = "вегетарианец, без глютена...",
                value = dietaryRestrictions,
                onChange = onDietaryChange,
            )
            Spacer(Modifier.height(20.dp))
            TextEditorField(
                label = "Аллергии",
                placeholder = "орехи, молочные продукты...",
                value = allergies,
                onChange = onAllergiesChange,
            )
        }

        PrimaryButton(text = "Далее", enabled = true, onClick = onNext)
    }
}

@Composable
private fun TextEditorField(
    label: String,
    placeholder: String,
    value: String,
    onChange: (String) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Column {
        Text(
            label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF555555),
        )
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(fontSize = 16.sp, color = DeepInk),
            cursorBrush = SolidColor(Coral),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 90.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MealCardBg)
                .onFocusChanged { focused = it.isFocused }
                .border(
                    width = 2.dp,
                    color = if (focused) Coral else MealBorderGray,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(14.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, fontSize = 16.sp, color = Color(0xFFAAAAAA))
                }
                inner()
            },
        )
    }
}

@Composable
private fun ChatScreen(
    messages: List<AiMealChatMessage>,
    isTyping: Boolean,
    hasMealPlan: Boolean,
    savedPlanId: String?,
    isSaving: Boolean,
    isSaved: Boolean,
    errorMessage: String?,
    showSchedulePicker: Boolean,
    scheduleMode: String,
    selectedDate: String?,
    selectedWeekdays: List<String>,
    bookedOneOffDates: List<String>,
    bookedRecurringDays: List<String>,
    showConflictDialog: Boolean,
    conflicts: List<AiMealConflictPlan>,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onAddPlanTapped: () -> Unit,
    onSetScheduleMode: (String) -> Unit,
    onSelectDate: (String?) -> Unit,
    onToggleWeekday: (String) -> Unit,
    onSaveWithSchedule: () -> Unit,
    onDismissSchedulePicker: () -> Unit,
    onConfirmReplace: () -> Unit,
    onDismissConflict: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty() || isTyping) {
            listState.animateScrollToItem(
                listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1,
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MealOffWhite)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        GymGenieToolbar(
            title = "План питания",
            showBackNavigation = true,
            onBackClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                onBack()
            },
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)

                .pointerInput(Unit) {
                    detectTapGestures {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (messages.isEmpty() && !isTyping) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_chatbot_preview),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Опишите какой рацион вы хотите получить",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFAAAAAA),
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                    }
                }
            }
            items(messages) { msg -> ChatBubble(message = msg) }
            if (isTyping) item { TypingBubble() }
        }

        errorMessage?.let {
            Text(
                it,
                fontSize = 13.sp,
                color = Color.Red,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        if (hasMealPlan && !isSaved && !showSchedulePicker) {
            Button(
                onClick = onAddPlanTapped,
                colors = ButtonDefaults.buttonColors(containerColor = MealGreenSave),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    "Добавить план",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color.White,
                )
            }
        }

        if (showSchedulePicker) {
            AiMealSchedulePickerCard(
                scheduleMode = scheduleMode,
                selectedDate = selectedDate,
                selectedWeekdays = selectedWeekdays,
                bookedOneOffDates = bookedOneOffDates,
                bookedRecurringDays = bookedRecurringDays,
                isSaving = isSaving,
                onSetScheduleMode = onSetScheduleMode,
                onSelectDate = onSelectDate,
                onToggleWeekday = onToggleWeekday,
                onSave = onSaveWithSchedule,
                onCancel = onDismissSchedulePicker,
            )
        }

        if (isSaved) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MealGreenLight)
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Рацион сохранён!",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MealGreenText,
                )
            }
        }

        if (showConflictDialog) {
            AlertDialog(
                onDismissRequest = onDismissConflict,
                title = { Text("Заменить существующий план?") },
                text = {
                    val names = conflicts.joinToString(", ") { it.planName }
                    Text("На выбранные даты уже есть план: «$names». Заменить его новым?")
                },
                confirmButton = {
                    TextButton(onClick = onConfirmReplace) {
                        Text("Заменить", color = Color(0xFFDC2626))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissConflict) { Text("Отмена") }
                },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = Color(0xFFEEEEEE), shape = RoundedCornerShape(0.dp))
                .padding(start = 16.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            var inputFocused by remember { mutableStateOf(false) }
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                textStyle = TextStyle(fontSize = 16.sp, color = DeepInk),
                cursorBrush = SolidColor(Coral),
                singleLine = false,
                maxLines = 4,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MealCardBg)
                    .border(
                        width = 2.dp,
                        color = if (inputFocused) Coral else MealBorderGray,
                        shape = RoundedCornerShape(24.dp),
                    )
                    .onFocusChanged { inputFocused = it.isFocused }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (input.isEmpty()) {
                            Text("Напишите запрос...", fontSize = 16.sp, color = Color(0xFFAAAAAA))
                        }
                        inner()
                    }
                },
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (input.isNotBlank()) Coral else MealBorderGray)
                    .clickable(enabled = input.isNotBlank() && !isTyping) {

                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onSend(input)
                        input = ""
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_send),
                    contentDescription = "Отправить",
                    modifier = Modifier.size(20.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White),
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: AiMealChatMessage) {
    val isUser = message.role == AiMealChatMessage.Role.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isUser) Coral else MealCardBg)
                .then(
                    if (!isUser) Modifier.border(
                        width = 1.dp,
                        color = Color(0xFFEEEEEE),
                        shape = RoundedCornerShape(18.dp),
                    ) else Modifier,
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = message.text,
                fontSize = 14.sp,
                color = if (isUser) Color.White else DeepInk,
                lineHeight = 21.sp,
            )
        }
    }
}

@Composable
private fun TypingBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "meal_typing")
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MealCardBg)
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(18.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { idx ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, delayMillis = idx * 150, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "meal_dot_$idx",
            )
            Text("●", fontSize = 14.sp, color = Color(0xFFAAAAAA).copy(alpha = alpha))
        }
    }
}

@Composable
private fun AiMealSchedulePickerCard(
    scheduleMode: String,
    selectedDate: String?,
    selectedWeekdays: List<String>,
    bookedOneOffDates: List<String>,
    bookedRecurringDays: List<String>,
    isSaving: Boolean,
    onSetScheduleMode: (String) -> Unit,
    onSelectDate: (String?) -> Unit,
    onToggleWeekday: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MealCardBg)
            .border(1.5.dp, Color(0xFFEDEDEF), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Расписание",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = DeepInk,
        )

        AiMealScheduleToggle(mode = scheduleMode, onSelect = onSetScheduleMode)

        when (scheduleMode) {
            "ONE_OFF" -> AiMealDateStrip(
                bookedDates = bookedOneOffDates,
                selectedDate = selectedDate,
                onSelect = onSelectDate,
            )
            "RECURRING" -> AiMealWeekdayChips(
                bookedDays = bookedRecurringDays,
                selectedDays = selectedWeekdays,
                onToggle = onToggleWeekday,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                PrimaryButton(
                    text = "Отмена",
                    enabled = !isSaving,
                    color = Color(0xFFE5E7EB),
                    onClick = onCancel,
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                PrimaryButton(
                    text = if (isSaving) "Сохранение..." else "Сохранить",
                    enabled = !isSaving,
                    color = MealGreenSave,
                    onClick = onSave,
                )
            }
        }
    }
}

@Composable
private fun AiMealScheduleToggle(mode: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF3F4F6))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        listOf("ONE_OFF" to "Разово", "RECURRING" to "По дням").forEach { (value, label) ->
            val isSelected = mode == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isSelected) Coral else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isSelected) Color.White else DeepInk,
                )
            }
        }
    }
}

@Composable
private fun AiMealDateStrip(
    bookedDates: List<String>,
    selectedDate: String?,
    onSelect: (String?) -> Unit,
) {
    val today = java.time.LocalDate.now()
    val dates = (0L until 14L).map { offset ->
        val date = today.plusDays(offset)
        Triple(
            date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
            date.dayOfMonth,
            weekdayShortFromDayOfWeek(date.dayOfWeek),
        )
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(dates) { (iso, day, weekday) ->
            val isBooked = iso in bookedDates
            val isSelected = iso == selectedDate
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(70.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        when {
                            isSelected -> Coral
                            isBooked -> Color(0xFFF3F4F6)
                            else -> Color.White
                        },
                    )
                    .border(
                        1.5.dp,
                        if (isSelected) Coral else Color(0xFFEDEDEF),
                        RoundedCornerShape(14.dp),
                    )
                    .alpha(if (isBooked && !isSelected) 0.55f else 1f)
                    .clickable { onSelect(if (isSelected) null else iso) },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        weekday,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color.White else MealMutedGray,
                    )
                    Text(
                        "$day",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            isSelected -> Color.White
                            isBooked -> MealMutedGray
                            else -> DeepInk
                        },
                    )
                    if (isBooked && !isSelected) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = MealMutedGray,
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun AiMealWeekdayChips(
    bookedDays: List<String>,
    selectedDays: List<String>,
    onToggle: (String) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(WeekdayPairs) { (wire, label) ->
            val isBooked = wire in bookedDays
            val isSelected = wire in selectedDays
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        when {
                            isSelected -> Coral
                            isBooked -> Color(0xFFF3F4F6)
                            else -> Color.White
                        },
                    )
                    .border(
                        1.5.dp,
                        if (isSelected) Coral else Color(0xFFEDEDEF),
                        RoundedCornerShape(50),
                    )
                    .alpha(if (isBooked && !isSelected) 0.55f else 1f)
                    .clickable { onToggle(wire) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = when {
                            isSelected -> Color.White
                            isBooked -> MealMutedGray
                            else -> DeepInk
                        },
                    )
                    if (isBooked && !isSelected) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = MealMutedGray,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    color: Color = Coral,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(color.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled) {

                focusManager.clearFocus()
                keyboardController?.hide()
                onClick()
            }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}
