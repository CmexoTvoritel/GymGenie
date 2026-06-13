package com.asc.gymgenie.activity.entity

import com.asc.gymgenie.user.entity.UserEntity
import com.asc.gymgenie.workout.entity.WorkoutScheduleType
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.time.LocalDate
import java.util.*

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

    var goal: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", length = 20)
    var scheduleType: WorkoutScheduleType? = null,

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "user_activity_schedule_days",
        joinColumns = [JoinColumn(name = "user_activity_id")]
    )
    @Column(name = "day_of_week", length = 16, nullable = false)
    var scheduleDays: MutableSet<String> = mutableSetOf(),

    @Column(name = "one_off_date")
    var oneOffDate: LocalDate? = null,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)
