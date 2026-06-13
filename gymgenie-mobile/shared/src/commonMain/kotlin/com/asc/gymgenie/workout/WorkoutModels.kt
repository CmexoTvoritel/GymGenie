package com.asc.gymgenie.workout

import kotlinx.serialization.Serializable

@Serializable
enum class WorkoutScheduleType {
    ONE_TIME, RECURRING
}

@Serializable
data class WorkoutPlanShortResponse(
    val id: String,
    val name: String,
    val description: String? = null,
    val isActive: Boolean,
    val daysCount: Int = 0,
    val createdBy: String = "USER",

    val scheduleType: String = WorkoutScheduleType.ONE_TIME.name,

    val scheduleDays: List<String> = emptyList(),
    val restSeconds: Int = 60,

    val primaryMuscleGroup: String? = null,
    val exercisesCount: Int = 0,
    val totalSets: Int = 0,
    val estimatedMinutes: Int = 0,
)

@Serializable
data class UpdateWorkoutPlanRequest(
    val name: String? = null,
    val description: String? = null,
    val isActive: Boolean? = null,
    val scheduleType: WorkoutScheduleType? = null,
    val scheduleDays: List<String>? = null,
    val restSeconds: Int? = null,
    val exercises: List<SimpleWorkoutExerciseItem>? = null,
)

data class ActiveWorkoutSession(
    val planId: String,
    val planName: String,
    val exercises: List<ActiveExercise>,
    val restSeconds: Int = 60,
)

data class ActiveExercise(
    val exerciseId: String,
    val exerciseName: String,
    val muscleGroupLabel: String,
    val imageUrl: String?,
    val techniqueTip: String?,
    val sets: Int,
    val reps: Int,
    val weightKg: Double?,
    val setWeightsKg: List<Double?>? = null,
)

data class CompletedSet(
    val exerciseIndex: Int,
    val setIndex: Int,
    val repsActual: Int,
    val weightActual: Double,
    val completedAt: Long,
)

@Serializable
data class CreateSimpleWorkoutRequest(
    val name: String,
    val description: String? = null,
    val restSeconds: Int,
    val scheduleType: WorkoutScheduleType = WorkoutScheduleType.ONE_TIME,
    val scheduleDays: List<String> = emptyList(),
    val exercises: List<SimpleWorkoutExerciseItem>,
)

@Serializable
data class SimpleWorkoutExerciseItem(
    val exerciseId: String,
    val sets: Int,
    val reps: Int,
    val setWeightsKg: List<Double?>? = null,
)

@Serializable
data class SubmitWorkoutSessionRequest(
    val name: String,
    val workoutPlanDayId: String? = null,
    val startedAt: Long,
    val finishedAt: Long,
    val status: String = "COMPLETED",
    val notes: String? = null,
    val sets: List<SubmitSessionSetItem> = emptyList(),
    val totalPlannedSets: Int? = null,
    val totalPlannedExercises: Int? = null,
)

@Serializable
data class SubmitSessionSetItem(
    val exerciseId: String,
    val setNumber: Int,
    val reps: Int? = null,
    val weightKg: Double? = null,
    val completed: Boolean = true,
    val durationSeconds: Int? = null,
)

@Serializable
data class WorkoutSessionResponse(
    val id: String,
    val name: String,
    val status: String,
)

@Serializable
data class WorkoutPlanResponse(
    val id: String,
    val name: String,
    val description: String? = null,
    val createdBy: String = "USER",
    val isActive: Boolean = true,
    val scheduleType: String = "ONE_TIME",
    val days: List<WorkoutPlanDayResponse> = emptyList(),
)

@Serializable
data class WorkoutPlanDayResponse(
    val id: String,
    val dayOfWeek: String,
    val name: String,
    val orderIndex: Int = 0,
    val exercises: List<WorkoutPlanExerciseResponse> = emptyList(),
)

@Serializable
data class WorkoutPlanExerciseResponse(
    val id: String,
    val exerciseId: String,
    val exerciseNameRu: String,
    val exerciseNameEn: String,
    val muscleGroup: String,
    val sets: Int,
    val reps: Int,
    val weightKg: Double? = null,
    val restSeconds: Int = 60,
    val orderIndex: Int = 0,

    val secondsPer10Reps: Int? = null,

    val setWeightsKg: List<Double?>? = null,
)

@Serializable
data class WorkoutSessionHistoryItem(
    val id: String,
    val name: String,
    val startedAt: Double,
    val finishedAt: Double? = null,
    val status: String,
    val totalSets: Int = 0,
    val completedSets: Int = 0,
    val totalExercises: Int = 0,
    val completedExercises: Int = 0,
    val totalReps: Int = 0,
    val primaryMuscleGroup: String? = null,
    val durationMinutes: Int? = null,
)

fun WorkoutPlanResponse.toActiveSession(): ActiveWorkoutSession {
    val day = days.minByOrNull { it.orderIndex }
    val exercises = day?.exercises
        ?.sortedBy { it.orderIndex }
        ?.map { ex ->
            ActiveExercise(
                exerciseId = ex.exerciseId,
                exerciseName = ex.exerciseNameRu,
                muscleGroupLabel = ex.muscleGroup,
                imageUrl = null,
                techniqueTip = null,
                sets = ex.sets,
                reps = ex.reps,
                weightKg = ex.weightKg,
                setWeightsKg = ex.setWeightsKg,
            )
        } ?: emptyList()
    val restSeconds = day?.exercises?.firstOrNull()?.restSeconds ?: 60
    return ActiveWorkoutSession(
        planId = id,
        planName = name,
        exercises = exercises,
        restSeconds = restSeconds,
    )
}
