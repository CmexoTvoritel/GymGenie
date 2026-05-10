package com.asc.gymgenie.nutrition.repository

import com.asc.gymgenie.nutrition.entity.FoodCategory
import com.asc.gymgenie.nutrition.entity.FoodProductEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface FoodProductRepository : JpaRepository<FoodProductEntity, UUID> {

    fun findByIsActiveTrue(): List<FoodProductEntity>

    fun findByCategoryAndIsActiveTrue(category: FoodCategory): List<FoodProductEntity>

    fun findByNameRuContainingIgnoreCaseAndIsActiveTrue(query: String): List<FoodProductEntity>

    fun findByCategoryAndNameRuContainingIgnoreCaseAndIsActiveTrue(
        category: FoodCategory,
        query: String
    ): List<FoodProductEntity>
}
