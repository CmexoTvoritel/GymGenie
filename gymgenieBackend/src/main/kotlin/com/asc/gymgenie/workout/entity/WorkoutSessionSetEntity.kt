package com.asc.gymgenie.workout.entity

import com.asc.gymgenie.exercise.entity.ExerciseEntity
import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "workout_session_sets")
class WorkoutSessionSetEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_session_id", nullable = false)
    var workoutSession: WorkoutSessionEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    var exercise: ExerciseEntity,

    @Column(nullable = false)
    var setNumber: Int,

    var reps: Int? = null,

    var weightKg: Double? = null,

    @Column(nullable = false)
    var completed: Boolean = false,

    var durationSeconds: Int? = null
)
