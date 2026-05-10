package com.asc.gymgenie.nutrition

import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// Domain models
// ---------------------------------------------------------------------------

/**
 * Active meal plan for a user.
 *
 * The plan is structured as 7 [MealPlanDay]s (one per `DayOfWeek`), and each
 * day owns its own ordered list of [Meal]s. The shape is normalized: each
 * level (plan / day / meal / item) is identifiable by its own id so the UI
 * can patch a single meal item without having to refetch the entire plan
 * tree on every mutation.
 *
 * Day-of-week values are kept as raw strings ("MONDAY", "TUESDAY", ...) to
 * mirror the backend `java.time.DayOfWeek` enum names. Parsing them into a
 * platform-specific type would force `kotlinx-datetime` (or equivalent) into
 * every consumer; the strings are stable, server-driven, and easy to compare.
 */
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

// ---------------------------------------------------------------------------
// Display helpers
// ---------------------------------------------------------------------------

/**
 * Localized human label for a meal type. Falls back to [Meal.name] when the
 * server emits a meal type that this build does not yet ship support for —
 * keeps the UI rendering even after backend additions.
 */
fun Meal.mealTypeDisplayName(): String = when (mealType) {
    "BREAKFAST" -> "Завтрак"
    "LUNCH" -> "Обед"
    "DINNER" -> "Ужин"
    "SNACK" -> "Перекус"
    else -> name
}

fun Meal.mealTypeEmoji(): String = when (mealType) {
    "BREAKFAST" -> "☀️"
    "LUNCH" -> "🥗"
    "DINNER" -> "🌙"
    "SNACK" -> "🍎"
    else -> "🍽️"
}

/**
 * Suggested time slot for the meal type. Server does not currently model
 * scheduled time per meal; the slot is a UI hint only.
 */
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

// ---------------------------------------------------------------------------
// Wire-format DTOs
// ---------------------------------------------------------------------------

/**
 * Wire format for `GET /api/v1/nutrition/plans/active` and any plan-mutating
 * endpoint that returns the full plan tree. Kept internal to the nutrition
 * package so transport-layer concerns never leak into the domain models that
 * presenters consume.
 *
 * Optional fields default to safe values so a forward-compatible backend
 * payload (extra fields, null-where-empty) never breaks the mapping.
 */
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

/**
 * Wire format for a single [Meal]. Named with the `Dto` suffix to distinguish
 * it from the public [MealResponse] that meal-item endpoints return.
 */
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

/**
 * Public wire format for `POST /api/v1/nutrition/meals/{mealId}/items`.
 *
 * Exposed (not `internal`) because [NutritionApi.addItemToMeal] returns it
 * directly so the caller can decide whether to refetch the full plan or
 * patch the local meal in place.
 */
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

// ---------------------------------------------------------------------------
// DTO -> domain mappers
// ---------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------
// Request DTOs
// ---------------------------------------------------------------------------

/**
 * Body of `POST /api/v1/nutrition/plans`. The plan tree is sent in full —
 * see [defaultWeeklyPlanRequest] for the canonical "starter" payload.
 */
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

/**
 * Body of `POST /api/v1/nutrition/meals/{mealId}/items`.
 *
 * Two valid usage modes:
 *  - **Catalog mode**: pass [foodProductId] and [amountGrams]; the backend
 *    fills in the name/macros from the product row, so [name] and the macro
 *    fields can stay at their defaults.
 *  - **Free-text mode**: pass [name] and the macro values directly without a
 *    [foodProductId] (currently unused on Android, but the wire shape allows it).
 */
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

// ---------------------------------------------------------------------------
// Default plan factory
// ---------------------------------------------------------------------------

/**
 * Days of the week in the canonical Monday-first order. Kept private — the
 * factory below is the only consumer.
 */
private val WeekOrder: List<String> = listOf(
    "MONDAY",
    "TUESDAY",
    "WEDNESDAY",
    "THURSDAY",
    "FRIDAY",
    "SATURDAY",
    "SUNDAY",
)

/**
 * Canonical 4-meal-per-day starter plan factory.
 *
 * Each day ships with empty BREAKFAST/LUNCH/DINNER/SNACK slots — the caller
 * can then populate items via the food picker. Localized meal names are
 * picked from [Meal.mealTypeDisplayName] so the same Russian labels render
 * in the UI after the round-trip to the server.
 *
 * No longer consumed by [HomeViewModel] — kept as a building block for any
 * future flow that wants to bootstrap a weekly plan via the legacy
 * `/api/v1/nutrition/...` endpoints.
 */
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
