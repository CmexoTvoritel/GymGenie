package com.asc.gymgenie.activity.service

import com.asc.gymgenie.activity.dto.ActivityCatalogResponse
import com.asc.gymgenie.activity.dto.ActivityCheckinRequest
import com.asc.gymgenie.activity.dto.ActivityHistoryDayResponse
import com.asc.gymgenie.activity.dto.ActivityLogResponse
import com.asc.gymgenie.activity.dto.ActivityTodayResponse
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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
    fun getTodayActivities(userId: UUID): List<ActivityTodayResponse> {
        val today = LocalDate.now()
        val plan = userActivityRepository.findAllByUserIdWithActivity(userId)
        if (plan.isEmpty()) return emptyList()

        val logsByActivityId: Map<UUID, ActivityLogEntity> = activityLogRepository
            .findByUserIdAndLogDate(userId, today)
            .associateBy { it.activity.id!! }

        return plan
            .sortedBy { it.activity.sortOrder }
            .map { ua ->
                val def = ua.activity
                ActivityTodayResponse(
                    activityId = def.id!!,
                    name = def.name,
                    ring = def.ring,
                    kind = def.kind,
                    presets = parsePresets(def.presets),
                    unit = def.unit,
                    goal = ua.goal ?: def.defaultGoal,
                    inverse = def.inverse,
                    logValue = logsByActivityId[def.id]?.value ?: 0
                )
            }
    }

    @Transactional
    fun checkin(userId: UUID, activityId: UUID, request: ActivityCheckinRequest): ActivityLogResponse {
        val date = request.date
            ?: throw BadRequestException("date is required")

        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val activity = activityDefinitionRepository.findById(activityId)
            .orElseThrow { NotFoundException("Activity not found") }

        // BINARY: value is always 1 regardless of what the client sends.
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
    fun addToPlan(userId: UUID, activityId: UUID) {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val activity = activityDefinitionRepository.findById(activityId)
            .orElseThrow { NotFoundException("Activity not found") }

        if (userActivityRepository.existsByUserIdAndActivityId(userId, activityId)) {
            throw ConflictException("Activity is already in the user's plan")
        }

        userActivityRepository.save(
            UserActivityEntity(
                user = user,
                activity = activity,
                goal = null
            )
        )
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

        // Effective goal lookup per activity, derived from the user's current plan.
        val effectiveGoalByActivity: Map<UUID, Int?> = plan.associate {
            it.activity.id!! to (it.goal ?: it.activity.defaultGoal)
        }
        val plannedActivityIds: Set<UUID> = plan.mapNotNull { it.activity.id }.toSet()
        val plannedCount = plannedActivityIds.size

        val logsByDate: Map<LocalDate, List<ActivityLogEntity>> = logs.groupBy { it.logDate }

        // Enumerate every date in the inclusive range so the client gets a dense result,
        // even for days with no logs (completionPct = 0.0).
        val days = mutableListOf<ActivityHistoryDayResponse>()
        var cursor = startDate
        while (!cursor.isAfter(endDate)) {
            val dayLogs = logsByDate[cursor].orEmpty()

            val completionPct = computeCompletion(
                dayLogs = dayLogs,
                plannedActivityIds = plannedActivityIds,
                plannedCount = plannedCount,
                effectiveGoalByActivity = effectiveGoalByActivity
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

    /**
     * Share of currently planned activities that hit their goal on this day.
     *
     * Notes:
     * - "Hit goal" means recorded value >= effective goal (or value > 0 when no goal is set, e.g. BINARY).
     * - Logs for activities the user no longer has in their plan are still returned in [ActivityHistoryDayResponse.logs]
     *   but do *not* contribute to the completion ratio — completion is measured against the *current* plan.
     * - Returns 0.0 when the user has no planned activities (avoids division by zero and matches an empty plan UX).
     */
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

    private fun parsePresets(raw: String?): List<Int> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
    }
}
