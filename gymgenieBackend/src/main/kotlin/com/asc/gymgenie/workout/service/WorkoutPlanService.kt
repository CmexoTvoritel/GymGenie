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

        // Use the in-memory entity directly — days and exercises were populated by
        // populateSimpleDays into savedPlan.days / savedDay.exercises, so no re-fetch needed.
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
            // Full plan-shape edit: replace days + exercises atomically.
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
                    ?.map { SimpleWorkoutExerciseItem(it.exercise.id!!, it.sets, it.reps) }
                ?: emptyList()

            validateSimpleSchedule(effectiveScheduleType, effectiveScheduleDays)

            plan.scheduleType = effectiveScheduleType
            // Clear existing days; orphanRemoval + cascade ALL on WorkoutPlanEntity.days will delete day rows
            // and (transitively) their exercises via the same cascade on WorkoutPlanDayEntity.exercises.
            plan.days.clear()
            // Force the orphan delete to be flushed before re-inserting days that share the same plan.
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
        // Re-fetch through the repository so the entity graph is reapplied with the new day/exercise rows.
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
                        restSeconds = ex.restSeconds,
                        orderIndex = ex.orderIndex,
                        notes = ex.notes
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
            totalSets = firstDayExercises.sumOf { it.sets }
        )
    }

    private fun validateSimpleSchedule(scheduleType: WorkoutScheduleType, scheduleDays: List<DayOfWeek>) {
        if (scheduleType == WorkoutScheduleType.RECURRING && scheduleDays.isEmpty()) {
            throw BadRequestException("At least one schedule day is required for recurring workouts")
        }
    }

    /**
     * Creates one [WorkoutPlanDayEntity] per resolved day (placeholder Monday for ONE_TIME, distinct schedule days for
     * RECURRING) and persists each provided exercise with the shared rest interval.
     *
     * Caller is responsible for clearing any pre-existing days when this is invoked from an edit flow.
     */
    private fun populateSimpleDays(
        plan: WorkoutPlanEntity,
        scheduleType: WorkoutScheduleType,
        scheduleDays: List<DayOfWeek>,
        restSeconds: Int,
        exercises: List<SimpleWorkoutExerciseItem>
    ) {
        val daysToCreate: List<DayOfWeek> = when (scheduleType) {
            WorkoutScheduleType.ONE_TIME -> listOf(DayOfWeek.MONDAY) // placeholder day for one-time workouts
            WorkoutScheduleType.RECURRING -> scheduleDays.distinct()
        }

        // Resolve all referenced exercises up front so we fail fast on bad ids and avoid repeated lookups per day.
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
            // Keep the in-memory plan.days collection consistent with the DB so callers that read the plan
            // back through the same persistence context (first-level cache) see the freshly created days.
            plan.days.add(savedDay)

            resolvedExercises.forEachIndexed { index, (item, exercise) ->
                val savedExercise = workoutPlanExerciseRepository.save(
                    WorkoutPlanExerciseEntity(
                        workoutPlanDay = savedDay,
                        exercise = exercise,
                        sets = item.sets,
                        reps = item.reps,
                        weightKg = null,
                        restSeconds = restSeconds,
                        orderIndex = index,
                        notes = null
                    )
                )
                savedDay.exercises.add(savedExercise)
            }
        }
    }

    companion object {
        private const val SIMPLE_WORKOUT_DAY_NAME = "Тренировка"
        private const val DEFAULT_REST_SECONDS = 60
    }
}
