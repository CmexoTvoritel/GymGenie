package com.asc.gymgenie.ai.nutrition

import com.asc.gymgenie.ai.nutrition.dto.AiMealChatRequest
import com.asc.gymgenie.ai.nutrition.dto.AiMealChatResponse
import com.asc.gymgenie.ai.nutrition.dto.SaveMealPlanRequest
import com.asc.gymgenie.ai.nutrition.dto.SaveMealPlanResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/ai/meal")
class AiMealController(
    private val mealAiService: MealAiService
) {

    @PostMapping("/chat")
    fun chat(
        authentication: Authentication,
        @Valid @RequestBody request: AiMealChatRequest
    ): ResponseEntity<AiMealChatResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(mealAiService.chat(userId, request))
    }

    @PostMapping("/save")
    fun saveMealPlan(
        authentication: Authentication,
        @Valid @RequestBody request: SaveMealPlanRequest
    ): ResponseEntity<SaveMealPlanResponse> {
        val userId = UUID.fromString(authentication.name)
        val planId = mealAiService.saveMealPlan(userId, request)
        return ResponseEntity.ok(SaveMealPlanResponse(planId))
    }

    @PutMapping("/save/{planId}")
    fun replaceMealPlan(
        authentication: Authentication,
        @PathVariable planId: UUID,
        @Valid @RequestBody request: SaveMealPlanRequest
    ): ResponseEntity<SaveMealPlanResponse> {
        val userId = UUID.fromString(authentication.name)
        val id = mealAiService.replaceMealPlan(userId, planId, request)
        return ResponseEntity.ok(SaveMealPlanResponse(id))
    }

    @DeleteMapping("/session")
    fun clearSession(authentication: Authentication): ResponseEntity<Void> {
        val userId = UUID.fromString(authentication.name)
        mealAiService.clearSession(userId)
        return ResponseEntity.noContent().build()
    }
}
