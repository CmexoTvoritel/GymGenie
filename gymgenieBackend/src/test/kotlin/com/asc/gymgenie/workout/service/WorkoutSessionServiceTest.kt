package com.asc.gymgenie.workout.service

import com.asc.gymgenie.common.exception.BadRequestException
import com.asc.gymgenie.common.exception.NotFoundException
import com.asc.gymgenie.exercise.entity.DifficultyLevel
import com.asc.gymgenie.exercise.entity.ExerciseCategory
import com.asc.gymgenie.exercise.entity.ExerciseEntity
import com.asc.gymgenie.exercise.entity.MuscleGroup
import com.asc.gymgenie.exercise.repository.ExerciseRepository
import com.asc.gymgenie.user.entity.UserEntity
import com.asc.gymgenie.user.repository.UserRepository
import com.asc.gymgenie.workout.dto.*
import com.asc.gymgenie.workout.entity.SessionStatus
import com.asc.gymgenie.workout.entity.WorkoutSessionEntity
import com.asc.gymgenie.workout.entity.WorkoutSessionSetEntity
import com.asc.gymgenie.workout.repository.WorkoutPlanDayRepository
import com.asc.gymgenie.workout.repository.WorkoutSessionRepository
import com.asc.gymgenie.workout.repository.WorkoutSessionSetRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

open class WorkoutSessionServiceTest {

    private lateinit var sessionRepository: WorkoutSessionRepository
    private lateinit var sessionSetRepository: WorkoutSessionSetRepository
    private lateinit var planDayRepository: WorkoutPlanDayRepository
    private lateinit var userRepository: UserRepository
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var service: WorkoutSessionService

    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        sessionRepository = mockk()
        sessionSetRepository = mockk()
        planDayRepository = mockk()
        userRepository = mockk()
        exerciseRepository = mockk()

        service = WorkoutSessionService(
            sessionRepository = sessionRepository,
            sessionSetRepository = sessionSetRepository,
            planDayRepository = planDayRepository,
            userRepository = userRepository,
            exerciseRepository = exerciseRepository
        )
    }

    // ---- helpers ----

    private fun createUser(id: UUID = userId): UserEntity {
        val user = UserEntity(
            email = "ivan@example.com",
            passwordHash = "hash",
            firstName = "Иван"
        )
        user.id = id
        return user
    }

    private fun createExercise(
        id: UUID = UUID.randomUUID(),
        nameRu: String = "Жим лёжа",
        nameEn: String = "Bench Press"
    ): ExerciseEntity {
        val entity = ExerciseEntity(
            nameRu = nameRu,
            nameEn = nameEn,
            muscleGroup = MuscleGroup.CHEST,
            category = ExerciseCategory.STRENGTH,
            difficultyLevel = DifficultyLevel.INTERMEDIATE
        )
        entity.id = id
        return entity
    }

    private fun createSession(
        id: UUID = UUID.randomUUID(),
        user: UserEntity = createUser(),
        name: String = "Тренировка груди",
        status: SessionStatus = SessionStatus.IN_PROGRESS,
        startedAt: Instant = Instant.now(),
        finishedAt: Instant? = null,
        sets: MutableList<WorkoutSessionSetEntity> = mutableListOf()
    ): WorkoutSessionEntity {
        val session = WorkoutSessionEntity(
            user = user,
            name = name,
            startedAt = startedAt,
            finishedAt = finishedAt,
            status = status,
            sets = sets
        )
        session.id = id
        return session
    }

    private fun createSessionSet(
        id: UUID = UUID.randomUUID(),
        session: WorkoutSessionEntity,
        exercise: ExerciseEntity,
        setNumber: Int = 1,
        reps: Int? = 10,
        weightKg: Double? = 60.0,
        completed: Boolean = true
    ): WorkoutSessionSetEntity {
        val set = WorkoutSessionSetEntity(
            workoutSession = session,
            exercise = exercise,
            setNumber = setNumber,
            reps = reps,
            weightKg = weightKg,
            completed = completed
        )
        set.id = id
        return set
    }

    // ---- start ----

    @Test
    fun start_success() {
        val user = createUser()
        val request = StartWorkoutSessionRequest(
            name = "Утренняя тренировка",
            workoutPlanDayId = null,
            notes = "Разминка 5 минут"
        )

        every { userRepository.findById(userId) } returns Optional.of(user)

        val sessionSlot = slot<WorkoutSessionEntity>()
        every { sessionRepository.save(capture(sessionSlot)) } answers {
            sessionSlot.captured.also { it.id = UUID.randomUUID() }
        }

        val response = service.start(userId, request)

        assertEquals("Утренняя тренировка", response.name)
        assertEquals(SessionStatus.IN_PROGRESS, response.status)
        assertEquals("Разминка 5 минут", response.notes)

        verify { sessionRepository.save(any()) }
    }

    // ---- submit ----

    @Test
    fun submit_success() {
        val user = createUser()
        val exerciseId = UUID.randomUUID()
        val exercise = createExercise(id = exerciseId)
        val now = Instant.now()

        val request = SubmitWorkoutSessionRequest(
            name = "Тренировка ног",
            startedAt = now.minusSeconds(3600),
            finishedAt = now,
            status = SessionStatus.COMPLETED,
            notes = "Отличная тренировка",
            totalPlannedSets = 4,
            totalPlannedExercises = 2,
            sets = listOf(
                SubmitSessionSetItem(
                    exerciseId = exerciseId,
                    setNumber = 1,
                    reps = 12,
                    weightKg = 80.0,
                    completed = true
                )
            )
        )

        every { userRepository.findById(userId) } returns Optional.of(user)
        every { exerciseRepository.findById(exerciseId) } returns Optional.of(exercise)

        val sessionId = UUID.randomUUID()
        val sessionSlot = slot<WorkoutSessionEntity>()
        every { sessionRepository.save(capture(sessionSlot)) } answers {
            sessionSlot.captured.also { it.id = sessionId }
        }

        val setSlot = slot<WorkoutSessionSetEntity>()
        every { sessionSetRepository.save(capture(setSlot)) } answers {
            setSlot.captured.also { it.id = UUID.randomUUID() }
        }

        // After submit, service re-fetches the session with sets
        val savedSession = createSession(
            id = sessionId,
            user = user,
            name = "Тренировка ног",
            status = SessionStatus.COMPLETED,
            startedAt = now.minusSeconds(3600),
            finishedAt = now
        )
        val savedSet = createSessionSet(
            session = savedSession,
            exercise = exercise,
            setNumber = 1,
            reps = 12,
            weightKg = 80.0,
            completed = true
        )
        savedSession.sets.add(savedSet)

        every { sessionRepository.findById(sessionId) } returns Optional.of(savedSession)

        val response = service.submit(userId, request)

        assertEquals(sessionId, response.id)
        assertEquals("Тренировка ног", response.name)
        assertEquals(SessionStatus.COMPLETED, response.status)
        assertEquals(1, response.sets.size)
        assertEquals(12, response.sets[0].reps)
        assertEquals(80.0, response.sets[0].weightKg)
    }

    @Test
    fun submit_finishedAtBeforeStartedAt_throwsBadRequest() {
        val now = Instant.now()
        val request = SubmitWorkoutSessionRequest(
            name = "Тренировка",
            startedAt = now,
            finishedAt = now.minusSeconds(3600),
            status = SessionStatus.COMPLETED
        )

        val exception = assertFailsWith<BadRequestException> {
            service.submit(userId, request)
        }

        assertEquals("finishedAt must not be before startedAt", exception.message)
    }

    @Test
    fun submit_inProgressStatus_throwsBadRequest() {
        val now = Instant.now()
        val request = SubmitWorkoutSessionRequest(
            name = "Тренировка",
            startedAt = now.minusSeconds(3600),
            finishedAt = now,
            status = SessionStatus.IN_PROGRESS
        )

        val exception = assertFailsWith<BadRequestException> {
            service.submit(userId, request)
        }

        assertEquals("Submitted session must be in a finished state (COMPLETED or CANCELLED)", exception.message)
    }

    // ---- addSet ----

    @Test
    fun addSet_success() {
        val sessionId = UUID.randomUUID()
        val exerciseId = UUID.randomUUID()
        val exercise = createExercise(id = exerciseId)
        val session = createSession(id = sessionId, status = SessionStatus.IN_PROGRESS)

        val request = AddSessionSetRequest(
            exerciseId = exerciseId,
            setNumber = 1,
            reps = 10,
            weightKg = 50.0,
            completed = true
        )

        every { sessionRepository.findByIdAndUserId(sessionId, userId) } returns session
        every { exerciseRepository.findById(exerciseId) } returns Optional.of(exercise)

        val setSlot = slot<WorkoutSessionSetEntity>()
        every { sessionSetRepository.save(capture(setSlot)) } answers {
            setSlot.captured.also { it.id = UUID.randomUUID() }
        }

        // After adding set, service re-fetches the session
        val updatedSession = createSession(id = sessionId, status = SessionStatus.IN_PROGRESS)
        val newSet = createSessionSet(
            session = updatedSession,
            exercise = exercise,
            setNumber = 1,
            reps = 10,
            weightKg = 50.0,
            completed = true
        )
        updatedSession.sets.add(newSet)

        every { sessionRepository.findById(sessionId) } returns Optional.of(updatedSession)

        val response = service.addSet(userId, sessionId, request)

        assertEquals(1, response.sets.size)
        assertEquals(10, response.sets[0].reps)
        assertEquals(50.0, response.sets[0].weightKg)

        verify { sessionSetRepository.save(any()) }
    }

    @Test
    fun addSet_finishedSession_throwsBadRequest() {
        val sessionId = UUID.randomUUID()
        val session = createSession(id = sessionId, status = SessionStatus.COMPLETED)

        val request = AddSessionSetRequest(
            exerciseId = UUID.randomUUID(),
            setNumber = 1,
            reps = 10,
            completed = true
        )

        every { sessionRepository.findByIdAndUserId(sessionId, userId) } returns session

        val exception = assertFailsWith<BadRequestException> {
            service.addSet(userId, sessionId, request)
        }

        assertEquals("Cannot add sets to a finished session", exception.message)
    }

    // ---- finish ----

    @Test
    fun finish_success() {
        val sessionId = UUID.randomUUID()
        val session = createSession(id = sessionId, status = SessionStatus.IN_PROGRESS)

        val request = FinishWorkoutSessionRequest(
            status = SessionStatus.COMPLETED,
            notes = "Всё прошло отлично"
        )

        every { sessionRepository.findByIdAndUserId(sessionId, userId) } returns session
        every { sessionRepository.save(any<WorkoutSessionEntity>()) } answers { firstArg() }

        val response = service.finish(userId, sessionId, request)

        assertEquals(SessionStatus.COMPLETED, response.status)
        assertEquals("Всё прошло отлично", response.notes)

        verify { sessionRepository.save(any<WorkoutSessionEntity>()) }
    }

    @Test
    fun finish_alreadyFinished_throwsBadRequest() {
        val sessionId = UUID.randomUUID()
        val session = createSession(id = sessionId, status = SessionStatus.COMPLETED)

        val request = FinishWorkoutSessionRequest(status = SessionStatus.COMPLETED)

        every { sessionRepository.findByIdAndUserId(sessionId, userId) } returns session

        val exception = assertFailsWith<BadRequestException> {
            service.finish(userId, sessionId, request)
        }

        assertEquals("Session is already finished", exception.message)
    }

    // ---- delete ----

    @Test
    fun delete_success() {
        val sessionId = UUID.randomUUID()
        val session = createSession(id = sessionId)

        every { sessionRepository.findByIdAndUserId(sessionId, userId) } returns session
        every { sessionRepository.delete(session) } returns Unit

        service.delete(userId, sessionId)

        verify { sessionRepository.delete(session) }
    }
}
