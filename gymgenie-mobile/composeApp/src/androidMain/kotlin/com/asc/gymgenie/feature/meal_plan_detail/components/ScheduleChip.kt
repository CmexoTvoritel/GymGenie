package com.asc.gymgenie.feature.meal_plan_detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.nutrition.AiMealType
import com.asc.gymgenie.nutrition.MealPlanDetail
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

private val BorderColor = Color(0xFFEDEDEF)

private data class SchedulePalette(val bg: Color, val emoji: String)

private fun paletteForMealType(wireValue: String): SchedulePalette = when (wireValue.uppercase()) {
    "BREAKFAST" -> SchedulePalette(Color(0xFFFFF6D6), "☀️")
    "LUNCH" -> SchedulePalette(Color(0xFFFFEEDD), "🥗")
    "DINNER" -> SchedulePalette(Color(0xFFE6E9FF), "🌙")
    else -> SchedulePalette(Color(0xFFF4F4F6), "🍽️")
}

@Composable
fun ScheduleChip(
    plan: MealPlanDetail,
    modifier: Modifier = Modifier,
) {
    val firstMealType = plan.meals.firstOrNull()?.mealType ?: "BREAKFAST"
    val mealType = AiMealType.fromWireValue(firstMealType)
    val palette = paletteForMealType(firstMealType)
    val mealLabel = mealType?.displayName ?: "Приём пищи"

    val dateLabel = buildDateLabel(plan)
    val dayLabel = buildDayLabel(plan)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.5.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.bg),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = palette.emoji, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            if (dayLabel != null) {
                Text(
                    text = dayLabel.uppercase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = MutedText,
                    letterSpacing = 0.5.sp,
                )
            }
            Text(
                text = "$mealLabel, $dateLabel",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = DeepInk,
            )
        }
    }
}

private fun buildDateLabel(plan: MealPlanDetail): String {
    val dateStr = plan.oneOffDate
    if (dateStr != null) {
        val parsed = runCatching { LocalDate.parse(dateStr) }.getOrNull()
        if (parsed != null) {
            val day = parsed.dayOfMonth
            val month = monthNameRu(parsed.monthNumber)
            val weekday = weekdayNameRu(parsed.dayOfWeek.name)
            return "$day $month, $weekday"
        }
        return dateStr
    }

    if (plan.scheduleDays.isNotEmpty()) {
        return plan.scheduleDays.joinToString(", ") { weekdayShortRu(it) }
    }

    return plan.createdAt.take(10)
}

private fun buildDayLabel(plan: MealPlanDetail): String? {
    val dateStr = plan.oneOffDate ?: return null
    val parsed = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return null
    val today = kotlinx.datetime.Clock.System.now()
        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
    return when (parsed) {
        today -> "Сегодня"
        else -> weekdayNameRu(parsed.dayOfWeek.name)
    }
}

private fun monthNameRu(month: Int): String = when (month) {
    1 -> "января"
    2 -> "февраля"
    3 -> "марта"
    4 -> "апреля"
    5 -> "мая"
    6 -> "июня"
    7 -> "июля"
    8 -> "августа"
    9 -> "сентября"
    10 -> "октября"
    11 -> "ноября"
    12 -> "декабря"
    else -> ""
}

private fun weekdayNameRu(wire: String): String = when (wire.uppercase()) {
    "MONDAY" -> "понедельник"
    "TUESDAY" -> "вторник"
    "WEDNESDAY" -> "среда"
    "THURSDAY" -> "четверг"
    "FRIDAY" -> "пятница"
    "SATURDAY" -> "суббота"
    "SUNDAY" -> "воскресенье"
    else -> wire.lowercase()
}

private fun weekdayShortRu(wire: String): String = when (wire.uppercase()) {
    "MONDAY" -> "Пн"
    "TUESDAY" -> "Вт"
    "WEDNESDAY" -> "Ср"
    "THURSDAY" -> "Чт"
    "FRIDAY" -> "Пт"
    "SATURDAY" -> "Сб"
    "SUNDAY" -> "Вс"
    else -> wire
}
