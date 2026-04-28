package com.asc.gymgenie.workout.service

import com.asc.gymgenie.common.exception.BadRequestException
import com.asc.gymgenie.common.exception.NotFoundException
import com.asc.gymgenie.exercise.dto.PagedResponse
import com.asc.gymgenie.exercise.repository.ExerciseRepository
import com.asc.gymgenie.user.repository.UserRepository
import com.asc.gymgenie.workout.dto.*
import com.asc.gymgenie.workout.entity.*
import com.asc.gymgenie.workout.repository.WorkoutPlanDayRepository
import com.asc.gymgenie.workout.repository.WorkoutPlanExerciseRepository
import com.asc.gymgenie.workout.repository.WorkoutPlanRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.util.*

@Service
class WorkoutPlanService(
    private val workoutPlanRepository: WorkoutPlanRepository,
    private val workoutPlanDayRepository: WorkoutPlanDayRepository,
    private val workoutPlanExerciseRepository: WorkoutPlanExerciseRepository,
    private val userRepository: UserRepository,
    private val exerciseRepository: ExerciseRepository
) {

    fun getById(userId: UUID, planId: UUID): WorkoutPlanResponse {
        val plan = findPlanByIdAndUser(planId, userId)
        return plan.toResponse()
    }

    fun getAllByUser(userId: UUID, page: Int, size: Int): PagedResponse<WorkoutPlanShortResponse> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        val result = workoutPlanRepository.findByUserId(userId, pageable)
        return PagedResponse(
            content = result.content.map { it.toShortResponse() },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            last = result.isLast
        )
    }

    fun getActiveByUser(userId: UUID): List<WorkoutPlanShortResponse> {
        return workoutPlanRepository.findByUserIdAndIsActiveTrue(userId)
            .map { it.toShortResponse() }
    }

    @Transactional
    fun create(userId: UUID, request: CreateWorkoutPlanRequest): WorkoutPlanResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }

        val plan = WorkoutPlanEntity(
            user = user,
            name = request.name,
            description = request.description,
            createdBy = request.createdBy
        )

        val savedPlan = workoutPlanRepository.save(plan)

        request.days.forEach { dayRequest ->
            val day = WorkoutPlanDayEntity(
                workoutPlan = savedPlan,
                dayOfWeek = dayRequest.dayOfWeek,
                name = dayRequest.name,
                orderIndex = dayRequest.orderIndex
            )
            val savedDay = workoutPlanDayRepository.save(day)

            dayRequest.exercises.forEach { exerciseRequest ->
                val exercise = exerciseRepository.findById(exerciseRequest.exerciseId)
                    .orElseThrow { NotFoundException("Exercise not found: ${exerciseRequest.exerciseId}") }

                workoutPlanExerciseRepository.save(
                    WorkoutPlanExerciseEntity(
                        workoutPlanDay = savedDay,
                        exercise = exercise,
                        sets = exerciseRequest.sets,
                        reps = exerciseRequest.reps,
                        weightKg = exerciseRequest.weightKg,
                        restSeconds = exerciseRequest.restSeconds,
                        orderIndex = exerciseRequest.orderIndex,
                        notes = exerciseRequest.notes
                    )
                )
            }
        }

        return workoutPlanRepository.findById(savedPlan.id!!)
            .orElseThrow { NotFoundException("Plan not found") }
            .toResponse()
    }

    @Transactional
    fun createSimpleWorkout(userId: UUID, request: CreateSimpleWorkoutRequest): WorkoutPlanResponse {
        if (request.scheduleType == WorkoutScheduleType.RECURRING && request.scheduleDays.isEmpty()) {
            throw BadRequestException("At least one schedule day is required for recurring workouts")
        }

        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }

        val plan = WorkoutPlanEntity(
            user = user,
            name = request.name,
            description = null,
            createdBy = CreatedBy.USER,
            isActive = true,
            scheduleType = request.scheduleType
        )
        val savedPlan = workoutPlanRepository.save(plan)

        val daysToCreate: List<DayOfWeek> = when (request.scheduleType) {
            WorkoutScheduleType.ONE_TIME -> listOf(DayOfWeek.MONDAY) // placeholder day for one-time workouts
            WorkoutScheduleType.RECURRING -> request.scheduleDays.distinct()
        }

        daysToCreate.forEachIndexed { dayIndex, dayOfWeek ->
            val day = WorkoutPlanDayEntity(
                workoutPlan = savedPlan,
                dayOfWeek = dayOfWeek,
                name = SIMPLE_WORKOUT_DAY_NAME,
                orderIndex = dayIndex
            )
            val savedDay = workoutPlanDayRepository.save(day)

            request.exercises.forEachIndexed { index, item ->
                val exercise = exerciseRepository.findById(item.exerciseId)
                    .orElseThrow { NotFoundException("Exercise not found: ${item.exerciseId}") }

                workoutPlanExerciseRepository.save(
                    WorkoutPlanExerciseEntity(
                        workoutPlanDay = savedDay,
                        exercise = exercise,
                        sets = item.sets,
                        reps = item.reps,
                        weightKg = null,
                        restSeconds = request.restSeconds,
                        orderIndex = index,
                        notes = null
                    )
                )
            }
        }

        return workoutPlanRepository.findById(savedPlan.id!!)
            .orElseThrow { NotFoundException("Plan not found") }
            .toResponse()
    }

    @Transactional
    fun update(userId: UUID, planId: UUID, request: UpdateWorkoutPlanRequest): WorkoutPlanResponse {
        val plan = findPlanByIdAndUser(planId, userId)

        request.name?.let { plan.name = it }
        request.description?.let { plan.description = it }
        request.isActive?.let { plan.isActive = it }

        return workoutPlanRepository.save(plan).toResponse()
    }

    @Transactional
    fun addDay(userId: UUID, planId: UUID, request: CreateWorkoutPlanDayRequest): WorkoutPlanResponse {
        val plan = findPlanByIdAndUser(planId, userId)

        val day = WorkoutPlanDayEntity(
            workoutPlan = plan,
            dayOfWeek = request.dayOfWeek,
            name = request.name,
            orderIndex = request.orderIndex
        )
        val savedDay = workoutPlanDayRepository.save(day)

        request.exercises.forEach { exerciseRequest ->
            val exercise = exerciseRepository.findById(exerciseRequest.exerciseId)
                .orElseThrow { NotFoundException("Exercise not found: ${exerciseRequest.exerciseId}") }

            workoutPlanExerciseRepository.save(
                WorkoutPlanExerciseEntity(
                    workoutPlanDay = savedDay,
                    exercise = exercise,
                    sets = exerciseRequest.sets,
                    reps = exerciseRequest.reps,
                    weightKg = exerciseRequest.weightKg,
                    restSeconds = exerciseRequest.restSeconds,
                    orderIndex = exerciseRequest.orderIndex,
                    notes = exerciseRequest.notes
                )
            )
        }

        return workoutPlanRepository.findById(plan.id!!)
            .orElseThrow { NotFoundException("Plan not found") }
            .toResponse()
    }

    @Transactional
    fun delete(userId: UUID, planId: UUID) {
        val plan = findPlanByIdAndUser(planId, userId)
        workoutPlanRepository.delete(plan)
    }

    private fun findPlanByIdAndUser(planId: UUID, userId: UUID): WorkoutPlanEntity {
        return workoutPlanRepository.findByIdAndUserId(planId, userId)
            ?: throw NotFoundException("Workout plan not found")
    }

    private fun WorkoutPlanEntity.toResponse() = WorkoutPlanResponse(
        id = id!!,
        name = name,
        description = description,
        createdBy = createdBy,
        isActive = isActive,
        scheduleType = scheduleType,
        days = days.map { day ->
            WorkoutPlanDayResponse(
                id = day.id!!,
                dayOfWeek = day.dayOfWeek,
                name = day.name,
                orderIndex = day.orderIndex,
                exercises = day.exercises.map { ex ->
                    WorkoutPlanExerciseResponse(
                        id = ex.id!!,
                        exerciseId = ex.exercise.id!!,
                        exerciseNameRu = ex.exercise.nameRu,
                        exerciseNameEn = ex.exercise.nameEn,
                        muscleGroup = ex.exercise.muscleGroup,
                        difficultyLevel = ex.exercise.difficultyLevel,
                        sets = ex.sets,
                        reps = ex.reps,
                        weightKg = ex.weightKg,
                        restSeconds = ex.restSeconds,
                        orderIndex = ex.orderIndex,
                        notes = ex.notes
                    )
                }
            )
        }
    )

    private fun WorkoutPlanEntity.toShortResponse() = WorkoutPlanShortResponse(
        id = id!!,
        name = name,
        description = description,
        createdBy = createdBy,
        isActive = isActive,
        scheduleType = scheduleType,
        daysCount = days.size
    )

    companion object {
        private const val SIMPLE_WORKOUT_DAY_NAME = "Тренировка"
    }
}
