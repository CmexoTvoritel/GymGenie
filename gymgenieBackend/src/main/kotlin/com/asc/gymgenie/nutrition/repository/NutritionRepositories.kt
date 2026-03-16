package com.asc.gymgenie.nutrition.repository

import com.asc.gymgenie.nutrition.entity.MealEntity
import com.asc.gymgenie.nutrition.entity.MealItemEntity
import com.asc.gymgenie.nutrition.entity.MealPlanDayEntity
import com.asc.gymgenie.nutrition.entity.MealPlanEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface MealPlanRepository : JpaRepository<MealPlanEntity, UUID> {
    fun findByUserId(userId: UUID): List<MealPlanEntity>
    fun findByUserIdAndIsActiveTrue(userId: UUID): MealPlanEntity?
    fun findByIdAndUserId(id: UUID, userId: UUID): MealPlanEntity?
}

interface MealPlanDayRepository : JpaRepository<MealPlanDayEntity, UUID>

interface MealRepository : JpaRepository<MealEntity, UUID>

interface MealItemRepository : JpaRepository<MealItemEntity, UUID>
