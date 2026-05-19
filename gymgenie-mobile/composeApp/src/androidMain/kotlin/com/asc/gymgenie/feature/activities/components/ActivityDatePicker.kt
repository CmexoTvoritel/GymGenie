package com.asc.gymgenie.feature.activities.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.nutrition.todayLocalDate
import com.asc.gymgenie.ui.theme.DeepInk
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@Composable
fun ActivityDatePicker(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val today = todayLocalDate()
    val label = when (selectedDate) {
        today -> "Сегодня"
        today.minus(1, DateTimeUnit.DAY) -> "Вчера"
        today.plus(1, DateTimeUnit.DAY) -> "Завтра"
        else -> "${selectedDate.dayOfMonth} ${monthName(selectedDate.month)}"
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DateArrowButton(
            forward = false,
            onClick = { onDateSelected(selectedDate.minus(1, DateTimeUnit.DAY)) },
        )

        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepInk,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                    val cal = java.util.Calendar.getInstance().apply {
                        set(selectedDate.year, selectedDate.monthNumber - 1, selectedDate.dayOfMonth)
                    }
                    android.app.DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            onDateSelected(LocalDate(year, month + 1, day))
                        },
                        cal.get(java.util.Calendar.YEAR),
                        cal.get(java.util.Calendar.MONTH),
                        cal.get(java.util.Calendar.DAY_OF_MONTH),
                    ).show()
                }
                .padding(vertical = 8.dp),
        )

        DateArrowButton(
            forward = true,
            onClick = { onDateSelected(selectedDate.plus(1, DateTimeUnit.DAY)) },
        )
    }
}

@Composable
private fun DateArrowButton(forward: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0xFFF4F4F6))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (forward) Icons.AutoMirrored.Filled.ArrowForward
            else Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = Color(0xFF0A0A0A),
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun monthName(month: Month): String = when (month) {
    Month.JANUARY -> "января"
    Month.FEBRUARY -> "февраля"
    Month.MARCH -> "марта"
    Month.APRIL -> "апреля"
    Month.MAY -> "мая"
    Month.JUNE -> "июня"
    Month.JULY -> "июля"
    Month.AUGUST -> "августа"
    Month.SEPTEMBER -> "сентября"
    Month.OCTOBER -> "октября"
    Month.NOVEMBER -> "ноября"
    Month.DECEMBER -> "декабря"
    else -> month.name
}
