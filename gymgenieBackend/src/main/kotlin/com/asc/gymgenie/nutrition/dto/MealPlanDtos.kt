package com.asc.gymgenie.nutrition.dto

import com.asc.gymgenie.nutrition.entity.MealGoal
import com.asc.gymgenie.nutrition.entity.MealType
import com.asc.gymgenie.nutrition.entity.NutritionCreatedBy
import com.asc.gymgenie.workout.entity.WorkoutScheduleType
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.util.*

data class MealPlanShortResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val goal: MealGoal?,
    val totalCalories: Int?,
    val mealsCount: Int,
    val createdBy: NutritionCreatedBy,
    val createdAt: Instant
)

data class MealPlanDetailResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val goal: MealGoal?,
    val totalCalories: Int?,
    val createdBy: NutritionCreatedBy,
    val scheduleType: WorkoutScheduleType?,

    val scheduleDays: List<String>,
    val oneOffDate: String?,
    val meals: List<MealResponse>,
    val createdAt: Instant
)

data class MealResponse(
    val id: UUID,
    val mealType: MealType,
    val name: String,
    val estimatedCalories: Int?,
    val dishes: List<DishResponse>
)

data class DishResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val portionDescription: String?,
    val calories: Int?,
    val proteinG: Int?,
    val carbsG: Int?,
    val fatG: Int?,
    val foodProductId: UUID? = null,
    val grams: Double? = null,
    val foodCategory: String? = null
)

data class CreateManualMealPlanRequest(

    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:Size(max = 500)
    val description: String? = null,

    @field:NotNull
    val mealType: MealType,

    val goal: MealGoal? = null,

    @field:NotNull
    val scheduleType: WorkoutScheduleType,

    val scheduleDays: List<String> = emptyList(),

    val oneOffDate: LocalDate? = null,

    @field:NotEmpty
    @field:Valid
    val items: List<ManualMealItemDto>
)

data class ManualMealItemDto(

    val foodProductId: UUID? = null,

    @field:DecimalMin(value = "0.1", inclusive = true)
    @field:DecimalMax(value = "5000.0", inclusive = true)
    val grams: Double,

    val name: String? = null,
    val calories: Int? = null,
    val proteinG: Int? = null,
    val carbsG: Int? = null,
    val fatG: Int? = null,
    val portionDescription: String? = null,
)

data class BookedDaysResponse(

    val recurringDays: List<String>,

    val oneOffDates: List<String>
)
