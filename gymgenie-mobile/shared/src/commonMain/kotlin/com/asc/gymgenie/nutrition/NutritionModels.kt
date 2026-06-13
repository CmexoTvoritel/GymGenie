package com.asc.gymgenie.nutrition

import kotlinx.serialization.Serializable

data class MealPlan(
    val id: String,
    val name: String,
    val isActive: Boolean,
    val days: List<MealPlanDay>,
)

data class MealPlanDay(
    val id: String,
    val dayOfWeek: String,
    val meals: List<Meal>,
)

data class Meal(
    val id: String,
    val mealType: String,
    val name: String,
    val totalCalories: Int,
    val totalProteinG: Double,
    val totalFatG: Double,
    val totalCarbsG: Double,
    val orderIndex: Int,
    val items: List<MealItem>,
)

data class MealItem(
    val id: String,
    val name: String,
    val foodProductId: String?,
    val amountGrams: Double?,
    val calories: Int,
    val proteinG: Double,
    val fatG: Double,
    val carbsG: Double,
    val orderIndex: Int,
)

fun Meal.mealTypeDisplayName(): String = when (mealType) {
    "BREAKFAST" -> "Завтрак"
    "LUNCH" -> "Обед"
    "DINNER" -> "Ужин"
    "SNACK" -> "Перекус"
    else -> name
}

fun Meal.mealTypeTime(): String = when (mealType) {
    "BREAKFAST" -> "08:00"
    "LUNCH" -> "13:00"
    "SNACK" -> "16:00"
    "DINNER" -> "19:00"
    else -> ""
}

fun MealPlanDay.dayOfWeekDisplayName(): String = when (dayOfWeek) {
    "MONDAY" -> "Понедельник"
    "TUESDAY" -> "Вторник"
    "WEDNESDAY" -> "Среда"
    "THURSDAY" -> "Четверг"
    "FRIDAY" -> "Пятница"
    "SATURDAY" -> "Суббота"
    "SUNDAY" -> "Воскресенье"
    else -> dayOfWeek
}

@Serializable
internal data class MealPlanResponse(
    val id: String,
    val name: String,
    val isActive: Boolean = false,
    val createdBy: String? = null,
    val days: List<MealPlanDayResponse> = emptyList(),
)

@Serializable
internal data class MealPlanDayResponse(
    val id: String,
    val dayOfWeek: String,
    val meals: List<MealResponseDto> = emptyList(),
)

@Serializable
internal data class MealResponseDto(
    val id: String,
    val mealType: String,
    val name: String = "",
    val totalCalories: Int = 0,
    val totalProteinG: Double = 0.0,
    val totalFatG: Double = 0.0,
    val totalCarbsG: Double = 0.0,
    val orderIndex: Int = 0,
    val items: List<MealItemResponse> = emptyList(),
)

@Serializable
internal data class MealItemResponse(
    val id: String,
    val name: String = "",
    val foodProductId: String? = null,
    val amountGrams: Double? = null,
    val calories: Int = 0,
    val proteinG: Double = 0.0,
    val fatG: Double = 0.0,
    val carbsG: Double = 0.0,
    val portionSize: Double? = null,
    val portionUnit: String? = null,
    val iconUrl: String? = null,
    val orderIndex: Int = 0,
)

@Serializable
data class MealResponse(
    val id: String,
    val mealType: String,
    val name: String = "",
    val totalCalories: Int = 0,
    val totalProteinG: Double = 0.0,
    val totalFatG: Double = 0.0,
    val totalCarbsG: Double = 0.0,
    val orderIndex: Int = 0,
)

internal fun MealPlanResponse.toDomain(): MealPlan = MealPlan(
    id = id,
    name = name,
    isActive = isActive,
    days = days.map { it.toDomain() },
)

internal fun MealPlanDayResponse.toDomain(): MealPlanDay = MealPlanDay(
    id = id,
    dayOfWeek = dayOfWeek,
    meals = meals
        .map { it.toDomain() }
        .sortedBy { it.orderIndex },
)

internal fun MealResponseDto.toDomain(): Meal = Meal(
    id = id,
    mealType = mealType,
    name = name,
    totalCalories = totalCalories,
    totalProteinG = totalProteinG,
    totalFatG = totalFatG,
    totalCarbsG = totalCarbsG,
    orderIndex = orderIndex,
    items = items
        .map { it.toDomain() }
        .sortedBy { it.orderIndex },
)

internal fun MealItemResponse.toDomain(): MealItem = MealItem(
    id = id,
    name = name,
    foodProductId = foodProductId,
    amountGrams = amountGrams,
    calories = calories,
    proteinG = proteinG,
    fatG = fatG,
    carbsG = carbsG,
    orderIndex = orderIndex,
)

@Serializable
data class CreateMealPlanRequestDto(
    val name: String,
    val createdBy: String = "USER",
    val days: List<CreateMealPlanDayRequestDto>,
)

@Serializable
data class CreateMealPlanDayRequestDto(
    val dayOfWeek: String,
    val meals: List<CreateMealRequestDto>,
)

@Serializable
data class CreateMealRequestDto(
    val mealType: String,
    val name: String,
    val totalCalories: Int = 0,
    val items: List<AddMealItemRequestDto> = emptyList(),
)

@Serializable
data class AddMealItemRequestDto(
    val name: String = "",
    val foodProductId: String? = null,
    val amountGrams: Double? = null,
    val calories: Int = 0,
    val proteinG: Double = 0.0,
    val fatG: Double = 0.0,
    val carbsG: Double = 0.0,
    val orderIndex: Int = 0,
)

private val WeekOrder: List<String> = listOf(
    "MONDAY",
    "TUESDAY",
    "WEDNESDAY",
    "THURSDAY",
    "FRIDAY",
    "SATURDAY",
    "SUNDAY",
)

fun defaultWeeklyPlanRequest(name: String = "Мой план питания"): CreateMealPlanRequestDto {
    val mealTemplate = listOf(
        "BREAKFAST" to "Завтрак",
        "LUNCH" to "Обед",
        "SNACK" to "Перекус",
        "DINNER" to "Ужин",
    )
    val days = WeekOrder.map { dayOfWeek ->
        CreateMealPlanDayRequestDto(
            dayOfWeek = dayOfWeek,
            meals = mealTemplate.map { (type, label) ->
                CreateMealRequestDto(
                    mealType = type,
                    name = label,
                )
            },
        )
    }
    return CreateMealPlanRequestDto(
        name = name,
        createdBy = "USER",
        days = days,
    )
}
