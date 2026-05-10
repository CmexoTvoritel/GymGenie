package com.asc.gymgenie.nutrition.entity

/**
 * High-level user goal for a generated meal plan. Used by the AI service
 * to bias macro split and total calorie target.
 */
enum class MealGoal {
    LOSE_WEIGHT,
    MAINTAIN,
    GAIN_MUSCLE
}

/**
 * Type of meal in a daily plan. Kept intentionally small (3 main meals)
 * because the current AI flow generates a single-day plan with three slots.
 */
enum class MealType {
    BREAKFAST, // Завтрак
    LUNCH,     // Обед
    DINNER     // Ужин
}

/**
 * Source that produced a meal plan. AI plans are produced by the GigaChat
 * generator under /api/v1/ai/meal; USER plans are built manually by the
 * client through the /api/v1/meal-plans/manual endpoint.
 */
enum class NutritionCreatedBy {
    AI,
    USER
}
