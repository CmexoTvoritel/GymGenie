package com.asc.gymgenie.presentation

import com.asc.gymgenie.exercise.ExerciseApi
import com.asc.gymgenie.workout.WorkoutApi
import com.asc.gymgenie.workout.WorkoutScheduleType
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreateWorkoutViewModelTest {

    private lateinit var vm: CreateWorkoutViewModel

    private val dummyClient = HttpClient(MockEngine) {
        engine {
            addHandler { respond("", HttpStatusCode.OK) }
        }
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = CreateWorkoutViewModel(
            exerciseApi = ExerciseApi(dummyClient),
            workoutApi = WorkoutApi(dummyClient),
        )
    }

    @AfterTest
    fun tearDown() {
        vm.onCleared()
        Dispatchers.resetMain()
    }

    // --- rest seconds ---

    @Test
    fun setRestSeconds_clampsToMinimum() {
        vm.setRestSeconds(5)
        assertEquals(CreateWorkoutLimits.MIN_REST_SECONDS, vm.state.value.restSeconds)
    }

    @Test
    fun setRestSeconds_clampsToMaximum() {
        vm.setRestSeconds(700)
        assertEquals(CreateWorkoutLimits.MAX_REST_SECONDS, vm.state.value.restSeconds)
    }

    @Test
    fun setRestSeconds_acceptsValueInRange() {
        vm.setRestSeconds(120)
        assertEquals(120, vm.state.value.restSeconds)
    }

    @Test
    fun incrementRestSeconds_addsStep() {
        vm.setRestSeconds(60)
        vm.incrementRestSeconds()
        assertEquals(65, vm.state.value.restSeconds)
    }

    @Test
    fun decrementRestSeconds_subtractsStep() {
        vm.setRestSeconds(60)
        vm.decrementRestSeconds()
        assertEquals(55, vm.state.value.restSeconds)
    }

    @Test
    fun decrementRestSeconds_doesNotGoBelowMin() {
        vm.setRestSeconds(CreateWorkoutLimits.MIN_REST_SECONDS)
        vm.decrementRestSeconds()
        assertEquals(CreateWorkoutLimits.MIN_REST_SECONDS, vm.state.value.restSeconds)
    }

    // --- schedule ---

    @Test
    fun setScheduleType_clearsDaysOnChange() {
        vm.setScheduleType(WorkoutScheduleType.RECURRING)
        vm.toggleScheduleDay("MONDAY")
        vm.toggleScheduleDay("FRIDAY")
        assertEquals(2, vm.state.value.scheduleDays.size)

        vm.setScheduleType(WorkoutScheduleType.ONE_TIME)
        assertTrue(vm.state.value.scheduleDays.isEmpty())
    }

    @Test
    fun setScheduleType_sameType_doesNotClearDays() {
        vm.setScheduleType(WorkoutScheduleType.RECURRING)
        vm.toggleScheduleDay("MONDAY")
        vm.setScheduleType(WorkoutScheduleType.RECURRING)
        assertEquals(setOf("MONDAY"), vm.state.value.scheduleDays)
    }

    @Test
    fun toggleScheduleDay_addsAndRemoves() {
        vm.toggleScheduleDay("MONDAY")
        assertTrue("MONDAY" in vm.state.value.scheduleDays)

        vm.toggleScheduleDay("MONDAY")
        assertFalse("MONDAY" in vm.state.value.scheduleDays)
    }

    // --- exercises / normalization ---

    private fun exercise(
        sets: Int = 3,
        reps: Int = 12,
        requiresWeight: Boolean = false,
        setWeightsKg: List<Double?>? = null,
    ) = PendingExercise(
        exerciseId = "ex-1",
        exerciseNameRu = "Жим лёжа",
        exerciseNameEn = "Bench Press",
        muscleGroupKey = "CHEST",
        sets = sets,
        reps = reps,
        requiresWeight = requiresWeight,
        setWeightsKg = setWeightsKg,
    )

    @Test
    fun addExercise_clampsSetsAndReps() {
        vm.addExercise(exercise(sets = 15, reps = 30))
        val added = vm.state.value.exercises.first()
        assertEquals(CreateWorkoutLimits.MAX_SETS, added.sets)
        assertEquals(CreateWorkoutLimits.MAX_REPS, added.reps)
    }

    @Test
    fun addExercise_clampsLowSetsAndReps() {
        vm.addExercise(exercise(sets = 0, reps = 1))
        val added = vm.state.value.exercises.first()
        assertEquals(CreateWorkoutLimits.MIN_SETS, added.sets)
        assertEquals(CreateWorkoutLimits.MIN_REPS, added.reps)
    }

    @Test
    fun addExercise_normalizesWeights_noWeight() {
        vm.addExercise(exercise(requiresWeight = false, setWeightsKg = listOf(50.0, 60.0)))
        val added = vm.state.value.exercises.first()
        assertNull(added.setWeightsKg)
    }

    @Test
    fun addExercise_normalizesWeights_noExistingWeights() {
        vm.addExercise(exercise(sets = 3, requiresWeight = true, setWeightsKg = null))
        val added = vm.state.value.exercises.first()
        val weights = assertNotNull(added.setWeightsKg)
        assertEquals(3, weights.size)
        assertTrue(weights.all { it == CreateWorkoutLimits.DEFAULT_WEIGHT_KG })
    }

    @Test
    fun addExercise_normalizesWeights_padsList() {
        vm.addExercise(
            exercise(
                sets = 4,
                requiresWeight = true,
                setWeightsKg = listOf(30.0, 40.0),
            ),
        )
        val added = vm.state.value.exercises.first()
        val weights = assertNotNull(added.setWeightsKg)
        assertEquals(4, weights.size)
        assertEquals(30.0, weights[0])
        assertEquals(40.0, weights[1])
        assertEquals(40.0, weights[2])
        assertEquals(40.0, weights[3])
    }

    @Test
    fun addExercise_normalizesWeights_truncatesList() {
        vm.addExercise(
            exercise(
                sets = 3,
                requiresWeight = true,
                setWeightsKg = listOf(10.0, 20.0, 30.0, 40.0, 50.0),
            ),
        )
        val added = vm.state.value.exercises.first()
        val weights = assertNotNull(added.setWeightsKg)
        assertEquals(3, weights.size)
        assertEquals(listOf(10.0, 20.0, 30.0), weights)
    }

    @Test
    fun addExercise_normalizesWeights_exactMatch() {
        vm.addExercise(
            exercise(
                sets = 3,
                requiresWeight = true,
                setWeightsKg = listOf(50.0, 60.0, 70.0),
            ),
        )
        val added = vm.state.value.exercises.first()
        assertEquals(listOf(50.0, 60.0, 70.0), added.setWeightsKg)
    }

    @Test
    fun addExercise_clampsWeightValues() {
        vm.addExercise(
            exercise(
                sets = 2,
                requiresWeight = true,
                setWeightsKg = listOf(600.0, -5.0),
            ),
        )
        val added = vm.state.value.exercises.first()
        val weights = assertNotNull(added.setWeightsKg)
        assertEquals(CreateWorkoutLimits.MAX_WEIGHT_KG, weights[0])
        assertEquals(CreateWorkoutLimits.MIN_WEIGHT_KG, weights[1])
    }

    @Test
    fun addExercise_preservesNullWeightInList() {
        vm.addExercise(
            exercise(
                sets = 2,
                requiresWeight = true,
                setWeightsKg = listOf(null, 50.0),
            ),
        )
        val added = vm.state.value.exercises.first()
        val weights = assertNotNull(added.setWeightsKg)
        assertNull(weights[0])
        assertEquals(50.0, weights[1])
    }

    // --- updateExerciseAt ---

    @Test
    fun updateExerciseAt_validIndex_normalizesAndReplaces() {
        vm.addExercise(exercise(sets = 3, reps = 10))
        vm.updateExerciseAt(0, exercise(sets = 20, reps = 1))
        val updated = vm.state.value.exercises.first()
        assertEquals(CreateWorkoutLimits.MAX_SETS, updated.sets)
        assertEquals(CreateWorkoutLimits.MIN_REPS, updated.reps)
    }

    @Test
    fun updateExerciseAt_invalidIndex_noChange() {
        vm.addExercise(exercise(sets = 3, reps = 10))
        val before = vm.state.value.exercises
        vm.updateExerciseAt(5, exercise(sets = 1, reps = 5))
        assertEquals(before, vm.state.value.exercises)
    }

    // --- removeExerciseAt ---

    @Test
    fun removeExerciseAt_validIndex_removes() {
        vm.addExercise(exercise())
        vm.addExercise(exercise().copy(exerciseId = "ex-2"))
        assertEquals(2, vm.state.value.exercises.size)

        vm.removeExerciseAt(0)
        assertEquals(1, vm.state.value.exercises.size)
        assertEquals("ex-2", vm.state.value.exercises.first().exerciseId)
    }

    @Test
    fun removeExerciseAt_invalidIndex_noChange() {
        vm.addExercise(exercise())
        vm.removeExerciseAt(5)
        assertEquals(1, vm.state.value.exercises.size)
    }

    @Test
    fun removeExerciseAt_negativeIndex_noChange() {
        vm.addExercise(exercise())
        vm.removeExerciseAt(-1)
        assertEquals(1, vm.state.value.exercises.size)
    }

    // --- saveWorkout validation ---

    @Test
    fun saveWorkout_emptyName_setsError() {
        vm.setWorkoutName("   ")
        vm.addExercise(exercise())
        vm.saveWorkout()
        assertEquals("Введите название тренировки", vm.state.value.errorMessage)
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun saveWorkout_noExercises_setsError() {
        vm.setWorkoutName("My Workout")
        vm.saveWorkout()
        assertEquals("Добавьте хотя бы одно упражнение", vm.state.value.errorMessage)
    }

    @Test
    fun saveWorkout_recurringWithoutDays_setsError() {
        vm.setWorkoutName("My Workout")
        vm.addExercise(exercise())
        vm.setScheduleType(WorkoutScheduleType.RECURRING)
        vm.saveWorkout()
        assertEquals("Выберите хотя бы один день недели", vm.state.value.errorMessage)
    }

    // --- setWorkoutName / setDescription / clearError / reset ---

    @Test
    fun setWorkoutName_updatesState() {
        vm.setWorkoutName("Leg Day")
        assertEquals("Leg Day", vm.state.value.workoutName)
    }

    @Test
    fun setDescription_updatesState() {
        vm.setDescription("Focus on quads")
        assertEquals("Focus on quads", vm.state.value.description)
    }

    @Test
    fun clearError_clearsErrorMessage() {
        vm.saveWorkout() // triggers error because name is empty
        assertNotNull(vm.state.value.errorMessage)
        vm.clearError()
        assertNull(vm.state.value.errorMessage)
    }

    @Test
    fun reset_clearsState() {
        vm.setWorkoutName("Workout")
        vm.setDescription("Desc")
        vm.setRestSeconds(120)
        vm.addExercise(exercise())
        vm.setScheduleType(WorkoutScheduleType.RECURRING)
        vm.toggleScheduleDay("MONDAY")

        vm.reset()

        val state = vm.state.value
        assertEquals("", state.workoutName)
        assertEquals("", state.description)
        assertEquals(CreateWorkoutLimits.DEFAULT_REST_SECONDS, state.restSeconds)
        assertTrue(state.exercises.isEmpty())
        assertEquals(WorkoutScheduleType.ONE_TIME, state.scheduleType)
        assertTrue(state.scheduleDays.isEmpty())
        assertNull(state.errorMessage)
        assertFalse(state.isSaving)
        assertFalse(state.isSaved)
    }
}
