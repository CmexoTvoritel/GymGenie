package com.asc.gymgenie.nutrition.service

import com.asc.gymgenie.common.exception.BadRequestException
import com.asc.gymgenie.common.exception.NotFoundException
import com.asc.gymgenie.exercise.dto.PagedResponse
import com.asc.gymgenie.nutrition.dto.BookedDaysResponse
import com.asc.gymgenie.nutrition.dto.CreateManualMealPlanRequest
import com.asc.gymgenie.nutrition.dto.DishResponse
import com.asc.gymgenie.nutrition.dto.MealPlanDetailResponse
import com.asc.gymgenie.nutrition.dto.MealPlanShortResponse
import com.asc.gymgenie.nutrition.dto.ManualMealItemDto
import com.asc.gymgenie.nutrition.dto.MealResponse
import com.asc.gymgenie.nutrition.entity.DishEntity
import com.asc.gymgenie.nutrition.entity.FoodCategory
import com.asc.gymgenie.nutrition.entity.FoodProductEntity
import com.asc.gymgenie.nutrition.entity.MealEntity
import com.asc.gymgenie.nutrition.entity.MealPlanEntity
import com.asc.gymgenie.nutrition.entity.MealType
import com.asc.gymgenie.nutrition.entity.NutritionCreatedBy
import com.asc.gymgenie.nutrition.repository.FoodProductRepository
import com.asc.gymgenie.nutrition.repository.MealPlanRepository
import com.asc.gymgenie.user.repository.UserRepository
import com.asc.gymgenie.workout.entity.WorkoutScheduleType
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.roundToInt

@Service
class MealPlanService(
    private val mealPlanRepository: MealPlanRepository,
    private val foodProductRepository: FoodProductRepository,
    private val userRepository: UserRepository
) {

    @Transactional(readOnly = true)
    fun getAllByUser(userId: UUID, page: Int, size: Int): PagedResponse<MealPlanShortResponse> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        val result = mealPlanRepository.findByUserId(userId, pageable)
        return PagedResponse(
            content = result.content.map { it.toShortResponse() },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            last = result.isLast
        )
    }

    @Transactional(readOnly = true)
    fun getById(userId: UUID, planId: UUID): MealPlanDetailResponse {
        val plan = mealPlanRepository.findByUserIdAndId(userId, planId)
            ?: throw NotFoundException("Meal plan not found")
        return plan.toDetailResponse()
    }

    @Transactional
    fun delete(userId: UUID, planId: UUID) {
        val plan = mealPlanRepository.findByUserIdAndId(userId, planId)
            ?: throw NotFoundException("Meal plan not found")
        mealPlanRepository.delete(plan)
    }

    @Transactional
    fun createManualMealPlan(userId: UUID, request: CreateManualMealPlanRequest): MealPlanDetailResponse {
        validateScheduling(request.scheduleType, request.scheduleDays, request.oneOffDate)

        if (request.items.isEmpty()) {

            throw BadRequestException("Meal plan must contain at least one item")
        }

        val normalizedScheduleDays = if (request.scheduleType == WorkoutScheduleType.RECURRING) {
            normalizeScheduleDays(request.scheduleDays)
        } else {
            emptyList()
        }
        val normalizedOneOffDate = if (request.scheduleType == WorkoutScheduleType.ONE_TIME) {
            request.oneOffDate
        } else {
            null
        }

        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }

        data class ScaledMacros(
            val name: String,
            val grams: Double,
            val calories: Int?,
            val proteinG: Int?,
            val carbsG: Int?,
            val fatG: Int?,
            val foodProductId: UUID?,
            val foodCategory: FoodCategory? = null,
        )

        val scaledItems: List<ScaledMacros> = request.items.map { item ->
            if (item.foodProductId != null) {
                val product = foodProductRepository.findById(item.foodProductId)
                    .orElseThrow { BadRequestException("Food product not found: ${item.foodProductId}") }
                ScaledMacros(
                    name = product.nameRu,
                    grams = item.grams,
                    calories = scalePer100g(product.caloriesPer100g, item.grams),
                    proteinG = scalePer100g(product.proteinPer100g, item.grams),
                    carbsG = scalePer100g(product.carbsPer100g, item.grams),
                    fatG = scalePer100g(product.fatPer100g, item.grams),
                    foodProductId = product.id,
                    foodCategory = product.category,
                )
            } else {
                ScaledMacros(
                    name = item.name ?: "Блюдо",
                    grams = item.grams,
                    calories = item.calories,
                    proteinG = item.proteinG,
                    carbsG = item.carbsG,
                    fatG = item.fatG,
                    foodProductId = null,
                )
            }
        }

        val mealEstimatedCalories = scaledItems.mapNotNull { it.calories }.sum().takeIf { it > 0 }

        val plan = MealPlanEntity(
            user = user,
            name = request.name.take(100),
            description = request.description?.take(500),
            goal = request.goal,
            totalCalories = mealEstimatedCalories,
            createdBy = NutritionCreatedBy.USER,
            scheduleType = request.scheduleType,
            scheduleDays = normalizedScheduleDays.toMutableSet(),
            oneOffDate = normalizedOneOffDate,
            primaryMealType = request.mealType.name
        )

        val meal = MealEntity(
            mealPlan = plan,
            mealType = request.mealType,
            name = mealName(request.mealType),
            estimatedCalories = mealEstimatedCalories
        )

        scaledItems.forEach { scaled ->
            meal.dishes.add(
                DishEntity(
                    meal = meal,
                    name = scaled.name.take(150),
                    description = null,
                    portionDescription = "${scaled.grams.toInt()}г",
                    calories = scaled.calories,
                    proteinG = scaled.proteinG,
                    carbsG = scaled.carbsG,
                    fatG = scaled.fatG,
                    foodProductId = scaled.foodProductId,
                    grams = scaled.grams,
                    foodCategory = scaled.foodCategory
                )
            )
        }
        plan.meals.add(meal)

        val saved = mealPlanRepository.save(plan)
        return saved.toDetailResponse()
    }

    @Transactional(readOnly = true)
    fun getBookedDays(userId: UUID, mealType: MealType): BookedDaysResponse {
        val plans = mealPlanRepository.findByUserIdAndMealType(userId, mealType)

        val recurringDays = plans
            .asSequence()
            .filter { it.scheduleType == WorkoutScheduleType.RECURRING }
            .flatMap { it.scheduleDays.asSequence() }
            .map { it.uppercase() }
            .filter { isValidDayOfWeek(it) }
            .distinct()
            .sortedBy { DayOfWeek.valueOf(it).value }
            .toList()

        val oneOffDates = plans
            .asSequence()
            .filter { it.scheduleType == WorkoutScheduleType.ONE_TIME }
            .mapNotNull { it.oneOffDate }
            .distinct()
            .sorted()
            .map { it.format(DateTimeFormatter.ISO_LOCAL_DATE) }
            .toList()

        return BookedDaysResponse(recurringDays = recurringDays, oneOffDates = oneOffDates)
    }

    private fun validateScheduling(
        scheduleType: WorkoutScheduleType,
        scheduleDays: List<String>,
        oneOffDate: LocalDate?
    ) {
        when (scheduleType) {
            WorkoutScheduleType.RECURRING -> {
                if (scheduleDays.isEmpty()) {
                    throw BadRequestException("scheduleDays is required for RECURRING meal plans")
                }

                scheduleDays.forEach { raw ->
                    if (!isValidDayOfWeek(raw.uppercase())) {
                        throw BadRequestException("Invalid day-of-week value: $raw")
                    }
                }
            }
            WorkoutScheduleType.ONE_TIME -> {
                if (oneOffDate == null) {
                    throw BadRequestException("oneOffDate is required for ONE_TIME meal plans")
                }
            }
        }
    }

    private fun normalizeScheduleDays(raw: List<String>): List<String> =
        raw.asSequence()
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sortedBy { DayOfWeek.valueOf(it).value }
            .toList()

    private fun isValidDayOfWeek(raw: String): Boolean =
        try {
            DayOfWeek.valueOf(raw)
            true
        } catch (_: IllegalArgumentException) {
            false
        }

    private fun scalePer100g(per100g: Double, grams: Double): Int? {
        val raw = per100g * grams / 100.0
        if (raw.isNaN() || raw.isInfinite()) return null
        return raw.roundToInt().coerceAtLeast(0)
    }

    private fun mealName(mealType: MealType): String = when (mealType) {
        MealType.BREAKFAST -> "Завтрак"
        MealType.LUNCH -> "Обед"
        MealType.DINNER -> "Ужин"
    }

    private fun MealPlanEntity.toShortResponse() = MealPlanShortResponse(
        id = id!!,
        name = name,
        description = description,
        goal = goal,
        totalCalories = totalCalories,
        mealsCount = meals.size,
        createdBy = createdBy,
        createdAt = createdAt
    )

    private fun MealPlanEntity.toDetailResponse() = MealPlanDetailResponse(
        id = id!!,
        name = name,
        description = description,
        goal = goal,
        totalCalories = totalCalories,
        createdBy = createdBy,
        scheduleType = scheduleType,
        scheduleDays = scheduleDays.toList(),
        oneOffDate = oneOffDate?.toString(),
        meals = meals.map { it.toResponse() },
        createdAt = createdAt
    )

    private fun MealEntity.toResponse() = MealResponse(
        id = id!!,
        mealType = mealType,
        name = name,
        estimatedCalories = estimatedCalories,
        dishes = dishes.map { it.toResponse() }
    )

    private fun DishEntity.toResponse() = DishResponse(
        id = id!!,
        name = name,
        description = description,
        portionDescription = portionDescription,
        calories = calories,
        proteinG = proteinG,
        carbsG = carbsG,
        fatG = fatG,
        foodProductId = foodProductId,
        grams = grams,
        foodCategory = foodCategory?.name
    )
}
