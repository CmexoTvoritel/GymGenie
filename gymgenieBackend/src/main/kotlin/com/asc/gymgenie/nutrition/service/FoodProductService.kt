package com.asc.gymgenie.nutrition.service

import com.asc.gymgenie.common.exception.NotFoundException
import com.asc.gymgenie.nutrition.dto.FoodProductResponse
import com.asc.gymgenie.nutrition.entity.FoodCategory
import com.asc.gymgenie.nutrition.entity.FoodProductEntity
import com.asc.gymgenie.nutrition.repository.FoodProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class FoodProductService(
    private val foodProductRepository: FoodProductRepository
) {

    fun search(query: String?, category: FoodCategory?): List<FoodProductResponse> {
        val normalizedQuery = query?.trim()?.takeIf { it.isNotEmpty() }

        val products: List<FoodProductEntity> = when {
            normalizedQuery != null && category != null ->
                foodProductRepository.findByCategoryAndNameRuContainingIgnoreCaseAndIsActiveTrue(
                    category = category,
                    query = normalizedQuery
                )

            normalizedQuery != null ->
                foodProductRepository.findByNameRuContainingIgnoreCaseAndIsActiveTrue(normalizedQuery)

            category != null ->
                foodProductRepository.findByCategoryAndIsActiveTrue(category)

            else ->
                foodProductRepository.findByIsActiveTrue()
        }

        return products
            .sortedBy { it.nameRu.lowercase() }
            .map { it.toResponse() }
    }

    fun getById(id: UUID): FoodProductResponse {
        return foodProductRepository.findById(id)
            .orElseThrow { NotFoundException("Food product not found") }
            .toResponse()
    }

    private fun FoodProductEntity.toResponse() = FoodProductResponse(
        id = id!!,
        nameRu = nameRu,
        nameEn = nameEn,
        category = category,
        emoji = emoji,
        caloriesPer100g = caloriesPer100g,
        proteinPer100g = proteinPer100g,
        fatPer100g = fatPer100g,
        carbsPer100g = carbsPer100g,
        fiberPer100g = fiberPer100g,
        sugarPer100g = sugarPer100g
    )
}
