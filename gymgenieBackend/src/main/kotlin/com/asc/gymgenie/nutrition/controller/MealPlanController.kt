package com.asc.gymgenie.nutrition.controller

import com.asc.gymgenie.exercise.dto.PagedResponse
import com.asc.gymgenie.nutrition.dto.BookedDaysResponse
import com.asc.gymgenie.nutrition.dto.CreateManualMealPlanRequest
import com.asc.gymgenie.nutrition.dto.MealPlanDetailResponse
import com.asc.gymgenie.nutrition.dto.MealPlanShortResponse
import com.asc.gymgenie.nutrition.entity.MealType
import com.asc.gymgenie.nutrition.service.MealPlanService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/meal-plans")
class MealPlanController(
    private val mealPlanService: MealPlanService
) {

    @GetMapping
    fun getAll(
        authentication: Authentication,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedResponse<MealPlanShortResponse>> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(mealPlanService.getAllByUser(userId, page, size))
    }

    @GetMapping("/booked-days")
    fun getBookedDays(
        authentication: Authentication,
        @RequestParam mealType: MealType
    ): ResponseEntity<BookedDaysResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(mealPlanService.getBookedDays(userId, mealType))
    }

    @GetMapping("/{id}")
    fun getById(
        authentication: Authentication,
        @PathVariable id: UUID
    ): ResponseEntity<MealPlanDetailResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(mealPlanService.getById(userId, id))
    }

    @PostMapping("/manual")
    fun createManual(
        authentication: Authentication,
        @Valid @RequestBody request: CreateManualMealPlanRequest
    ): ResponseEntity<MealPlanDetailResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(mealPlanService.createManualMealPlan(userId, request))
    }

    @DeleteMapping("/{id}")
    fun delete(
        authentication: Authentication,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        val userId = UUID.fromString(authentication.name)
        mealPlanService.delete(userId, id)
        return ResponseEntity.noContent().build()
    }
}
