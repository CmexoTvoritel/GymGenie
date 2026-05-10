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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.nutrition.TodayMealDish
import com.asc.gymgenie.nutrition.TodayMealPlanCard
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText

private val MealCardBorder = Color(0xFFEDEDEF)

// ---------------------------------------------------------------------------
// Meal-type → palette / display mapping
// ---------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------
// Section
// ---------------------------------------------------------------------------

/**
 * Meal-plan section on the Home screen.
 *
 * Reads its data exclusively from [todayPlans] — the list of meal-plan cards
 * the [com.asc.gymgenie.presentation.HomeViewModel] resolved as "applies
 * today". Two rendering branches:
 *
 *  - [todayPlans] is empty → empty-state CTA that opens the manual creation
 *    flow (the actual route is hoisted to the parent via [onCreatePlan]).
 *  - [todayPlans] is non-empty → one row per plan/meal. Tapping a row raises
 *    [onPlanTap] with the underlying plan id so the parent can push the
 *    detail screen — this section is intentionally state-free.
 */
@Composable
fun MealPlanSection(
    todayPlans: List<TodayMealPlanCard>,
    onPlanTap: (planId: String) -> Unit,
    onCreatePlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalKcal = todayPlans.sumOf { it.estimatedCalories ?: 0 }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MealSectionHeader(
            title = "План питания",
            subtitle = if (todayPlans.isEmpty()) {
                "Сегодня нет запланированных приёмов пищи"
            } else {
                "${todayPlans.size} ${pluralizeMeals(todayPlans.size)} · $totalKcal ккал"
            },
        )

        if (todayPlans.isEmpty()) {
            EmptyPlanCard(onCreate = onCreatePlan)
        } else {
            todayPlans.forEach { card ->
                MealRow(
                    card = card,
                    onTap = { onPlanTap(card.planId) },
                )
            }
        }
    }
}

@Composable
private fun MealSectionHeader(title: String, subtitle: String) {
    Column {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DeepInk,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = MutedText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
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
            text = "Нет плана питания на сегодня",
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DeepInk,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Создай рацион — расписание сам подскажет, что есть в этот день",
            fontSize = 13.sp,
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
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepInk,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = card.planName,
                    fontSize = 11.5.sp,
                    color = MutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dishesSummary(card.dishes, card.mealName),
                fontSize = 12.sp,
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepInk,
                )
                Text(
                    text = "ККАЛ",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MutedText,
                )
            }
        }
    }
}

/**
 * Renders a short summary of a meal's dish list. Falls back to the meal's
 * own name when the dish list is empty (e.g. a plan that only carries an
 * AI-generated title without a per-dish breakdown).
 */
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
