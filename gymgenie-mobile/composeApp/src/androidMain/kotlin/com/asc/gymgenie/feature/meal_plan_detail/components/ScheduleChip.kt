package com.asc.gymgenie.feature.meal_plan_detail.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.R
import com.asc.gymgenie.nutrition.AiMealType
import com.asc.gymgenie.nutrition.MealPlanDetail
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.utils.monthNameGenitiveFromInt
import com.asc.gymgenie.utils.weekdayNameRu
import com.asc.gymgenie.utils.weekdayShortRu
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

private val BorderColor = Color(0xFFEDEDEF)

private data class SchedulePalette(val bg: Color, @DrawableRes val iconRes: Int)

private fun paletteForMealType(wireValue: String): SchedulePalette = when (wireValue.uppercase()) {
    "BREAKFAST" -> SchedulePalette(Color(0xFFFFF6D6), R.drawable.ic_breakfast)
    "LUNCH" -> SchedulePalette(Color(0xFFFFEEDD), R.drawable.ic_lunch)
    "DINNER" -> SchedulePalette(Color(0xFFE6E9FF), R.drawable.ic_dinner)
    else -> SchedulePalette(Color(0xFFF4F4F6), R.drawable.ic_lunch)
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
            Image(
                painter = painterResource(id = palette.iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                contentScale = ContentScale.Fit,
            )
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
            val month = monthNameGenitiveFromInt(parsed.monthNumber)
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

