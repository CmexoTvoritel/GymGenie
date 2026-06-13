package com.asc.gymgenie.workout.entity

import com.asc.gymgenie.exercise.entity.ExerciseEntity
import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "workout_plan_exercises")
class WorkoutPlanExerciseEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_plan_day_id", nullable = false)
    var workoutPlanDay: WorkoutPlanDayEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    var exercise: ExerciseEntity,

    @Column(nullable = false)
    var sets: Int = 3,

    @Column(nullable = false)
    var reps: Int = 10,

    var weightKg: Double? = null,

    @Column(name = "set_weights_kg", columnDefinition = "text")
    var setWeightsKg: String? = null,

    var restSeconds: Int = 60,

    @Column(nullable = false)
    var orderIndex: Int = 0,

    @Column(length = 500)
    var notes: String? = null
)
