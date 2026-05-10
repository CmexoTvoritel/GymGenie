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

/**
 * CRUD service for saved meal plans. AI-driven generation lives in
 * [com.asc.gymgenie.ai.nutrition.MealAiService]; this service owns the manual
 * (user-built) creation path plus all read/delete endpoints exposed under
 * `/api/v1/meal-plans`.
 */
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

    /**
     * Creates a meal plan manually from catalog products.
     *
     * The flow is:
     *  1. Validate input (items non-empty; schedule fields consistent with [WorkoutScheduleType]).
     *  2. Resolve every [ManualMealItemDto.foodProductId] up front — fail with
     *     [BadRequestException] if any product is missing so we never persist a
     *     partially-resolved plan.
     *  3. Persist a single [MealPlanEntity] with `createdBy = USER`, one
     *     [MealEntity] of [CreateManualMealPlanRequest.mealType], and one
     *     [DishEntity] per item (macros derived from per-100g catalog values
     *     scaled by the requested grams).
     *  4. Sum dish calories into the plan-level `totalCalories` and meal-level
     *     `estimatedCalories` so list/short responses reflect the manual plan
     *     consistently with AI-generated plans.
     */
    @Transactional
    fun createManualMealPlan(userId: UUID, request: CreateManualMealPlanRequest): MealPlanDetailResponse {
        validateScheduling(request.scheduleType, request.scheduleDays, request.oneOffDate)

        if (request.items.isEmpty()) {
            // Belt-and-braces alongside @NotEmpty on the DTO — keeps the contract
            // explicit at the service boundary for any non-controller caller.
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

        // Resolve every catalog product up front. Failing fast here means we
        // never end up with a half-persisted plan referencing a missing product.
        val resolvedItems: List<Pair<ManualMealItemDto, FoodProductEntity>> = request.items.map { item ->
            val product = foodProductRepository.findById(item.foodProductId)
                .orElseThrow { BadRequestException("Food product not found: ${item.foodProductId}") }
            item to product
        }

        // Pre-compute macros so the meal/plan totals can be initialized before
        // dish entities are constructed (DishEntity.meal is non-null and we want
        // to avoid a placeholder reference).
        data class ScaledMacros(
            val product: FoodProductEntity,
            val grams: Double,
            val calories: Int?,
            val proteinG: Int?,
            val carbsG: Int?,
            val fatG: Int?
        )

        val scaledItems: List<ScaledMacros> = resolvedItems.map { (item, product) ->
            ScaledMacros(
                product = product,
                grams = item.grams,
                calories = scalePer100g(product.caloriesPer100g, item.grams),
                proteinG = scalePer100g(product.proteinPer100g, item.grams),
                carbsG = scalePer100g(product.carbsPer100g, item.grams),
                fatG = scalePer100g(product.fatPer100g, item.grams)
            )
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
            oneOffDate = normalizedOneOffDate
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
                    name = scaled.product.nameRu.take(150),
                    description = null,
                    portionDescription = "${scaled.grams.toInt()}г",
                    calories = scaled.calories,
                    proteinG = scaled.proteinG,
                    carbsG = scaled.carbsG,
                    fatG = scaled.fatG
                )
            )
        }
        plan.meals.add(meal)

        // Persist the aggregate root: CascadeType.ALL on MealPlanEntity.meals
        // and MealEntity.dishes propagates the inserts in a single save call.
        val saved = mealPlanRepository.save(plan)
        return saved.toDetailResponse()
    }

    /**
     * Returns the calendar slots already occupied by the user's existing plans
     * that include a meal of [mealType]. Splits results by scheduling mode so the
     * mobile client can render distinct "weekday" and "specific date" pickers.
     */
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

    // ================================================================
    // Validation helpers
    // ================================================================

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
                // Validate every entry parses to a DayOfWeek before persisting.
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

    /**
     * Scales a per-100g nutritional value by the requested grams and rounds to
     * the nearest integer. Returns `null` only when the input itself is non-finite,
     * which should never happen for catalog rows but guards the persistence column.
     */
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

    // ================================================================
    // Mappers
    // ================================================================

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
        oneOffDate = oneOffDate,
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
        fatG = fatG
    )
}
