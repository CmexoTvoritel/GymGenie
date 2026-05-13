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
        difficultyLevels: List<DifficultyLevel>,
        requiresEquipment: Boolean?,
        sortByDifficulty: String?,
        sortByCalories: String?,
        page: Int,
        size: Int
    ): PagedResponse<ExerciseShortResponse> {
        val pageable = PageRequest.of(page, size, resolveSort(sortByDifficulty, sortByCalories))
        val result = exerciseRepository.findWithFilters(
            muscleGroup = muscleGroup,
            category = category,
            difficultyLevels = difficultyLevels.ifEmpty { null },
            requiresWeight = requiresEquipment,
            pageable = pageable
        )
        return result.toPagedShortResponse()
    }

    fun search(
        query: String,
        difficultyLevels: List<DifficultyLevel>,
        requiresEquipment: Boolean?,
        sortByDifficulty: String?,
        sortByCalories: String?,
        page: Int,
        size: Int
    ): PagedResponse<ExerciseShortResponse> {
        val pageable = PageRequest.of(page, size, resolveSort(sortByDifficulty, sortByCalories))
        val result = exerciseRepository.search(
            query = query,
            difficultyLevels = difficultyLevels.ifEmpty { null },
            requiresWeight = requiresEquipment,
            pageable = pageable
        )
        return result.toPagedShortResponse()
    }

    private fun resolveSort(sortByDifficulty: String?, sortByCalories: String?): Sort {
        val orders = mutableListOf<Sort.Order>()
        if (sortByDifficulty != null) {
            val direction = if (sortByDifficulty.uppercase() == "ASC") Sort.Direction.ASC else Sort.Direction.DESC
            orders.add(Sort.Order(direction, "difficultyOrder"))
        }
        if (sortByCalories != null) {
            if (sortByCalories.uppercase() == "ASC") {
                orders.add(Sort.Order.asc("caloriesBurned").nullsLast())
            } else {
                orders.add(Sort.Order.desc("caloriesBurned").nullsLast())
            }
        }
        return if (orders.isEmpty()) Sort.by(Sort.Order.asc("nameRu")) else Sort.by(orders)
    }

    fun getMuscleGroups(): List<MuscleGroupInfo> {
        return MuscleGroup.entries.map { group ->
            MuscleGroupInfo(
                key = group.name,
                nameRu = MUSCLE_GROUP_NAMES_RU.getValue(group),
                nameEn = MUSCLE_GROUP_NAMES_EN.getValue(group),
                imageUrl = null
            )
        }
    }

    @Transactional
    fun create(request: CreateExerciseRequest): ExerciseResponse {
        val exercise = ExerciseEntity(
            nameRu = request.nameRu,
            nameEn = request.nameEn,
            description = request.description,
            muscleGroup = request.muscleGroup,
            secondaryMuscleGroups = request.secondaryMuscleGroups,
            category = request.category,
            difficultyLevel = request.difficultyLevel,
            durationMinutes = request.durationMinutes,
            caloriesBurned = request.caloriesBurned,
            rating = request.rating,
            imageUrl = request.imageUrl,
            videoUrl = request.videoUrl,
            instructions = request.instructions,
            equipment = request.equipment,
            techniqueTip = request.techniqueTip,
            defaultRepsMin = request.defaultRepsMin,
            defaultRepsMax = request.defaultRepsMax,
            defaultWeightPercentage = request.defaultWeightPercentage,
            requiresWeight = request.requiresWeight
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
        request.secondaryMuscleGroups?.let { exercise.secondaryMuscleGroups = it }
        request.category?.let { exercise.category = it }
        request.difficultyLevel?.let { exercise.difficultyLevel = it }
        request.durationMinutes?.let { exercise.durationMinutes = it }
        request.caloriesBurned?.let { exercise.caloriesBurned = it }
        request.rating?.let { exercise.rating = it }
        request.imageUrl?.let { exercise.imageUrl = it }
        request.videoUrl?.let { exercise.videoUrl = it }
        request.instructions?.let { exercise.instructions = it }
        request.equipment?.let { exercise.equipment = it }
        request.techniqueTip?.let { exercise.techniqueTip = it }
        request.defaultRepsMin?.let { exercise.defaultRepsMin = it }
        request.defaultRepsMax?.let { exercise.defaultRepsMax = it }
        request.defaultWeightPercentage?.let { exercise.defaultWeightPercentage = it }
        request.requiresWeight?.let { exercise.requiresWeight = it }

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
        secondaryMuscleGroups = secondaryMuscleGroups,
        category = category,
        difficultyLevel = difficultyLevel,
        durationMinutes = durationMinutes,
        caloriesBurned = caloriesBurned,
        rating = rating,
        imageUrl = imageUrl,
        videoUrl = videoUrl,
        instructions = instructions,
        equipment = equipment,
        techniqueTip = techniqueTip,
        defaultRepsMin = defaultRepsMin,
        defaultRepsMax = defaultRepsMax,
        defaultWeightPercentage = defaultWeightPercentage,
        requiresWeight = requiresWeight
    )

    private fun ExerciseEntity.toShortResponse() = ExerciseShortResponse(
        id = id!!,
        nameRu = nameRu,
        nameEn = nameEn,
        muscleGroup = muscleGroup,
        secondaryMuscleGroups = secondaryMuscleGroups,
        category = category,
        difficultyLevel = difficultyLevel,
        durationMinutes = durationMinutes,
        caloriesBurned = caloriesBurned,
        rating = rating,
        imageUrl = imageUrl,
        requiresWeight = requiresWeight
    )

    private fun Page<ExerciseEntity>.toPagedShortResponse() = PagedResponse(
        content = content.map { it.toShortResponse() },
        page = number,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
        last = isLast
    )

    companion object {
        private val MUSCLE_GROUP_NAMES_RU: Map<MuscleGroup, String> = mapOf(
            MuscleGroup.CHEST to "Грудь",
            MuscleGroup.BACK to "Спина",
            MuscleGroup.SHOULDERS to "Плечи",
            MuscleGroup.BICEPS to "Бицепс",
            MuscleGroup.TRICEPS to "Трицепс",
            MuscleGroup.FOREARMS to "Предплечья",
            MuscleGroup.ABS to "Пресс",
            MuscleGroup.QUADRICEPS to "Квадрицепс",
            MuscleGroup.HAMSTRINGS to "Бицепс бедра",
            MuscleGroup.GLUTES to "Ягодицы",
            MuscleGroup.CALVES to "Икры",
            MuscleGroup.FULL_BODY to "Всё тело",
            MuscleGroup.CARDIO to "Кардио"
        )

        private val MUSCLE_GROUP_NAMES_EN: Map<MuscleGroup, String> = mapOf(
            MuscleGroup.CHEST to "Chest",
            MuscleGroup.BACK to "Back",
            MuscleGroup.SHOULDERS to "Shoulders",
            MuscleGroup.BICEPS to "Biceps",
            MuscleGroup.TRICEPS to "Triceps",
            MuscleGroup.FOREARMS to "Forearms",
            MuscleGroup.ABS to "Abs",
            MuscleGroup.QUADRICEPS to "Quadriceps",
            MuscleGroup.HAMSTRINGS to "Hamstrings",
            MuscleGroup.GLUTES to "Glutes",
            MuscleGroup.CALVES to "Calves",
            MuscleGroup.FULL_BODY to "Full Body",
            MuscleGroup.CARDIO to "Cardio"
        )
    }
}
