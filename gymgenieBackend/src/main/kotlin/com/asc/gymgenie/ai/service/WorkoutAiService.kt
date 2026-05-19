package com.asc.gymgenie.ai.service

import com.asc.gymgenie.ai.client.GigaChatClient
import com.asc.gymgenie.ai.client.dto.GigaChatMessage
import com.asc.gymgenie.ai.dto.*
import com.asc.gymgenie.common.exception.BadRequestException
import com.asc.gymgenie.common.exception.NotFoundException
import com.asc.gymgenie.exercise.repository.ExerciseRepository
import com.asc.gymgenie.user.repository.UserRepository
import com.asc.gymgenie.workout.entity.*
import com.asc.gymgenie.workout.repository.WorkoutPlanDayRepository
import com.asc.gymgenie.workout.repository.WorkoutPlanExerciseRepository
import com.asc.gymgenie.workout.repository.WorkoutPlanRepository
import tools.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.util.UUID

@Service
class WorkoutAiService(
    private val userRepository: UserRepository,
    private val exerciseRepository: ExerciseRepository,
    private val workoutPlanRepository: WorkoutPlanRepository,
    private val workoutPlanDayRepository: WorkoutPlanDayRepository,
    private val workoutPlanExerciseRepository: WorkoutPlanExerciseRepository,
    private val gigaChatClient: GigaChatClient,
    private val sessionStore: ConversationSessionStore,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun chat(userId: UUID, request: AiChatRequest): AiChatResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }

        if (sessionStore.isEmpty(userId)) {
            // Save profile data from request to the user's record (only if provided)
            var profileUpdated = false
            request.ageYears?.let { user.ageYears = it; profileUpdated = true }
            request.heightCm?.let { user.heightCm = it; profileUpdated = true }
            request.weightKg?.let { user.weightKg = it; profileUpdated = true }
            request.experience?.let { user.experience = it; profileUpdated = true }
            request.frequency?.let { user.frequency = it; profileUpdated = true }
            request.healthIssues?.let { user.healthIssues = it; profileUpdated = true }
            if (profileUpdated) userRepository.save(user)

            // First message: load exercise catalog + user profile into context, then atomically initialize session.
            // requiresWeight + defaultWeightPct are exposed so GigaChat can decide whether to emit setWeightsKg
            // and pick a plausible starting weight per exercise.
            // defaultWeightPct is a fraction of body weight (e.g. 0.6 = 60% of user body weight).
            val exercises = exerciseRepository.findAll().map { ex ->
                mapOf<String, Any?>(
                    "exerciseId" to ex.id.toString(),
                    "name" to ex.nameRu,
                    "muscleGroup" to ex.muscleGroup.name,
                    "category" to ex.category.name,
                    "difficulty" to ex.difficultyLevel.name,
                    "requiresWeight" to ex.requiresWeight,
                    "defaultWeightPct" to ex.defaultWeightPercentage
                )
            }
            val contextMessage = buildContextMessage(
                weightKg = request.weightKg ?: user.weightKg,
                heightCm = request.heightCm ?: user.heightCm,
                age = request.ageYears ?: user.birthDate?.let { Period.between(it, LocalDate.now()).years },
                gender = user.gender?.name,
                healthIssues = request.healthIssues ?: user.healthIssues,
                experience = request.experience ?: user.experience,
                frequency = request.frequency ?: user.frequency,
                userMessage = request.message,
                exercises = exercises
            )
            // initializeIfEmpty is atomic; if a concurrent request beat us, fall through to addMessages below
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
        log.debug("GigaChat raw response for user {}: {}", userId, rawResponse)

        sessionStore.addMessages(userId, GigaChatMessage("assistant", rawResponse))

        val stripped = rawResponse.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val cleanedJson = repairGigaChatJson(escapeControlCharsInStrings(stripped))

        val response = parseAiResponse(cleanedJson)

        // Defensive filter: GigaChat occasionally hallucinates exercise UUIDs that are
        // not present in the catalog. Strip those before the response reaches the
        // client so the user is not blocked by a downstream save-time validation
        // failure. The save/replace paths still validate as a second line of defence.
        if (response.type == AiResponseType.WORKOUT && response.workout != null) {
            val workout = response.workout
            val requestedIds = workout.exercises.map { it.exerciseId }.toSet()
            val existingExercises = exerciseRepository.findAllById(requestedIds)
            val existingIds = existingExercises.map { it.id!! }.toSet()

            val (valid, hallucinated) = workout.exercises.partition { it.exerciseId in existingIds }

            if (hallucinated.isNotEmpty()) {
                hallucinated.forEach { ex ->
                    log.warn(
                        "Removing hallucinated exercise from AI response (exerciseId={}, userId={})",
                        ex.exerciseId, userId
                    )
                }

                if (valid.isEmpty()) {
                    sessionStore.addMessages(
                        userId,
                        GigaChatMessage(
                            "user",
                            "Ошибка: все упражнения из твоего ответа не найдены в каталоге. " +
                                "Пожалуйста, составь тренировку строго используя только id упражнений " +
                                "из предоставленного каталога."
                        )
                    )
                    throw BadRequestException("AI returned an invalid response, please try again")
                }

                log.info(
                    "Delivering workout '{}' with {} exercises after removing {} hallucinated (userId={})",
                    workout.name, valid.size, hallucinated.size, userId
                )
            }

            // Reconcile per-set weights against the declared sets count for each surviving exercise.
            // GigaChat occasionally returns a setWeightsKg array whose length drifts from `sets`
            // (off-by-one, mismatched pyramid plan, etc.). We silently pad/trim instead of rejecting
            // the exercise so we don't discard otherwise good weight guidance. The catalog tells us
            // whether weights are expected at all — for requiresWeight=false exercises we drop the
            // field entirely even if the model emitted one by mistake.
            val exerciseEntityMap = existingExercises.associateBy { it.id!! }
            val normalized = valid.map { ex ->
                val catalogEntry = exerciseEntityMap[ex.exerciseId]
                reconcileAiExerciseSetWeights(ex, catalogEntry?.requiresWeight ?: false)
            }

            // Step 2: build a deterministic textual description of the validated
            // exercises using real catalog names, then ask GigaChat (as a fresh
            // standalone call — NOT part of session history) to wrap it into a
            // friendly user-facing message. This split eliminates the divergence
            // we used to see when the model wrote the message and picked the
            // exercises in the same call.
            val workoutRest = workout.restSeconds
                ?: normalized.firstOrNull()?.restSeconds
                ?: 60

            val exerciseDescriptions = normalized.mapIndexed { index, ex ->
                val name = exerciseEntityMap[ex.exerciseId]?.nameRu ?: "Упражнение ${index + 1}"
                val repsText = if (ex.reps != null) "${ex.reps} повторений" else "до отказа"
                val weightsText = ex.setWeightsKg
                    ?.filterNotNull()
                    ?.takeIf { it.isNotEmpty() }
                    ?.joinToString(separator = "/") { formatWeightForMessage(it) }
                    ?.let { ", веса по подходам: $it кг" }
                    ?: ""
                "${index + 1}) $name — ${ex.sets} подходов по $repsText$weightsText"
            }.joinToString(";\n")

            val fullDescription = "Отдых между подходами: ${workoutRest} сек\n$exerciseDescriptions"

            val friendlyMessage = generateFriendlyMessage(workout.name, fullDescription)

            return AiChatResponse(
                AiResponseType.WORKOUT,
                friendlyMessage,
                workout.copy(exercises = normalized, restSeconds = workoutRest)
            )
        }

        return response
    }

    @Transactional
    fun saveWorkout(userId: UUID, request: SaveWorkoutRequest): UUID {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }

        val exerciseIds = request.exercises.map { it.exerciseId }.toSet()
        val exerciseMap = exerciseRepository.findAllById(exerciseIds).associateBy { it.id!! }

        val missing = exerciseIds - exerciseMap.keys
        if (missing.isNotEmpty()) {
            throw BadRequestException("Unknown exercise IDs: $missing")
        }

        val plan = workoutPlanRepository.save(
            WorkoutPlanEntity(
                user = user,
                name = request.name.take(100),
                description = request.description?.take(500),
                createdBy = CreatedBy.AI,
                isActive = true,
                scheduleType = WorkoutScheduleType.ONE_TIME
            )
        )

        val day = workoutPlanDayRepository.save(
            WorkoutPlanDayEntity(
                workoutPlan = plan,
                dayOfWeek = DayOfWeek.MONDAY,
                name = "Тренировка",
                orderIndex = 0
            )
        )

        request.exercises.forEachIndexed { index, ex ->
            val normalizedSetWeights = normalizeAndValidateSetWeights(ex)
            workoutPlanExerciseRepository.save(
                WorkoutPlanExerciseEntity(
                    workoutPlanDay = day,
                    exercise = exerciseMap[ex.exerciseId]!!,
                    sets = ex.sets,
                    reps = ex.reps,
                    weightKg = normalizedSetWeights?.firstOrNull { it != null },
                    setWeightsKg = encodeSetWeights(normalizedSetWeights),
                    restSeconds = request.restSeconds,
                    orderIndex = index,
                    notes = ex.notes
                )
            )
        }

        return plan.id!!
    }

    @Transactional
    fun replaceWorkout(userId: UUID, planId: UUID, request: SaveWorkoutRequest): UUID {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }

        val plan = workoutPlanRepository.findById(planId)
            .orElseThrow { NotFoundException("Workout plan not found") }

        if (plan.user.id != userId) {
            throw BadRequestException("You do not own this workout plan")
        }

        val exerciseIds = request.exercises.map { it.exerciseId }.toSet()
        val exerciseMap = exerciseRepository.findAllById(exerciseIds).associateBy { it.id!! }
        val missing = exerciseIds - exerciseMap.keys
        if (missing.isNotEmpty()) {
            throw BadRequestException("Unknown exercise IDs: $missing")
        }

        // Update plan metadata
        plan.name = request.name.take(100)
        plan.description = request.description?.take(500)
        workoutPlanRepository.save(plan)

        // Remove existing days via entity-level delete so JPA lifecycle callbacks
        // fire and CascadeType.ALL/orphanRemoval on Day.exercises deletes exercises too.
        val existingDays = workoutPlanDayRepository.findAllByWorkoutPlan(plan)
        workoutPlanDayRepository.deleteAll(existingDays)

        // Create fresh day + exercises
        val day = workoutPlanDayRepository.save(
            WorkoutPlanDayEntity(
                workoutPlan = plan,
                dayOfWeek = DayOfWeek.MONDAY,
                name = "Тренировка",
                orderIndex = 0
            )
        )

        request.exercises.forEachIndexed { index, ex ->
            val normalizedSetWeights = normalizeAndValidateSetWeights(ex)
            workoutPlanExerciseRepository.save(
                WorkoutPlanExerciseEntity(
                    workoutPlanDay = day,
                    exercise = exerciseMap[ex.exerciseId]!!,
                    sets = ex.sets,
                    reps = ex.reps,
                    weightKg = normalizedSetWeights?.firstOrNull { it != null },
                    setWeightsKg = encodeSetWeights(normalizedSetWeights),
                    restSeconds = request.restSeconds,
                    orderIndex = index,
                    notes = ex.notes
                )
            )
        }

        return plan.id!!
    }

    /**
     * Normalizes the per-set weights submitted on an AI-sourced exercise and validates the basic
     * invariants we persist (size matches `sets`, no negative or non-finite values). Returns `null`
     * when the client did not send weights — the entity column stays unpopulated in that case.
     *
     * Range/step constraints (multiples of 2.5 kg, 0–500 kg) are guidance for the AI model rather
     * than hard server-side rules: we don't want to reject a user-edited plan just because a
     * trainer entered 27 kg or 600 kg deliberately. We only block clearly broken payloads.
     */
    private fun normalizeAndValidateSetWeights(ex: AiWorkoutExerciseDto): List<Double?>? {
        val weights = ex.setWeightsKg?.takeIf { it.isNotEmpty() } ?: return null
        if (weights.size != ex.sets) {
            throw BadRequestException(
                "setWeightsKg size (${weights.size}) must equal sets (${ex.sets}) for exercise ${ex.exerciseId}"
            )
        }
        weights.forEach { value ->
            if (value != null && (value < 0.0 || !value.isFinite())) {
                throw BadRequestException(
                    "setWeightsKg contains an invalid value ($value) for exercise ${ex.exerciseId}"
                )
            }
        }
        return weights
    }

    private fun encodeSetWeights(weights: List<Double?>?): String? {
        if (weights.isNullOrEmpty()) return null
        return objectMapper.writeValueAsString(weights)
    }

    /**
     * Aligns GigaChat's `setWeightsKg` array with the declared `sets` count and the catalog's
     * `requiresWeight` flag for one exercise. The model is occasionally inconsistent:
     *  - it may emit a setWeightsKg of size != sets (off-by-one, mismatched pyramid)
     *  - it may emit weights for an exercise whose catalog entry doesn't require weight
     *  - it may emit a list of all nulls
     *
     * Strategy:
     *  - requiresWeight=false → drop the weights regardless of what the model produced.
     *  - non-finite/negative entries collapse to null so we never persist garbage.
     *  - list shorter than sets → pad with nulls (let the user fill in remaining sets).
     *  - list longer than sets → trim to sets.
     *  - all-null list → drop entirely (no useful guidance from the model).
     *
     * This is silent reconciliation by design: weight is an enhancement, not a contract,
     * so we don't reject the exercise on shape mismatches.
     */
    private fun reconcileAiExerciseSetWeights(
        ex: AiWorkoutExerciseParsedDto,
        requiresWeight: Boolean
    ): AiWorkoutExerciseParsedDto {
        val raw = ex.setWeightsKg
        if (!requiresWeight) {
            return if (raw == null) ex else ex.copy(setWeightsKg = null)
        }
        if (raw.isNullOrEmpty()) return ex

        val sanitized = raw.map { w ->
            when {
                w == null -> null
                !w.isFinite() || w < 0.0 -> null
                else -> w
            }
        }
        val aligned: List<Double?> = when {
            sanitized.size == ex.sets -> sanitized
            sanitized.size > ex.sets -> sanitized.take(ex.sets)
            else -> sanitized + List(ex.sets - sanitized.size) { null }
        }
        val cleaned = if (aligned.all { it == null }) null else aligned
        return if (cleaned == raw) ex else ex.copy(setWeightsKg = cleaned)
    }

    /**
     * Formats a kilogram value for human-facing prose. Drops the trailing `.0` for whole numbers
     * (so "60" instead of "60.0") but keeps decimals when the user picked a non-integer weight.
     */
    private fun formatWeightForMessage(value: Double): String {
        return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }

    fun clearSession(userId: UUID) {
        sessionStore.clearSession(userId)
    }

    // GigaChat sometimes emits raw control characters (e.g. literal '\n', '\r', '\t')
    // inside JSON string values, which is invalid JSON and breaks Jackson with
    // StreamReadException: Illegal unquoted character ((CTRL-CHAR, code 10)).
    //
    // This helper walks the raw JSON char-by-char, tracks whether the cursor is
    // currently inside a JSON string literal (between unescaped double quotes),
    // and replaces any unescaped control character (code < 32) found inside a
    // string with its proper JSON escape sequence. Characters outside string
    // literals are left untouched (real structural whitespace must remain).
    //
    // Correctness notes:
    //  - An escape backslash inside a string defers handling of the next char,
    //    so sequences like \" or \n are passed through verbatim and do not
    //    toggle the inString state.
    //  - Only chars with code < 32 are rewritten; printable chars (incl. non-ASCII)
    //    are emitted as-is to preserve Unicode (e.g. Cyrillic) content.
    private fun escapeControlCharsInStrings(json: String): String {
        val out = StringBuilder(json.length + 16)
        var inString = false
        var escaped = false
        for (ch in json) {
            if (inString) {
                if (escaped) {
                    // Previous char was an unescaped backslash inside a string.
                    // Pass this char through as part of the escape sequence.
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

    // GigaChat omits the closing } of the last array element, e.g.:
    //   "notes":null],}}}   or   "notes":null]} }}
    // Fix: count all } after the ], put one before ], keep the rest after.
    private fun repairGigaChatJson(json: String): String =
        json.replace(Regex("""(null|true|false|-?\d+(?:\.\d+)?|"[^"]*")\s*\]([\s,}]+)$""")) { m ->
            val value = m.groupValues[1]
            val braces = m.groupValues[2].filter { it == '}' }
            if (braces.length >= 1) "$value}]${braces.drop(1)}" else "$value}]"
        }

    /**
     * Produces the user-facing message for a generated workout via a standalone
     * GigaChat call.
     *
     * This call is intentionally NOT added to the conversation session history:
     * it is an internal formatting step, not part of the user's dialog. Keeping
     * it out of history avoids polluting future turns with rendered prose and
     * prevents the model from confusing this output with the structured
     * workout/clarification contract.
     *
     * If the call fails for any reason we fall back to a deterministic
     * template-built message so the user always receives a coherent response.
     */
    private fun generateFriendlyMessage(workoutName: String, exerciseDescriptions: String): String {
        val prompt = """
Ты дружелюбный фитнес-тренер. Напиши короткое мотивирующее сообщение для пользователя о тренировке которую ты составил.

Название тренировки: $workoutName

Упражнения (перечисли все в том же порядке, точно с теми же параметрами):
$exerciseDescriptions

Требования к сообщению:
- Начни с приветствия (например "Привет!")
- Упомяни название тренировки
- Перечисли ВСЕ упражнения с их параметрами точно как указано выше — не меняй порядок, не добавляй лишних, не убирай
- Заверши мотивирующей фразой
- Только текст, без JSON, без markdown, без заголовков
        """.trimIndent()

        return try {
            gigaChatClient.chat(listOf(GigaChatMessage("user", prompt)))
        } catch (e: Exception) {
            log.warn("Failed to generate friendly message, using fallback", e)
            "Привет! Я составил для тебя тренировку «$workoutName»:\n$exerciseDescriptions\nУдачной тренировки!"
        }
    }

    private fun parseAiResponse(json: String): AiChatResponse {
        return try {
            val node = objectMapper.readTree(json)
            val type = node.get("type")?.asText()?.lowercase()

            when (type) {
                "clarification" -> {
                    val message = node.get("message")?.asText()
                        ?: throw BadRequestException("AI response missing 'message' field")
                    AiChatResponse(AiResponseType.CLARIFICATION, message)
                }
                "workout" -> {
                    val workoutNode = node.get("workout")
                        ?: throw BadRequestException("AI response missing 'workout' field")
                    val workout = objectMapper.treeToValue(workoutNode, AiWorkoutDto::class.java)
                    // Workout responses no longer carry a 'message' field — it is
                    // produced by a separate GigaChat call after exercise
                    // validation. Leave it empty here; the caller fills it in.
                    AiChatResponse(AiResponseType.WORKOUT, "", workout)
                }
                else -> throw BadRequestException("Unexpected AI response type: $type")
            }
        } catch (e: BadRequestException) {
            throw e
        } catch (e: Exception) {
            log.error("Failed to parse AI response: {}", json, e)
            throw BadRequestException("AI returned an invalid response, please try again")
        }
    }

    private fun buildContextMessage(
        weightKg: Double?,
        heightCm: Double?,
        age: Int?,
        gender: String?,
        healthIssues: String?,
        experience: String?,
        frequency: String?,
        userMessage: String,
        exercises: List<Map<String, Any?>>
    ): String {
        val exercisesJson = objectMapper.writeValueAsString(exercises)
        return buildString {
            appendLine("=== Данные пользователя ===")
            append("Вес: ${weightKg?.let { "${it} кг" } ?: "не указан"}")
            append(" | Рост: ${heightCm?.let { "${it} см" } ?: "не указан"}")
            append(" | Возраст: ${age?.let { "${it} лет" } ?: "не указан"}")
            appendLine(" | Пол: ${gender ?: "не указан"}")
            appendLine("Опыт тренировок: ${experience ?: "не указан"} | Частота: ${frequency ?: "не указана"}")
            appendLine("Ограничения по здоровью: ${healthIssues ?: "нет"}")
            appendLine()
            appendLine("=== Запрос ===")
            appendLine(userMessage)
            appendLine()
            appendLine("=== Доступные упражнения (только из этого списка) ===")
            append(exercisesJson)
        }
    }

    companion object {
        private val SYSTEM_PROMPT = """
Ты персональный AI-тренер в фитнес-приложении GymGenie.

ПРАВИЛА:
- Отвечай ТОЛЬКО валидным JSON без markdown блоков, без ```json, только чистый JSON
- Составляй тренировки ТОЛЬКО из упражнений в предоставленном каталоге — используй их точные exerciseId
- Учитывай физические параметры и ограничения по здоровью пользователя
- Задавай уточняющий вопрос только если информации реально недостаточно, иначе сразу составляй тренировку
- Отвечай на русском языке
- Если пользователь просит изменить или отредактировать тренировку — верни type:"workout" с ПОЛНОЙ обновлённой тренировкой (все упражнения, включая неизменённые)

ПРАВИЛА ДЛЯ ВЕСОВ (setWeightsKg):
- Для упражнений с requiresWeight=true ОБЯЗАТЕЛЬНО укажи setWeightsKg: массив весов в кг по числу подходов (длина массива = sets)
  Пример: "setWeightsKg": [60.0, 70.0, 80.0] для трёх подходов
  Веса подбирай реалистично, исходя из веса тела пользователя и опыта. Поле defaultWeightPct в каталоге — доля от веса тела (0.6 = 60%), используй как отправную точку:
    - BEGINNER: 30–50% от веса тела для базовых упражнений
    - INTERMEDIATE: 50–80% от веса тела
    - ADVANCED: 80–120% от веса тела
  Все значения кратны 2.5 кг, в диапазоне 0–500 кг
  Варианты схемы: одинаковый вес во всех подходах, пирамида вверх (разминка → рабочие), пирамида вниз (drop set)
- Для упражнений с requiresWeight=false поле setWeightsKg НЕ ВКЛЮЧАЙ в JSON вообще (не null, не пустой массив — просто отсутствие поля)

КРИТИЧЕСКИЕ ПРАВИЛА ДЛЯ ID УПРАЖНЕНИЙ:
1. Каждый exerciseId в твоём ответе ДОЛЖЕН быть скопирован дословно (символ в символ, без изменений) из поля exerciseId соответствующего элемента каталога. Это обычная операция copy-paste, а не генерация нового значения.
2. ЗАПРЕЩЕНО использовать любые идентификаторы упражнений, которых нет в каталоге. Не выдумывай UUID, не модифицируй существующие, не комбинируй части разных id.
3. Если нужное тебе упражнение отсутствует в каталоге — НЕ изобретай для него id. Вместо этого выбери ближайшее подходящее упражнение, которое РЕАЛЬНО присутствует в каталоге (по той же мышечной группе/категории), и используй его exerciseId.
4. Перед тем как отправить ответ, мысленно сверь КАЖДЫЙ exerciseId в поле workout.exercises с полем exerciseId в каталоге. Если хотя бы один id не находит точного совпадения в каталоге — замени упражнение на ближайшее существующее.
5. Поле exerciseId в твоём ответе и поле exerciseId в каталоге — это одно и то же значение в обе стороны: то, что ты возвращаешь, должно буквально присутствовать в исходном списке.
6. Нарушение этих правил делает тренировку непригодной для использования: пользователь не сможет её сохранить, и весь ответ будет отброшен.

ФОРМАТ ОТВЕТА — строго один из двух вариантов:

Вариант 1 (уточнение, когда нужна доп. информация):
{"type":"clarification","message":"вопрос пользователю"}

Вариант 2 (готовая тренировка). restSeconds — время отдыха между подходами в секундах для всей тренировки (одно значение на уровне workout, не per-exercise). Подбирай от 60 до 180 секунд в зависимости от типа тренировки и уровня подготовки. Пример с двумя упражнениями: первое с весом (requiresWeight=true), второе без (requiresWeight=false):
{"type":"workout","workout":{"name":"название","description":"краткое описание","estimatedDurationMinutes":60,"restSeconds":90,"exercises":[{"exerciseId":"<id из каталога с requiresWeight=true>","sets":3,"reps":10,"notes":null,"setWeightsKg":[60.0,70.0,80.0]},{"exerciseId":"<id из каталога с requiresWeight=false>","sets":3,"reps":15,"notes":null}]}}
        """.trimIndent()
    }
}
