package com.asc.gymgenie.nutrition.entity

/**
 * Category for the global food product catalog ([FoodProductEntity]).
 *
 * Independent of meal-plan generation — used only by the catalog/search API.
 */
enum class FoodCategory {
    MEAT,        // Мясо и птица
    FISH,        // Рыба и морепродукты
    DAIRY,       // Молочные продукты
    EGGS,        // Яйца
    GRAINS,      // Злаки и крупы
    LEGUMES,     // Бобовые
    VEGETABLES,  // Овощи и зелень
    FRUITS,      // Фрукты и ягоды
    NUTS_SEEDS,  // Орехи и семена
    OILS,        // Масла
    OTHER        // Другое
}
