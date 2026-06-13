package com.asc.gymgenie.exercise.service

import com.asc.gymgenie.common.exception.NotFoundException
import com.asc.gymgenie.exercise.dto.CreateExerciseRequest
import com.asc.gymgenie.exercise.entity.DifficultyLevel
import com.asc.gymgenie.exercise.entity.ExerciseCategory
import com.asc.gymgenie.exercise.entity.ExerciseEntity
import com.asc.gymgenie.exercise.entity.MuscleGroup
import com.asc.gymgenie.exercise.repository.ExerciseRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

open class ExerciseServiceTest {

    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var exerciseService: ExerciseService

    @BeforeEach
    fun setUp() {
        exerciseRepository = mockk()
        exerciseService = ExerciseService(exerciseRepository)
    }

    // ---- helpers ----

    private fun createExercise(
        id: UUID = UUID.randomUUID(),
        nameRu: String = "Жим лёжа",
        nameEn: String = "Bench Press",
        muscleGroup: MuscleGroup = MuscleGroup.CHEST,
        category: ExerciseCategory = ExerciseCategory.STRENGTH,
        difficultyLevel: DifficultyLevel = DifficultyLevel.INTERMEDIATE,
        description: String? = "Базовое упражнение на грудные мышцы",
        secondsPer10Reps: Int? = 45,
        caloriesBurned: Int? = 80,
        rating: Double? = 4.5,
        imageUrl: String? = "https://example.com/bench.jpg",
        videoUrl: String? = null,
        instructions: List<String> = listOf("Лягте на скамью", "Возьмите штангу"),
        equipment: List<String> = listOf("Штанга", "Скамья"),
        techniqueTip: String? = "Держите лопатки сведёнными",
        defaultRepsMin: Int? = 8,
        defaultRepsMax: Int? = 12,
        defaultWeightPercentage: Double? = null,
        requiresWeight: Boolean = true
    ): ExerciseEntity {
        val entity = ExerciseEntity(
            nameRu = nameRu,
            nameEn = nameEn,
            muscleGroup = muscleGroup,
            category = category,
            difficultyLevel = difficultyLevel,
            description = description,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
            secondsPer10Reps = secondsPer10Reps,
            caloriesBurned = caloriesBurned,
            rating = rating,
            imageUrl = imageUrl,
            videoUrl = videoUrl,
            instructions = instructions,
            equipment = equipment,
            techniqueTip = techniqueTip,
            defaultRepsMin = defaultRepsMin,
            defaultRepsMax = defaultRepsMax,
            defaultWeightPercentage = defaultWeightPercentage,
            requiresWeight = requiresWeight
        )
        entity.id = id
        return entity
    }

    // ---- getById ----

    @Test
    fun getById_success() {
        val exerciseId = UUID.randomUUID()
        val exercise = createExercise(id = exerciseId)

        every { exerciseRepository.findById(exerciseId) } returns Optional.of(exercise)

        val response = exerciseService.getById(exerciseId)

        assertEquals(exerciseId, response.id)
        assertEquals("Жим лёжа", response.nameRu)
        assertEquals("Bench Press", response.nameEn)
        assertEquals(MuscleGroup.CHEST, response.muscleGroup)
        assertEquals(ExerciseCategory.STRENGTH, response.category)
        assertEquals(DifficultyLevel.INTERMEDIATE, response.difficultyLevel)
        assertEquals("Базовое упражнение на грудные мышцы", response.description)
        assertEquals(listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS), response.secondaryMuscleGroups)
        assertEquals(45, response.secondsPer10Reps)
        assertEquals(80, response.caloriesBurned)
        assertEquals(4.5, response.rating)
        assertEquals("https://example.com/bench.jpg", response.imageUrl)
        assertEquals(listOf("Лягте на скамью", "Возьмите штангу"), response.instructions)
        assertEquals(listOf("Штанга", "Скамья"), response.equipment)
        assertEquals("Держите лопатки сведёнными", response.techniqueTip)
        assertEquals(8, response.defaultRepsMin)
        assertEquals(12, response.defaultRepsMax)
        assertEquals(true, response.requiresWeight)
    }

    @Test
    fun getById_notFound_throwsNotFoundException() {
        val exerciseId = UUID.randomUUID()

        every { exerciseRepository.findById(exerciseId) } returns Optional.empty()

        val exception = assertFailsWith<NotFoundException> {
            exerciseService.getById(exerciseId)
        }

        assertEquals("Exercise not found", exception.message)
    }

    // ---- getMuscleGroups ----

    @Test
    fun getMuscleGroups_returns13Groups() {
        val groups = exerciseService.getMuscleGroups()

        assertEquals(13, groups.size)
        assertEquals(MuscleGroup.entries.size, groups.size)

        val keys = groups.map { it.key }
        MuscleGroup.entries.forEach { mg ->
            assert(mg.name in keys) { "Missing muscle group: ${mg.name}" }
        }

        // Verify Russian/English names are populated
        val chestGroup = groups.first { it.key == "CHEST" }
        assertEquals("Грудь", chestGroup.nameRu)
        assertEquals("Chest", chestGroup.nameEn)

        val absGroup = groups.first { it.key == "ABS" }
        assertEquals("Пресс", absGroup.nameRu)
        assertEquals("Abs", absGroup.nameEn)

        val cardioGroup = groups.first { it.key == "CARDIO" }
        assertEquals("Кардио", cardioGroup.nameRu)
        assertEquals("Cardio", cardioGroup.nameEn)
    }

    // ---- create ----

    @Test
    fun create_success() {
        val request = CreateExerciseRequest(
            nameRu = "Приседания",
            nameEn = "Squats",
            description = "Базовое упражнение на ноги",
            muscleGroup = MuscleGroup.QUADRICEPS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            category = ExerciseCategory.STRENGTH,
            difficultyLevel = DifficultyLevel.BEGINNER,
            secondsPer10Reps = 50,
            caloriesBurned = 100,
            rating = 4.8,
            imageUrl = "https://example.com/squats.jpg",
            instructions = listOf("Встаньте прямо", "Присядьте"),
            equipment = listOf("Штанга"),
            techniqueTip = "Колени не выходят за носки",
            defaultRepsMin = 10,
            defaultRepsMax = 15,
            requiresWeight = true
        )

        val exerciseId = UUID.randomUUID()
        val exerciseSlot = slot<ExerciseEntity>()
        every { exerciseRepository.save(capture(exerciseSlot)) } answers {
            exerciseSlot.captured.also { it.id = exerciseId }
        }

        val response = exerciseService.create(request)

        assertEquals(exerciseId, response.id)
        assertEquals("Приседания", response.nameRu)
        assertEquals("Squats", response.nameEn)
        assertEquals(MuscleGroup.QUADRICEPS, response.muscleGroup)
        assertEquals(ExerciseCategory.STRENGTH, response.category)
        assertEquals(DifficultyLevel.BEGINNER, response.difficultyLevel)
        assertEquals(100, response.caloriesBurned)
        assertEquals(true, response.requiresWeight)

        verify { exerciseRepository.save(any()) }
    }

    // ---- delete ----

    @Test
    fun delete_success() {
        val exerciseId = UUID.randomUUID()

        every { exerciseRepository.existsById(exerciseId) } returns true
        every { exerciseRepository.deleteById(exerciseId) } returns Unit

        exerciseService.delete(exerciseId)

        verify { exerciseRepository.existsById(exerciseId) }
        verify { exerciseRepository.deleteById(exerciseId) }
    }

    @Test
    fun delete_notFound_throwsNotFoundException() {
        val exerciseId = UUID.randomUUID()

        every { exerciseRepository.existsById(exerciseId) } returns false

        val exception = assertFailsWith<NotFoundException> {
            exerciseService.delete(exerciseId)
        }

        assertEquals("Exercise not found", exception.message)
    }
}
