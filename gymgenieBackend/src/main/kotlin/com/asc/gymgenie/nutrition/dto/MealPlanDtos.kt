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

// ===== List/short =====

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

// ===== Detail =====

data class MealPlanDetailResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val goal: MealGoal?,
    val totalCalories: Int?,
    val createdBy: NutritionCreatedBy,
    val scheduleType: WorkoutScheduleType?,
    /** Upper-case weekday names (e.g. ["MONDAY", "WEDNESDAY"]); empty unless [scheduleType] is RECURRING. */
    val scheduleDays: List<String>,
    /** Bound calendar date; non-null only when [scheduleType] is ONE_TIME. */
    val oneOffDate: LocalDate?,
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
    val fatG: Int?
)

// ===== Manual creation =====

/**
 * Manual meal-plan creation request. The client picks catalog products
 * (each with a portion in grams) and the server expands them into a single
 * [com.asc.gymgenie.nutrition.entity.MealEntity] of type [mealType] with one
 * [com.asc.gymgenie.nutrition.entity.DishEntity] per item.
 */
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

    /** Upper-case weekday names (e.g. ["MONDAY"]); required when [scheduleType] is RECURRING. */
    val scheduleDays: List<String> = emptyList(),

    /** Required when [scheduleType] is ONE_TIME. */
    val oneOffDate: LocalDate? = null,

    @field:NotEmpty
    @field:Valid
    val items: List<ManualMealItemDto>
)

data class ManualMealItemDto(

    @field:NotNull
    val foodProductId: UUID,

    @field:DecimalMin(value = "0.1", inclusive = true)
    @field:DecimalMax(value = "5000.0", inclusive = true)
    val grams: Double
)

// ===== Booked-days view =====

/**
 * Calendar slots already occupied by the user's existing plans for a given
 * meal type. Used by the mobile manual-creation flow to disable already-booked
 * days/dates in the date picker.
 */
data class BookedDaysResponse(
    /** Distinct upper-case weekday names from RECURRING plans (e.g. ["MONDAY", "WEDNESDAY"]). */
    val recurringDays: List<String>,
    /** Distinct ISO-8601 date strings (yyyy-MM-dd) from ONE_TIME plans. */
    val oneOffDates: List<String>
)
