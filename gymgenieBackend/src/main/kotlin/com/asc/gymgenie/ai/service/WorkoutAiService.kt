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

    fun chat(userId: UUID, request: AiChatRequest): AiChatResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }

        if (sessionStore.isEmpty(userId)) {
            // First message: load exercise catalog + user profile into context, then atomically initialize session
            val exercises = exerciseRepository.findAll().map { ex ->
                mapOf(
                    "id" to ex.id.toString(),
                    "n" to ex.nameRu,
                    "m" to ex.muscleGroup.name,
                    "c" to ex.category.name,
                    "d" to ex.difficultyLevel.name
                )
            }
            val age = user.birthDate?.let { Period.between(it, LocalDate.now()).years }
            val contextMessage = buildContextMessage(
                weightKg = user.weightKg,
                heightCm = user.heightCm,
                age = age,
                gender = user.gender?.name,
                healthIssues = request.healthIssues,
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

        val cleanedJson = rawResponse.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return parseAiResponse(cleanedJson)
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
            workoutPlanExerciseRepository.save(
                WorkoutPlanExerciseEntity(
                    workoutPlanDay = day,
                    exercise = exerciseMap[ex.exerciseId]!!,
                    sets = ex.sets,
                    reps = ex.reps,
                    restSeconds = ex.restSeconds,
                    orderIndex = index,
                    notes = ex.notes
                )
            )
        }

        return plan.id!!
    }

    fun clearSession(userId: UUID) {
        sessionStore.clearSession(userId)
    }

    private fun parseAiResponse(json: String): AiChatResponse {
        return try {
            val node = objectMapper.readTree(json)
            val type = node.get("type")?.asText()
            val message = node.get("message")?.asText()
                ?: throw BadRequestException("AI response missing 'message' field")

            when (type) {
                "clarification" -> AiChatResponse(AiResponseType.CLARIFICATION, message)
                "workout" -> {
                    val workoutNode = node.get("workout")
                        ?: throw BadRequestException("AI response missing 'workout' field")
                    val workout = objectMapper.treeToValue(workoutNode, AiWorkoutDto::class.java)
                    AiChatResponse(AiResponseType.WORKOUT, message, workout)
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
        userMessage: String,
        exercises: List<Map<String, String>>
    ): String {
        val exercisesJson = objectMapper.writeValueAsString(exercises)
        return buildString {
            appendLine("=== Данные пользователя ===")
            append("Вес: ${weightKg?.let { "${it} кг" } ?: "не указан"}")
            append(" | Рост: ${heightCm?.let { "${it} см" } ?: "не указан"}")
            append(" | Возраст: ${age?.let { "${it} лет" } ?: "не указан"}")
            appendLine(" | Пол: ${gender ?: "не указан"}")
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
- Составляй тренировки ТОЛЬКО из упражнений в предоставленном каталоге — используй их точные id
- Учитывай физические параметры и ограничения по здоровью пользователя
- Задавай уточняющий вопрос только если информации реально недостаточно, иначе сразу составляй тренировку
- Отвечай на русском языке

ФОРМАТ ОТВЕТА — строго один из двух вариантов:

Вариант 1 (уточнение, когда нужна доп. информация):
{"type":"clarification","message":"вопрос пользователю"}

Вариант 2 (готовая тренировка):
{"type":"workout","message":"дружелюбное описание тренировки для пользователя","workout":{"name":"название","description":"краткое описание","estimatedDurationMinutes":60,"exercises":[{"exerciseId":"uuid из каталога","sets":3,"reps":12,"restSeconds":60,"notes":null}]}}
        """.trimIndent()
    }
}
