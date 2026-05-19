package com.asc.gymgenie.activity.entity

import com.asc.gymgenie.user.entity.UserEntity
import com.asc.gymgenie.workout.entity.WorkoutScheduleType
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.time.LocalDate
import java.util.*

/**
 * Link between a user and an activity definition they've added to their personal plan.
 *
 * The unique constraint on (user_id, activity_id) guarantees a user cannot plan the same activity twice.
 *
 * Scheduling semantics:
 *  - [scheduleType] = null  → activity appears every day (backward compatible default)
 *  - [scheduleType] = RECURRING → activity appears on the weekdays listed in [scheduleDays]
 *  - [scheduleType] = ONE_TIME  → activity appears only on [oneOffDate]
 */
@Entity
@Table(
    name = "user_activities",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_activities_user_activity",
            columnNames = ["user_id", "activity_id"]
        )
    ]
)
class UserActivityEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    var activity: ActivityDefinitionEntity,

    /** Optional per-user override of [ActivityDefinitionEntity.defaultGoal]. */
    var goal: Int? = null,

    /**
     * Scheduling mode. Nullable because pre-existing rows never had it set
     * and we treat null as "every day" for backward compatibility.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", length = 20)
    var scheduleType: WorkoutScheduleType? = null,

    /**
     * Recurring weekdays this activity is bound to (e.g. {"MONDAY", "WEDNESDAY"}).
     * Upper-case strings matching [java.time.DayOfWeek] names.
     * Empty for [WorkoutScheduleType.ONE_TIME] and for the "every day" default.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "user_activity_schedule_days",
        joinColumns = [JoinColumn(name = "user_activity_id")]
    )
    @Column(name = "day_of_week", length = 16, nullable = false)
    var scheduleDays: MutableSet<String> = mutableSetOf(),

    /**
     * Calendar date this activity is bound to when [scheduleType] is
     * [WorkoutScheduleType.ONE_TIME]. Null for RECURRING and "every day" activities.
     */
    @Column(name = "one_off_date")
    var oneOffDate: LocalDate? = null,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)
