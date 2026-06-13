package com.asc.gymgenie.exercise.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Formula
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.*

@Entity
@Table(name = "exercises")
class ExerciseEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false, length = 100)
    var nameRu: String,

    @Column(nullable = false, length = 100)
    var nameEn: String,

    @Column(length = 2000)
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var muscleGroup: MuscleGroup,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var secondaryMuscleGroups: List<MuscleGroup> = emptyList(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: ExerciseCategory,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var difficultyLevel: DifficultyLevel,

    @Column(name = "seconds_per_10_reps")
    var secondsPer10Reps: Int? = null,

    var caloriesBurned: Int? = null,

    @Column(precision = 2)
    var rating: Double? = null,

    @Column(length = 500)
    var imageUrl: String? = null,

    @Column(length = 500)
    var videoUrl: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var instructions: List<String> = emptyList(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var equipment: List<String> = emptyList(),

    @Column(length = 500)
    var techniqueTip: String? = null,

    var defaultRepsMin: Int? = null,

    var defaultRepsMax: Int? = null,

    var defaultWeightPercentage: Double? = null,

    @Column(nullable = false, columnDefinition = "boolean default false")
    var requiresWeight: Boolean = false,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
) {

    @Formula("CASE difficulty_level WHEN 'BEGINNER' THEN 1 WHEN 'INTERMEDIATE' THEN 2 WHEN 'ADVANCED' THEN 3 ELSE 4 END")
    var difficultyOrder: Int = 0
}
