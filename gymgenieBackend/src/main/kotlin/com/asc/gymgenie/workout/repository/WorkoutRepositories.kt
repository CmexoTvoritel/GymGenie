package com.asc.gymgenie.workout.repository

import com.asc.gymgenie.workout.entity.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.*

interface WorkoutPlanRepository : JpaRepository<WorkoutPlanEntity, UUID> {

    // No @EntityGraph here: Hibernate 7 throws when combining a collection-fetch join
    // with Pageable (firstResult/maxResults). Lazy loading is handled by the
    // @Transactional(readOnly = true) on WorkoutPlanService.getAllByUser instead.
    fun findByUserId(userId: UUID, pageable: Pageable): Page<WorkoutPlanEntity>

    // No @EntityGraph — same reason as findByUserId: Hibernate 7 throws MultipleBagFetchException
    // when fetching two nested OneToMany bags (days → exercises) in one JOIN FETCH query.
    // Lazy loading handled by @Transactional(readOnly = true) on getActiveByUser.
    fun findByUserIdAndIsActiveTrue(userId: UUID): List<WorkoutPlanEntity>

    fun findByIdAndUserId(id: UUID, userId: UUID): WorkoutPlanEntity?
}

interface WorkoutPlanDayRepository : JpaRepository<WorkoutPlanDayEntity, UUID> {
    fun findAllByWorkoutPlan(workoutPlan: WorkoutPlanEntity): List<WorkoutPlanDayEntity>
}

interface WorkoutPlanExerciseRepository : JpaRepository<WorkoutPlanExerciseEntity, UUID>

interface WorkoutSessionRepository : JpaRepository<WorkoutSessionEntity, UUID> {
    fun findByUserId(userId: UUID, pageable: Pageable): Page<WorkoutSessionEntity>
    fun findByIdAndUserId(id: UUID, userId: UUID): WorkoutSessionEntity?

    @Query("""
        SELECT s FROM WorkoutSessionEntity s
        WHERE s.user.id = :userId
        AND s.startedAt >= :from AND s.startedAt < :to
        ORDER BY s.startedAt DESC
    """)
    fun findByUserIdAndDateRange(userId: UUID, from: Instant, to: Instant): List<WorkoutSessionEntity>
}

interface WorkoutSessionSetRepository : JpaRepository<WorkoutSessionSetEntity, UUID> {
    fun findByWorkoutSessionId(sessionId: UUID): List<WorkoutSessionSetEntity>
}
