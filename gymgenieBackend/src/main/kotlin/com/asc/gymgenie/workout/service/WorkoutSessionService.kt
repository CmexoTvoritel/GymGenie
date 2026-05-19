package com.asc.gymgenie.workout.service

import com.asc.gymgenie.common.exception.BadRequestException
import com.asc.gymgenie.common.exception.NotFoundException
import com.asc.gymgenie.exercise.dto.PagedResponse
import com.asc.gymgenie.exercise.repository.ExerciseRepository
import com.asc.gymgenie.user.repository.UserRepository
import com.asc.gymgenie.workout.dto.*
import com.asc.gymgenie.workout.entity.SessionStatus
import com.asc.gymgenie.workout.entity.WorkoutSessionEntity
import com.asc.gymgenie.workout.entity.WorkoutSessionSetEntity
import com.asc.gymgenie.workout.repository.WorkoutPlanDayRepository
import com.asc.gymgenie.workout.repository.WorkoutSessionRepository
import com.asc.gymgenie.workout.repository.WorkoutSessionSetRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*

@Service
class WorkoutSessionService(
    private val sessionRepository: WorkoutSessionRepository,
    private val sessionSetRepository: WorkoutSessionSetRepository,
    private val planDayRepository: WorkoutPlanDayRepository,
    private val userRepository: UserRepository,
    private val exerciseRepository: ExerciseRepository
) {

    @Transactional(readOnly = true)
    fun getById(userId: UUID, sessionId: UUID): WorkoutSessionResponse {
        val session = findSessionByIdAndUser(sessionId, userId)
        return session.toResponse()
    }

    @Transactional(readOnly = true)
    fun getAllByUser(userId: UUID, page: Int, size: Int): PagedResponse<WorkoutSessionShortResponse> {
        val pageable = PageRequest.of(page, size, Sort.by("startedAt").descending())
        val result = sessionRepository.findByUserId(userId, pageable)
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
    fun getByDate(userId: UUID, date: LocalDate): List<WorkoutSessionShortResponse> {
        val from = date.atStartOfDay().toInstant(ZoneOffset.UTC)
        val to = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        return sessionRepository.findByUserIdAndDateRange(userId, from, to)
            .map { it.toShortResponse() }
    }

    @Transactional
    fun submit(userId: UUID, request: SubmitWorkoutSessionRequest): WorkoutSessionResponse {
        if (request.finishedAt.isBefore(request.startedAt)) {
            throw BadRequestException("finishedAt must not be before startedAt")
        }
        if (request.status == SessionStatus.IN_PROGRESS) {
            throw BadRequestException("Submitted session must be in a finished state (COMPLETED or CANCELLED)")
        }

        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }

        val planDay = request.workoutPlanDayId?.let {
            planDayRepository.findById(it).orElseGet { null }
        }

        val savedSession = sessionRepository.save(
            WorkoutSessionEntity(
                user = user,
                workoutPlanDay = planDay,
                name = request.name,
                startedAt = request.startedAt,
                finishedAt = request.finishedAt,
                status = request.status,
                notes = request.notes
            )
        )

        request.sets.forEach { item ->
            val exercise = exerciseRepository.findById(item.exerciseId)
                .orElseThrow { NotFoundException("Exercise not found: ${item.exerciseId}") }

            sessionSetRepository.save(
                WorkoutSessionSetEntity(
                    workoutSession = savedSession,
                    exercise = exercise,
                    setNumber = item.setNumber,
                    reps = item.reps,
                    weightKg = item.weightKg,
                    completed = item.completed,
                    durationSeconds = item.durationSeconds
                )
            )
        }

        return sessionRepository.findById(savedSession.id!!)
            .orElseThrow { NotFoundException("Session not found") }
            .toResponse()
    }

    @Transactional
    fun start(userId: UUID, request: StartWorkoutSessionRequest): WorkoutSessionResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }

        val planDay = request.workoutPlanDayId?.let {
            planDayRepository.findById(it).orElseGet { null }
        }

        val session = sessionRepository.save(
            WorkoutSessionEntity(
                user = user,
                workoutPlanDay = planDay,
                name = request.name,
                notes = request.notes
            )
        )

        return session.toResponse()
    }

    @Transactional
    fun addSet(userId: UUID, sessionId: UUID, request: AddSessionSetRequest): WorkoutSessionResponse {
        val session = findSessionByIdAndUser(sessionId, userId)

        if (session.status != SessionStatus.IN_PROGRESS) {
            throw BadRequestException("Cannot add sets to a finished session")
        }

        val exercise = exerciseRepository.findById(request.exerciseId)
            .orElseThrow { NotFoundException("Exercise not found") }

        sessionSetRepository.save(
            WorkoutSessionSetEntity(
                workoutSession = session,
                exercise = exercise,
                setNumber = request.setNumber,
                reps = request.reps,
                weightKg = request.weightKg,
                completed = request.completed,
                durationSeconds = request.durationSeconds
            )
        )

        return sessionRepository.findById(session.id!!)
            .orElseThrow { NotFoundException("Session not found") }
            .toResponse()
    }

    @Transactional
    fun updateSet(userId: UUID, sessionId: UUID, setId: UUID, request: UpdateSessionSetRequest): WorkoutSessionResponse {
        val session = findSessionByIdAndUser(sessionId, userId)
        val set = sessionSetRepository.findById(setId)
            .orElseThrow { NotFoundException("Set not found") }

        if (set.workoutSession.id != session.id) {
            throw BadRequestException("Set does not belong to this session")
        }

        request.reps?.let { set.reps = it }
        request.weightKg?.let { set.weightKg = it }
        request.completed?.let { set.completed = it }
        request.durationSeconds?.let { set.durationSeconds = it }

        sessionSetRepository.save(set)

        return sessionRepository.findById(session.id!!)
            .orElseThrow { NotFoundException("Session not found") }
            .toResponse()
    }

    @Transactional
    fun finish(userId: UUID, sessionId: UUID, request: FinishWorkoutSessionRequest): WorkoutSessionResponse {
        val session = findSessionByIdAndUser(sessionId, userId)

        if (session.status != SessionStatus.IN_PROGRESS) {
            throw BadRequestException("Session is already finished")
        }

        session.status = request.status
        session.finishedAt = Instant.now()
        request.notes?.let { session.notes = it }

        return sessionRepository.save(session).toResponse()
    }

    @Transactional
    fun delete(userId: UUID, sessionId: UUID) {
        val session = findSessionByIdAndUser(sessionId, userId)
        sessionRepository.delete(session)
    }

    private fun findSessionByIdAndUser(sessionId: UUID, userId: UUID): WorkoutSessionEntity {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
            ?: throw NotFoundException("Workout session not found")
    }

    private fun WorkoutSessionEntity.toResponse() = WorkoutSessionResponse(
        id = id!!,
        name = name,
        workoutPlanDayId = workoutPlanDay?.id,
        startedAt = startedAt,
        finishedAt = finishedAt,
        status = status,
        notes = notes,
        sets = sets.map { set ->
            WorkoutSessionSetResponse(
                id = set.id!!,
                exerciseId = set.exercise.id!!,
                exerciseNameRu = set.exercise.nameRu,
                exerciseNameEn = set.exercise.nameEn,
                setNumber = set.setNumber,
                reps = set.reps,
                weightKg = set.weightKg,
                completed = set.completed,
                durationSeconds = set.durationSeconds
            )
        }
    )

    private fun WorkoutSessionEntity.toShortResponse(): WorkoutSessionShortResponse {
        val allSets = sets
        val distinctExerciseIds = allSets.map { it.exercise.id!! }.distinct()
        val completedExerciseIds = allSets.filter { it.completed }.map { it.exercise.id!! }.distinct()
        val totalReps = allSets.sumOf { it.reps ?: 0 }
        val primaryMuscleGroup = allSets
            .groupBy { it.exercise.muscleGroup.name }
            .maxByOrNull { it.value.size }
            ?.key
            ?: workoutPlanDay?.exercises
                ?.groupBy { it.exercise.muscleGroup.name }
                ?.maxByOrNull { it.value.size }
                ?.key
        val durationMinutes = finishedAt?.let {
            ((it.epochSecond - startedAt.epochSecond) / 60).toInt().coerceAtLeast(1)
        }

        return WorkoutSessionShortResponse(
            id = id!!,
            name = name,
            startedAt = startedAt,
            finishedAt = finishedAt,
            status = status,
            totalSets = workoutPlanDay?.exercises?.sumOf { it.sets } ?: allSets.size,
            completedSets = allSets.count { it.completed },
            totalExercises = workoutPlanDay?.exercises?.size ?: distinctExerciseIds.size,
            completedExercises = completedExerciseIds.size,
            totalReps = totalReps,
            primaryMuscleGroup = primaryMuscleGroup,
            durationMinutes = durationMinutes,
        )
    }
}
