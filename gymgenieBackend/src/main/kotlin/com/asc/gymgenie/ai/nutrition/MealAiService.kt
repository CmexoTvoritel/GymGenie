package com.asc.gymgenie.ai.nutrition

import com.asc.gymgenie.ai.client.GigaChatClient
import com.asc.gymgenie.ai.client.dto.GigaChatMessage
import com.asc.gymgenie.ai.nutrition.dto.*
import com.asc.gymgenie.common.exception.BadRequestException
import com.asc.gymgenie.common.exception.NotFoundException
import com.asc.gymgenie.nutrition.entity.*
import com.asc.gymgenie.nutrition.repository.DishRepository
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

/**
 * AI-driven meal-plan generator. Mirrors [com.asc.gymgenie.ai.service.WorkoutAiService]
 * end-to-end with three deliberate divergences:
 *  - There is no catalog to validate against (food items are free-form), so the
 *    response from GigaChat is accepted as-is once it parses cleanly.
 *  - The generated plan is single-day (no `WorkoutPlanDay` analogue): plan -> meals -> dishes.
 *  - A separate [MealConversationSessionStore] keeps meal and workout dialogs isolated.
 */
@Service
class MealAiService(
    private val userRepository: UserRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val mealRepository: MealRepository,
    private val dishRepository: DishRepository,
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
                userMessage = request.message
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

        // For a meal plan, GigaChat is allowed to write any food items it likes
        // (no catalog validation). To keep parity with the workout flow's two-step
        // message generation, we also produce a friendly user-facing message via
        // a separate, history-free GigaChat call. Failure falls back to a
        // deterministic template.
        if (parsed.type == AiMealResponseType.MEAL_PLAN && parsed.mealPlan != null) {
            val plan = parsed.mealPlan
            if (plan.meals.isEmpty()) {
                // Defensive: a meal_plan response with zero meals is a contract
                // violation — surface as 400 instead of persisting an empty plan later.
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
                        fatG = dishReq.fatG
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
        appendLine()
        appendLine("=== Запрос ===")
        append(userMessage)
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
- Учитывай физические параметры (вес, рост, возраст, пол), цель (LOSE_WEIGHT / MAINTAIN / GAIN_MUSCLE), диетические ограничения и аллергии пользователя
- Задавай уточняющий вопрос только если информации реально недостаточно, иначе сразу составляй план
- Отвечай на русском языке (все названия блюд, описания и сообщения — на русском)
- Если пользователь просит изменить план — верни type:"meal_plan" с ПОЛНЫМ обновлённым планом (все приёмы пищи, включая неизменённые)
- Не выдумывай ингредиенты которые противоречат указанным аллергиям или ограничениям

СТРУКТУРА ПЛАНА:
- План состоит из приёмов пищи (meals): минимум один из BREAKFAST, LUNCH, DINNER
- Допустимые значения mealType строго: "BREAKFAST", "LUNCH", "DINNER" (заглавными)
- Каждый приём пищи содержит блюда (dishes) с названием, кратким описанием, размером порции (например "150г"), калориями и БЖУ
- Все числовые поля (totalCalories, estimatedCalories, calories, proteinG, carbsG, fatG) — целые числа в граммах/килокалориях. Можно указать null если не уверен в значении.

ФОРМАТ ОТВЕТА — строго один из двух вариантов:

Вариант 1 (уточнение, когда нужна доп. информация):
{"type":"clarification","message":"вопрос пользователю"}

Вариант 2 (готовый план питания):
{"type":"meal_plan","mealPlan":{"name":"название плана","description":"краткое описание","totalCalories":2000,"meals":[{"mealType":"BREAKFAST","name":"Завтрак","estimatedCalories":500,"dishes":[{"name":"Овсянка с ягодами","description":"Овсянка на молоке с черникой","portionDescription":"250г","calories":350,"proteinG":12,"carbsG":55,"fatG":8}]}]}}
        """.trimIndent()
    }
}
