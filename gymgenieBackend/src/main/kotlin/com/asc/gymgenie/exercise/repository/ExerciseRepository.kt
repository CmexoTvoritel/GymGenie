package com.asc.gymgenie.exercise.repository

import com.asc.gymgenie.exercise.entity.DifficultyLevel
import com.asc.gymgenie.exercise.entity.ExerciseCategory
import com.asc.gymgenie.exercise.entity.ExerciseEntity
import com.asc.gymgenie.exercise.entity.MuscleGroup
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface ExerciseRepository : JpaRepository<ExerciseEntity, UUID> {

    fun findByMuscleGroup(muscleGroup: MuscleGroup, pageable: Pageable): Page<ExerciseEntity>

    fun findByCategory(category: ExerciseCategory, pageable: Pageable): Page<ExerciseEntity>

    fun findByDifficultyLevel(difficultyLevel: DifficultyLevel, pageable: Pageable): Page<ExerciseEntity>

    @Query("""
        SELECT e FROM ExerciseEntity e
        WHERE (cast(:muscleGroup as string) IS NULL OR e.muscleGroup = :muscleGroup)
        AND (cast(:category as string) IS NULL OR e.category = :category)
        AND (cast(:difficultyLevel as string) IS NULL OR e.difficultyLevel = :difficultyLevel)
    """)
    fun findWithFilters(
        @Param("muscleGroup") muscleGroup: MuscleGroup?,
        @Param("category") category: ExerciseCategory?,
        @Param("difficultyLevel") difficultyLevel: DifficultyLevel?,
        pageable: Pageable
    ): Page<ExerciseEntity>

    @Query("""
        SELECT e FROM ExerciseEntity e
        WHERE LOWER(e.nameRu) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(e.nameEn) LIKE LOWER(CONCAT('%', :query, '%'))
    """)
    fun search(@Param("query") query: String, pageable: Pageable): Page<ExerciseEntity>
}
