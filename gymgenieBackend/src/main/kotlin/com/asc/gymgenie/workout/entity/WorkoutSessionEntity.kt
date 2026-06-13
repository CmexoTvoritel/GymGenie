package com.asc.gymgenie.workout.entity

import com.asc.gymgenie.user.entity.UserEntity
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.*

@Entity
@Table(name = "workout_sessions")
class WorkoutSessionEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_plan_day_id")
    var workoutPlanDay: WorkoutPlanDayEntity? = null,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false)
    var startedAt: Instant = Instant.now(),

    var finishedAt: Instant? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SessionStatus = SessionStatus.IN_PROGRESS,

    @Column(length = 1000)
    var notes: String? = null,

    var totalPlannedSets: Int? = null,

    var totalPlannedExercises: Int? = null,

    @OneToMany(mappedBy = "workoutSession", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("setNumber ASC")
    var sets: MutableList<WorkoutSessionSetEntity> = mutableListOf(),

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)
