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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.Period
import java.util.UUID
import kotlin.math.roundToInt

/**
 * AI-driven meal-plan generator. Mirrors [com.asc.gymgenie.ai.service.WorkoutAiService]
 * end-to-end with two deliberate divergences:
 *  - The generated plan is single-day (no `WorkoutPlanDay` analogue): plan -> meals -> dishes.
 *  - A separate [MealConversationSessionStore] keeps meal and workout dialogs isolated.
 *
 * Like the workout flow, dishes are validated against the food product catalog:
 * each dish must reference a real [FoodProductEntity] via `foodProductId`. Hallucinated
 * product IDs are stripped; macros are recalculated from catalog per-100g values.
 */
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

    // ================================================================
    // Chat
    // ================================================================

    @Transactional
    fun chat(userId: UUID, request: AiMealChatRequest): AiMealChatResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }

        if (sessionStore.isEmpty(userId)) {
            // First message of a session: persist any newly supplied profile fields
            // and seed the GigaChat conversation with the system prompt + a context
            // message (profile + goal + restrictions + the user's first prompt).
            var profileUpdated = false
            request.ageYears?.let { user.ageYears = it; profileUpdated = true }
            request.heightCm?.let { user.heightCm = it; profileUpdated = true }
            request.weightKg?.let { user.weightKg = it; profileUpdated = true }
            if (profileUpdated) userRepository.save(user)

            // Load the food product catalog so GigaChat can reference real products.
            // Only send foodProductId, nameRu, and category — macros are recalculated
            // server-side from per-100g values. Keeping the payload small avoids
            // exceeding GigaChat's 8192-token context window.
            val foodProducts = foodProductRepository.findByIsActiveTrue().map { fp ->
                mapOf<String, Any?>(
                    "foodProductId" to fp.id.toString(),
                    "nameRu" to fp.nameRu,
                    "category" to fp.category.name
                )
            }

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
                userMessage = request.message,
                foodProducts = foodProducts
            )

            val initialized = sessionStore.initializeIfEmpty(
                userId,
                GigaChatMessage("system", SYSTEM_PROMPT),
                GigaChatMessage("user", contextMessage)
            )
            if (!initialized) {
                // Lost the race against a concurrent first call — treat as
                // a regular follow-up and append only the user message.
                sessionStore.addMessages(userId, GigaChatMessage("user", request.message))
            }
        } else {
            sessionStore.addMessages(userId, GigaChatMessage("user", request.message))
        }

        val history = sessionStore.getHistory(userId)
        val rawResponse = gigaChatClient.chat(history)
        log.debug("GigaChat raw meal response for user {}: {}", userId, rawResponse)

        sessionStore.addMessages(userId, GigaChatMessage("assistant", rawResponse))

        val stripped = rawResponse.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val cleanedJson = repairGigaChatJson(escapeControlCharsInStrings(stripped))

        val parsed = parseAiResponse(cleanedJson)

        // Validate the plan against the food product catalog (same approach as
        // WorkoutAiService validates exercise IDs): recalculate macros from catalog
        // per-100g values, strip hallucinated product IDs, and reject if ALL products
        // are hallucinated.
        if (parsed.type == AiMealResponseType.MEAL_PLAN && parsed.mealPlan != null) {
            var plan = parsed.mealPlan
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

            // Validate food product IDs across all dishes
            val allDishes = plan.meals.flatMap { it.dishes }
            val requestedProductIds = allDishes.mapNotNull { it.foodProductId }.toSet()

            if (requestedProductIds.isEmpty()) {
                // AI ignored the catalog entirely — all dishes are free-text
                sessionStore.addMessages(
                    userId,
                    GigaChatMessage(
                        "user",
                        "Ошибка: ни одно блюдо не содержит foodProductId из каталога. " +
                            "Пожалуйста, составь план питания строго используя только foodProductId " +
                            "из предоставленного каталога продуктов."
                    )
                )
                throw BadRequestException("AI returned an invalid response, please try again")
            }

            val existingProducts = foodProductRepository.findAllById(requestedProductIds)
            val existingProductMap = existingProducts.associateBy { it.id!! }
            val existingProductIds = existingProductMap.keys

            val reconciledMeals = plan.meals.map { meal ->
                val reconciledDishes = meal.dishes.map { dish ->
                    if (dish.foodProductId != null && dish.foodProductId in existingProductIds) {
                        val product = existingProductMap[dish.foodProductId]!!
                        val grams = dish.grams ?: 100.0
                        dish.copy(
                            name = product.nameRu,
                            calories = scalePer100g(product.caloriesPer100g, grams),
                            proteinG = scalePer100g(product.proteinPer100g, grams),
                            carbsG = scalePer100g(product.carbsPer100g, grams),
                            fatG = scalePer100g(product.fatPer100g, grams),
                            portionDescription = "${grams.toInt()}г",
                            grams = grams
                        )
                    } else if (dish.foodProductId != null) {
                        log.warn(
                            "Removing hallucinated food product from AI response (foodProductId={}, userId={})",
                            dish.foodProductId, userId
                        )
                        dish.copy(foodProductId = null, grams = null)
                    } else {
                        dish
                    }
                }
                val mealCalories = reconciledDishes.mapNotNull { it.calories }.sum().takeIf { it > 0 }
                meal.copy(dishes = reconciledDishes, estimatedCalories = mealCalories ?: meal.estimatedCalories)
            }

            val totalCalories = reconciledMeals.flatMap { it.dishes }.mapNotNull { it.calories }.sum().takeIf { it > 0 }
            plan = plan.copy(meals = reconciledMeals, totalCalories = totalCalories ?: plan.totalCalories)

            val validProductCount = reconciledMeals.flatMap { it.dishes }.count { it.foodProductId != null }
            if (validProductCount == 0) {
                sessionStore.addMessages(
                    userId,
                    GigaChatMessage(
                        "user",
                        "Ошибка: все продукты из твоего ответа не найдены в каталоге. " +
                            "Пожалуйста, составь план питания строго используя только foodProductId " +
                            "из предоставленного каталога."
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

    // ================================================================
    // Persistence
    // ================================================================

    @Transactional
    fun saveMealPlan(userId: UUID, request: SaveMealPlanRequest): UUID {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }

        val plan = mealPlanRepository.save(
            MealPlanEntity(
                user = user,
                name = request.name.take(100),
                description = request.description?.take(500),
                goal = request.goal,
                totalCalories = request.totalCalories,
                createdBy = NutritionCreatedBy.AI
            )
        )

        persistMeals(plan, request.meals)

        return plan.id!!
    }

    @Transactional
    fun replaceMealPlan(userId: UUID, planId: UUID, request: SaveMealPlanRequest): UUID {
        val plan = mealPlanRepository.findByUserIdAndId(userId, planId)
            ?: throw NotFoundException("Meal plan not found")

        // Update plan-level metadata before swapping out the children. Doing the
        // metadata update first keeps `goal`/`totalCalories` consistent even if
        // the meal recreate step throws.
        plan.name = request.name.take(100)
        plan.description = request.description?.take(500)
        plan.goal = request.goal ?: plan.goal
        plan.totalCalories = request.totalCalories ?: plan.totalCalories
        mealPlanRepository.save(plan)

        // Remove existing meals via repository (orphanRemoval + CascadeType.ALL on
        // MealPlanEntity.meals also wipes their dishes). We delete through the
        // child repository rather than `plan.meals.clear()` to avoid surprising
        // detach behaviour and to mirror what `WorkoutPlanService` does for days.
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

        // Validate all referenced food product IDs up front (mirrors WorkoutAiService.saveWorkout)
        val allFoodProductIds = meals.flatMap { it.dishes }.mapNotNull { it.foodProductId }.toSet()
        if (allFoodProductIds.isNotEmpty()) {
            val existingIds = foodProductRepository.findAllById(allFoodProductIds).mapNotNull { it.id }.toSet()
            val missing = allFoodProductIds - existingIds
            if (missing.isNotEmpty()) {
                throw BadRequestException("Unknown food product IDs: $missing")
            }
        }

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

            mealReq.dishes.forEach { dishReq ->
                dishRepository.save(
                    DishEntity(
                        meal = meal,
                        name = dishReq.name.take(150),
                        description = dishReq.description?.take(500),
                        portionDescription = dishReq.portionDescription?.take(50),
                        calories = dishReq.calories,
                        proteinG = dishReq.proteinG,
                        carbsG = dishReq.carbsG,
                        fatG = dishReq.fatG,
                        foodProductId = dishReq.foodProductId,
                        grams = dishReq.grams
                    )
                )
            }
        }
    }

    // ================================================================
    // GigaChat response parsing
    // ================================================================

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
                    // The friendly user-facing `message` is filled in by a
                    // follow-up GigaChat call; leave empty here.
                    AiMealChatResponse(AiMealResponseType.MEAL_PLAN, "", plan)
                }

                else -> throw BadRequestException("Unexpected AI response type: $type")
            }
        } catch (e: BadRequestException) {
            throw e
        } catch (e: Exception) {
            log.error("Failed to parse AI meal response: {}", json, e)
            throw BadRequestException("AI returned an invalid response, please try again")
        }
    }

    private fun parseMealType(raw: String): MealType? {
        val normalized = raw.trim().uppercase()
        return MealType.entries.firstOrNull { it.name == normalized }
    }

    /**
     * Same string-aware control-char escaper as the workout flow. GigaChat
     * occasionally inlines literal `\n`/`\r`/`\t` inside a JSON string,
     * which is invalid JSON and breaks Jackson. We rewrite those chars to
     * their proper escapes only when the cursor is inside a string literal.
     */
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

    /**
     * GigaChat sometimes drops the closing `}` of the last array element, e.g.
     *   `"fatG":8],}}}`  or  `"calories":350]} }}`
     * Insert one `}` before `]` and keep the remaining trailing `}`s after.
     */
    private fun repairGigaChatJson(json: String): String =
        json.replace(Regex("""(null|true|false|-?\d+(?:\.\d+)?|"[^"]*")\s*\]([\s,}]+)$""")) { m ->
            val value = m.groupValues[1]
            val braces = m.groupValues[2].filter { it == '}' }
            if (braces.length >= 1) "$value}]${braces.drop(1)}" else "$value}]"
        }

    // ================================================================
    // Friendly message generation (history-free)
    // ================================================================

    /**
     * Scales a per-100g nutritional value to the given gram weight, returning
     * a rounded non-negative integer or `null` on non-finite input.
     */
    private fun scalePer100g(per100g: Double, grams: Double): Int? {
        val raw = per100g * grams / 100.0
        if (raw.isNaN() || raw.isInfinite()) return null
        return raw.roundToInt().coerceAtLeast(0)
    }

    /**
     * Builds a deterministic, human-readable summary of the parsed plan.
     * Used both as input to the friendly-message GigaChat call and as the
     * fallback message body if that call fails.
     */
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

    /**
     * Produces a friendly, motivating message describing the generated plan.
     *
     * Intentionally NOT added to the conversation session history — like the
     * workout flow, this is an internal formatting step and including it
     * would pollute future turns with rendered prose.
     */
    private fun generateFriendlyMessage(planName: String, planSummary: String): String {
        val prompt = """
Ты дружелюбный диетолог в фитнес-приложении. Напиши короткое мотивирующее сообщение
для пользователя о плане питания, который ты составил.

Название плана: $planName

Состав плана (точно следуй порядку и параметрам, ничего не добавляй и не убирай):
$planSummary

Требования к сообщению:
- Начни с короткого приветствия
- Кратко упомяни название плана
- Перечисли все приёмы пищи в том же порядке с теми же блюдами и параметрами
- Заверши одной мотивирующей фразой
- Только текст, без JSON, без markdown, без заголовков
        """.trimIndent()

        return try {
            gigaChatClient.chat(listOf(GigaChatMessage("user", prompt)))
        } catch (e: Exception) {
            log.warn("Failed to generate friendly meal message, using fallback", e)
            "Привет! Я составил для тебя план питания «$planName»:\n$planSummary\nПриятного аппетита!"
        }
    }

    // ================================================================
    // Context message
    // ================================================================

    private fun buildContextMessage(
        weightKg: Double?,
        heightCm: Double?,
        age: Int?,
        gender: String?,
        goal: MealGoal?,
        dietaryRestrictions: String?,
        allergies: String?,
        userMessage: String,
        foodProducts: List<Map<String, Any?>>
    ): String = buildString {
        appendLine("=== Данные пользователя ===")
        append("Вес: ${weightKg?.let { "$it кг" } ?: "не указан"}")
        append(" | Рост: ${heightCm?.let { "$it см" } ?: "не указан"}")
        append(" | Возраст: ${age?.let { "$it лет" } ?: "не указан"}")
        appendLine(" | Пол: ${gender ?: "не указан"}")
        appendLine("Цель: ${goalLabel(goal)}")
        appendLine("Диетические ограничения: ${dietaryRestrictions?.takeIf { it.isNotBlank() } ?: "нет"}")
        appendLine("Аллергии: ${allergies?.takeIf { it.isNotBlank() } ?: "нет"}")
        appendLine()
        appendLine("=== Запрос ===")
        appendLine(userMessage)
        appendLine()
        appendLine("=== Доступные продукты (только из этого каталога) ===")
        append(objectMapper.writeValueAsString(foodProducts))
    }

    private fun goalLabel(goal: MealGoal?): String = when (goal) {
        MealGoal.LOSE_WEIGHT -> "снижение веса"
        MealGoal.MAINTAIN -> "поддержание веса"
        MealGoal.GAIN_MUSCLE -> "набор мышечной массы"
        null -> "не указана"
    }

    companion object {
        private val SYSTEM_PROMPT = """
Ты персональный AI-диетолог в фитнес-приложении GymGenie.

ПРАВИЛА:
- Отвечай ТОЛЬКО валидным JSON без markdown блоков, без ```json, только чистый JSON
- Составляй планы питания ТОЛЬКО из продуктов в предоставленном каталоге — используй их точные foodProductId
- Учитывай физические параметры (вес, рост, возраст, пол), цель (LOSE_WEIGHT / MAINTAIN / GAIN_MUSCLE), диетические ограничения и аллергии пользователя
- Задавай уточняющий вопрос только если информации реально недостаточно, иначе сразу составляй план
- Отвечай на русском языке (все названия блюд, описания и сообщения — на русском)
- Если пользователь просит изменить план — верни type:"meal_plan" с ПОЛНЫМ обновлённым планом (все приёмы пищи, включая неизменённые)
- Не выдумывай продукты которые противоречат указанным аллергиям или ограничениям

КРИТИЧЕСКИЕ ПРАВИЛА ДЛЯ ПРОДУКТОВ:
1. Каждое блюдо (dish) ДОЛЖНО соответствовать ровно одному продукту из предоставленного каталога
2. Поле foodProductId в каждом блюде ДОЛЖНО быть скопировано дословно из поля foodProductId каталога. Это обычная операция copy-paste, а не генерация нового значения
3. ЗАПРЕЩЕНО использовать продукты, которых нет в каталоге. Не выдумывай UUID, не модифицируй существующие, не комбинируй части разных id
4. Поле grams — масса порции продукта в граммах (число от 1 до 2000)
5. Поле name блюда = nameRu продукта из каталога (скопируй дословно)
6. НЕ рассчитывай calories, proteinG, carbsG, fatG — сервер пересчитает макросы автоматически из каталога. Можешь указать приблизительные значения или null
7. Если нужного продукта нет в каталоге — подбери ближайший по категории и названию из тех, что ЕСТЬ в каталоге
8. Составные блюда разбивай на отдельные продукты: "курица с рисом" = два dish (один с Куриной грудкой, другой с Рисом)
9. Перед тем как отправить ответ, мысленно сверь КАЖДЫЙ foodProductId с каталогом. Если хотя бы один id не находит точного совпадения — замени на ближайший существующий
10. Нарушение этих правил делает план непригодным для использования: пользователь не сможет его сохранить, и весь ответ будет отброшен

СТРУКТУРА ПЛАНА:
- План состоит из приёмов пищи (meals): минимум один из BREAKFAST, LUNCH, DINNER
- Допустимые значения mealType строго: "BREAKFAST", "LUNCH", "DINNER" (заглавными)
- Каждый приём пищи содержит блюда (dishes), каждое блюдо = один продукт из каталога с указанием граммовки
- portionDescription = строка вида "150г" (масса порции)
- totalCalories и estimatedCalories можно указать null — сервер пересчитает из каталога

ФОРМАТ ОТВЕТА — строго один из двух вариантов:

Вариант 1 (уточнение, когда нужна доп. информация):
{"type":"clarification","message":"вопрос пользователю"}

Вариант 2 (готовый план питания):
{"type":"meal_plan","mealPlan":{"name":"название плана","description":"краткое описание","totalCalories":null,"meals":[{"mealType":"BREAKFAST","name":"Завтрак","estimatedCalories":null,"dishes":[{"foodProductId":"<id из каталога>","name":"Овсяные хлопья","grams":80,"portionDescription":"80г","calories":null,"proteinG":null,"carbsG":null,"fatG":null}]}]}}
        """.trimIndent()
    }
}
