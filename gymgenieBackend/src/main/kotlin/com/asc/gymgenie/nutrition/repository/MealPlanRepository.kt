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

    /**
     * Lists meal plans for a single user, paginated.
     *
     * No `@EntityGraph` here — Hibernate 7 throws when a collection-fetch JOIN is
     * combined with `Pageable`. Lazy access of `meals`/`dishes` must happen inside
     * the same `@Transactional(readOnly = true)` boundary on the service.
     */
    fun findByUserId(userId: UUID, pageable: Pageable): Page<MealPlanEntity>

    fun findByUserIdAndId(userId: UUID, id: UUID): MealPlanEntity?

    /**
     * Returns plans owned by [userId] that contain at least one meal of [mealType].
     * Used by the booked-days lookup to determine which weekdays / one-off dates
     * are already occupied for a given meal slot.
     *
     * `DISTINCT` because the JOIN against `meals` would otherwise multiply rows
     * for plans with multiple meals of the same type.
     */
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
