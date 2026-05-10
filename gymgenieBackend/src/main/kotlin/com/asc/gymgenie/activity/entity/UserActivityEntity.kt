package com.asc.gymgenie.activity.entity

import com.asc.gymgenie.user.entity.UserEntity
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.*

/**
 * Link between a user and an activity definition they've added to their personal plan.
 *
 * The unique constraint on (user_id, activity_id) guarantees a user cannot plan the same activity twice.
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

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)
