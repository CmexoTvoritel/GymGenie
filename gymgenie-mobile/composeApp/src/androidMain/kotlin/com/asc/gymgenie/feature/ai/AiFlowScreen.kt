package com.asc.gymgenie.feature.ai

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.asc.gymgenie.R
import com.asc.gymgenie.ai.AiChatMessage
import com.asc.gymgenie.ai.AiFlowStep
import com.asc.gymgenie.ai.AiProfileData
import com.asc.gymgenie.ai.AiViewModel
import com.asc.gymgenie.feature.nutrition.AiMealFlowScreen
import com.asc.gymgenie.ui.components.GymGenieToolbar
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.WarmOffWhite
import kotlin.math.roundToInt
import org.koin.core.context.GlobalContext

private val OffWhite = WarmOffWhite
private val CardBg = Color(0xFFFFFFFF)
private val BorderGray = Color(0xFFE0E0E0)
private val MutedGray = Color(0xFF888888)
private val GreenSave = Color(0xFF22C55E)
private val GreenLight = Color(0xFFF0FDF4)
private val GreenText = Color(0xFF16A34A)

@Composable
fun AiFlowScreen(onBottomBarVisibilityChanged: (Boolean) -> Unit = {}) {
    val koin = remember { GlobalContext.get() }
    val viewModel = remember { koin.get<AiViewModel>() }
    DisposableEffect(Unit) { onDispose { viewModel.onCleared() } }

    val state by viewModel.state.collectAsState()

    BackHandler(enabled = state.step != AiFlowStep.CHOOSE) {
        viewModel.goBack()
    }

    LaunchedEffect(state.step) {
        onBottomBarVisibilityChanged(state.step == AiFlowStep.CHOOSE)
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
        label = "ai_step",
    ) { step ->
        when (step) {
            AiFlowStep.CHOOSE -> ChooseScreen(
                onNext = { viewModel.goTo(AiFlowStep.PROFILE) },
                onBottomBarVisibilityChanged = onBottomBarVisibilityChanged,
            )
            AiFlowStep.PROFILE -> ProfileScreen(
                profile = state.profile,
                onBack = { viewModel.goBack() },
                onNext = { viewModel.goTo(AiFlowStep.EXPERIENCE) },
                onProfileChange = { viewModel.updateProfile { _ -> it } },
            )
            AiFlowStep.EXPERIENCE -> ExperienceScreen(
                profile = state.profile,
                onBack = { viewModel.goBack() },
                onNext = { viewModel.goTo(AiFlowStep.HEALTH) },
                onProfileChange = { viewModel.updateProfile { _ -> it } },
            )
            AiFlowStep.HEALTH -> HealthScreen(
                profile = state.profile,
                onBack = { viewModel.goBack() },
                onNext = { viewModel.goTo(AiFlowStep.CHAT) },
                onProfileChange = { viewModel.updateProfile { _ -> it } },
            )
            AiFlowStep.CHAT -> ChatScreen(
                messages = state.messages,
                isTyping = state.isTyping,
                hasWorkout = state.lastWorkout != null,
                savedPlanId = state.savedPlanId,
                isSaving = state.isSaving,
                isSaved = state.isSaved,
                errorMessage = state.errorMessage,
                onBack = { viewModel.goBack() },
                onSend = { viewModel.sendMessage(it) },
                onSave = { viewModel.saveWorkout() },
            )
        }
    }
}

@Composable
private fun ChooseScreen(
    onNext: () -> Unit,
    onBottomBarVisibilityChanged: (Boolean) -> Unit,
) {
    // Local presentation flag for the AI meal flow. The meal flow is
    // entirely self-contained (`AiMealFlowScreen`) and renders its own
    // header with a close button, so we just gate it on a boolean and
    // overlay it on top of the chooser.
    var showMealFlow by remember { mutableStateOf(false) }

    BackHandler(enabled = showMealFlow) {
        showMealFlow = false
    }

    LaunchedEffect(showMealFlow) {
        onBottomBarVisibilityChanged(!showMealFlow)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .navigationBarsPadding(),
    ) {
        GymGenieToolbar(title = "ИИ тренер")

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(40.dp))
            Text(
                "Что вы хотите сгенерировать?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(32.dp))
            GenerateTypeCard(
                emoji = "🏋️",
                title = "План тренировки",
                subtitle = "Персональная программа на основе ваших данных",
                enabled = true,
                onClick = onNext,
            )
            Spacer(Modifier.height(14.dp))
            GenerateTypeCard(
                emoji = "🥗",
                title = "План питания",
                subtitle = "Персональный рацион на основе ваших целей",
                enabled = true,
                onClick = { showMealFlow = true },
            )
        }
    }

    AnimatedVisibility(
        visible = showMealFlow,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
    ) {
        AiMealFlowScreen(
            onDismiss = { showMealFlow = false },
        )
    }
}

@Composable
private fun GenerateTypeCard(
    emoji: String,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    var hovered by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(
                width = 2.dp,
                color = if (enabled && hovered) Coral else BorderGray,
                shape = RoundedCornerShape(16.dp),
            )
            .then(
                if (enabled) Modifier.clickable {
                    hovered = false
                    onClick()
                } else Modifier
            )
            .alpha(if (enabled) 1f else 0.45f)
            .padding(24.dp),
    ) {
        Column {
            Text(emoji, fontSize = 30.sp)
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = DeepInk)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, fontSize = 15.sp, color = MutedGray)
        }
    }
}

@Composable
private fun ProfileScreen(
    profile: AiProfileData,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onProfileChange: (AiProfileData) -> Unit,
) {
    val canProceed = profile.isProfileFilled
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .navigationBarsPadding(),
    ) {
        GymGenieToolbar(
            title = "План тренировки",
            showBackNavigation = true,
            onBackClick = onBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text("Введите данные профиля", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DeepInk)
            Spacer(Modifier.height(24.dp))
            SliderField(label = "Возраст", value = profile.age, min = 10, max = 80) { v ->
                onProfileChange(profile.copy(age = v))
            }
            SliderField(label = "Рост (см)", value = profile.height, min = 100, max = 220) { v ->
                onProfileChange(profile.copy(height = v))
            }
            SliderField(label = "Вес (кг)", value = profile.weight, min = 30, max = 150) { v ->
                onProfileChange(profile.copy(weight = v))
            }
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
                    .background(BorderGray),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExperienceScreen(
    profile: AiProfileData,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onProfileChange: (AiProfileData) -> Unit,
) {
    val canProceed = profile.isExperienceFilled
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .navigationBarsPadding(),
    ) {
        GymGenieToolbar(
            title = "План тренировки",
            showBackNavigation = true,
            onBackClick = onBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text("Ваш опыт", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DeepInk)
            Spacer(Modifier.height(24.dp))
            Text(
                "Как давно вы занимаетесь спортом?",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF555555),
            )
            Spacer(Modifier.height(8.dp))
            ChipGroup(
                options = listOf("Давно", "Недавно", "Не занимался"),
                selected = profile.experience,
                onSelect = { onProfileChange(profile.copy(experience = it)) },
            )
            Spacer(Modifier.height(28.dp))
            Text(
                "Как часто вы занимаетесь спортом?",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF555555),
            )
            Spacer(Modifier.height(8.dp))
            ChipGroup(
                options = listOf("Часто", "Редко", "Не занимался"),
                selected = profile.frequency,
                onSelect = { onProfileChange(profile.copy(frequency = it)) },
            )
        }
        PrimaryButton(text = "Далее", enabled = canProceed, onClick = onNext)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HealthScreen(
    profile: AiProfileData,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onProfileChange: (AiProfileData) -> Unit,
) {
    val canProceed = profile.isHealthFilled
    var textareaFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .navigationBarsPadding()
            .imePadding()
            // Tap on any non-interactive area (paddings, labels, screen
            // background) clears focus from the limitations text field. The
            // chips, BasicTextField, header back button, and primary CTA
            // dismiss the keyboard from their own click handlers.
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            },
    ) {
        GymGenieToolbar(
            title = "План тренировки",
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
            Text("Здоровье", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DeepInk)
            Spacer(Modifier.height(24.dp))
            Text(
                "Есть ли у вас ограничения по здоровью?",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF555555),
            )
            Spacer(Modifier.height(8.dp))
            ChipGroup(
                options = listOf(AiProfileData.HEALTH_YES, AiProfileData.HEALTH_NO),
                selected = profile.hasLimitations,
                onSelect = { choice ->
                    onProfileChange(
                        profile.copy(
                            hasLimitations = choice,
                            limitationsDesc = if (choice == AiProfileData.HEALTH_NO) "" else profile.limitationsDesc,
                        ),
                    )
                },
            )

            if (profile.hasLimitations == AiProfileData.HEALTH_YES) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Опишите ваши ограничения",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF555555),
                )
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = profile.limitationsDesc,
                    onValueChange = { onProfileChange(profile.copy(limitationsDesc = it)) },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = DeepInk,
                    ),
                    cursorBrush = SolidColor(Coral),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBg)
                        .onFocusChanged { textareaFocused = it.isFocused }
                        .border(
                            width = 2.dp,
                            color = if (textareaFocused) Coral else BorderGray,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(14.dp),
                    decorationBox = { inner ->
                        if (profile.limitationsDesc.isEmpty()) {
                            Text(
                                "Например: грыжа позвоночника",
                                fontSize = 16.sp,
                                color = Color(0xFFAAAAAA),
                            )
                        }
                        inner()
                    },
                )
            }
        }
        PrimaryButton(text = "Далее", enabled = canProceed, onClick = onNext)
    }
}

@Composable
private fun ChatScreen(
    messages: List<AiChatMessage>,
    isTyping: Boolean,
    hasWorkout: Boolean,
    savedPlanId: String?,
    isSaving: Boolean,
    isSaved: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onSave: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty() || isTyping) {
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        GymGenieToolbar(
            title = "AI Тренер",
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
                // Tapping a chat bubble or any empty space in the messages
                // list clears focus from the input field. The input field
                // itself and the send button consume their own taps.
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
                            "Опишите какую тренировку вы хотите получить",
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
            items(messages) { msg ->
                ChatBubble(message = msg)
            }
            if (isTyping) {
                item { TypingBubble() }
            }
        }

        errorMessage?.let {
            Text(
                it,
                fontSize = 13.sp,
                color = Color.Red,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        if (hasWorkout && !isSaved) {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                PrimaryButton(
                    text = when {
                        isSaving -> "Сохранение..."
                        savedPlanId != null -> "✓ Обновить тренировку"
                        else -> "✓ Добавить тренировку"
                    },
                    enabled = !isSaving,
                    color = GreenSave,
                    onClick = onSave,
                )
            }
        }

        if (isSaved) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(GreenLight)
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Тренировка добавлена! ✓",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenText,
                )
            }
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
                    .background(CardBg)
                    .border(
                        width = 2.dp,
                        color = if (inputFocused) Coral else BorderGray,
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
                    .background(if (input.isNotBlank()) Coral else BorderGray)
                    .clickable(enabled = input.isNotBlank() && !isTyping) {
                        // Drop focus on send so the keyboard collapses; the
                        // user can tap the field again to compose a follow-up.
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onSend(input)
                        input = ""
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("↑", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ChatBubble(message: AiChatMessage) {
    val isUser = message.role == AiChatMessage.Role.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isUser) Coral else CardBg)
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
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
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
                label = "dot_$idx",
            )
            Text("●", fontSize = 14.sp, color = Color(0xFFAAAAAA).copy(alpha = alpha))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroup(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            val active = selected == opt
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (active) Coral else CardBg)
                    .border(
                        width = 2.dp,
                        color = if (active) Coral else BorderGray,
                        shape = RoundedCornerShape(24.dp),
                    )
                    .clickable {
                        // Picking a chip should clear focus from any sibling
                        // text input (e.g. the health limitations field on
                        // the health step).
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onSelect(opt)
                    }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Text(
                    opt,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) Color.White else DeepInk,
                )
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
                // The CTA always advances/saves — clear focus first so the
                // keyboard doesn't sit on top of the next state (next step
                // or "saved" success card).
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
