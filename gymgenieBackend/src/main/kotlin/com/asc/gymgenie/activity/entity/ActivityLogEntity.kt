package com.asc.gymgenie.activity.entity

import com.asc.gymgenie.user.entity.UserEntity
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.time.LocalDate
import java.util.*

/**
 * A single check-in by a user for a given activity on a given calendar day.
 *
 * The unique constraint on (user_id, activity_id, log_date) enforces "one log per activity per day"
 * and is what makes the [com.asc.gymgenie.activity.service.ActivityService] check-in flow an upsert
 * rather than a free-form insert.
 */
@Entity
@Table(
    name = "activity_logs",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_activity_logs_user_activity_date",
            columnNames = ["user_id", "activity_id", "log_date"]
        )
    ],
    indexes = [
        Index(name = "ix_activity_logs_user_date", columnList = "user_id, log_date")
    ]
)
class ActivityLogEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    var activity: ActivityDefinitionEntity,

    @Column(name = "log_date", nullable = false)
    var logDate: LocalDate,

    /** 1 for BINARY check-ins, raw count for COUNTER/PRESET. */
    @Column(nullable = false)
    var value: Int,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
)
