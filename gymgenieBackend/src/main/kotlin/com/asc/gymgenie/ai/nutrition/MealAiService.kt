package com.asc.gymgenie.ai.nutrition

import com.asc.gymgenie.ai.client.GigaChatClient
import com.asc.gymgenie.ai.client.dto.GigaChatMessage
import com.asc.gymgenie.ai.nutrition.dto.*
import com.asc.gymgenie.common.exception.BadRequestException
import com.asc.gymgenie.common.exception.NotFoundException
import com.asc.gymgenie.nutrition.entity.*
import com.asc.gymgenie.nutrition.repository.DishRepository
import com.asc.gymgenie.nutrition.repository.FoodProductRepository
import com.asc.gymgenie.nutrition.repository.MealPlanRepository
import com.asc.gymgenie.nutrition.repository.MealRepository
import com.asc.gymgenie.user.repository.UserRepository
import com.asc.gymgenie.workout.entity.WorkoutScheduleType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.roundToInt

@Service
class MealAiService(
    private val userRepository: UserRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val mealRepository: MealRepository,
    private val dishRepository: DishRepository,
    private val foodProductRepository: FoodProductRepository,
    private val gigaChatClient: GigaChatClient,
    private val sessionStore: MealConversationSessionStore,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Volatile
    private var cachedCatalog: Pair<Long, String>? = null
    private val catalogCacheTtlMs = 5 * 60 * 1000L

    private fun getCatalogText(): String {
        val now = System.currentTimeMillis()
        cachedCatalog?.let { (timestamp, text) ->
            if (now - timestamp < catalogCacheTtlMs) return text
        }
        val text = buildCatalogText()
        cachedCatalog = now to text
        return text
    }

    private fun buildCatalogText(): String = buildString {
        val products = foodProductRepository.findByIsActiveTrue()
        appendLine("foodProductId|nameRu|category|cal|protein|fat|carbs (per 100g)")
        products.forEach { fp ->
            appendLine("${fp.id}|${fp.nameRu}|${fp.category.name}|${fp.caloriesPer100g}|${fp.proteinPer100g}|${fp.fatPer100g}|${fp.carbsPer100g}")
        }
    }.trimEnd()

    @Transactional
    fun chat(userId: UUID, request: AiMealChatRequest): AiMealChatResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }

        if (sessionStore.isEmpty(userId)) {

            var profileUpdated = false
            request.ageYears?.let { user.ageYears = it; profileUpdated = true }
            request.heightCm?.let { user.heightCm = it; profileUpdated = true }
            request.weightKg?.let { user.weightKg = it; profileUpdated = true }
            if (profileUpdated) userRepository.save(user)

            val contextMessage = buildContextMessage(
                weightKg = request.weightKg ?: user.weightKg,
                heightCm = request.heightCm ?: user.heightCm,
                age = request.ageYears
                    ?: user.ageYears
                    ?: user.birthDate?.let { Period.between(it, LocalDate.now()).years },
                gender = user.gender?.name,
                goal = request.goal,
                dietaryRestrictions = request.dietaryRestrictions,
                allergies = request.allergies,
                mealType = request.mealType,
                userMessage = request.message
            )

            val initialized = sessionStore.initializeIfEmpty(
                userId,
                GigaChatMessage("system", SYSTEM_PROMPT),
                GigaChatMessage("user", contextMessage)
            )
            if (!initialized) {

                sessionStore.addMessages(userId, GigaChatMessage("user", request.message))
            }
        } else {
            sessionStore.addMessages(userId, GigaChatMessage("user", request.message))
        }

        val history = sessionStore.getHistory(userId)
        val rawResponse = gigaChatClient.chat(history)
        log.debug("GigaChat raw meal response for user {}: {}", userId, rawResponse)

        sessionStore.addMessages(userId, GigaChatMessage("assistant", rawResponse))

        val stripped = extractJsonFromResponse(rawResponse)
        val cleanedJson = repairGigaChatJson(escapeControlCharsInStrings(stripped))

        val parsed = parseAiResponse(cleanedJson)

        if (parsed.type == AiMealResponseType.MEAL_PLAN && parsed.mealPlan != null) {
            val plan = parsed.mealPlan
            if (plan.meals.isEmpty()) {
                sessionStore.addMessages(
                    userId,
                    GigaChatMessage(
                        "user",
                        "Ошибка: ответ должен содержать хотя бы один приём пищи. Сгенерируй план заново."
                    )
                )
                throw BadRequestException("AI returned an invalid response, please try again")
            }

            val planSummary = buildPlanSummary(plan)
            val friendlyMessage = generateFriendlyMessage(plan.name, planSummary)
            return AiMealChatResponse(
                type = AiMealResponseType.MEAL_PLAN,
                message = friendlyMessage,
                mealPlan = plan
            )
        }

        return parsed
    }

    fun clearSession(userId: UUID) {
        sessionStore.clearSession(userId)
    }

    @Transactional(readOnly = true)
    fun getBookedDays(userId: UUID, mealType: String? = null): AiMealBookedDaysResponse {
        val plans = findScheduledPlans(userId, mealType)

        val recurringDays = plans
            .filter { it.scheduleType == WorkoutScheduleType.RECURRING }
            .flatMap { it.scheduleDays }
            .distinct()
            .sortedBy { day -> try { DayOfWeek.valueOf(day).value } catch (_: Exception) { 99 } }

        val oneOffDates = plans
            .filter { it.scheduleType == WorkoutScheduleType.ONE_TIME }
            .mapNotNull { it.oneOffDate }
            .distinct()
            .sorted()
            .map { it.format(DateTimeFormatter.ISO_LOCAL_DATE) }

        return AiMealBookedDaysResponse(recurringDays = recurringDays, oneOffDates = oneOffDates)
    }

    @Transactional(readOnly = true)
    fun checkConflicts(
        userId: UUID,
        scheduleType: String,
        oneOffDate: String?,
        scheduleDays: List<String>,
        mealType: String? = null
    ): AiMealConflictCheckResponse {
        val plans = findScheduledPlans(userId, mealType)

        val conflicts = when (scheduleType.uppercase()) {
            "ONE_TIME" -> {
                val date = oneOffDate?.let { LocalDate.parse(it) }
                    ?: return AiMealConflictCheckResponse(false)
                plans.filter { it.scheduleType == WorkoutScheduleType.ONE_TIME && it.oneOffDate == date }
                    .map { AiMealConflictPlan(it.id!!, it.name) }
            }
            "RECURRING" -> {
                val requestedDays = scheduleDays.map { it.uppercase() }.toSet()
                plans.filter { it.scheduleType == WorkoutScheduleType.RECURRING && it.scheduleDays.any { d -> d in requestedDays } }
                    .map { AiMealConflictPlan(it.id!!, it.name) }
            }
            else -> emptyList()
        }

        return AiMealConflictCheckResponse(
            hasConflicts = conflicts.isNotEmpty(),
            conflicts = conflicts
        )
    }

    private fun findScheduledPlans(userId: UUID, mealType: String?): List<MealPlanEntity> {
        val normalized = mealType?.trim()?.uppercase()
        return if (normalized != null && isValidMealType(normalized)) {
            mealPlanRepository.findScheduledByUserIdAndPrimaryMealType(userId, normalized)
        } else {
            mealPlanRepository.findScheduledByUserId(userId)
        }
    }

    private fun isValidMealType(value: String): Boolean =
        value == "BREAKFAST" || value == "LUNCH" || value == "DINNER"

    @Transactional
    fun saveMealPlan(userId: UUID, request: SaveMealPlanRequest): UUID {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }

        if (request.replaceConflictPlanIds.isNotEmpty()) {
            request.replaceConflictPlanIds.forEach { conflictId ->
                mealPlanRepository.findByUserIdAndId(userId, conflictId)?.let {
                    mealPlanRepository.delete(it)
                }
            }
            mealPlanRepository.flush()
        }

        val parsedScheduleType = request.scheduleType?.let { parseScheduleType(it) }
        val parsedOneOffDate = if (parsedScheduleType == WorkoutScheduleType.ONE_TIME) {
            request.oneOffDate?.let { LocalDate.parse(it) }
        } else null
        val normalizedDays = if (parsedScheduleType == WorkoutScheduleType.RECURRING) {
            normalizeDays(request.scheduleDays)
        } else mutableSetOf()

        val primaryMealType = derivePrimaryMealType(request)

        val plan = mealPlanRepository.save(
            MealPlanEntity(
                user = user,
                name = request.name.take(100),
                description = request.description?.take(500),
                goal = request.goal,
                totalCalories = request.totalCalories,
                createdBy = NutritionCreatedBy.AI,
                scheduleType = parsedScheduleType,
                oneOffDate = parsedOneOffDate,
                scheduleDays = normalizedDays,
                primaryMealType = primaryMealType
            )
        )

        persistMeals(plan, request.meals)

        return plan.id!!
    }

    @Transactional
    fun replaceMealPlan(userId: UUID, planId: UUID, request: SaveMealPlanRequest): UUID {
        val plan = mealPlanRepository.findByUserIdAndId(userId, planId)
            ?: throw NotFoundException("Meal plan not found")

        if (request.replaceConflictPlanIds.isNotEmpty()) {
            request.replaceConflictPlanIds
                .filter { it != planId }
                .forEach { conflictId ->
                    mealPlanRepository.findByUserIdAndId(userId, conflictId)?.let {
                        mealPlanRepository.delete(it)
                    }
                }
            mealPlanRepository.flush()
        }

        plan.name = request.name.take(100)
        plan.description = request.description?.take(500)
        plan.goal = request.goal ?: plan.goal
        plan.totalCalories = request.totalCalories ?: plan.totalCalories
        plan.primaryMealType = derivePrimaryMealType(request) ?: plan.primaryMealType

        val parsedScheduleType = request.scheduleType?.let { parseScheduleType(it) }
        plan.scheduleType = parsedScheduleType
        plan.oneOffDate = if (parsedScheduleType == WorkoutScheduleType.ONE_TIME) {
            request.oneOffDate?.let { LocalDate.parse(it) }
        } else null
        plan.scheduleDays = if (parsedScheduleType == WorkoutScheduleType.RECURRING) {
            normalizeDays(request.scheduleDays)
        } else mutableSetOf()

        mealPlanRepository.save(plan)

        val existingMeals = mealRepository.findAllByMealPlan(plan)
        if (existingMeals.isNotEmpty()) {
            mealRepository.deleteAll(existingMeals)
            mealRepository.flush()
        }

        persistMeals(plan, request.meals)

        return plan.id!!
    }

    private fun persistMeals(plan: MealPlanEntity, meals: List<SaveMealRequest>) {
        if (meals.isEmpty()) {
            throw BadRequestException("Meal plan must contain at least one meal")
        }

        val allFoodProductIds = meals.flatMap { it.dishes }.mapNotNull { it.foodProductId }.toSet()
        val productMap: Map<UUID, FoodProductEntity> = if (allFoodProductIds.isNotEmpty()) {
            val products = foodProductRepository.findAllById(allFoodProductIds)
            val existingIds = products.mapNotNull { it.id }.toSet()
            val missing = allFoodProductIds - existingIds
            if (missing.isNotEmpty()) {
                throw BadRequestException("Unknown food product IDs: $missing")
            }
            products.associateBy { it.id!! }
        } else {
            emptyMap()
        }

        var planTotalCalories = 0
        var hasAnyCalories = false

        meals.forEach { mealReq ->
            val mealType = parseMealType(mealReq.mealType)
                ?: throw BadRequestException("Unknown meal type: ${mealReq.mealType}")

            val meal = mealRepository.save(
                MealEntity(
                    mealPlan = plan,
                    mealType = mealType,
                    name = mealReq.name.take(100),
                    estimatedCalories = mealReq.estimatedCalories
                )
            )

            var mealCaloriesSum = 0
            var hasMealCalories = false

            mealReq.dishes.forEach { dishReq ->
                val product = dishReq.foodProductId?.let { productMap[it] }

                val resolvedName: String
                val resolvedPortionDescription: String?
                val resolvedCalories: Int?
                val resolvedProtein: Int?
                val resolvedCarbs: Int?
                val resolvedFat: Int?

                if (product != null) {
                    val grams = dishReq.grams ?: 100.0
                    resolvedName = product.nameRu
                    resolvedPortionDescription = "${grams.toInt()}г"
                    resolvedCalories = scalePer100g(product.caloriesPer100g, grams)
                    resolvedProtein = scalePer100g(product.proteinPer100g, grams)
                    resolvedCarbs = scalePer100g(product.carbsPer100g, grams)
                    resolvedFat = scalePer100g(product.fatPer100g, grams)
                } else {
                    resolvedName = dishReq.name.take(150)
                    resolvedPortionDescription = dishReq.portionDescription?.take(50)
                    resolvedCalories = dishReq.calories
                    resolvedProtein = dishReq.proteinG
                    resolvedCarbs = dishReq.carbsG
                    resolvedFat = dishReq.fatG
                }

                resolvedCalories?.let { mealCaloriesSum += it; hasMealCalories = true }

                dishRepository.save(
                    DishEntity(
                        meal = meal,
                        name = resolvedName,
                        description = dishReq.description?.take(500),
                        portionDescription = resolvedPortionDescription,
                        calories = resolvedCalories,
                        proteinG = resolvedProtein,
                        carbsG = resolvedCarbs,
                        fatG = resolvedFat,
                        foodProductId = dishReq.foodProductId,
                        grams = dishReq.grams,
                        foodCategory = product?.category
                    )
                )
            }

            if (hasMealCalories) {
                meal.estimatedCalories = mealCaloriesSum
                mealRepository.save(meal)
                planTotalCalories += mealCaloriesSum
                hasAnyCalories = true
            }
        }

        if (hasAnyCalories) {
            plan.totalCalories = planTotalCalories
        }
    }

    private fun derivePrimaryMealType(request: SaveMealPlanRequest): String? {
        val explicit = request.mealType?.trim()?.uppercase()
        if (explicit != null && isValidMealType(explicit)) return explicit

        val fromMeals = request.meals.firstOrNull()?.mealType?.trim()?.uppercase()
        return if (fromMeals != null && isValidMealType(fromMeals)) fromMeals else null
    }

    private fun parseScheduleType(raw: String): WorkoutScheduleType? =
        try { WorkoutScheduleType.valueOf(raw.uppercase()) } catch (_: Exception) { null }

    private fun normalizeDays(days: List<String>): MutableSet<String> =
        days.map { it.trim().uppercase() }
            .filter { day -> try { DayOfWeek.valueOf(day); true } catch (_: Exception) { false } }
            .distinct()
            .toMutableSet()

    private fun parseAiResponse(json: String): AiMealChatResponse {
        return try {
            val node = objectMapper.readTree(json)
            val type = node.get("type")?.asText()?.lowercase()

            when (type) {
                "clarification" -> {
                    val message = node.get("message")?.asText()
                        ?: throw BadRequestException("AI response missing 'message' field")
                    AiMealChatResponse(AiMealResponseType.CLARIFICATION, message)
                }

                "meal_plan", "mealplan" -> {
                    val planNode = node.get("mealPlan") ?: node.get("meal_plan")
                        ?: throw BadRequestException("AI response missing 'mealPlan' field")
                    val plan = objectMapper.treeToValue(planNode, AiMealPlanDto::class.java)

                    AiMealChatResponse(AiMealResponseType.MEAL_PLAN, "", plan)
                }

                else -> throw BadRequestException("Unexpected AI response type: $type")
            }
        } catch (e: BadRequestException) {
            throw e
        } catch (e: Exception) {
            log.error("Failed to parse AI meal response (length={}): {}", json.length, json.take(1000), e)
            throw BadRequestException("AI returned an invalid response, please try again")
        }
    }

    private fun parseMealType(raw: String): MealType? {
        val normalized = raw.trim().uppercase()
        return MealType.entries.firstOrNull { it.name == normalized }
    }

    private fun escapeControlCharsInStrings(json: String): String {
        val out = StringBuilder(json.length + 16)
        var inString = false
        var escaped = false
        for (ch in json) {
            if (inString) {
                if (escaped) {
                    out.append(ch)
                    escaped = false
                    continue
                }
                when {
                    ch == '\\' -> {
                        out.append(ch)
                        escaped = true
                    }
                    ch == '"' -> {
                        out.append(ch)
                        inString = false
                    }
                    ch.code < 0x20 -> {
                        when (ch) {
                            '\n' -> out.append("\\n")
                            '\r' -> out.append("\\r")
                            '\t' -> out.append("\\t")
                            '\b' -> out.append("\\b")
                            '' -> out.append("\\f")
                            else -> out.append("\\u").append("%04x".format(ch.code))
                        }
                    }
                    else -> out.append(ch)
                }
            } else {
                out.append(ch)
                if (ch == '"') {
                    inString = true
                }
            }
        }
        return out.toString()
    }

    private fun extractJsonFromResponse(raw: String): String {
        var text = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            val extracted = text.substring(firstBrace, lastBrace + 1)
            log.debug("Extracted JSON from position {} to {} (total length {})", firstBrace, lastBrace, text.length)
            return extracted
        }

        log.warn("Could not find JSON object boundaries in GigaChat response (length={}): {}",
            text.length, text.take(500))
        return text
    }

    private fun repairGigaChatJson(json: String): String =
        json.replace(Regex("""(null|true|false|-?\d+(?:\.\d+)?|"[^"]*")\s*\]([\s,}]+)$""")) { m ->
            val value = m.groupValues[1]
            val braces = m.groupValues[2].filter { it == '}' }
            if (braces.length >= 1) "$value}]${braces.drop(1)}" else "$value}]"
        }

    private fun scalePer100g(per100g: Double, grams: Double): Int? {
        val raw = per100g * grams / 100.0
        if (raw.isNaN() || raw.isInfinite()) return null
        return raw.roundToInt().coerceAtLeast(0)
    }

    private fun buildPlanSummary(plan: AiMealPlanDto): String = buildString {
        plan.meals.forEachIndexed { index, meal ->
            val mealLabel = mealLabel(meal.mealType)
            val cal = meal.estimatedCalories?.let { " (~$it ккал)" } ?: ""
            append("${index + 1}) $mealLabel — ${meal.name}$cal:")
            if (meal.dishes.isEmpty()) {
                appendLine(" —")
            } else {
                appendLine()
                meal.dishes.forEachIndexed { dishIdx, dish ->
                    val portion = dish.portionDescription?.let { " — $it" } ?: ""
                    val dishCal = dish.calories?.let { ", $it ккал" } ?: ""
                    appendLine("   ${dishIdx + 1}. ${dish.name}$portion$dishCal")
                }
            }
        }
    }.trimEnd()

    private fun mealLabel(rawType: String): String = when (parseMealType(rawType)) {
        MealType.BREAKFAST -> "Завтрак"
        MealType.LUNCH -> "Обед"
        MealType.DINNER -> "Ужин"
        null -> rawType
    }

    private fun generateFriendlyMessage(planName: String, planSummary: String): String {
        return "Привет! Я составил для тебя план питания «$planName»:\n\n$planSummary\n\nПриятного аппетита!"
    }

    private fun buildContextMessage(
        weightKg: Double?,
        heightCm: Double?,
        age: Int?,
        gender: String?,
        goal: MealGoal?,
        dietaryRestrictions: String?,
        allergies: String?,
        mealType: String?,
        userMessage: String
    ): String = buildString {
        appendLine("=== Данные пользователя ===")
        append("Вес: ${weightKg?.let { "$it кг" } ?: "не указан"}")
        append(" | Рост: ${heightCm?.let { "$it см" } ?: "не указан"}")
        append(" | Возраст: ${age?.let { "$it лет" } ?: "не указан"}")
        appendLine(" | Пол: ${gender ?: "не указан"}")
        appendLine("Цель: ${goalLabel(goal)}")
        appendLine("Диетические ограничения: ${dietaryRestrictions?.takeIf { it.isNotBlank() } ?: "нет"}")
        appendLine("Аллергии: ${allergies?.takeIf { it.isNotBlank() } ?: "нет"}")
        appendLine("Тип приёма пищи: ${mealTypeLabel(mealType)}")
        appendLine()
        appendLine("=== Запрос ===")
        appendLine(userMessage)
        appendLine()
        appendLine("=== Доступные продукты (только из этого каталога, формат: pipe-delimited) ===")
        append(getCatalogText())
    }

    private fun goalLabel(goal: MealGoal?): String = when (goal) {
        MealGoal.LOSE_WEIGHT -> "снижение веса"
        MealGoal.MAINTAIN -> "поддержание веса"
        MealGoal.GAIN_MUSCLE -> "набор мышечной массы"
        null -> "не указана"
    }

    private fun mealTypeLabel(mealType: String?): String = when (mealType?.trim()?.uppercase()) {
        "BREAKFAST" -> "Завтрак"
        "LUNCH" -> "Обед"
        "DINNER" -> "Ужин"
        else -> "не указан"
    }

    companion object {
        private val SYSTEM_PROMPT = """
Ты персональный AI-диетолог в фитнес-приложении GymGenie.

КРИТИЧНО: Твой ответ — это ТОЛЬКО чистый JSON. Ни одного символа до { и после }. Никаких рассуждений, пояснений, markdown, ```json блоков. Только JSON-объект.

ПРАВИЛА:
- Отвечай ТОЛЬКО валидным JSON — ничего кроме JSON-объекта в ответе
- Составляй план питания ТОЛЬКО из продуктов в предоставленном каталоге — используй их точные foodProductId
- Учитывай физические параметры (вес, рост, возраст, пол), цель (LOSE_WEIGHT / MAINTAIN / GAIN_MUSCLE), диетические ограничения и аллергии пользователя
- Задавай уточняющий вопрос только если информации реально недостаточно, иначе сразу составляй план
- Отвечай на русском языке (все названия блюд, описания и сообщения — на русском)
- Если пользователь просит изменить план — верни type:"meal_plan" с ПОЛНЫМ обновлённым планом (один приём пищи со всеми продуктами)
- Не выдумывай продукты которые противоречат указанным аллергиям или ограничениям
- Генерируй план для ОДНОГО приёма пищи — того типа (mealType), который указан в данных пользователя

ПРАВИЛА СОСТАВЛЕНИЯ ПРИЁМА ПИЩИ:
1. Рассчитай дневную норму калорий по формуле Миффлина-Сан Жеора:
   - Мужчины: BMR = 10 × вес(кг) + 6.25 × рост(см) − 5 × возраст − 161 + 166
   - Женщины: BMR = 10 × вес(кг) + 6.25 × рост(см) − 5 × возраст − 161
   - Умножь BMR на коэффициент активности 1.55 (умеренная активность)
   - LOSE_WEIGHT: дефицит 15-20% от TDEE
   - MAINTAIN: TDEE без изменений
   - GAIN_MUSCLE: профицит 10-15% от TDEE
2. Рассчитай калории для ОДНОГО приёма пищи: завтрак ~25-30% от TDEE, обед ~35-40% от TDEE, ужин ~25-30% от TDEE
3. Баланс БЖУ:
   - Белок: 1.6-2.2г на кг массы тела в день (для LOSE_WEIGHT ближе к 2.2г, для GAIN_MUSCLE 1.8-2г) — раздели пропорционально на этот приём
   - Жиры: 0.8-1.2г на кг массы тела в день (минимум 0.8г) — раздели пропорционально
   - Углеводы: остаток калорий после белков и жиров
4. Приём пищи ОБЯЗАТЕЛЬНО должен содержать 4-6 разных продуктов для разнообразия
5. Приём пищи ДОЛЖЕН включать источник белка (мясо, рыба, яйца, творог, бобовые)
6. Подбирай РЕАЛИСТИЧНЫЕ порции: обед для мужчины 80-95кг это НЕ 100г курицы — это 200-300г мяса + 150-200г гарнира + овощи + соус/масло
7. НИКОГДА не делай приём пищи из 1-2 продуктов — это не реалистичный план
8. Используй данные per-100g из каталога для расчёта точной граммовки: если нужно 40г белка в обед, а в курице 23г белка на 100г, то нужно ~170г курицы
9. Суммарные калории всех блюд в приёме ДОЛЖНЫ попадать в рассчитанный коридор для этого типа приёма (±10%)

КАТАЛОГ ПРОДУКТОВ (формат: pipe-delimited таблица):
- Каталог передан в формате: foodProductId|nameRu|category|cal|protein|fat|carbs (per 100g)
- Каждая строка — один продукт с полями через |

КРИТИЧЕСКИЕ ПРАВИЛА ДЛЯ ПРОДУКТОВ:
1. Каждое блюдо (dish) ДОЛЖНО соответствовать ровно одному продукту из предоставленного каталога
2. Поле foodProductId в каждом блюде ДОЛЖНО быть скопировано дословно из первого столбца каталога. Это обычная операция copy-paste, а не генерация нового значения
3. ЗАПРЕЩЕНО использовать продукты, которых нет в каталоге. Не выдумывай UUID, не модифицируй существующие, не комбинируй части разных id
4. Для каждого продукта в каталоге указаны cal, protein, fat, carbs (per 100g) — используй эти значения, чтобы рассчитать нужное количество граммов каждого продукта так, чтобы суммарный приём соответствовал калорийной цели и балансу БЖУ
5. Поле grams — ВСЕГДА масса порции продукта в ГРАММАХ (число от 1 до 2000). Даже если продукт считается поштучно (яйца, бананы и т.д.), ты ОБЯЗАН указать примерный вес в граммах (1 яйцо ≈ 60г, 1 желток ≈ 18г, 1 белок ≈ 33г, 1 банан ≈ 120г). НИКОГДА не пиши количество штук в поле grams — только граммы. Подбирай граммовку осознанно на основе нутриентов per-100g, а не наугад
6. Поле name блюда = nameRu продукта из каталога (скопируй дословно из второго столбца)
7. НЕ рассчитывай calories, proteinG, carbsG, fatG в ответе — сервер пересчитает макросы автоматически из каталога. Можешь указать приблизительные значения или null. Но ты ДОЛЖЕН использовать данные per-100g из каталога при выборе граммовки, чтобы итоговый приём попадал в целевую калорийность
8. Если нужного продукта нет в каталоге — подбери ближайший по категории и названию из тех, что ЕСТЬ в каталоге
9. Составные блюда разбивай на отдельные продукты: "курица с рисом" = два dish (один с Куриной грудкой, другой с Рисом)
10. Перед тем как отправить ответ, мысленно сверь КАЖДЫЙ foodProductId с каталогом. Если хотя бы один id не находит точного совпадения — замени на ближайший существующий
11. Нарушение этих правил делает план непригодным для использования: пользователь не сможет его сохранить, и весь ответ будет отброшен

СТРУКТУРА ПЛАНА:
- План состоит из ОДНОГО приёма пищи — того типа, который указал пользователь
- Массив meals ДОЛЖЕН содержать ровно один элемент с mealType, который указал пользователь
- Допустимые значения mealType строго: "BREAKFAST", "LUNCH", "DINNER" (заглавными)
- Приём пищи содержит блюда (dishes), каждое блюдо = один продукт из каталога с указанием граммовки
- portionDescription = строка вида "150г" (масса порции в граммах, НИКОГДА не "1шт" — всегда граммы)
- totalCalories и estimatedCalories можно указать null — сервер пересчитает из каталога

ФОРМАТ ОТВЕТА — строго один из двух вариантов (НИЧЕГО кроме JSON — ни слова до или после):

Вариант 1 (уточнение):
{"type":"clarification","message":"вопрос пользователю"}

Вариант 2 (готовый план, ОБЯЗАТЕЛЬНО 4-6 dishes, ровно ОДИН meal):
{"type":"meal_plan","mealPlan":{"name":"название","description":"описание","totalCalories":null,"meals":[{"mealType":"BREAKFAST","name":"Завтрак","estimatedCalories":null,"dishes":[{"foodProductId":"<id>","name":"Овсяные хлопья","grams":80,"portionDescription":"80г","calories":null,"proteinG":null,"carbsG":null,"fatG":null},{"foodProductId":"<id>","name":"Яичный белок","grams":100,"portionDescription":"100г","calories":null,"proteinG":null,"carbsG":null,"fatG":null},{"foodProductId":"<id>","name":"Банан","grams":120,"portionDescription":"120г","calories":null,"proteinG":null,"carbsG":null,"fatG":null},{"foodProductId":"<id>","name":"Творог 5%","grams":150,"portionDescription":"150г","calories":null,"proteinG":null,"carbsG":null,"fatG":null}]}]}}

НАПОМИНАНИЕ: приём пищи ОБЯЗАН содержать 4-6 разных продуктов. План из 1-3 продуктов — НЕВАЛИДЕН и будет отброшен. Массив meals ДОЛЖЕН содержать ровно ОДИН элемент.
        """.trimIndent()
    }
}
