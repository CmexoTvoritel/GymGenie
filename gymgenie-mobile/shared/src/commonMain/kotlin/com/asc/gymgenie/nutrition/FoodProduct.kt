package com.asc.gymgenie.nutrition

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

        fun fromKeyOrOther(raw: String): FoodCategory =
            entries.firstOrNull { it.name == raw } ?: OTHER
    }
}

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
