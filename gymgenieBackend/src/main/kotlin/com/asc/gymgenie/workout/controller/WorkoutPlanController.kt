package com.asc.gymgenie.workout.controller

import com.asc.gymgenie.exercise.dto.PagedResponse
import com.asc.gymgenie.workout.dto.*
import com.asc.gymgenie.workout.service.WorkoutPlanService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/workout-plans")
class WorkoutPlanController(
    private val workoutPlanService: WorkoutPlanService
) {

    @GetMapping
    fun getAll(
        authentication: Authentication,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedResponse<WorkoutPlanShortResponse>> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(workoutPlanService.getAllByUser(userId, page, size))
    }

    @GetMapping("/active")
    fun getActive(authentication: Authentication): ResponseEntity<List<WorkoutPlanShortResponse>> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(workoutPlanService.getActiveByUser(userId))
    }

    @GetMapping("/{id}")
    fun getById(
        authentication: Authentication,
        @PathVariable id: UUID
    ): ResponseEntity<WorkoutPlanResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(workoutPlanService.getById(userId, id))
    }

    @PostMapping
    fun create(
        authentication: Authentication,
        @Valid @RequestBody request: CreateWorkoutPlanRequest
    ): ResponseEntity<WorkoutPlanResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.status(HttpStatus.CREATED).body(workoutPlanService.create(userId, request))
    }

    @PostMapping("/simple")
    fun createSimplePlan(
        authentication: Authentication,
        @Valid @RequestBody request: CreateSimpleWorkoutRequest
    ): ResponseEntity<WorkoutPlanResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(workoutPlanService.createSimpleWorkout(userId, request))
    }

    @PutMapping("/{id}")
    fun update(
        authentication: Authentication,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateWorkoutPlanRequest
    ): ResponseEntity<WorkoutPlanResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(workoutPlanService.update(userId, id, request))
    }

    @PostMapping("/{id}/days")
    fun addDay(
        authentication: Authentication,
        @PathVariable id: UUID,
        @Valid @RequestBody request: CreateWorkoutPlanDayRequest
    ): ResponseEntity<WorkoutPlanResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.status(HttpStatus.CREATED).body(workoutPlanService.addDay(userId, id, request))
    }

    @DeleteMapping("/{id}")
    fun delete(
        authentication: Authentication,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        val userId = UUID.fromString(authentication.name)
        workoutPlanService.delete(userId, id)
        return ResponseEntity.noContent().build()
    }
}
