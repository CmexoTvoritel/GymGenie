package com.asc.gymgenie.nutrition

import kotlinx.serialization.Serializable

enum class MealGoal(val wireValue: String, val displayName: String) {
    LOSE_WEIGHT("LOSE_WEIGHT", "Похудение"),
    MAINTAIN("MAINTAIN", "Поддержание формы"),
    GAIN_MUSCLE("GAIN_MUSCLE", "Набор мышечной массы");

    companion object {

        fun fromWireValue(value: String?): MealGoal? =
            entries.firstOrNull { it.wireValue == value }
    }
}

enum class AiMealType(val wireValue: String, val displayName: String) {
    BREAKFAST("BREAKFAST", "Завтрак"),
    LUNCH("LUNCH", "Обед"),
    DINNER("DINNER", "Ужин");

    companion object {
        fun fromWireValue(value: String?): AiMealType? =
            entries.firstOrNull { it.wireValue == value }
    }
}

@Serializable
data class PlannedDishItem(
    val name: String,
    val description: String? = null,
    val portionDescription: String? = null,
    val calories: Int? = null,
    val proteinG: Int? = null,
    val carbsG: Int? = null,
    val fatG: Int? = null,
    val foodProductId: String? = null,
    val grams: Double? = null,
)

@Serializable
data class PlannedMealItem(

    val mealType: String,
    val name: String,
    val estimatedCalories: Int? = null,
    val dishes: List<PlannedDishItem> = emptyList(),
)

@Serializable
data class AiMealPlanData(
    val name: String,

    val description: String? = null,

    val goal: String? = null,
    val totalCalories: Int? = null,
    val meals: List<PlannedMealItem> = emptyList(),
)

@Serializable
enum class AiMealResponseType {
    CLARIFICATION,
    MEAL_PLAN,
}

@Serializable
data class AiMealChatRequest(
    val message: String,
    val ageYears: Int? = null,

    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val goal: String? = null,
    val dietaryRestrictions: String? = null,
    val allergies: String? = null,

    val mealType: String? = null,
)

@Serializable
data class AiMealChatResponse(
    val type: AiMealResponseType,
    val message: String,
    val mealPlan: AiMealPlanData? = null,
)

@Serializable
data class SaveMealPlanRequest(
    val name: String,

    val description: String? = null,
    val goal: String? = null,
    val totalCalories: Int? = null,

    val mealType: String? = null,
    val scheduleType: String? = null,
    val scheduleDays: List<String> = emptyList(),
    val oneOffDate: String? = null,
    val replaceConflictPlanIds: List<String> = emptyList(),
    val meals: List<PlannedMealItem> = emptyList(),
)

@Serializable
data class SaveMealPlanResponse(
    val mealPlanId: String,
)

@Serializable
data class AiMealBookedDaysResponse(
    val recurringDays: List<String> = emptyList(),
    val oneOffDates: List<String> = emptyList(),
)

@Serializable
data class AiMealConflictCheckResponse(
    val hasConflicts: Boolean,
    val conflicts: List<AiMealConflictPlan> = emptyList(),
)

@Serializable
data class AiMealConflictPlan(
    val planId: String,
    val planName: String,
)

@Serializable
data class MealPlanShortInfo(
    val id: String,
    val name: String,
    val description: String? = null,
    val goal: String? = null,
    val totalCalories: Int? = null,
    val mealsCount: Int = 0,

    val createdBy: String? = null,
    val createdAt: String,
)

@Serializable
data class MealPlanDetail(
    val id: String,
    val name: String,
    val description: String? = null,
    val goal: String? = null,
    val totalCalories: Int? = null,

    val createdBy: String? = null,
    val scheduleType: String? = null,
    val scheduleDays: List<String> = emptyList(),
    val oneOffDate: String? = null,
    val meals: List<MealPlanDetailMeal> = emptyList(),
    val createdAt: String,
)

@Serializable
data class MealPlanDetailMeal(
    val id: String,
    val mealType: String,
    val name: String,
    val estimatedCalories: Int? = null,
    val dishes: List<MealPlanDetailDish> = emptyList(),
)

@Serializable
data class MealPlanDetailDish(
    val id: String,
    val name: String,
    val description: String? = null,
    val portionDescription: String? = null,
    val calories: Int? = null,
    val proteinG: Int? = null,
    val carbsG: Int? = null,
    val fatG: Int? = null,
    val foodProductId: String? = null,
    val grams: Double? = null,
    val foodCategory: String? = null,
)
