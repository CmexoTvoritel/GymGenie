package com.asc.gymgenie.nutrition

import kotlinx.serialization.Serializable

/**
 * Wire format and small helpers for the manual meal-plan creation flow.
 *
 * Three concerns are kept here intentionally:
 *  - request/response DTOs for the two new endpoints
 *    (`/api/v1/meal-plans/booked-days`, `/api/v1/meal-plans/manual`)
 *  - the `AddedMealItem` domain row that the editor screen accumulates before
 *    calling the create endpoint
 *  - the `MealKind` enum that drives the kcal-range hint and the wire
 *    `mealType` value
 *
 * The flow deliberately reuses [FoodProduct] (already defined in
 * `FoodProduct.kt`) instead of introducing a parallel "lite" DTO: a single
 * domain shape keeps the picker, info screen, grams sheet, and editor list
 * consistent without duplicating mappers per surface.
 */

// ---------------------------------------------------------------------------
// Booked days view
// ---------------------------------------------------------------------------

/**
 * Slots already occupied for a given meal type.
 *
 * `recurringDays` contains upper-case `DayOfWeek` names ("MONDAY", ...) and
 * `oneOffDates` contains ISO-8601 (`yyyy-MM-dd`) strings — both shapes match
 * the backend `BookedDaysResponse`. Strings are used (instead of platform date
 * types) so the response is trivially de/serializable in `commonMain`.
 */
@Serializable
data class BookedDaysResponse(
    val recurringDays: List<String> = emptyList(),
    val oneOffDates: List<String> = emptyList(),
)

// ---------------------------------------------------------------------------
// Create-manual-plan request
// ---------------------------------------------------------------------------

/**
 * One catalog product portion in the create request.
 *
 * Grams are typed as `Double` to match the backend's
 * `@DecimalMin/@DecimalMax` constraint (0.1..5000.0) — fractional grams are
 * unusual in the UI, but the wire contract is decimal, and forcing `Int`
 * here would silently truncate at the JSON boundary.
 */
@Serializable
data class ManualMealItemRequest(
    val foodProductId: String? = null,
    val grams: Double,
    val name: String? = null,
    val calories: Int? = null,
    val proteinG: Int? = null,
    val carbsG: Int? = null,
    val fatG: Int? = null,
)

/**
 * Body of `POST /api/v1/meal-plans/manual`.
 *
 * Fields are flat and string-typed where the backend uses enums (mealType,
 * goal, scheduleType): the manual-flow presenter is the source of truth for
 * which enum values are valid, and `String` keeps the wire shape forward-
 * compatible if the backend introduces a new value the mobile build does not
 * yet recognise.
 *
 * Both [scheduleDays] and [oneOffDate] are populated conditionally — the
 * backend rejects RECURRING with empty days and ONE_TIME without a date, but
 * the presenter builds the request only when [CreateMealPlanUiState.canSave]
 * is true so that contract is enforced before the call leaves the device.
 */
@Serializable
data class CreateManualMealPlanRequest(
    val name: String,
    val description: String? = null,
    val mealType: String,
    val goal: String? = null,
    val scheduleType: String,
    val scheduleDays: List<String> = emptyList(),
    val oneOffDate: String? = null,
    val items: List<ManualMealItemRequest>,
)

// ---------------------------------------------------------------------------
// Domain row inside the editor
// ---------------------------------------------------------------------------

/**
 * Item accumulated by the editor before the plan is saved.
 *
 * Holds the originally-picked [FoodProduct] (so the editor row keeps emoji,
 * name, per-100g macros to render the live totals) plus the user-chosen
 * portion in grams and a unique [uid] used as a stable list-row key.
 *
 * `uid` is allocated client-side (random Long) because the editor accumulates
 * items locally — the backend will assign real ids only after `save()`.
 */
data class AddedMealItem(
    val uid: Long,
    val product: FoodProduct,
    val grams: Double,
    val hasCatalogProduct: Boolean = true,
) {
    /** Live macro totals scaled to the chosen portion. */
    val portion: FoodPortionMacros get() = product.macrosForGrams(grams)
}

// ---------------------------------------------------------------------------
// Meal kind (UI hint + wire mapping)
// ---------------------------------------------------------------------------

/**
 * Three meal kinds the manual flow lets the user create.
 *
 * `wireValue` is what we ship in `CreateManualMealPlanRequest.mealType` and
 * matches the backend `MealType` enum names. The kcal range hint is
 * UI-only — the server does not validate it.
 */
enum class ManualMealKind(
    val wireValue: String,
    val displayName: String,
    val kcalHintRu: String,
) {
    BREAKFAST("BREAKFAST", "Завтрак", "350–500 ккал"),
    LUNCH("LUNCH", "Обед", "600–800 ккал"),
    DINNER("DINNER", "Ужин", "400–600 ккал");

    companion object {
        fun fromWireValue(value: String?): ManualMealKind? =
            entries.firstOrNull { it.wireValue == value }
    }
}

// ---------------------------------------------------------------------------
// Schedule mode
// ---------------------------------------------------------------------------

/**
 * The two scheduling modes available in the setup screen.
 *
 * Modeled as an enum (instead of two free-form strings) so the presenter
 * cannot accidentally produce an invalid mode and the wire conversion is
 * centralised in [wireValue].
 */
enum class ManualScheduleMode(val wireValue: String) {
    ONE_OFF("ONE_TIME"),
    RECURRING("RECURRING"),
}
