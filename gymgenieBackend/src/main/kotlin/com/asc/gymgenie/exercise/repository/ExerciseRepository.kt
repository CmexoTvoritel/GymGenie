package com.asc.gymgenie.exercise.repository

import com.asc.gymgenie.exercise.entity.DifficultyLevel
import com.asc.gymgenie.exercise.entity.ExerciseCategory
import com.asc.gymgenie.exercise.entity.ExerciseEntity
import com.asc.gymgenie.exercise.entity.MuscleGroup
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface ExerciseRepository : JpaRepository<ExerciseEntity, UUID> {

    fun findByMuscleGroup(muscleGroup: MuscleGroup, pageable: Pageable): Page<ExerciseEntity>

    fun findByCategory(category: ExerciseCategory, pageable: Pageable): Page<ExerciseEntity>

    fun findByDifficultyLevel(difficultyLevel: DifficultyLevel, pageable: Pageable): Page<ExerciseEntity>

    @Query("""
        SELECT e FROM ExerciseEntity e
        WHERE (:muscleGroup IS NULL OR e.muscleGroup = :muscleGroup)
        AND (:category IS NULL OR e.category = :category)
        AND (:difficultyLevel IS NULL OR e.difficultyLevel = :difficultyLevel)
    """)
    fun findWithFilters(
        muscleGroup: MuscleGroup?,
        category: ExerciseCategory?,
        difficultyLevel: DifficultyLevel?,
        pageable: Pageable
    ): Page<ExerciseEntity>

    @Query("""
        SELECT e FROM ExerciseEntity e
        WHERE LOWER(e.nameRu) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(e.nameEn) LIKE LOWER(CONCAT('%', :query, '%'))
    """)
    fun search(query: String, pageable: Pageable): Page<ExerciseEntity>
}
