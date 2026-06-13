package com.asc.gymgenie.feature.workout_history.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.utils.MonthNamesNominative
import com.asc.gymgenie.utils.weekdayShortFromKotlinxDayOfWeek
import com.asc.gymgenie.workout.WorkoutSessionHistoryItem
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

private val InkBlack = Color(0xFF0A0A0A)
private val BorderGray = Color(0xFFEDEDEF)
private val MutedGray = Color(0xFF8B8B92)
private val CompletedAccent = Color(0xFF22A06B)
private val IncompleteAccent = Color(0xFFE89B12)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelector(
    selectedDate: LocalDate,
    today: LocalDate,
    weekDates: List<LocalDate>,
    weekSessions: Map<String, List<WorkoutSessionHistoryItem>>,
    onDateSelected: (LocalDate) -> Unit,
    onShiftWeek: (Int) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        val dateLabel = when {
            selectedDate == today -> "Сегодня"
            selectedDate == today.minus(1, DateTimeUnit.DAY) -> "Вчера"
            else -> "${selectedDate.dayOfMonth} ${MonthNamesNominative.getOrElse(selectedDate.monthNumber) { "" }.lowercase()}"
        }
        val monthName = MonthNamesNominative.getOrElse(selectedDate.monthNumber) { "" }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = dateLabel,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = InkBlack,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$monthName ${selectedDate.year}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MutedGray,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onShiftWeek(-1) },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Предыдущая неделя",
                    tint = InkBlack,
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                weekDates.forEach { date ->
                    val isSelected = date == selectedDate
                    val isToday = date == today
                    val isFuture = date > today
                    val daySessions = weekSessions[date.toString()] ?: emptyList()
                    val hasCompleted = daySessions.any { it.status == "COMPLETED" }
                    val hasCancelled = daySessions.any { it.status == "CANCELLED" }

                    WeekDayButton(
                        date = date,
                        isSelected = isSelected,
                        isToday = isToday,
                        isFuture = isFuture,
                        hasCompleted = hasCompleted,
                        hasCancelled = hasCancelled,
                        onClick = { onDateSelected(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            IconButton(
                onClick = { onShiftWeek(1) },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Следующая неделя",
                    tint = InkBlack,
                )
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = selectedDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        val todayEndMillis = today.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= todayEndMillis
                }
            },
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val instant = Instant.fromEpochMilliseconds(millis)
                        val localDate = instant.toLocalDateTime(TimeZone.UTC).date
                        onDateSelected(localDate)
                    }
                    showDatePicker = false
                }) {
                    Text("Выбрать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private val FutureGray = Color(0xFFD0D0D4)

@Composable
private fun WeekDayButton(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    hasCompleted: Boolean,
    hasCancelled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Coral else Color.Transparent,
        label = "dayBg",
    )
    val textColor = when {
        isFuture -> FutureGray
        isSelected -> Color.White
        isToday -> Coral
        else -> InkBlack
    }
    val dayAbbr = weekdayShortFromKotlinxDayOfWeek(date.dayOfWeek)

    val borderModifier = when {
        isFuture -> Modifier
        isSelected -> Modifier
        else -> Modifier.border(1.dp, BorderGray, RoundedCornerShape(14.dp))
    }

    Column(
        modifier = modifier
            .then(
                if (isSelected) Modifier.shadow(4.dp, RoundedCornerShape(14.dp))
                else Modifier
            )
            .clip(RoundedCornerShape(14.dp))
            .then(borderModifier)
            .background(if (isFuture) Color.Transparent else bgColor)
            .then(
                if (isFuture) Modifier else Modifier.clickable(onClick = onClick)
            )
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = dayAbbr,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = when {
                isFuture -> FutureGray
                isSelected -> Color.White.copy(alpha = 0.7f)
                else -> MutedGray
            },
        )
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasCompleted) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else CompletedAccent),
                )
            }
            if (hasCancelled) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White.copy(alpha = 0.6f) else IncompleteAccent),
                )
            }
            if (!hasCompleted && !hasCancelled) {
                Box(modifier = Modifier.size(5.dp))
            }
        }
    }
}
