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
import tools.jackson.core.JacksonException
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import java.time.DayOfWeek
import java.util.*

@Service
class WorkoutPlanService(
    private val workoutPlanRepository: WorkoutPlanRepository,
    private val workoutPlanDayRepository: WorkoutPlanDayRepository,
    private val workoutPlanExerciseRepository: WorkoutPlanExerciseRepository,
    private val userRepository: UserRepository,
    private val exerciseRepository: ExerciseRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional(readOnly = true)
    fun getById(userId: UUID, planId: UUID): WorkoutPlanResponse {
        val plan = findPlanByIdAndUser(planId, userId)
        return plan.toResponse()
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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
        validateSimpleSchedule(request.scheduleType, request.scheduleDays)

        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }

        val plan = WorkoutPlanEntity(
            user = user,
            name = request.name,
            description = request.description,
            createdBy = CreatedBy.USER,
            isActive = true,
            scheduleType = request.scheduleType
        )
        val savedPlan = workoutPlanRepository.save(plan)

        populateSimpleDays(
            plan = savedPlan,
            scheduleType = request.scheduleType,
            scheduleDays = request.scheduleDays,
            restSeconds = request.restSeconds,
            exercises = request.exercises
        )

        return savedPlan.toResponse()
    }

    @Transactional
    fun update(userId: UUID, planId: UUID, request: UpdateWorkoutPlanRequest): WorkoutPlanResponse {
        val plan = findPlanByIdAndUser(planId, userId)

        request.name?.let { plan.name = it }
        request.description?.let { plan.description = it }
        request.isActive?.let { plan.isActive = it }

        val schedulingChanged =
            request.scheduleType != null || request.scheduleDays != null || request.exercises != null

        if (schedulingChanged) {

            val effectiveScheduleType = request.scheduleType ?: plan.scheduleType
            val effectiveScheduleDays = when {
                request.scheduleDays != null -> request.scheduleDays
                effectiveScheduleType == WorkoutScheduleType.RECURRING ->
                    plan.days.map { it.dayOfWeek }.distinct()
                else -> emptyList()
            }
            val effectiveRestSeconds = request.restSeconds
                ?: plan.days.asSequence()
                    .sortedBy { it.orderIndex }
                    .firstOrNull()
                    ?.exercises
                    ?.minByOrNull { it.orderIndex }
                    ?.restSeconds
                ?: DEFAULT_REST_SECONDS
            val effectiveExercises = request.exercises
                ?: plan.days.minByOrNull { it.orderIndex }?.exercises
                    ?.sortedBy { it.orderIndex }
                    ?.map {
                        SimpleWorkoutExerciseItem(
                            exerciseId = it.exercise.id!!,
                            sets = it.sets,
                            reps = it.reps,

                            setWeightsKg = decodeSetWeights(it.setWeightsKg)
                        )
                    }
                ?: emptyList()

            validateSimpleSchedule(effectiveScheduleType, effectiveScheduleDays)

            plan.scheduleType = effectiveScheduleType

            plan.days.clear()

            workoutPlanRepository.saveAndFlush(plan)

            populateSimpleDays(
                plan = plan,
                scheduleType = effectiveScheduleType,
                scheduleDays = effectiveScheduleDays,
                restSeconds = effectiveRestSeconds,
                exercises = effectiveExercises
            )
        }

        val savedPlan = workoutPlanRepository.save(plan)

        return workoutPlanRepository.findByIdAndUserId(savedPlan.id!!, userId)
            ?.toResponse()
            ?: throw NotFoundException("Workout plan not found")
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
                        setWeightsKg = decodeSetWeights(ex.setWeightsKg),
                        restSeconds = ex.restSeconds,
                        orderIndex = ex.orderIndex,
                        notes = ex.notes,
                        secondsPer10Reps = ex.exercise.secondsPer10Reps
                    )
                }
            )
        }
    )

    private fun WorkoutPlanEntity.toShortResponse(): WorkoutPlanShortResponse {
        val firstDay = days.minByOrNull { it.orderIndex }
        val firstDayExercises = firstDay?.exercises?.sortedBy { it.orderIndex } ?: emptyList()
        val allDayExercises = days.flatMap { it.exercises }

        val scheduleDaysList = if (scheduleType == WorkoutScheduleType.RECURRING) {
            days.map { it.dayOfWeek }.distinct()
        } else emptyList()

        val restSecs = firstDayExercises.firstOrNull()?.restSeconds ?: DEFAULT_REST_SECONDS

        val primaryMg = allDayExercises
            .groupingBy { it.exercise.muscleGroup }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?.name

        val totalSeconds = firstDayExercises.sumOf { ex ->
            val secPer10 = ex.exercise.secondsPer10Reps ?: DEFAULT_SECONDS_PER_10_REPS
            val workSeconds = (secPer10.toDouble() / 10.0) * ex.reps * ex.sets
            val restTotalSeconds = restSecs * (ex.sets - 1)
            workSeconds + restTotalSeconds
        }
        val estimatedMinutes = maxOf(1, (totalSeconds / 60).toInt())

        return WorkoutPlanShortResponse(
            id = id!!,
            name = name,
            description = description,
            createdBy = createdBy,
            isActive = isActive,
            scheduleType = scheduleType,
            daysCount = days.size,
            scheduleDays = scheduleDaysList,
            restSeconds = restSecs,
            primaryMuscleGroup = primaryMg,
            exercisesCount = firstDayExercises.size,
            totalSets = firstDayExercises.sumOf { it.sets },
            estimatedMinutes = estimatedMinutes
        )
    }

    private fun validateSimpleSchedule(scheduleType: WorkoutScheduleType, scheduleDays: List<DayOfWeek>) {
        if (scheduleType == WorkoutScheduleType.RECURRING && scheduleDays.isEmpty()) {
            throw BadRequestException("At least one schedule day is required for recurring workouts")
        }
    }

    private fun populateSimpleDays(
        plan: WorkoutPlanEntity,
        scheduleType: WorkoutScheduleType,
        scheduleDays: List<DayOfWeek>,
        restSeconds: Int,
        exercises: List<SimpleWorkoutExerciseItem>
    ) {
        val daysToCreate: List<DayOfWeek> = when (scheduleType) {
            WorkoutScheduleType.ONE_TIME -> listOf(DayOfWeek.MONDAY)
            WorkoutScheduleType.RECURRING -> scheduleDays.distinct()
        }

        exercises.forEach { validateSetWeights(it) }

        val resolvedExercises = exercises.map { item ->
            item to exerciseRepository.findById(item.exerciseId)
                .orElseThrow { NotFoundException("Exercise not found: ${item.exerciseId}") }
        }

        daysToCreate.forEachIndexed { dayIndex, dayOfWeek ->
            val day = WorkoutPlanDayEntity(
                workoutPlan = plan,
                dayOfWeek = dayOfWeek,
                name = SIMPLE_WORKOUT_DAY_NAME,
                orderIndex = dayIndex
            )
            val savedDay = workoutPlanDayRepository.save(day)

            plan.days.add(savedDay)

            resolvedExercises.forEachIndexed { index, (item, exercise) ->
                val normalizedSetWeights = normalizeSetWeights(item.setWeightsKg)
                val savedExercise = workoutPlanExerciseRepository.save(
                    WorkoutPlanExerciseEntity(
                        workoutPlanDay = savedDay,
                        exercise = exercise,
                        sets = item.sets,
                        reps = item.reps,
                        weightKg = unifiedWeightFor(normalizedSetWeights),
                        setWeightsKg = encodeSetWeights(normalizedSetWeights),
                        restSeconds = restSeconds,
                        orderIndex = index,
                        notes = null
                    )
                )
                savedDay.exercises.add(savedExercise)
            }
        }
    }

    private fun validateSetWeights(item: SimpleWorkoutExerciseItem) {
        val weights = item.setWeightsKg ?: return
        if (weights.isEmpty()) return
        if (weights.size != item.sets) {
            throw BadRequestException(
                "setWeightsKg size (${weights.size}) must equal sets (${item.sets}) for exercise ${item.exerciseId}"
            )
        }
        weights.forEach { value ->
            if (value != null && (value < 0.0 || !value.isFinite())) {
                throw BadRequestException(
                    "setWeightsKg contains an invalid value ($value) for exercise ${item.exerciseId}"
                )
            }
        }
    }

    private fun normalizeSetWeights(weights: List<Double?>?): List<Double?>? =
        weights?.takeIf { it.isNotEmpty() }

    private fun unifiedWeightFor(weights: List<Double?>?): Double? =
        weights?.firstOrNull { it != null }

    private fun encodeSetWeights(weights: List<Double?>?): String? {
        if (weights.isNullOrEmpty()) return null
        return objectMapper.writeValueAsString(weights)
    }

    private fun decodeSetWeights(json: String?): List<Double?>? {
        if (json.isNullOrBlank()) return null
        return try {
            objectMapper.readValue(json, SET_WEIGHTS_TYPE).takeIf { it.isNotEmpty() }
        } catch (_: JacksonException) {

            null
        }
    }

    companion object {
        private const val SIMPLE_WORKOUT_DAY_NAME = "Тренировка"
        private const val DEFAULT_REST_SECONDS = 60
        private const val DEFAULT_SECONDS_PER_10_REPS = 30

        private val SET_WEIGHTS_TYPE = object : TypeReference<List<Double?>>() {}
    }
}
