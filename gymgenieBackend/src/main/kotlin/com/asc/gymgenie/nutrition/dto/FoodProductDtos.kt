package com.asc.gymgenie.nutrition.dto

import com.asc.gymgenie.nutrition.entity.FoodCategory
import java.util.*

data class FoodProductResponse(
    val id: UUID,
    val nameRu: String,
    val nameEn: String?,
    val category: FoodCategory,
    val emoji: String?,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val carbsPer100g: Double,
    val fiberPer100g: Double?,
    val sugarPer100g: Double?
)
