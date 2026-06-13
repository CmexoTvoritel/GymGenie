package com.asc.gymgenie.feature.food_picker

import com.asc.gymgenie.R
import com.asc.gymgenie.nutrition.FoodCategory

internal fun FoodCategory.categoryDrawable(): Int = when (this) {
    FoodCategory.MEAT -> R.drawable.ic_food_meat
    FoodCategory.FISH -> R.drawable.ic_food_fish
    FoodCategory.DAIRY -> R.drawable.ic_food_milk
    FoodCategory.EGGS -> R.drawable.ic_food_egg
    FoodCategory.GRAINS -> R.drawable.ic_food_cereals
    FoodCategory.LEGUMES -> R.drawable.ic_food_legumes
    FoodCategory.VEGETABLES -> R.drawable.ic_food_vegetables
    FoodCategory.FRUITS -> R.drawable.ic_food_fruits
    FoodCategory.NUTS_SEEDS -> R.drawable.ic_food_nuts
    FoodCategory.OILS -> R.drawable.ic_food_oil
    FoodCategory.OTHER -> R.drawable.ic_food_other
}

internal fun deriveFoodDrawable(dishName: String): Int {
    val lower = dishName.lowercase()
    return when {
        lower.containsAny("курица", "куриц", "грудка", "мяс", "говядин", "свинин", "стейк", "индейк") -> R.drawable.ic_food_meat
        lower.containsAny("рыб", "лосось", "тунец", "форель", "сёмга", "семга") -> R.drawable.ic_food_fish
        lower.containsAny("яйц", "омлет") -> R.drawable.ic_food_egg
        lower.containsAny("молок", "творог", "йогурт", "кефир", "сыр", "сметан") -> R.drawable.ic_food_milk
        lower.containsAny("каш", "овсян", "рис", "гречк", "хлеб", "тост", "паст", "макарон", "спагетт") -> R.drawable.ic_food_cereals
        lower.containsAny("салат", "овощ", "капуст", "помидор", "огурц", "морков", "свекл", "брокколи") -> R.drawable.ic_food_vegetables
        lower.containsAny("фрукт", "яблок", "банан", "апельсин", "ягод") -> R.drawable.ic_food_fruits
        lower.containsAny("орех", "миндал", "фундук", "кешью", "семечк") -> R.drawable.ic_food_nuts
        lower.containsAny("масл", "оливк") -> R.drawable.ic_food_oil
        lower.containsAny("фасол", "горох", "чечевиц", "нут", "бобов") -> R.drawable.ic_food_legumes
        else -> R.drawable.ic_food_other
    }
}

private fun String.containsAny(vararg needles: String): Boolean = needles.any { this.contains(it) }
