package com.asc.gymgenie.nutrition

import kotlinx.serialization.Serializable

/**
 * Transport + presentation models for the AI-powered meal planning feature.
 *
 * These mirror the workout-side `AiModels.kt` one-to-one in shape: the backend
 * either returns a clarification question or a fully-formed plan in a single
 * response, and the presenter caches it on state until the user saves.
 *
 * Names are deliberately distinct from the existing weekly [MealPlan] /
 * [Meal] / [MealItem] tree (see `NutritionModels.kt`) so the AI feature can
 * coexist with the manual-builder feature inside the same package without
 * symbol collisions.
 */

// ---------------------------------------------------------------------------
// Display enums
// ---------------------------------------------------------------------------

/**
 * High-level training/diet objective the user picks before chatting with the
 * AI. The wire value is the enum name (`"LOSE_WEIGHT"`, `"MAINTAIN"`,
 * `"GAIN_MUSCLE"`) — keeping the enum on the client lets the UI render a
 * localized label without dragging localization into the API contract.
 */
enum class MealGoal(val wireValue: String, val displayName: String) {
    LOSE_WEIGHT("LOSE_WEIGHT", "Похудение"),
    MAINTAIN("MAINTAIN", "Поддержание формы"),
    GAIN_MUSCLE("GAIN_MUSCLE", "Набор мышечной массы");

    companion object {
        /** Resolves a wire value back to the enum, tolerating unknown values. */
        fun fromWireValue(value: String?): MealGoal? =
            entries.firstOrNull { it.wireValue == value }
    }
}

/**
 * Meal slot inside a single day. Kept tight (3 entries) intentionally — the AI
 * coach is configured to produce breakfast / lunch / dinner only. New slots
 * (e.g. `SNACK`) would need a backend change first.
 */
enum class AiMealType(val wireValue: String, val displayName: String, val emoji: String) {
    BREAKFAST("BREAKFAST", "Завтрак", "☀️"),
    LUNCH("LUNCH", "Обед", "🥗"),
    DINNER("DINNER", "Ужин", "🌙");

    companion object {
        fun fromWireValue(value: String?): AiMealType? =
            entries.firstOrNull { it.wireValue == value }
    }
}

// ---------------------------------------------------------------------------
// Domain payload (returned inside the chat response and re-sent on save)
// ---------------------------------------------------------------------------

@Serializable
data class PlannedDishItem(
    val name: String,
    val description: String? = null,
    val portionDescription: String? = null,
    val calories: Int? = null,
    val proteinG: Int? = null,
    val carbsG: Int? = null,
    val fatG: Int? = null,
)

@Serializable
data class PlannedMealItem(
    /** Wire string for [AiMealType]. Stored raw to be tolerant of forward-compat additions. */
    val mealType: String,
    val name: String,
    val estimatedCalories: Int? = null,
    val dishes: List<PlannedDishItem> = emptyList(),
)

@Serializable
data class AiMealPlanData(
    val name: String,
    /**
     * Free-form summary the AI may attach to the plan. Nullable because the
     * model is not contractually required to return one — older / cached
     * responses can land without it, and forcing a non-null deserializer would
     * make the whole chat reply fail to decode.
     */
    val description: String? = null,
    /** Wire string for [MealGoal]. Optional because the AI may omit it. */
    val goal: String? = null,
    val totalCalories: Int? = null,
    val meals: List<PlannedMealItem> = emptyList(),
)

// ---------------------------------------------------------------------------
// Chat endpoint
// ---------------------------------------------------------------------------

/**
 * Discriminator for the AI meal response kind. Mirrors the workout feature's
 * `AiResponseType` so the chat presenter logic stays symmetric.
 */
@Serializable
enum class AiMealResponseType {
    CLARIFICATION,
    MEAL_PLAN,
}

@Serializable
data class AiMealChatRequest(
    val message: String,
    val ageYears: Int? = null,
    /**
     * Height/weight are typed as [Double] to match the backend contract — the
     * server stores them as decimals and an `Int` here would cause silent
     * truncation when the user enters fractional values somewhere upstream.
     * The picker UI captures whole-number input, so callers convert via
     * [Int.toDouble] when building the request.
     */
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val goal: String? = null,
    val dietaryRestrictions: String? = null,
    val allergies: String? = null,
)

@Serializable
data class AiMealChatResponse(
    val type: AiMealResponseType,
    val message: String,
    val mealPlan: AiMealPlanData? = null,
)

// ---------------------------------------------------------------------------
// Save / replace endpoints
// ---------------------------------------------------------------------------

/**
 * Body of `POST /api/v1/ai/meal/save` and `PUT /api/v1/ai/meal/save/{id}`.
 *
 * Mirrors [AiMealPlanData] with the [goal] explicitly set by the presenter
 * from the user's selection on the goal screen — the AI may have produced its
 * own goal hint inside the plan, but we always trust the explicit user choice
 * for persistence so the saved record matches what the user intended.
 */
@Serializable
data class SaveMealPlanRequest(
    val name: String,
    /**
     * Mirrors [AiMealPlanData.description] — nullable so a plan saved without
     * an AI-provided summary serializes cleanly instead of falling back to an
     * empty string that the backend would have to special-case.
     */
    val description: String? = null,
    val goal: String? = null,
    val totalCalories: Int? = null,
    val meals: List<PlannedMealItem> = emptyList(),
)

@Serializable
data class SaveMealPlanResponse(
    val mealPlanId: String,
)

// ---------------------------------------------------------------------------
// CRUD list / detail
// ---------------------------------------------------------------------------

@Serializable
data class MealPlanShortInfo(
    val id: String,
    val name: String,
    val description: String? = null,
    val goal: String? = null,
    val totalCalories: Int? = null,
    val mealsCount: Int = 0,
    /**
     * Origin tag the backend stamps on the plan ("USER" for manual,
     * "AI" for AI-generated, ...). Defaulted because the field was added
     * after this DTO shipped — keeps an old payload deserializable.
     */
    val createdBy: String? = null,
    val createdAt: String,
)

/**
 * Persisted full plan returned by `GET /api/v1/meal-plans/{id}`. The wire
 * shape mirrors [AiMealPlanData] but every nested item carries a server id so
 * future per-meal / per-dish edit endpoints can patch a single node without
 * having to refetch the whole plan.
 *
 * [scheduleType], [scheduleDays] and [oneOffDate] describe when the plan is
 * meant to be shown:
 *  - `scheduleType == "RECURRING"` → [scheduleDays] is non-empty, every entry
 *    is an upper-case `DayOfWeek` name (e.g. `"MONDAY"`).
 *  - `scheduleType == "ONE_TIME"`  → [oneOffDate] is set to an ISO-8601
 *    `yyyy-MM-dd` date string.
 *  - `scheduleType == null`        → legacy plan saved before scheduling
 *    existed; treat as "not visible on today's home".
 *
 * Strings are used (instead of `kotlinx.datetime.LocalDate` / a wire enum) so
 * the response stays trivially deserializable in `commonMain` without forcing
 * every consumer to depend on `kotlinx-datetime`.
 */
@Serializable
data class MealPlanDetail(
    val id: String,
    val name: String,
    val description: String? = null,
    val goal: String? = null,
    val totalCalories: Int? = null,
    /**
     * Origin tag stamped by the backend. Defaulted for forward-compat with
     * older payloads that did not yet include the field.
     */
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
)
