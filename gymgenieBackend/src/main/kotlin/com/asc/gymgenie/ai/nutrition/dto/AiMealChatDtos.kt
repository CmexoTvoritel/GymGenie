package com.asc.gymgenie.ai.nutrition.dto

import com.asc.gymgenie.nutrition.entity.MealGoal
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.util.UUID

data class AiMealChatRequest(
    @field:NotBlank
    val message: String,

    val ageYears: Int? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,

    val goal: MealGoal? = null,

    @field:Size(max = 500)
    val dietaryRestrictions: String? = null,

    @field:Size(max = 500)
    val allergies: String? = null,

    val mealType: String? = null
)

data class AiMealChatResponse(
    val type: AiMealResponseType,
    val message: String,
    val mealPlan: AiMealPlanDto? = null
)

enum class AiMealResponseType {
    CLARIFICATION, MEAL_PLAN
}

data class AiMealPlanDto(
    val name: String,
    val description: String? = null,
    val totalCalories: Int? = null,
    val meals: List<AiMealDto> = emptyList()
)

data class AiMealDto(

    val mealType: String,
    val name: String,
    val estimatedCalories: Int? = null,
    val dishes: List<AiDishDto> = emptyList()
)

data class AiDishDto(
    val name: String,
    val description: String? = null,
    val portionDescription: String? = null,
    val calories: Int? = null,
    val proteinG: Int? = null,
    val carbsG: Int? = null,
    val fatG: Int? = null,
    val foodProductId: UUID? = null,
    val grams: Double? = null
)

data class SaveMealPlanRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:Size(max = 500)
    val description: String? = null,

    val goal: MealGoal? = null,

    val totalCalories: Int? = null,

    val mealType: String? = null,

    val scheduleType: String? = null,
    val scheduleDays: List<String> = emptyList(),
    val oneOffDate: String? = null,

    val replaceConflictPlanIds: List<UUID> = emptyList(),

    @field:Valid
    @field:NotEmpty
    val meals: List<SaveMealRequest> = emptyList()
)

data class SaveMealRequest(

    @field:NotBlank
    val mealType: String,

    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    val estimatedCalories: Int? = null,

    @field:Valid
    val dishes: List<SaveDishRequest> = emptyList()
)

data class SaveDishRequest(
    @field:NotBlank
    @field:Size(max = 150)
    val name: String,

    @field:Size(max = 500)
    val description: String? = null,

    @field:Size(max = 50)
    val portionDescription: String? = null,

    val calories: Int? = null,
    val proteinG: Int? = null,
    val carbsG: Int? = null,
    val fatG: Int? = null,
    val foodProductId: UUID? = null,
    val grams: Double? = null
)

data class SaveMealPlanResponse(
    val mealPlanId: UUID
)

data class AiMealBookedDaysResponse(
    val recurringDays: List<String> = emptyList(),
    val oneOffDates: List<String> = emptyList()
)

data class AiMealConflictCheckResponse(
    val hasConflicts: Boolean,
    val conflicts: List<AiMealConflictPlan> = emptyList()
)

data class AiMealConflictPlan(
    val planId: UUID,
    val planName: String
)
