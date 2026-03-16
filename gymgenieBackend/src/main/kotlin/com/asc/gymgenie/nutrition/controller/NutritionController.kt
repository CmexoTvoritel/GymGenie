package com.asc.gymgenie.nutrition.controller

import com.asc.gymgenie.nutrition.dto.*
import com.asc.gymgenie.nutrition.service.NutritionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/nutrition")
class NutritionController(
    private val nutritionService: NutritionService
) {

    @GetMapping("/plans")
    fun getAll(authentication: Authentication): ResponseEntity<List<MealPlanShortResponse>> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(nutritionService.getAllByUser(userId))
    }

    @GetMapping("/plans/active")
    fun getActive(authentication: Authentication): ResponseEntity<MealPlanResponse> {
        val userId = UUID.fromString(authentication.name)
        val plan = nutritionService.getActiveByUser(userId)
            ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(plan)
    }

    @GetMapping("/plans/{id}")
    fun getById(
        authentication: Authentication,
        @PathVariable id: UUID
    ): ResponseEntity<MealPlanResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(nutritionService.getById(userId, id))
    }

    @PostMapping("/plans")
    fun create(
        authentication: Authentication,
        @Valid @RequestBody request: CreateMealPlanRequest
    ): ResponseEntity<MealPlanResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.status(HttpStatus.CREATED).body(nutritionService.create(userId, request))
    }

    @PutMapping("/plans/{id}")
    fun update(
        authentication: Authentication,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateMealPlanRequest
    ): ResponseEntity<MealPlanResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(nutritionService.update(userId, id, request))
    }

    @DeleteMapping("/plans/{id}")
    fun delete(
        authentication: Authentication,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        val userId = UUID.fromString(authentication.name)
        nutritionService.delete(userId, id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/days/{dayId}/meals")
    fun addMealToDay(
        authentication: Authentication,
        @PathVariable dayId: UUID,
        @Valid @RequestBody request: CreateMealRequest
    ): ResponseEntity<MealPlanResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.status(HttpStatus.CREATED).body(nutritionService.addMealToDay(userId, dayId, request))
    }

    @PutMapping("/meals/{mealId}")
    fun updateMeal(
        authentication: Authentication,
        @PathVariable mealId: UUID,
        @Valid @RequestBody request: UpdateMealRequest
    ): ResponseEntity<MealResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(nutritionService.updateMeal(userId, mealId, request))
    }

    @PostMapping("/meals/{mealId}/items")
    fun addItemToMeal(
        authentication: Authentication,
        @PathVariable mealId: UUID,
        @Valid @RequestBody request: CreateMealItemRequest
    ): ResponseEntity<MealResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.status(HttpStatus.CREATED).body(nutritionService.addItemToMeal(userId, mealId, request))
    }

    @PutMapping("/meal-items/{itemId}")
    fun updateMealItem(
        authentication: Authentication,
        @PathVariable itemId: UUID,
        @Valid @RequestBody request: UpdateMealItemRequest
    ): ResponseEntity<MealItemResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(nutritionService.updateMealItem(userId, itemId, request))
    }

    @DeleteMapping("/meal-items/{itemId}")
    fun deleteMealItem(
        authentication: Authentication,
        @PathVariable itemId: UUID
    ): ResponseEntity<Void> {
        val userId = UUID.fromString(authentication.name)
        nutritionService.deleteMealItem(userId, itemId)
        return ResponseEntity.noContent().build()
    }
}
