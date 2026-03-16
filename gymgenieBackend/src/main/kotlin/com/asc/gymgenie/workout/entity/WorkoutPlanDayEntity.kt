package com.asc.gymgenie.workout.entity

import jakarta.persistence.*
import java.time.DayOfWeek
import java.util.*

@Entity
@Table(name = "workout_plan_days")
class WorkoutPlanDayEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_plan_id", nullable = false)
    var workoutPlan: WorkoutPlanEntity,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var dayOfWeek: DayOfWeek,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false)
    var orderIndex: Int = 0,

    @OneToMany(mappedBy = "workoutPlanDay", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    var exercises: MutableList<WorkoutPlanExerciseEntity> = mutableListOf()
)
