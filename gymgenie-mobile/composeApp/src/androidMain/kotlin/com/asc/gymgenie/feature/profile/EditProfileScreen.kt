package com.asc.gymgenie.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.feature.profile.components.ProfileBorder
import com.asc.gymgenie.feature.profile.components.SettingsGroupCard
import com.asc.gymgenie.feature.profile.components.SettingsRow
import com.asc.gymgenie.presentation.ProfileViewModel
import com.asc.gymgenie.ui.components.GymGenieToolbar
import com.asc.gymgenie.ui.components.ToolbarAction
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.user.UpdateUserProfileRequest
import com.asc.gymgenie.user.UserProfileStore
import org.koin.core.context.GlobalContext

private val EditBlack = Color(0xFF0A0A0A)
private val EditBorder = Color(0xFFEDEDEF)
private val EditMuted = Color(0xFF8B8B92)

@Composable
fun EditProfileScreen(
    profileViewModel: ProfileViewModel,
    form: EditFormHolder,
    onOpenMetrics: () -> Unit,
    onOpenExperience: () -> Unit,
    onOpenHealth: () -> Unit,
    onBack: () -> Unit,
) {
    val koin = remember { GlobalContext.get() }
    val userProfileStore = remember { koin.get<UserProfileStore>() }
    val profile by userProfileStore.profile.collectAsState()
    val updateState by profileViewModel.state.collectAsState()

    // Seed the shared form holder from the loaded profile exactly once.
    // After the first initialization, in-flight edits in sub-screens must not be clobbered when
    // the user returns and the profile flow re-emits.
    LaunchedEffect(profile) {
        val loaded = profile
        if (!form.initialized && loaded != null) {
            form.firstName = loaded.firstName ?: ""
            form.lastName = loaded.lastName ?: ""
            form.weightKg = loaded.weightKg?.toInt() ?: 70
            form.heightCm = loaded.heightCm?.toInt() ?: 175
            form.ageYears = loaded.ageYears ?: 25
            form.experience = loaded.experience ?: "Недавно"
            form.frequency = loaded.frequency ?: "Редко"
            form.hasHealthIssues = !loaded.healthIssues.isNullOrBlank()
            form.healthIssues = loaded.healthIssues ?: ""
            form.initialized = true
        }
    }

    LaunchedEffect(updateState.success) {
        if (updateState.success) {
            profileViewModel.consumeSuccess()
            form.initialized = false
            onBack()
        }
    }

    fun save() {
        if (!form.initialized) return
        profileViewModel.updateProfile(
            UpdateUserProfileRequest(
                firstName = form.firstName.takeIf { it.isNotBlank() },
                lastName = form.lastName.takeIf { it.isNotBlank() },
                weightKg = form.weightKg.toDouble(),
                heightCm = form.heightCm.toDouble(),
                ageYears = form.ageYears,
                experience = form.experience,
                frequency = form.frequency,
                healthIssues = if (form.hasHealthIssues && form.healthIssues.isNotBlank()) form.healthIssues else null,
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        GymGenieToolbar(
            title = "Редактировать",
            showBackNavigation = true,
            onBackClick = onBack,
            actions = listOf(
                ToolbarAction(
                    content = {
                        if (updateState.isLoading) {
                            CircularProgressIndicator(
                                color = Coral,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = "Сохранить",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Normal,
                            )
                        }
                    },
                    onClick = { if (!updateState.isLoading) save() },
                ),
            ),
        )

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
                val name = "${form.firstName} ${form.lastName}".trim()
                EditAvatar(name = name.ifEmpty { "?" }, size = 92)
            }

            EditFieldLabel("ИМЯ")
            EditInputField(
                value = form.firstName,
                onValueChange = { form.firstName = it },
                placeholder = "Введите имя",
            )

            EditFieldLabel("ФАМИЛИЯ")
            EditInputField(
                value = form.lastName,
                onValueChange = { form.lastName = it },
                placeholder = "Введите фамилию",
            )

            EditFieldLabel("EMAIL")
            EditInputField(
                value = profile?.email ?: "",
                onValueChange = {},
                placeholder = "email@example.com",
                enabled = false,
            )

            EditFieldLabel("БЕЗОПАСНОСТЬ")
            SettingsGroupCard {
                SettingsRow(label = "Сменить пароль", icon = Icons.Filled.Lock, onClick = {})
            }

            EditFieldLabel("ПАРАМЕТРЫ")
            SettingsGroupCard {
                SettingsRow(
                    icon = Icons.Filled.Person,
                    label = "Вес, рост, возраст",
                    value = "${form.weightKg} · ${form.heightCm} · ${form.ageYears}",
                    onClick = onOpenMetrics,
                )
                HorizontalDivider(color = ProfileBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Filled.FitnessCenter,
                    label = "Опыт и частота",
                    value = form.experience,
                    onClick = onOpenExperience,
                )
                HorizontalDivider(color = ProfileBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.Help,
                    label = "Здоровье",
                    value = if (form.hasHealthIssues) "Указано" else "Нет огранич.",
                    onClick = onOpenHealth,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 12.dp),
        ) {
            val saveError = updateState.error
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
                onClick = ::save,
                enabled = !updateState.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Coral),
            ) {
                if (updateState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text(text = "Сохранить изменения", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun StepBottomButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Coral),
        ) {
            Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = EditBlack,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
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
        textStyle = TextStyle(fontSize = 17.sp),
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
internal fun SliderField(
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
    var fieldHadFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    fun commitInput() {
        val digits = inputText.filter { it.isDigit() }
        val parsed = digits.toIntOrNull()
        if (parsed != null) {
            onChange(parsed.coerceIn(min, max))
        }
        fieldHadFocus = false
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
                            if (focusState.isFocused) {
                                fieldHadFocus = true
                            } else if (fieldHadFocus) {
                                commitInput()
                            }
                        },
                )
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            } else {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 60.dp, minHeight = 48.dp)
                        .pointerInput(value) {
                            detectTapGestures(onTap = {
                                inputText = if (value == 0) "" else value.toString()
                                isEditing = true
                            })
                        },
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text(
                        if (value == 0) "—" else value.toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepInk,
                    )
                }
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
                    .background(Color(0xFFE0E0E0)),
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
internal fun PickerGroup(
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White else EditBlack,
                )
            }
        }
    }
}

