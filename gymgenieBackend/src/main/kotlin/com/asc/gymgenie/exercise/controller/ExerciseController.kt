package com.asc.gymgenie.exercise.controller

import com.asc.gymgenie.exercise.dto.*
import com.asc.gymgenie.exercise.entity.DifficultyLevel
import com.asc.gymgenie.exercise.entity.ExerciseCategory
import com.asc.gymgenie.exercise.entity.MuscleGroup
import com.asc.gymgenie.exercise.service.ExerciseService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/exercises")
class ExerciseController(
    private val exerciseService: ExerciseService
) {

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): ResponseEntity<ExerciseResponse> {
        return ResponseEntity.ok(exerciseService.getById(id))
    }

    @GetMapping
    fun getAll(
        @RequestParam(required = false) muscleGroup: MuscleGroup?,
        @RequestParam(required = false) category: ExerciseCategory?,
        @RequestParam(required = false) difficultyLevel: DifficultyLevel?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedResponse<ExerciseShortResponse>> {
        return ResponseEntity.ok(exerciseService.getAll(muscleGroup, category, difficultyLevel, page, size))
    }

    @GetMapping("/search")
    fun search(
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedResponse<ExerciseShortResponse>> {
        return ResponseEntity.ok(exerciseService.search(query, page, size))
    }

    @PostMapping
    fun create(@Valid @RequestBody request: CreateExerciseRequest): ResponseEntity<ExerciseResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(exerciseService.create(request))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateExerciseRequest
    ): ResponseEntity<ExerciseResponse> {
        return ResponseEntity.ok(exerciseService.update(id, request))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        exerciseService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
