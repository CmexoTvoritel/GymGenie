package com.asc.gymgenie.activity.service

import com.asc.gymgenie.activity.dto.ActivityCheckinRequest
import com.asc.gymgenie.activity.dto.AddActivityToPlanRequest
import com.asc.gymgenie.activity.entity.*
import com.asc.gymgenie.activity.repository.ActivityDefinitionRepository
import com.asc.gymgenie.activity.repository.ActivityLogRepository
import com.asc.gymgenie.activity.repository.UserActivityRepository
import com.asc.gymgenie.common.exception.BadRequestException
import com.asc.gymgenie.common.exception.ConflictException
import com.asc.gymgenie.common.exception.NotFoundException
import com.asc.gymgenie.user.entity.UserEntity
import com.asc.gymgenie.user.repository.UserRepository
import com.asc.gymgenie.workout.entity.WorkoutScheduleType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

open class ActivityServiceTest {

    private lateinit var activityDefinitionRepository: ActivityDefinitionRepository
    private lateinit var userActivityRepository: UserActivityRepository
    private lateinit var activityLogRepository: ActivityLogRepository
    private lateinit var userRepository: UserRepository
    private lateinit var activityService: ActivityService

    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        activityDefinitionRepository = mockk()
        userActivityRepository = mockk()
        activityLogRepository = mockk()
        userRepository = mockk()

        activityService = ActivityService(
            activityDefinitionRepository = activityDefinitionRepository,
            userActivityRepository = userActivityRepository,
            activityLogRepository = activityLogRepository,
            userRepository = userRepository
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

    private fun createActivityDefinition(
        id: UUID = UUID.randomUUID(),
        slug: String = "water",
        name: String = "Вода",
        ring: ActivityRing = ActivityRing.LIFE,
        kind: ActivityKind = ActivityKind.COUNTER,
        presets: String? = "1,2,3,4,5,6,7,8",
        unit: String? = "стакан",
        defaultGoal: Int? = 8,
        inverse: Boolean = false,
        sortOrder: Int = 1
    ): ActivityDefinitionEntity {
        val entity = ActivityDefinitionEntity(
            slug = slug,
            name = name,
            ring = ring,
            kind = kind,
            presets = presets,
            unit = unit,
            defaultGoal = defaultGoal,
            inverse = inverse,
            sortOrder = sortOrder
        )
        entity.id = id
        return entity
    }

    private fun createBinaryActivity(
        id: UUID = UUID.randomUUID(),
        slug: String = "morning_exercise",
        name: String = "Зарядка"
    ): ActivityDefinitionEntity {
        return createActivityDefinition(
            id = id,
            slug = slug,
            name = name,
            ring = ActivityRing.MOVE,
            kind = ActivityKind.BINARY,
            presets = null,
            unit = null,
            defaultGoal = 1,
            sortOrder = 0
        )
    }

    private fun createActivityLog(
        id: UUID = UUID.randomUUID(),
        user: UserEntity,
        activity: ActivityDefinitionEntity,
        logDate: LocalDate = LocalDate.now(),
        value: Int = 1
    ): ActivityLogEntity {
        val log = ActivityLogEntity(
            user = user,
            activity = activity,
            logDate = logDate,
            value = value
        )
        log.id = id
        return log
    }

    // ---- getCatalog ----

    @Test
    fun getCatalog_success() {
        val activity1 = createActivityDefinition(
            slug = "water",
            name = "Вода",
            sortOrder = 1
        )
        val activity2 = createActivityDefinition(
            slug = "steps",
            name = "Шаги",
            ring = ActivityRing.MOVE,
            kind = ActivityKind.COUNTER,
            unit = "шаг",
            defaultGoal = 10000,
            sortOrder = 2
        )

        every { activityDefinitionRepository.findAllByOrderBySortOrderAsc() } returns listOf(activity1, activity2)

        val result = activityService.getCatalog()

        assertEquals(2, result.size)
        assertEquals("Вода", result[0].name)
        assertEquals(ActivityRing.LIFE, result[0].ring)
        assertEquals(ActivityKind.COUNTER, result[0].kind)
        assertEquals(8, result[0].defaultGoal)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8), result[0].presets)

        assertEquals("Шаги", result[1].name)
        assertEquals(ActivityRing.MOVE, result[1].ring)
        assertEquals(10000, result[1].defaultGoal)
    }

    // ---- checkin ----

    @Test
    fun checkin_binaryActivity_setsValueTo1() {
        val user = createUser()
        val binaryActivity = createBinaryActivity()
        val activityId = binaryActivity.id!!
        val date = LocalDate.of(2025, 5, 24)

        val request = ActivityCheckinRequest(date = date, value = 5) // value should be ignored for binary

        every { userRepository.findById(userId) } returns Optional.of(user)
        every { activityDefinitionRepository.findById(activityId) } returns Optional.of(binaryActivity)
        every {
            activityLogRepository.findByUserIdAndActivityIdAndLogDate(userId, activityId, date)
        } returns Optional.empty()

        val logSlot = slot<ActivityLogEntity>()
        every { activityLogRepository.save(capture(logSlot)) } answers {
            logSlot.captured.also { it.id = UUID.randomUUID() }
        }

        val response = activityService.checkin(userId, activityId, request)

        assertEquals(1, response.value) // binary always 1
        assertEquals(activityId, response.activityId)
        assertEquals(date, response.date)
    }

    @Test
    fun checkin_counterActivity_usesRequestValue() {
        val user = createUser()
        val counterActivity = createActivityDefinition(kind = ActivityKind.COUNTER)
        val activityId = counterActivity.id!!
        val date = LocalDate.of(2025, 5, 24)

        val request = ActivityCheckinRequest(date = date, value = 5)

        every { userRepository.findById(userId) } returns Optional.of(user)
        every { activityDefinitionRepository.findById(activityId) } returns Optional.of(counterActivity)
        every {
            activityLogRepository.findByUserIdAndActivityIdAndLogDate(userId, activityId, date)
        } returns Optional.empty()

        val logSlot = slot<ActivityLogEntity>()
        every { activityLogRepository.save(capture(logSlot)) } answers {
            logSlot.captured.also { it.id = UUID.randomUUID() }
        }

        val response = activityService.checkin(userId, activityId, request)

        assertEquals(5, response.value)
        assertEquals(activityId, response.activityId)
    }

    @Test
    fun checkin_updatesExistingLog() {
        val user = createUser()
        val activity = createActivityDefinition(kind = ActivityKind.COUNTER)
        val activityId = activity.id!!
        val date = LocalDate.of(2025, 5, 24)

        val existingLog = createActivityLog(
            user = user,
            activity = activity,
            logDate = date,
            value = 3
        )

        val request = ActivityCheckinRequest(date = date, value = 6)

        every { userRepository.findById(userId) } returns Optional.of(user)
        every { activityDefinitionRepository.findById(activityId) } returns Optional.of(activity)
        every {
            activityLogRepository.findByUserIdAndActivityIdAndLogDate(userId, activityId, date)
        } returns Optional.of(existingLog)
        every { activityLogRepository.save(any<ActivityLogEntity>()) } answers { firstArg() }

        val response = activityService.checkin(userId, activityId, request)

        assertEquals(6, response.value) // updated value
        assertEquals(activityId, response.activityId)
        assertEquals(date, response.date)

        // Verify existing log was mutated and saved, not a new one created
        assertEquals(6, existingLog.value)
    }

    // ---- addToPlan ----

    @Test
    fun addToPlan_success() {
        val user = createUser()
        val activity = createActivityDefinition()
        val activityId = activity.id!!

        val request = AddActivityToPlanRequest(
            scheduleType = WorkoutScheduleType.RECURRING,
            scheduleDays = listOf("MONDAY", "WEDNESDAY", "FRIDAY"),
            goal = 8
        )

        every { userRepository.findById(userId) } returns Optional.of(user)
        every { activityDefinitionRepository.findById(activityId) } returns Optional.of(activity)
        every { userActivityRepository.existsByUserIdAndActivityId(userId, activityId) } returns false

        val uaSlot = slot<UserActivityEntity>()
        every { userActivityRepository.save(capture(uaSlot)) } answers {
            uaSlot.captured.also { it.id = UUID.randomUUID() }
        }

        activityService.addToPlan(userId, activityId, request)

        verify { userActivityRepository.save(any()) }

        val saved = uaSlot.captured
        assertEquals(8, saved.goal)
        assertEquals(WorkoutScheduleType.RECURRING, saved.scheduleType)
        assertEquals(setOf("MONDAY", "WEDNESDAY", "FRIDAY"), saved.scheduleDays)
    }

    @Test
    fun addToPlan_alreadyExists_throwsConflict() {
        val user = createUser()
        val activity = createActivityDefinition()
        val activityId = activity.id!!

        val request = AddActivityToPlanRequest()

        every { userRepository.findById(userId) } returns Optional.of(user)
        every { activityDefinitionRepository.findById(activityId) } returns Optional.of(activity)
        every { userActivityRepository.existsByUserIdAndActivityId(userId, activityId) } returns true

        val exception = assertFailsWith<ConflictException> {
            activityService.addToPlan(userId, activityId, request)
        }

        assertEquals("Activity is already in the user's plan", exception.message)
    }

    @Test
    fun addToPlan_recurringWithoutDays_throwsBadRequest() {
        val user = createUser()
        val activity = createActivityDefinition()
        val activityId = activity.id!!

        val request = AddActivityToPlanRequest(
            scheduleType = WorkoutScheduleType.RECURRING,
            scheduleDays = emptyList()
        )

        every { userRepository.findById(userId) } returns Optional.of(user)
        every { activityDefinitionRepository.findById(activityId) } returns Optional.of(activity)
        every { userActivityRepository.existsByUserIdAndActivityId(userId, activityId) } returns false

        val exception = assertFailsWith<BadRequestException> {
            activityService.addToPlan(userId, activityId, request)
        }

        assertEquals("scheduleDays is required for RECURRING activities", exception.message)
    }

    // ---- removeFromPlan ----

    @Test
    fun removeFromPlan_success() {
        val activityId = UUID.randomUUID()

        every { userActivityRepository.existsByUserIdAndActivityId(userId, activityId) } returns true
        every { userActivityRepository.deleteByUserIdAndActivityId(userId, activityId) } returns Unit

        activityService.removeFromPlan(userId, activityId)

        verify { userActivityRepository.deleteByUserIdAndActivityId(userId, activityId) }
    }

    @Test
    fun removeFromPlan_notInPlan_throwsNotFoundException() {
        val activityId = UUID.randomUUID()

        every { userActivityRepository.existsByUserIdAndActivityId(userId, activityId) } returns false

        val exception = assertFailsWith<NotFoundException> {
            activityService.removeFromPlan(userId, activityId)
        }

        assertEquals("Activity is not in the user's plan", exception.message)
    }
}
