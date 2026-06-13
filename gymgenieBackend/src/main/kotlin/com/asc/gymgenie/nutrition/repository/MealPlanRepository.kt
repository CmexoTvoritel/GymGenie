package com.asc.gymgenie.nutrition.repository

import com.asc.gymgenie.nutrition.entity.DishEntity
import com.asc.gymgenie.nutrition.entity.MealEntity
import com.asc.gymgenie.nutrition.entity.MealPlanEntity
import com.asc.gymgenie.nutrition.entity.MealType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface MealPlanRepository : JpaRepository<MealPlanEntity, UUID> {

    fun findByUserId(userId: UUID, pageable: Pageable): Page<MealPlanEntity>

    fun findByUserIdAndId(userId: UUID, id: UUID): MealPlanEntity?

    @Query(
        """
        SELECT DISTINCT p FROM MealPlanEntity p
        LEFT JOIN FETCH p.scheduleDays
        WHERE p.user.id = :userId
        AND p.scheduleType IS NOT NULL
        AND p.isActive = true
        """
    )
    fun findScheduledByUserId(@Param("userId") userId: UUID): List<MealPlanEntity>

    @Query(
        """
        SELECT DISTINCT p FROM MealPlanEntity p
        LEFT JOIN FETCH p.scheduleDays
        WHERE p.user.id = :userId
        AND p.scheduleType IS NOT NULL
        AND p.isActive = true
        AND (p.primaryMealType = :primaryMealType OR p.primaryMealType IS NULL)
        """
    )
    fun findScheduledByUserIdAndPrimaryMealType(
        @Param("userId") userId: UUID,
        @Param("primaryMealType") primaryMealType: String
    ): List<MealPlanEntity>

    @Query(
        """
        SELECT DISTINCT p FROM MealPlanEntity p
        JOIN p.meals m
        WHERE p.user.id = :userId
        AND m.mealType = :mealType
        """
    )
    fun findByUserIdAndMealType(
        @Param("userId") userId: UUID,
        @Param("mealType") mealType: MealType
    ): List<MealPlanEntity>
}

interface MealRepository : JpaRepository<MealEntity, UUID> {
    fun findAllByMealPlan(mealPlan: MealPlanEntity): List<MealEntity>
}

interface DishRepository : JpaRepository<DishEntity, UUID> {
    fun findAllByMeal(meal: MealEntity): List<DishEntity>
}
