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

    /**
     * Unified weight kept for backward compatibility with clients that don't yet send per-set
     * weights. When [setWeightsKg] is populated, this carries the first non-null entry so legacy
     * readers see a sensible scalar.
     */
    var weightKg: Double? = null,

    /**
     * JSON-encoded list of per-set weights, aligned with [sets] when present.
     * Example: `"[50.0,52.5,55.0]"` for three sets, or `"[50.0,null,55.0]"` if set 2 has no weight yet.
     * `null` when the exercise has no per-set weight tracking.
     *
     * Stored as `text` (rather than `jsonb`) since the column is consumed only by the service layer
     * via Jackson and we have no need to query against its content.
     */
    @Column(name = "set_weights_kg", columnDefinition = "text")
    var setWeightsKg: String? = null,

    var restSeconds: Int = 60,

    @Column(nullable = false)
    var orderIndex: Int = 0,

    @Column(length = 500)
    var notes: String? = null
)
