package com.asc.gymgenie.nutrition

import kotlinx.serialization.Serializable

@Serializable
data class BookedDaysResponse(
    val recurringDays: List<String> = emptyList(),
    val oneOffDates: List<String> = emptyList(),
)

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

data class AddedMealItem(
    val uid: Long,
    val product: FoodProduct,
    val grams: Double,
    val hasCatalogProduct: Boolean = true,
) {

    val portion: FoodPortionMacros get() = product.macrosForGrams(grams)
}

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

enum class ManualScheduleMode(val wireValue: String) {
    ONE_OFF("ONE_TIME"),
    RECURRING("RECURRING"),
}
