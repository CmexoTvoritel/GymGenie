package com.asc.gymgenie.feature.activities

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.presentation.ActivitiesViewModel
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.utils.WeekdayLabelsRu
import com.asc.gymgenie.utils.backendToDayLabel
import com.asc.gymgenie.utils.dayLabelToBackend
import org.koin.core.context.GlobalContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val daysOfWeek = WeekdayLabelsRu

private enum class ScheduleMode { EVERY_DAY, RECURRING, ONE_TIME }

@Composable
fun ActivityScheduleSettingsScreen(
    activityId: String,
    activityName: String,
    initialScheduleType: String?,
    initialScheduleDays: List<String>,
    initialOneOffDate: String?,
    onBack: () -> Unit,
) {
    val koin = remember { GlobalContext.get() }
    val viewModel = remember { koin.get<ActivitiesViewModel>() }
    val state by viewModel.state.collectAsState()

    val initialMode = remember {
        when (initialScheduleType) {
            "RECURRING" -> ScheduleMode.RECURRING
            "ONE_TIME" -> ScheduleMode.ONE_TIME
            else -> ScheduleMode.EVERY_DAY
        }
    }
    val initialDayLabels = remember {
        initialScheduleDays.mapNotNull { backendToDayLabel[it] }.toSet()
    }

    var scheduleMode by rememberSaveable { mutableStateOf(initialMode) }
    var selectedDays by rememberSaveable { mutableStateOf(initialDayLabels.ifEmpty { daysOfWeek.toSet() }) }
    var oneOffDate by rememberSaveable { mutableStateOf(initialOneOffDate ?: "") }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 24.dp),
        ) {
            ScheduleTopBar(onBack = onBack)

            Spacer(modifier = Modifier.height(8.dp))

            ScheduleHeroSection(title = activityName)

            Spacer(modifier = Modifier.height(24.dp))

            ScheduleModeCard(
                selectedMode = scheduleMode,
                onModeSelected = { scheduleMode = it },
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (scheduleMode) {
                ScheduleMode.EVERY_DAY -> {

                }

                ScheduleMode.RECURRING -> {
                    ScheduleDaysOfWeekCard(
                        selectedDays = selectedDays,
                        onToggleDay = { day ->
                            selectedDays = if (day in selectedDays && selectedDays.size > 1) {
                                selectedDays - day
                            } else {
                                selectedDays + day
                            }
                        },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                ScheduleMode.ONE_TIME -> {
                    OneOffDateCard(
                        selectedDate = oneOffDate,
                        onDateSelected = { oneOffDate = it },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (state.scheduleUpdateError != null) {
                Text(
                    text = state.scheduleUpdateError ?: "",
                    color = Color.Red.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val (type, days, date) = resolveScheduleParams(
                        scheduleMode, selectedDays, oneOffDate,
                    )
                    viewModel.updateSchedule(
                        activityId = activityId,
                        scheduleType = type,
                        scheduleDays = days,
                        oneOffDate = date,
                    )
                    onBack()
                },
                enabled = isFormValid(scheduleMode, selectedDays, oneOffDate)
                        && !state.isScheduleUpdating,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentOrange,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(0.dp),
            ) {
                if (state.isScheduleUpdating) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "Сохранить расписание",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Text(
                    text = "Отмена",
                    color = MutedText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

private fun resolveScheduleParams(
    mode: ScheduleMode,
    selectedDays: Set<String>,
    oneOffDate: String,
): Triple<String?, List<String>, String?> = when (mode) {
    ScheduleMode.EVERY_DAY -> Triple(null, emptyList(), null)
    ScheduleMode.RECURRING -> {
        val backendDays = selectedDays.mapNotNull { dayLabelToBackend[it] }
        Triple("RECURRING", backendDays, null)
    }
    ScheduleMode.ONE_TIME -> Triple("ONE_TIME", emptyList(), oneOffDate)
}

private fun isFormValid(
    mode: ScheduleMode,
    selectedDays: Set<String>,
    oneOffDate: String,
): Boolean = when (mode) {
    ScheduleMode.EVERY_DAY -> true
    ScheduleMode.RECURRING -> selectedDays.isNotEmpty()
    ScheduleMode.ONE_TIME -> oneOffDate.isNotBlank()
}

@Composable
private fun ScheduleTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Расписание",
            color = DeepInk,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "‹",
                color = DeepInk,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ScheduleHeroSection(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            color = DeepInk,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ScheduleModeCard(
    selectedMode: ScheduleMode,
    onModeSelected: (ScheduleMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScheduleWhiteCard(modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Тип расписания",
                color = DeepInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            ScheduleMode.entries.forEach { mode ->
                val label = when (mode) {
                    ScheduleMode.EVERY_DAY -> "Каждый день"
                    ScheduleMode.RECURRING -> "По дням недели"
                    ScheduleMode.ONE_TIME -> "Один раз"
                }
                val isSelected = mode == selectedMode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onModeSelected(mode) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .then(
                                if (isSelected) Modifier.background(AccentOrange)
                                else Modifier.border(1.5.dp, MutedText.copy(alpha = 0.4f), CircleShape)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = label,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) DeepInk else MutedText,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleDaysOfWeekCard(
    selectedDays: Set<String>,
    onToggleDay: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScheduleWhiteCard(modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Дни недели",
                color = DeepInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                daysOfWeek.forEach { day ->
                    ScheduleDayChip(
                        label = day,
                        isSelected = day in selectedDays,
                        onClick = { onToggleDay(day) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleDayChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val baseModifier = Modifier
        .size(36.dp)
        .clip(CircleShape)

    val styledModifier = if (isSelected) {
        baseModifier.background(AccentOrange)
    } else {
        baseModifier.border(1.dp, MutedText.copy(alpha = 0.35f), CircleShape)
    }

    Box(
        modifier = styledModifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else DeepInk,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun OneOffDateCard(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val displayDate = remember(selectedDate) {
        if (selectedDate.isBlank()) "Выбрать дату"
        else {
            runCatching {
                val ld = LocalDate.parse(selectedDate)
                val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))
                ld.format(fmt)
            }.getOrDefault(selectedDate)
        }
    }

    ScheduleWhiteCard(modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Дата",
                color = DeepInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MutedText.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .clickable {
                        val today = LocalDate.now()
                        val initial = if (selectedDate.isNotBlank()) {
                            runCatching { LocalDate.parse(selectedDate) }.getOrDefault(today)
                        } else today

                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val picked = LocalDate.of(year, month + 1, day)
                                onDateSelected(picked.toString())
                            },
                            initial.year,
                            initial.monthValue - 1,
                            initial.dayOfMonth,
                        ).apply {
                            datePicker.minDate = System
                                .currentTimeMillis()
                        }.show()
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    text = displayDate,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selectedDate.isBlank()) MutedText else DeepInk,
                )
            }
        }
    }
}

@Composable
private fun ScheduleWhiteCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = DeepInk.copy(alpha = 0.06f),
                spotColor = DeepInk.copy(alpha = 0.08f),
            )
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White),
    ) {
        content()
    }
}
