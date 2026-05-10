package com.asc.gymgenie.ai.controller

import com.asc.gymgenie.ai.dto.*
import com.asc.gymgenie.ai.service.WorkoutAiService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/ai/chat")
class AiChatController(private val workoutAiService: WorkoutAiService) {

    @PostMapping
    fun chat(
        authentication: Authentication,
        @Valid @RequestBody request: AiChatRequest
    ): ResponseEntity<AiChatResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(workoutAiService.chat(userId, request))
    }

    @PostMapping("/save")
    fun saveWorkout(
        authentication: Authentication,
        @Valid @RequestBody request: SaveWorkoutRequest
    ): ResponseEntity<SaveWorkoutResponse> {
        val userId = UUID.fromString(authentication.name)
        val planId = workoutAiService.saveWorkout(userId, request)
        return ResponseEntity.ok(SaveWorkoutResponse(planId))
    }

    @PutMapping("/save/{planId}")
    fun replaceWorkout(
        authentication: Authentication,
        @PathVariable planId: UUID,
        @Valid @RequestBody request: SaveWorkoutRequest
    ): ResponseEntity<SaveWorkoutResponse> {
        val userId = UUID.fromString(authentication.name)
        val id = workoutAiService.replaceWorkout(userId, planId, request)
        return ResponseEntity.ok(SaveWorkoutResponse(id))
    }

    @DeleteMapping("/session")
    fun clearSession(authentication: Authentication): ResponseEntity<Void> {
        val userId = UUID.fromString(authentication.name)
        workoutAiService.clearSession(userId)
        return ResponseEntity.noContent().build()
    }
}
