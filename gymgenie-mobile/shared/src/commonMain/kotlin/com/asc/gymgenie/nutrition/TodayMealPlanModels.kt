package com.asc.gymgenie.nutrition

import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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

fun todayLocalDate(): LocalDate =
    Clock.System
        .now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date

fun LocalDate.toWireDayOfWeek(): String = when (dayOfWeek) {
    DayOfWeek.MONDAY -> "MONDAY"
    DayOfWeek.TUESDAY -> "TUESDAY"
    DayOfWeek.WEDNESDAY -> "WEDNESDAY"
    DayOfWeek.THURSDAY -> "THURSDAY"
    DayOfWeek.FRIDAY -> "FRIDAY"
    DayOfWeek.SATURDAY -> "SATURDAY"
    DayOfWeek.SUNDAY -> "SUNDAY"
}
