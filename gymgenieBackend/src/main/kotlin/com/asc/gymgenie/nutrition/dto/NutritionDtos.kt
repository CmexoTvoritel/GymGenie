package com.asc.gymgenie.nutrition.dto

import com.asc.gymgenie.nutrition.entity.MealType
import com.asc.gymgenie.workout.entity.CreatedBy
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.DayOfWeek
import java.util.*

// ===== Responses =====

data class MealPlanResponse(
    val id: UUID,
    val name: String,
    val isActive: Boolean,
    val createdBy: CreatedBy,
    val days: List<MealPlanDayResponse>
)

data class MealPlanShortResponse(
    val id: UUID,
    val name: String,
    val isActive: Boolean,
    val createdBy: CreatedBy,
    val daysCount: Int
)

data class MealPlanDayResponse(
    val id: UUID,
    val dayOfWeek: DayOfWeek,
    val meals: List<MealResponse>
)

data class MealResponse(
    val id: UUID,
    val mealType: MealType,
    val name: String,
    val description: String?,
    val totalCalories: Int,
    val totalProteinG: Double,
    val totalFatG: Double,
    val totalCarbsG: Double,
    val orderIndex: Int,
    val items: List<MealItemResponse>
)

data class MealItemResponse(
    val id: UUID,
    val name: String,
    val portionSize: String?,
    val portionUnit: String?,
    val calories: Int,
    val proteinG: Double,
    val fatG: Double,
    val carbsG: Double,
    val iconUrl: String?,
    val orderIndex: Int
)

// ===== Requests =====

data class CreateMealPlanRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    val createdBy: CreatedBy = CreatedBy.USER,

    @field:Valid
    val days: List<CreateMealPlanDayRequest> = emptyList()
)

data class UpdateMealPlanRequest(
    @field:Size(max = 100)
    val name: String? = null,

    val isActive: Boolean? = null
)

data class CreateMealPlanDayRequest(
    val dayOfWeek: DayOfWeek,

    @field:Valid
    val meals: List<CreateMealRequest> = emptyList()
)

data class CreateMealRequest(
    val mealType: MealType,

    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:Size(max = 500)
    val description: String? = null,

    val totalCalories: Int = 0,
    val totalProteinG: Double = 0.0,
    val totalFatG: Double = 0.0,
    val totalCarbsG: Double = 0.0,
    val orderIndex: Int = 0,

    @field:Valid
    val items: List<CreateMealItemRequest> = emptyList()
)

data class UpdateMealRequest(
    val mealType: MealType? = null,

    @field:Size(max = 100)
    val name: String? = null,

    @field:Size(max = 500)
    val description: String? = null,

    val totalCalories: Int? = null,
    val totalProteinG: Double? = null,
    val totalFatG: Double? = null,
    val totalCarbsG: Double? = null
)

data class CreateMealItemRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    val portionSize: String? = null,
    val portionUnit: String? = null,
    val calories: Int = 0,
    val proteinG: Double = 0.0,
    val fatG: Double = 0.0,
    val carbsG: Double = 0.0,
    val iconUrl: String? = null,
    val orderIndex: Int = 0
)

data class UpdateMealItemRequest(
    @field:Size(max = 100)
    val name: String? = null,

    val portionSize: String? = null,
    val portionUnit: String? = null,
    val calories: Int? = null,
    val proteinG: Double? = null,
    val fatG: Double? = null,
    val carbsG: Double? = null,
    val iconUrl: String? = null
)
