package com.asc.gymgenie.feature.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.presentation.ProfileViewModel
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.user.UpdateUserProfileRequest
import com.asc.gymgenie.user.UserProfileStore

private val EditBlack = Color(0xFF0A0A0A)
private val EditBorder = Color(0xFFEDEDEF)
private val EditMuted = Color(0xFF8B8B92)
private val EditSoft = Color(0xFFF4F4F6)

@Composable
fun EditProfileScreen(
    userProfileStore: UserProfileStore,
    profileViewModel: ProfileViewModel,
    onBack: () -> Unit,
) {
    val profile by userProfileStore.profile.collectAsState()
    val updateState by profileViewModel.state.collectAsState()

    var step by rememberSaveable { mutableIntStateOf(0) }
    var firstName by rememberSaveable { mutableStateOf(profile?.firstName ?: "") }
    var lastName by rememberSaveable { mutableStateOf(profile?.lastName ?: "") }
    var weightKg by rememberSaveable { mutableIntStateOf(profile?.weightKg?.toInt() ?: 70) }
    var heightCm by rememberSaveable { mutableIntStateOf(profile?.heightCm?.toInt() ?: 175) }
    var ageYears by rememberSaveable { mutableIntStateOf(profile?.ageYears ?: 25) }
    var experience by rememberSaveable { mutableStateOf(profile?.experience ?: "Недавно") }
    var frequency by rememberSaveable { mutableStateOf(profile?.frequency ?: "Редко") }
    var hasHealthIssues by rememberSaveable { mutableStateOf(!profile?.healthIssues.isNullOrBlank()) }
    var healthIssues by rememberSaveable { mutableStateOf(profile?.healthIssues ?: "") }

    LaunchedEffect(updateState.success) {
        if (updateState.success) {
            profileViewModel.consumeSuccess()
            onBack()
        }
    }

    fun save() {
        profileViewModel.updateProfile(
            UpdateUserProfileRequest(
                firstName = firstName.takeIf { it.isNotBlank() },
                lastName = lastName.takeIf { it.isNotBlank() },
                weightKg = weightKg.toDouble(),
                heightCm = heightCm.toDouble(),
                ageYears = ageYears,
                experience = experience,
                frequency = frequency,
                healthIssues = if (hasHealthIssues && healthIssues.isNotBlank()) healthIssues else null,
            )
        )
    }

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            if (targetState > initialState) {
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
            } else {
                slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
            }
        },
        modifier = Modifier.fillMaxSize(),
        label = "edit_step",
    ) { currentStep ->
        when (currentStep) {
            0 -> EditMainStep(
                firstName = firstName,
                lastName = lastName,
                onFirstNameChange = { firstName = it },
                onLastNameChange = { lastName = it },
                email = profile?.email ?: "",
                weightKg = weightKg,
                heightCm = heightCm,
                ageYears = ageYears,
                experience = experience,
                hasHealthIssues = hasHealthIssues,
                isLoading = updateState.isLoading,
                saveError = updateState.error,
                onBack = onBack,
                onSave = ::save,
                onNavigateTo = { step = it },
            )
            1 -> EditMetricsStep(
                weightKg = weightKg,
                heightCm = heightCm,
                ageYears = ageYears,
                onWeightChange = { weightKg = it },
                onHeightChange = { heightCm = it },
                onAgeChange = { ageYears = it },
                onBack = { step = 0 },
            )
            2 -> EditExperienceStep(
                experience = experience,
                frequency = frequency,
                onExperienceChange = { experience = it },
                onFrequencyChange = { frequency = it },
                onBack = { step = 0 },
            )
            3 -> EditHealthStep(
                hasHealthIssues = hasHealthIssues,
                healthIssues = healthIssues,
                onHasHealthIssuesChange = { hasHealthIssues = it },
                onHealthIssuesChange = { healthIssues = it },
                onBack = { step = 0 },
            )
        }
    }
}

@Composable
private fun EditMainStep(
    firstName: String,
    lastName: String,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    email: String,
    weightKg: Int,
    heightCm: Int,
    ageYears: Int,
    experience: String,
    hasHealthIssues: Boolean,
    isLoading: Boolean,
    saveError: String?,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onNavigateTo: (Int) -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = EditBlack,
                    )
                }
                Text(
                    text = "Редактировать",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EditBlack,
                )
            }
            TextButton(onClick = { onSave() }, enabled = !isLoading) {
                Text(text = "Сохр.", color = Coral, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                val name = "$firstName $lastName".trim()
                EditAvatar(name = name.ifEmpty { "?" }, size = 92)
            }

            EditFieldLabel("ИМЯ")
            EditInputField(
                value = firstName,
                onValueChange = onFirstNameChange,
                placeholder = "Введите имя",
            )

            EditFieldLabel("ФАМИЛИЯ")
            EditInputField(
                value = lastName,
                onValueChange = onLastNameChange,
                placeholder = "Введите фамилию",
            )

            EditFieldLabel("EMAIL")
            EditInputField(
                value = email,
                onValueChange = {},
                placeholder = "email@example.com",
                enabled = false,
            )

            EditFieldLabel("БЕЗОПАСНОСТЬ")
            EditCardRow(
                icon = Icons.Filled.Lock,
                label = "Сменить пароль",
                onClick = {},
            )

            EditFieldLabel("ПАРАМЕТРЫ")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.dp, EditBorder, RoundedCornerShape(18.dp)),
            ) {
                EditNavRow(
                    icon = Icons.Filled.Person,
                    label = "Вес, рост, возраст",
                    value = "$weightKg · $heightCm · $ageYears",
                    onClick = { onNavigateTo(1) },
                )
                HorizontalDivider(color = EditBorder, modifier = Modifier.padding(horizontal = 16.dp))
                EditNavRow(
                    icon = Icons.Filled.FitnessCenter,
                    label = "Опыт и частота",
                    value = experience,
                    onClick = { onNavigateTo(2) },
                )
                HorizontalDivider(color = EditBorder, modifier = Modifier.padding(horizontal = 16.dp))
                EditNavRow(
                    icon = Icons.AutoMirrored.Filled.Help,
                    label = "Здоровье",
                    value = if (hasHealthIssues) "Указано" else "Нет огранич.",
                    onClick = { onNavigateTo(3) },
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
            if (saveError != null) {
                Text(
                    text = saveError,
                    fontSize = 13.sp,
                    color = Color(0xFFE5484D),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
            }
            Button(
                onClick = onSave,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Coral),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text(text = "Сохранить изменения", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EditMetricsStep(
    weightKg: Int,
    heightCm: Int,
    ageYears: Int,
    onWeightChange: (Int) -> Unit,
    onHeightChange: (Int) -> Unit,
    onAgeChange: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .statusBarsPadding(),
    ) {
        StepHeader(title = "Параметры", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            SliderField(label = "Возраст", value = ageYears, min = 10, max = 80) { v -> onAgeChange(v) }
            SliderField(label = "Рост (см)", value = heightCm, min = 100, max = 220) { v -> onHeightChange(v) }
            SliderField(label = "Вес (кг)", value = weightKg, min = 30, max = 150) { v -> onWeightChange(v) }
            Spacer(modifier = Modifier.height(20.dp))
        }
        StepBottomButton(text = "Готово", onClick = onBack)
    }
}

@Composable
private fun EditExperienceStep(
    experience: String,
    frequency: String,
    onExperienceChange: (String) -> Unit,
    onFrequencyChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .statusBarsPadding(),
    ) {
        StepHeader(title = "Опыт", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            EditFieldLabel("КАК ДАВНО ВЫ ЗАНИМАЕТЕСЬ")
            PickerGroup(
                options = listOf("Давно", "Недавно", "Не занимался"),
                selected = experience,
                onSelect = onExperienceChange,
            )
            Spacer(modifier = Modifier.height(24.dp))
            EditFieldLabel("КАК ЧАСТО ВЫ ЗАНИМАЕТЕСЬ")
            PickerGroup(
                options = listOf("Часто", "Редко", "Не занимался"),
                selected = frequency,
                onSelect = onFrequencyChange,
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
        StepBottomButton(text = "Готово", onClick = onBack)
    }
}

@Composable
private fun EditHealthStep(
    hasHealthIssues: Boolean,
    healthIssues: String,
    onHasHealthIssuesChange: (Boolean) -> Unit,
    onHealthIssuesChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .statusBarsPadding(),
    ) {
        StepHeader(title = "Здоровье", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            EditFieldLabel("ОГРАНИЧЕНИЯ ПО ЗДОРОВЬЮ?")
            PickerGroup(
                options = listOf("Да", "Нет"),
                selected = if (hasHealthIssues) "Да" else "Нет",
                onSelect = { selected ->
                    onHasHealthIssuesChange(selected == "Да")
                    if (selected == "Нет") onHealthIssuesChange("")
                },
            )
            if (hasHealthIssues) {
                Spacer(modifier = Modifier.height(24.dp))
                EditFieldLabel("ОПИШИТЕ ПОДРОБНЕЕ")
                OutlinedTextField(
                    value = healthIssues,
                    onValueChange = onHealthIssuesChange,
                    placeholder = { Text("Например: грыжа позвоночника", color = EditMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Coral,
                        unfocusedBorderColor = EditBorder,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        StepBottomButton(text = "Готово", onClick = onBack)
    }
}

@Composable
private fun StepHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = EditBlack)
        }
        Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = EditBlack)
    }
}

@Composable
private fun StepBottomButton(text: String, onClick: () -> Unit) {
    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Coral),
        ) {
            Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EditAvatar(name: String, size: Int) {
    val initials = name.trim()
        .split("\\s+".toRegex())
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Coral, Color(0xFFFF8A6E)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initials, color = Color.White, fontSize = (size * 0.36f).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EditFieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = EditMuted,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun EditInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = EditMuted) },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Coral,
            unfocusedBorderColor = EditBorder,
            disabledBorderColor = EditBorder,
            disabledTextColor = EditMuted,
        ),
    )
}

@Composable
private fun EditCardRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, EditBorder, RoundedCornerShape(18.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(EditSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = EditBlack, modifier = Modifier.size(18.dp))
            }
            Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = EditBlack, modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFFC8C8CE), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun EditNavRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(EditSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = EditBlack, modifier = Modifier.size(18.dp))
        }
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = EditBlack, modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 14.sp, color = EditMuted)
        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFFC8C8CE), modifier = Modifier.size(18.dp))
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
    val density = LocalDensity.current
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
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF555555))
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
                    .height(4.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE0E0E0)),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .height(4.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Coral),
            )
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.CenterStart)
                    .then(
                        with(density) {
                            val offset = ((pct * (trackWidthPx - 20.dp.toPx()))).toDp()
                            Modifier.padding(start = offset.coerceAtLeast(0.dp))
                        },
                    )
                    .clip(CircleShape)
                    .background(Coral),
            )
        }

        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(min.toString(), fontSize = 11.sp, color = Color(0xFFAAAAAA))
            Text(max.toString(), fontSize = 11.sp, color = Color(0xFFAAAAAA))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PickerGroup(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 6.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isSelected) Coral else Color.White)
                    .border(
                        width = if (isSelected) 2.dp else 1.5.dp,
                        color = if (isSelected) Coral else EditBorder,
                        shape = RoundedCornerShape(24.dp),
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White else EditBlack,
                )
            }
        }
    }
}
