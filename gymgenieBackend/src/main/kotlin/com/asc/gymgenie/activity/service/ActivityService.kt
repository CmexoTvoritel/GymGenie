package com.asc.gymgenie.activity.service

import com.asc.gymgenie.activity.dto.ActivityCatalogResponse
import com.asc.gymgenie.activity.dto.ActivityCheckinRequest
import com.asc.gymgenie.activity.dto.ActivityHistoryDayResponse
import com.asc.gymgenie.activity.dto.ActivityLogResponse
import com.asc.gymgenie.activity.dto.ActivityTodayResponse
import com.asc.gymgenie.activity.dto.AddActivityToPlanRequest
import com.asc.gymgenie.activity.dto.UpdateActivityScheduleRequest
import com.asc.gymgenie.activity.entity.ActivityDefinitionEntity
import com.asc.gymgenie.activity.entity.ActivityKind
import com.asc.gymgenie.activity.entity.ActivityLogEntity
import com.asc.gymgenie.activity.entity.UserActivityEntity
import com.asc.gymgenie.activity.repository.ActivityDefinitionRepository
import com.asc.gymgenie.activity.repository.ActivityLogRepository
import com.asc.gymgenie.activity.repository.UserActivityRepository
import com.asc.gymgenie.common.exception.BadRequestException
import com.asc.gymgenie.common.exception.ConflictException
import com.asc.gymgenie.common.exception.NotFoundException
import com.asc.gymgenie.user.repository.UserRepository
import com.asc.gymgenie.workout.entity.WorkoutScheduleType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.*

@Service
class ActivityService(
    private val activityDefinitionRepository: ActivityDefinitionRepository,
    private val userActivityRepository: UserActivityRepository,
    private val activityLogRepository: ActivityLogRepository,
    private val userRepository: UserRepository
) {

    @Transactional(readOnly = true)
    fun getCatalog(): List<ActivityCatalogResponse> {
        return activityDefinitionRepository.findAllByOrderBySortOrderAsc()
            .map { it.toCatalogResponse() }
    }

    @Transactional(readOnly = true)
    fun getTodayActivities(userId: UUID, date: LocalDate = LocalDate.now()): List<ActivityTodayResponse> {
        val plan = userActivityRepository.findAllByUserIdWithActivity(userId)
        if (plan.isEmpty()) return emptyList()

        val dayOfWeek = date.dayOfWeek.name

        val filteredPlan = plan.filter { ua -> isScheduledFor(ua, date, dayOfWeek) }
        if (filteredPlan.isEmpty()) return emptyList()

        val logsByActivityId: Map<UUID, ActivityLogEntity> = activityLogRepository
            .findByUserIdAndLogDate(userId, date)
            .associateBy { it.activity.id!! }

        return filteredPlan
            .sortedBy { it.activity.sortOrder }
            .map { ua -> ua.toTodayResponse(logsByActivityId) }
    }

    @Transactional
    fun checkin(userId: UUID, activityId: UUID, request: ActivityCheckinRequest): ActivityLogResponse {
        val date = request.date
            ?: throw BadRequestException("date is required")

        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val activity = activityDefinitionRepository.findById(activityId)
            .orElseThrow { NotFoundException("Activity not found") }

        val effectiveValue = if (activity.kind == ActivityKind.BINARY) 1 else request.value

        val existing = activityLogRepository
            .findByUserIdAndActivityIdAndLogDate(userId, activityId, date)
            .orElse(null)

        val saved = if (existing != null) {
            existing.value = effectiveValue
            activityLogRepository.save(existing)
        } else {
            activityLogRepository.save(
                ActivityLogEntity(
                    user = user,
                    activity = activity,
                    logDate = date,
                    value = effectiveValue
                )
            )
        }

        return ActivityLogResponse(
            activityId = saved.activity.id!!,
            date = saved.logDate,
            value = saved.value
        )
    }

    @Transactional
    fun addToPlan(userId: UUID, activityId: UUID, request: AddActivityToPlanRequest) {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val activity = activityDefinitionRepository.findById(activityId)
            .orElseThrow { NotFoundException("Activity not found") }

        if (userActivityRepository.existsByUserIdAndActivityId(userId, activityId)) {
            throw ConflictException("Activity is already in the user's plan")
        }

        if (request.scheduleType != null) {
            validateScheduling(request.scheduleType, request.scheduleDays.orEmpty(), request.oneOffDate)
        }

        val normalizedDays = if (request.scheduleType == WorkoutScheduleType.RECURRING) {
            normalizeScheduleDays(request.scheduleDays.orEmpty())
        } else {
            mutableSetOf()
        }

        val effectiveOneOffDate = if (request.scheduleType == WorkoutScheduleType.ONE_TIME) {
            request.oneOffDate
        } else {
            null
        }

        userActivityRepository.save(
            UserActivityEntity(
                user = user,
                activity = activity,
                goal = request.goal,
                scheduleType = request.scheduleType,
                scheduleDays = normalizedDays,
                oneOffDate = effectiveOneOffDate
            )
        )
    }

    @Transactional
    fun updateSchedule(userId: UUID, activityId: UUID, request: UpdateActivityScheduleRequest): ActivityTodayResponse {
        val ua = userActivityRepository.findByUserIdAndActivityId(userId, activityId)
            .orElseThrow { NotFoundException("Activity is not in the user's plan") }

        val scheduleType = request.scheduleType

        if (scheduleType != null) {
            validateScheduling(scheduleType, request.scheduleDays, request.oneOffDate)
        }

        ua.scheduleType = scheduleType

        ua.scheduleDays.clear()
        if (scheduleType == WorkoutScheduleType.RECURRING) {
            ua.scheduleDays.addAll(normalizeScheduleDays(request.scheduleDays))
        }

        ua.oneOffDate = if (scheduleType == WorkoutScheduleType.ONE_TIME) {
            request.oneOffDate
        } else {
            null
        }

        val saved = userActivityRepository.save(ua)

        val today = LocalDate.now()
        val log = activityLogRepository
            .findByUserIdAndActivityIdAndLogDate(userId, activityId, today)
            .orElse(null)

        return saved.toTodayResponse(log)
    }

    @Transactional
    fun removeFromPlan(userId: UUID, activityId: UUID) {
        if (!userActivityRepository.existsByUserIdAndActivityId(userId, activityId)) {
            throw NotFoundException("Activity is not in the user's plan")
        }
        userActivityRepository.deleteByUserIdAndActivityId(userId, activityId)
    }

    @Transactional(readOnly = true)
    fun getHistory(
        userId: UUID,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<ActivityHistoryDayResponse> {
        if (endDate.isBefore(startDate)) {
            throw BadRequestException("endDate must be on or after startDate")
        }

        val plan = userActivityRepository.findAllByUserIdWithActivity(userId)
        val logs = activityLogRepository.findByUserIdAndLogDateBetween(userId, startDate, endDate)
        if (plan.isEmpty() && logs.isEmpty()) return emptyList()

        val effectiveGoalByActivity: Map<UUID, Int?> = plan.associate {
            it.activity.id!! to (it.goal ?: it.activity.defaultGoal)
        }

        val logsByDate: Map<LocalDate, List<ActivityLogEntity>> = logs.groupBy { it.logDate }

        val days = mutableListOf<ActivityHistoryDayResponse>()
        var cursor = startDate
        while (!cursor.isAfter(endDate)) {
            val dayOfWeek = cursor.dayOfWeek.name
            val scheduledPlan = plan.filter { ua -> isScheduledFor(ua, cursor, dayOfWeek) }
            val scheduledActivityIds = scheduledPlan.mapNotNull { it.activity.id }.toSet()
            val scheduledCount = scheduledActivityIds.size

            val scheduledGoals: Map<UUID, Int?> = effectiveGoalByActivity
                .filterKeys { it in scheduledActivityIds }

            val dayLogs = logsByDate[cursor].orEmpty()

            val completionPct = computeCompletion(
                dayLogs = dayLogs,
                plannedActivityIds = scheduledActivityIds,
                plannedCount = scheduledCount,
                effectiveGoalByActivity = scheduledGoals
            )

            days += ActivityHistoryDayResponse(
                date = cursor,
                completionPct = completionPct,
                logs = dayLogs.map {
                    ActivityLogResponse(
                        activityId = it.activity.id!!,
                        date = it.logDate,
                        value = it.value
                    )
                }
            )
            cursor = cursor.plusDays(1)
        }
        return days
    }

    private fun isScheduledFor(ua: UserActivityEntity, date: LocalDate, dayOfWeek: String): Boolean {
        return when (ua.scheduleType) {
            WorkoutScheduleType.RECURRING -> ua.scheduleDays.contains(dayOfWeek)
            WorkoutScheduleType.ONE_TIME -> ua.oneOffDate == date
            null -> true
        }
    }

    private fun validateScheduling(
        scheduleType: WorkoutScheduleType,
        scheduleDays: List<String>,
        oneOffDate: LocalDate?
    ) {
        when (scheduleType) {
            WorkoutScheduleType.RECURRING -> {
                if (scheduleDays.isEmpty()) {
                    throw BadRequestException("scheduleDays is required for RECURRING activities")
                }
                scheduleDays.forEach { raw ->
                    if (!isValidDayOfWeek(raw.uppercase())) {
                        throw BadRequestException("Invalid day-of-week value: $raw")
                    }
                }
            }
            WorkoutScheduleType.ONE_TIME -> {
                if (oneOffDate == null) {
                    throw BadRequestException("oneOffDate is required for ONE_TIME activities")
                }
            }
        }
    }

    private fun normalizeScheduleDays(raw: List<String>): MutableSet<String> =
        raw.asSequence()
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sortedBy { DayOfWeek.valueOf(it).value }
            .toMutableSet()

    private fun isValidDayOfWeek(raw: String): Boolean =
        try {
            DayOfWeek.valueOf(raw)
            true
        } catch (_: IllegalArgumentException) {
            false
        }

    private fun computeCompletion(
        dayLogs: List<ActivityLogEntity>,
        plannedActivityIds: Set<UUID>,
        plannedCount: Int,
        effectiveGoalByActivity: Map<UUID, Int?>
    ): Double {
        if (plannedCount == 0) return 0.0
        val completed = dayLogs.count { log ->
            val activityId = log.activity.id ?: return@count false
            if (activityId !in plannedActivityIds) return@count false
            val goal = effectiveGoalByActivity[activityId]
            if (goal == null) log.value > 0 else log.value >= goal
        }
        return completed.toDouble() / plannedCount.toDouble()
    }

    private fun ActivityDefinitionEntity.toCatalogResponse() = ActivityCatalogResponse(
        id = id!!,
        name = name,
        ring = ring,
        kind = kind,
        presets = parsePresets(presets),
        unit = unit,
        defaultGoal = defaultGoal,
        inverse = inverse
    )

    private fun UserActivityEntity.toTodayResponse(
        logsByActivityId: Map<UUID, ActivityLogEntity>
    ): ActivityTodayResponse {
        val def = activity
        return ActivityTodayResponse(
            activityId = def.id!!,
            name = def.name,
            ring = def.ring,
            kind = def.kind,
            presets = parsePresets(def.presets),
            unit = def.unit,
            goal = goal ?: def.defaultGoal,
            inverse = def.inverse,
            logValue = logsByActivityId[def.id]?.value ?: 0,
            scheduleType = scheduleType,
            scheduleDays = scheduleDays.toList().sortedBy { DayOfWeek.valueOf(it).value },
            oneOffDate = oneOffDate
        )
    }

    private fun UserActivityEntity.toTodayResponse(
        log: ActivityLogEntity?
    ): ActivityTodayResponse {
        val def = activity
        return ActivityTodayResponse(
            activityId = def.id!!,
            name = def.name,
            ring = def.ring,
            kind = def.kind,
            presets = parsePresets(def.presets),
            unit = def.unit,
            goal = goal ?: def.defaultGoal,
            inverse = def.inverse,
            logValue = log?.value ?: 0,
            scheduleType = scheduleType,
            scheduleDays = scheduleDays.toList().sortedBy { DayOfWeek.valueOf(it).value },
            oneOffDate = oneOffDate
        )
    }

    private fun parsePresets(raw: String?): List<Int> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
    }
}
