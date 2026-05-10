package com.asc.gymgenie.nutrition

/**
 * Domain model for a food product served by the nutrition catalog API.
 *
 * The model intentionally lives in `commonMain` so iOS and Android share the
 * exact same shape after the DTO->domain mapping. Per-100g macro values are
 * `Double` to preserve fractional accuracy when the UI scales them to the
 * actually-consumed gram amount.
 */
data class FoodProduct(
    val id: String,
    val nameRu: String,
    val nameEn: String?,
    val category: FoodCategory,
    val emoji: String?,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val carbsPer100g: Double,
    val fiberPer100g: Double?,
    val sugarPer100g: Double?,
)

/**
 * Server-driven taxonomy of food categories.
 *
 * The enum is the canonical source of category keys for filter requests
 * (`GET /api/v1/products?category=MEAT`) — the names must stay in sync with
 * the backend `FoodCategory` enum. Localized labels are provided by
 * [displayName] so the UI never hard-codes them.
 */
enum class FoodCategory {
    MEAT,
    FISH,
    DAIRY,
    EGGS,
    GRAINS,
    LEGUMES,
    VEGETABLES,
    FRUITS,
    NUTS_SEEDS,
    OILS,
    OTHER;

    fun displayName(): String = when (this) {
        MEAT -> "Мясо"
        FISH -> "Рыба"
        DAIRY -> "Молочные"
        EGGS -> "Яйца"
        GRAINS -> "Злаки"
        LEGUMES -> "Бобовые"
        VEGETABLES -> "Овощи"
        FRUITS -> "Фрукты"
        NUTS_SEEDS -> "Орехи"
        OILS -> "Масла"
        OTHER -> "Другое"
    }

    companion object {
        /**
         * Safe parse used by the DTO mapper. Unknown categories from the
         * server fall back to [OTHER] instead of throwing — the catalog must
         * keep rendering even if the backend introduces a new key the
         * mobile build hasn't shipped yet.
         */
        fun fromKeyOrOther(raw: String): FoodCategory =
            entries.firstOrNull { it.name == raw } ?: OTHER
    }
}

/**
 * Calculates the absolute kcal/protein/fat/carbs for a given gram amount.
 *
 * Kept on the domain object so both the picker preview and any future meal
 * editor can share the same arithmetic without duplicating the per-100g
 * scaling rule.
 */
data class FoodPortionMacros(
    val calories: Double,
    val proteinG: Double,
    val fatG: Double,
    val carbsG: Double,
    val fiberG: Double?,
    val sugarG: Double?,
)

fun FoodProduct.macrosForGrams(grams: Double): FoodPortionMacros {
    val factor = grams / 100.0
    return FoodPortionMacros(
        calories = caloriesPer100g * factor,
        proteinG = proteinPer100g * factor,
        fatG = fatPer100g * factor,
        carbsG = carbsPer100g * factor,
        fiberG = fiberPer100g?.let { it * factor },
        sugarG = sugarPer100g?.let { it * factor },
    )
}
