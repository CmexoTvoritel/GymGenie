package com.asc.gymgenie.nutrition

import kotlinx.serialization.Serializable

/**
 * Wire format for `GET /api/v1/products` and `GET /api/v1/products/{id}`.
 *
 * Kept internal to the nutrition module so transport-layer concerns
 * (kotlinx.serialization annotations, optional-with-default fields) never
 * leak into the [FoodProduct] domain model that presenters consume.
 */
@Serializable
internal data class FoodProductResponse(
    val id: String,
    val nameRu: String,
    val nameEn: String? = null,
    val category: String,
    val emoji: String? = null,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val carbsPer100g: Double,
    val fiberPer100g: Double? = null,
    val sugarPer100g: Double? = null,
)

internal fun FoodProductResponse.toDomain(): FoodProduct = FoodProduct(
    id = id,
    nameRu = nameRu,
    nameEn = nameEn?.takeIf { it.isNotBlank() },
    category = FoodCategory.fromKeyOrOther(category),
    emoji = emoji?.takeIf { it.isNotBlank() },
    caloriesPer100g = caloriesPer100g,
    proteinPer100g = proteinPer100g,
    fatPer100g = fatPer100g,
    carbsPer100g = carbsPer100g,
    fiberPer100g = fiberPer100g,
    sugarPer100g = sugarPer100g,
)
