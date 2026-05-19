package com.asc.gymgenie.feature.home.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
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
import com.asc.gymgenie.nutrition.TodayMealDish
import com.asc.gymgenie.nutrition.TodayMealPlanCard
import com.asc.gymgenie.nutrition.todayLocalDate
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.SoftCard
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.minus
import kotlinx.datetime.plus

private val MealCardBorder = Color(0xFFEDEDEF)

private val STANDARD_SLOTS = listOf("BREAKFAST", "LUNCH", "DINNER")

private data class MealPalette(val iconBg: Color, val iconFg: Color, val emoji: String)

private fun paletteFor(mealType: String): MealPalette = when (mealType.uppercase()) {
    "BREAKFAST" -> MealPalette(Color(0xFFFFF6D6), Color(0xFFD4A017), "☀️")
    "LUNCH" -> MealPalette(Color(0xFFFFEEDD), Color(0xFFE07B00), "🥗")
    "DINNER" -> MealPalette(Color(0xFFE6E9FF), Color(0xFF3B5BDB), "🌙")
    "SNACK" -> MealPalette(Color(0xFFE8F7E8), Color(0xFF2F9E44), "🍎")
    else -> MealPalette(Color(0xFFF3F2EF), Color(0xFF6E6E76), "🍽️")
}

private fun mealTypeDisplayName(mealType: String): String = when (mealType.uppercase()) {
    "BREAKFAST" -> "Завтрак"
    "LUNCH" -> "Обед"
    "DINNER" -> "Ужин"
    "SNACK" -> "Перекус"
    else -> mealType
}

@Composable
fun MealPlanSection(
    todayPlans: List<TodayMealPlanCard>,
    isLoading: Boolean,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onPlanTap: (planId: String) -> Unit,
    onCreatePlan: (mealType: String?, date: String?) -> Unit,
    isPremium: Boolean = true,
    onOpenPaywall: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val totalKcal = todayPlans.sumOf { it.estimatedCalories ?: 0 }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeaderPremium(
            title = "План питания",
            subtitle = if (!isPremium) "Доступен в Premium"
                       else when {
                           isLoading -> "Загрузка..."
                           todayPlans.isEmpty() -> "Нет планов на эту дату"
                           else -> "${todayPlans.size} ${pluralizeMeals(todayPlans.size)} · $totalKcal ккал"
                       },
        )

        if (!isPremium) {
            MealPlanLockedOverlay(onUnlock = onOpenPaywall)
        } else {
            MealDatePicker(
                selectedDate = selectedDate,
                onDateSelected = onDateSelected,
            )

            val dateIso = selectedDate.toString()

            if (isLoading) {
                MealPlansLoadingState()
            } else if (todayPlans.isEmpty()) {
                EmptyPlanCard(onCreate = { onCreatePlan(null, null) })
            } else {
                STANDARD_SLOTS.forEach { slot ->
                    val card = todayPlans.firstOrNull { it.mealType.uppercase() == slot }
                    if (card != null) {
                        MealRow(
                            card = card,
                            onTap = { onPlanTap(card.planId) },
                        )
                    } else {
                        EmptyMealSlotCard(
                            mealType = slot,
                            onCreatePlan = { mealType -> onCreatePlan(mealType, dateIso) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MealPlansLoadingState() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { SkeletonMealCard() }
    }
}

@Composable
private fun SkeletonMealCard() {
    val shimmer = Color(0xFFF3F2EF)
    val shimmerDark = Color(0xFFEAE9E6)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.5.dp, MealCardBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(shimmer),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box {
                Text(
                    text = "Завтрак",
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Transparent,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .matchParentSize()
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer),
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Box {
                Text(
                    text = "Блюдо · Блюдо · Блюдо",
                    fontSize = 14.sp,
                    color = Color.Transparent,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .matchParentSize()
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerDark),
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Box {
                Text(
                    text = "500",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Transparent,
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer),
                )
            }
            Box {
                Text(
                    text = "ККАЛ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Transparent,
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerDark),
                )
            }
        }
    }
}

@Composable
private fun MealDatePicker(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
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
        modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun EmptyPlanCard(onCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.5.dp, MealCardBorder, RoundedCornerShape(18.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "🍽️", fontSize = 30.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Нет плана питания на эту дату",
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DeepInk,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Создай рацион — расписание само подскажет, что есть в этот день",
            fontSize = 15.sp,
            color = MutedText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Coral)
                .clickable { onCreate() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Создать план",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun EmptyMealSlotCard(mealType: String, onCreatePlan: (mealType: String) -> Unit) {
    val palette = paletteFor(mealType)
    val title = mealTypeDisplayName(mealType)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.5.dp, MealCardBorder, RoundedCornerShape(18.dp))
            .clickable { onCreatePlan(mealType) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = palette.emoji, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Ещё не составлено — добавь сейчас",
                fontSize = 14.sp,
                color = MutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(DeepInk),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun MealRow(card: TodayMealPlanCard, onTap: () -> Unit) {
    val palette = paletteFor(card.mealType)
    val title = mealTypeDisplayName(card.mealType)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.5.dp, MealCardBorder, RoundedCornerShape(18.dp))
            .clickable { onTap() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = palette.emoji, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepInk,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = card.planName,
                    fontSize = 13.5.sp,
                    color = DeepInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dishesSummary(card.dishes, card.mealName),
                fontSize = 14.sp,
                color = MutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (card.estimatedCalories != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = card.estimatedCalories.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepInk,
                )
                Text(
                    text = "ККАЛ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MutedText,
                )
            }
        }
    }
}

private fun dishesSummary(dishes: List<TodayMealDish>, fallback: String): String {
    if (dishes.isEmpty()) return fallback.ifBlank { "Без описания" }
    return dishes.take(3).joinToString(" · ") { it.name }
}

private fun pluralizeMeals(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod10 == 1 && mod100 != 11 -> "приём пищи"
        mod10 in 2..4 && mod100 !in 12..14 -> "приёма пищи"
        else -> "приёмов пищи"
    }
}
