package com.asc.gymgenie.activity.repository

import com.asc.gymgenie.activity.entity.ActivityDefinitionEntity
import com.asc.gymgenie.activity.entity.ActivityLogEntity
import com.asc.gymgenie.activity.entity.UserActivityEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate
import java.util.*

interface ActivityDefinitionRepository : JpaRepository<ActivityDefinitionEntity, UUID> {
    fun findAllByOrderBySortOrderAsc(): List<ActivityDefinitionEntity>
    fun findBySlug(slug: String): ActivityDefinitionEntity?
}

interface UserActivityRepository : JpaRepository<UserActivityEntity, UUID> {

    /**
     * Fetches all planned activities for the user, eagerly loading both the
     * activity definition and the schedule-days element collection to avoid
     * N+1 queries during date filtering.
     *
     * Using `LEFT JOIN FETCH` for `scheduleDays` because the collection may
     * be empty (every-day / one-time activities) and we still want those rows.
     */
    @Query("""
        SELECT DISTINCT ua FROM UserActivityEntity ua
        JOIN FETCH ua.activity
        LEFT JOIN FETCH ua.scheduleDays
        WHERE ua.user.id = :userId
        ORDER BY ua.activity.sortOrder ASC
    """)
    fun findAllByUserIdWithActivity(userId: UUID): List<UserActivityEntity>

    fun findByUserIdAndActivityId(userId: UUID, activityId: UUID): Optional<UserActivityEntity>
    fun existsByUserIdAndActivityId(userId: UUID, activityId: UUID): Boolean

    @Modifying
    fun deleteByUserIdAndActivityId(userId: UUID, activityId: UUID)
}

interface ActivityLogRepository : JpaRepository<ActivityLogEntity, UUID> {
    fun findByUserIdAndLogDate(userId: UUID, logDate: LocalDate): List<ActivityLogEntity>
    fun findByUserIdAndActivityIdAndLogDate(
        userId: UUID,
        activityId: UUID,
        logDate: LocalDate
    ): Optional<ActivityLogEntity>
    fun findByUserIdAndLogDateBetween(
        userId: UUID,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<ActivityLogEntity>
}
