package com.asc.gymgenie.ai.nutrition.dto

import com.asc.gymgenie.nutrition.entity.MealGoal
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.util.UUID

// ===== Chat request/response =====

data class AiMealChatRequest(
    @field:NotBlank
    val message: String,

    val ageYears: Int? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,

    val goal: MealGoal? = null,

    /** Free-form dietary restrictions (e.g. вегетарианство, без свинины). */
    @field:Size(max = 500)
    val dietaryRestrictions: String? = null,

    /** Free-form list of allergies. */
    @field:Size(max = 500)
    val allergies: String? = null
)

data class AiMealChatResponse(
    val type: AiMealResponseType,
    val message: String,
    val mealPlan: AiMealPlanDto? = null
)

enum class AiMealResponseType {
    CLARIFICATION, MEAL_PLAN
}

// ===== Lenient parsed DTOs (used both for GigaChat parsing and the save path) =====
//
// All macro/calorie fields are nullable on purpose. GigaChat occasionally omits or
// nulls out individual fields and we prefer to persist `null` over fabricating a
// value just to satisfy a non-null contract. This mirrors the lenient
// `AiWorkoutExerciseParsedDto` approach in the workout flow.

data class AiMealPlanDto(
    val name: String,
    val description: String? = null,
    val totalCalories: Int? = null,
    val meals: List<AiMealDto> = emptyList()
)

data class AiMealDto(
    /** String form because GigaChat may reply with a Russian label or unknown value;
     *  the service maps it to [com.asc.gymgenie.nutrition.entity.MealType] safely. */
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

// ===== Save / replace =====

data class SaveMealPlanRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:Size(max = 500)
    val description: String? = null,

    val goal: MealGoal? = null,

    val totalCalories: Int? = null,

    @field:Valid
    @field:NotEmpty
    val meals: List<SaveMealRequest> = emptyList()
)

data class SaveMealRequest(
    /** String form for parity with [AiMealDto]; the service validates and converts to enum. */
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
