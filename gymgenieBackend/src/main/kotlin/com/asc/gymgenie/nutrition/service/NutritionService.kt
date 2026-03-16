package com.asc.gymgenie.nutrition.service

import com.asc.gymgenie.common.exception.NotFoundException
import com.asc.gymgenie.nutrition.dto.*
import com.asc.gymgenie.nutrition.entity.*
import com.asc.gymgenie.nutrition.repository.MealItemRepository
import com.asc.gymgenie.nutrition.repository.MealPlanDayRepository
import com.asc.gymgenie.nutrition.repository.MealPlanRepository
import com.asc.gymgenie.nutrition.repository.MealRepository
import com.asc.gymgenie.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class NutritionService(
    private val mealPlanRepository: MealPlanRepository,
    private val mealPlanDayRepository: MealPlanDayRepository,
    private val mealRepository: MealRepository,
    private val mealItemRepository: MealItemRepository,
    private val userRepository: UserRepository
) {

    fun getById(userId: UUID, planId: UUID): MealPlanResponse {
        val plan = findPlanByIdAndUser(planId, userId)
        return plan.toResponse()
    }

    fun getAllByUser(userId: UUID): List<MealPlanShortResponse> {
        return mealPlanRepository.findByUserId(userId).map { it.toShortResponse() }
    }

    fun getActiveByUser(userId: UUID): MealPlanResponse? {
        return mealPlanRepository.findByUserIdAndIsActiveTrue(userId)?.toResponse()
    }

    @Transactional
    fun create(userId: UUID, request: CreateMealPlanRequest): MealPlanResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }

        val plan = mealPlanRepository.save(
            MealPlanEntity(
                user = user,
                name = request.name,
                createdBy = request.createdBy
            )
        )

        request.days.forEach { dayReq ->
            val day = mealPlanDayRepository.save(
                MealPlanDayEntity(
                    mealPlan = plan,
                    dayOfWeek = dayReq.dayOfWeek
                )
            )

            dayReq.meals.forEach { mealReq ->
                val meal = mealRepository.save(
                    MealEntity(
                        mealPlanDay = day,
                        mealType = mealReq.mealType,
                        name = mealReq.name,
                        description = mealReq.description,
                        totalCalories = mealReq.totalCalories,
                        totalProteinG = mealReq.totalProteinG,
                        totalFatG = mealReq.totalFatG,
                        totalCarbsG = mealReq.totalCarbsG,
                        orderIndex = mealReq.orderIndex
                    )
                )

                mealReq.items.forEach { itemReq ->
                    mealItemRepository.save(
                        MealItemEntity(
                            meal = meal,
                            name = itemReq.name,
                            portionSize = itemReq.portionSize,
                            portionUnit = itemReq.portionUnit,
                            calories = itemReq.calories,
                            proteinG = itemReq.proteinG,
                            fatG = itemReq.fatG,
                            carbsG = itemReq.carbsG,
                            iconUrl = itemReq.iconUrl,
                            orderIndex = itemReq.orderIndex
                        )
                    )
                }
            }
        }

        return mealPlanRepository.findById(plan.id!!)
            .orElseThrow { NotFoundException("Plan not found") }
            .toResponse()
    }

    @Transactional
    fun update(userId: UUID, planId: UUID, request: UpdateMealPlanRequest): MealPlanResponse {
        val plan = findPlanByIdAndUser(planId, userId)

        request.name?.let { plan.name = it }
        request.isActive?.let { plan.isActive = it }

        return mealPlanRepository.save(plan).toResponse()
    }

    @Transactional
    fun addMealToDay(userId: UUID, dayId: UUID, request: CreateMealRequest): MealPlanResponse {
        val day = mealPlanDayRepository.findById(dayId)
            .orElseThrow { NotFoundException("Meal plan day not found") }

        val plan = day.mealPlan
        if (plan.user.id != userId) {
            throw NotFoundException("Meal plan day not found")
        }

        val meal = mealRepository.save(
            MealEntity(
                mealPlanDay = day,
                mealType = request.mealType,
                name = request.name,
                description = request.description,
                totalCalories = request.totalCalories,
                totalProteinG = request.totalProteinG,
                totalFatG = request.totalFatG,
                totalCarbsG = request.totalCarbsG,
                orderIndex = request.orderIndex
            )
        )

        request.items.forEach { itemReq ->
            mealItemRepository.save(
                MealItemEntity(
                    meal = meal,
                    name = itemReq.name,
                    portionSize = itemReq.portionSize,
                    portionUnit = itemReq.portionUnit,
                    calories = itemReq.calories,
                    proteinG = itemReq.proteinG,
                    fatG = itemReq.fatG,
                    carbsG = itemReq.carbsG,
                    iconUrl = itemReq.iconUrl,
                    orderIndex = itemReq.orderIndex
                )
            )
        }

        return mealPlanRepository.findById(plan.id!!)
            .orElseThrow { NotFoundException("Plan not found") }
            .toResponse()
    }

    @Transactional
    fun updateMeal(userId: UUID, mealId: UUID, request: UpdateMealRequest): MealResponse {
        val meal = mealRepository.findById(mealId)
            .orElseThrow { NotFoundException("Meal not found") }

        if (meal.mealPlanDay.mealPlan.user.id != userId) {
            throw NotFoundException("Meal not found")
        }

        request.mealType?.let { meal.mealType = it }
        request.name?.let { meal.name = it }
        request.description?.let { meal.description = it }
        request.totalCalories?.let { meal.totalCalories = it }
        request.totalProteinG?.let { meal.totalProteinG = it }
        request.totalFatG?.let { meal.totalFatG = it }
        request.totalCarbsG?.let { meal.totalCarbsG = it }

        return mealRepository.save(meal).toMealResponse()
    }

    @Transactional
    fun addItemToMeal(userId: UUID, mealId: UUID, request: CreateMealItemRequest): MealResponse {
        val meal = mealRepository.findById(mealId)
            .orElseThrow { NotFoundException("Meal not found") }

        if (meal.mealPlanDay.mealPlan.user.id != userId) {
            throw NotFoundException("Meal not found")
        }

        mealItemRepository.save(
            MealItemEntity(
                meal = meal,
                name = request.name,
                portionSize = request.portionSize,
                portionUnit = request.portionUnit,
                calories = request.calories,
                proteinG = request.proteinG,
                fatG = request.fatG,
                carbsG = request.carbsG,
                iconUrl = request.iconUrl,
                orderIndex = request.orderIndex
            )
        )

        return mealRepository.findById(meal.id!!)
            .orElseThrow { NotFoundException("Meal not found") }
            .toMealResponse()
    }

    @Transactional
    fun updateMealItem(userId: UUID, itemId: UUID, request: UpdateMealItemRequest): MealItemResponse {
        val item = mealItemRepository.findById(itemId)
            .orElseThrow { NotFoundException("Meal item not found") }

        if (item.meal.mealPlanDay.mealPlan.user.id != userId) {
            throw NotFoundException("Meal item not found")
        }

        request.name?.let { item.name = it }
        request.portionSize?.let { item.portionSize = it }
        request.portionUnit?.let { item.portionUnit = it }
        request.calories?.let { item.calories = it }
        request.proteinG?.let { item.proteinG = it }
        request.fatG?.let { item.fatG = it }
        request.carbsG?.let { item.carbsG = it }
        request.iconUrl?.let { item.iconUrl = it }

        return mealItemRepository.save(item).toItemResponse()
    }

    @Transactional
    fun deleteMealItem(userId: UUID, itemId: UUID) {
        val item = mealItemRepository.findById(itemId)
            .orElseThrow { NotFoundException("Meal item not found") }

        if (item.meal.mealPlanDay.mealPlan.user.id != userId) {
            throw NotFoundException("Meal item not found")
        }

        mealItemRepository.delete(item)
    }

    @Transactional
    fun delete(userId: UUID, planId: UUID) {
        val plan = findPlanByIdAndUser(planId, userId)
        mealPlanRepository.delete(plan)
    }

    private fun findPlanByIdAndUser(planId: UUID, userId: UUID): MealPlanEntity {
        return mealPlanRepository.findByIdAndUserId(planId, userId)
            ?: throw NotFoundException("Meal plan not found")
    }

    private fun MealPlanEntity.toResponse() = MealPlanResponse(
        id = id!!,
        name = name,
        isActive = isActive,
        createdBy = createdBy,
        days = days.map { day ->
            MealPlanDayResponse(
                id = day.id!!,
                dayOfWeek = day.dayOfWeek,
                meals = day.meals.map { it.toMealResponse() }
            )
        }
    )

    private fun MealPlanEntity.toShortResponse() = MealPlanShortResponse(
        id = id!!,
        name = name,
        isActive = isActive,
        createdBy = createdBy,
        daysCount = days.size
    )

    private fun MealEntity.toMealResponse() = MealResponse(
        id = id!!,
        mealType = mealType,
        name = name,
        description = description,
        totalCalories = totalCalories,
        totalProteinG = totalProteinG,
        totalFatG = totalFatG,
        totalCarbsG = totalCarbsG,
        orderIndex = orderIndex,
        items = items.map { it.toItemResponse() }
    )

    private fun MealItemEntity.toItemResponse() = MealItemResponse(
        id = id!!,
        name = name,
        portionSize = portionSize,
        portionUnit = portionUnit,
        calories = calories,
        proteinG = proteinG,
        fatG = fatG,
        carbsG = carbsG,
        iconUrl = iconUrl,
        orderIndex = orderIndex
    )
}
