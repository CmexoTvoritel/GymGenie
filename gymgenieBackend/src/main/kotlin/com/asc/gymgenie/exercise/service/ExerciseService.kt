package com.asc.gymgenie.exercise.service

import com.asc.gymgenie.common.exception.NotFoundException
import com.asc.gymgenie.exercise.dto.*
import com.asc.gymgenie.exercise.entity.DifficultyLevel
import com.asc.gymgenie.exercise.entity.ExerciseCategory
import com.asc.gymgenie.exercise.entity.ExerciseEntity
import com.asc.gymgenie.exercise.entity.MuscleGroup
import com.asc.gymgenie.exercise.repository.ExerciseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class ExerciseService(
    private val exerciseRepository: ExerciseRepository
) {

    fun getById(id: UUID): ExerciseResponse {
        val exercise = findById(id)
        return exercise.toResponse()
    }

    fun getAll(
        muscleGroup: MuscleGroup?,
        category: ExerciseCategory?,
        difficultyLevel: DifficultyLevel?,
        page: Int,
        size: Int
    ): PagedResponse<ExerciseShortResponse> {
        val pageable = PageRequest.of(page, size, Sort.by("nameRu").ascending())
        val result = exerciseRepository.findWithFilters(muscleGroup, category, difficultyLevel, pageable)
        return result.toPagedShortResponse()
    }

    fun search(query: String, page: Int, size: Int): PagedResponse<ExerciseShortResponse> {
        val pageable = PageRequest.of(page, size, Sort.by("nameRu").ascending())
        val result = exerciseRepository.search(query, pageable)
        return result.toPagedShortResponse()
    }

    @Transactional
    fun create(request: CreateExerciseRequest): ExerciseResponse {
        val exercise = ExerciseEntity(
            nameRu = request.nameRu,
            nameEn = request.nameEn,
            description = request.description,
            muscleGroup = request.muscleGroup,
            category = request.category,
            difficultyLevel = request.difficultyLevel,
            durationMinutes = request.durationMinutes,
            caloriesBurned = request.caloriesBurned,
            rating = request.rating,
            imageUrl = request.imageUrl,
            videoUrl = request.videoUrl,
            instructions = request.instructions,
            equipment = request.equipment
        )
        return exerciseRepository.save(exercise).toResponse()
    }

    @Transactional
    fun update(id: UUID, request: UpdateExerciseRequest): ExerciseResponse {
        val exercise = findById(id)

        request.nameRu?.let { exercise.nameRu = it }
        request.nameEn?.let { exercise.nameEn = it }
        request.description?.let { exercise.description = it }
        request.muscleGroup?.let { exercise.muscleGroup = it }
        request.category?.let { exercise.category = it }
        request.difficultyLevel?.let { exercise.difficultyLevel = it }
        request.durationMinutes?.let { exercise.durationMinutes = it }
        request.caloriesBurned?.let { exercise.caloriesBurned = it }
        request.rating?.let { exercise.rating = it }
        request.imageUrl?.let { exercise.imageUrl = it }
        request.videoUrl?.let { exercise.videoUrl = it }
        request.instructions?.let { exercise.instructions = it }
        request.equipment?.let { exercise.equipment = it }

        return exerciseRepository.save(exercise).toResponse()
    }

    @Transactional
    fun delete(id: UUID) {
        if (!exerciseRepository.existsById(id)) {
            throw NotFoundException("Exercise not found")
        }
        exerciseRepository.deleteById(id)
    }

    private fun findById(id: UUID): ExerciseEntity {
        return exerciseRepository.findById(id)
            .orElseThrow { NotFoundException("Exercise not found") }
    }

    private fun ExerciseEntity.toResponse() = ExerciseResponse(
        id = id!!,
        nameRu = nameRu,
        nameEn = nameEn,
        description = description,
        muscleGroup = muscleGroup,
        category = category,
        difficultyLevel = difficultyLevel,
        durationMinutes = durationMinutes,
        caloriesBurned = caloriesBurned,
        rating = rating,
        imageUrl = imageUrl,
        videoUrl = videoUrl,
        instructions = instructions,
        equipment = equipment
    )

    private fun ExerciseEntity.toShortResponse() = ExerciseShortResponse(
        id = id!!,
        nameRu = nameRu,
        nameEn = nameEn,
        muscleGroup = muscleGroup,
        category = category,
        difficultyLevel = difficultyLevel,
        durationMinutes = durationMinutes,
        caloriesBurned = caloriesBurned,
        rating = rating,
        imageUrl = imageUrl
    )

    private fun Page<ExerciseEntity>.toPagedShortResponse() = PagedResponse(
        content = content.map { it.toShortResponse() },
        page = number,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
        last = isLast
    )
}
