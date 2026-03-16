package com.asc.gymgenie.workout.controller

import com.asc.gymgenie.exercise.dto.PagedResponse
import com.asc.gymgenie.workout.dto.*
import com.asc.gymgenie.workout.service.WorkoutSessionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/v1/workout-sessions")
class WorkoutSessionController(
    private val workoutSessionService: WorkoutSessionService
) {

    @GetMapping
    fun getAll(
        authentication: Authentication,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedResponse<WorkoutSessionShortResponse>> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(workoutSessionService.getAllByUser(userId, page, size))
    }

    @GetMapping("/by-date")
    fun getByDate(
        authentication: Authentication,
        @RequestParam date: LocalDate
    ): ResponseEntity<List<WorkoutSessionShortResponse>> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(workoutSessionService.getByDate(userId, date))
    }

    @GetMapping("/{id}")
    fun getById(
        authentication: Authentication,
        @PathVariable id: UUID
    ): ResponseEntity<WorkoutSessionResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(workoutSessionService.getById(userId, id))
    }

    @PostMapping
    fun start(
        authentication: Authentication,
        @Valid @RequestBody request: StartWorkoutSessionRequest
    ): ResponseEntity<WorkoutSessionResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.status(HttpStatus.CREATED).body(workoutSessionService.start(userId, request))
    }

    @PostMapping("/{id}/sets")
    fun addSet(
        authentication: Authentication,
        @PathVariable id: UUID,
        @Valid @RequestBody request: AddSessionSetRequest
    ): ResponseEntity<WorkoutSessionResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.status(HttpStatus.CREATED).body(workoutSessionService.addSet(userId, id, request))
    }

    @PutMapping("/{sessionId}/sets/{setId}")
    fun updateSet(
        authentication: Authentication,
        @PathVariable sessionId: UUID,
        @PathVariable setId: UUID,
        @Valid @RequestBody request: UpdateSessionSetRequest
    ): ResponseEntity<WorkoutSessionResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(workoutSessionService.updateSet(userId, sessionId, setId, request))
    }

    @PostMapping("/{id}/finish")
    fun finish(
        authentication: Authentication,
        @PathVariable id: UUID,
        @Valid @RequestBody request: FinishWorkoutSessionRequest
    ): ResponseEntity<WorkoutSessionResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(workoutSessionService.finish(userId, id, request))
    }

    @DeleteMapping("/{id}")
    fun delete(
        authentication: Authentication,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        val userId = UUID.fromString(authentication.name)
        workoutSessionService.delete(userId, id)
        return ResponseEntity.noContent().build()
    }
}
