package com.asc.gymgenie.nutrition

import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Flattened, presentation-friendly projection of a [MealPlanDetail] for the
 * Home dashboard.
 *
 * The home meal-plan section does not need the full plan tree — it only needs
 * to render a card per plan that "applies today" with the meal type, name,
 * total/estimated calories and a short summary of the dishes inside. Mapping
 * to this dedicated model at the data-layer boundary keeps the section's
 * Composable / SwiftUI views agnostic of wire shapes.
 *
 * One [TodayMealPlanCard] corresponds to exactly one persisted [MealPlanDetail]
 * (which itself owns one meal). Multiple cards on the same day are expected
 * when the user scheduled overlapping plans for several meal types.
 */
data class TodayMealPlanCard(
    val planId: String,
    val planName: String,
    val mealType: String,
    val mealName: String,
    val estimatedCalories: Int?,
    val dishes: List<TodayMealDish>,
)

data class TodayMealDish(
    val id: String,
    val name: String,
    val portionDescription: String?,
    val calories: Int?,
)

/**
 * Decides whether a [MealPlanDetail] should appear on the home screen for the
 * given calendar day.
 *
 * Rules:
 *  - `RECURRING` plans match when [today]'s wire weekday name (e.g. `"MONDAY"`)
 *    is present in [MealPlanDetail.scheduleDays].
 *  - `ONE_TIME`  plans match when [MealPlanDetail.oneOffDate] equals today's
 *    ISO-8601 date string.
 *  - Anything else (legacy plans without scheduling, unknown wire values) is
 *    treated as "not visible today" — never shown by mistake.
 */
fun MealPlanDetail.appliesOn(today: LocalDate): Boolean {
    val type = scheduleType?.uppercase() ?: return false
    return when (type) {
        "RECURRING" -> {
            val todayWire = today.toWireDayOfWeek()
            scheduleDays.any { it.uppercase() == todayWire }
        }
        "ONE_TIME" -> oneOffDate == today.toString()
        else -> false
    }
}

/**
 * Domain mapper: collapse the full plan tree into the home-card shape. Every
 * meal in the plan becomes a card; in the manual flow there is exactly one
 * meal per plan, but the AI flow may emit several — both are supported.
 */
fun MealPlanDetail.toTodayCards(): List<TodayMealPlanCard> =
    meals.map { meal ->
        TodayMealPlanCard(
            planId = id,
            planName = name,
            mealType = meal.mealType,
            mealName = meal.name,
            estimatedCalories = meal.estimatedCalories,
            dishes = meal.dishes.map { dish ->
                TodayMealDish(
                    id = dish.id,
                    name = dish.name,
                    portionDescription = dish.portionDescription,
                    calories = dish.calories,
                )
            },
        )
    }

/**
 * Today's calendar date in the device's current timezone. Centralised here so
 * the home presenter and the filter helper compute "today" the same way.
 */
fun todayLocalDate(): LocalDate =
    Clock.System
        .now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date

/**
 * Wire-format `DayOfWeek` name (`"MONDAY"`, `"TUESDAY"`, ...) for a date.
 */
fun LocalDate.toWireDayOfWeek(): String = when (dayOfWeek) {
    DayOfWeek.MONDAY -> "MONDAY"
    DayOfWeek.TUESDAY -> "TUESDAY"
    DayOfWeek.WEDNESDAY -> "WEDNESDAY"
    DayOfWeek.THURSDAY -> "THURSDAY"
    DayOfWeek.FRIDAY -> "FRIDAY"
    DayOfWeek.SATURDAY -> "SATURDAY"
    DayOfWeek.SUNDAY -> "SUNDAY"
}
