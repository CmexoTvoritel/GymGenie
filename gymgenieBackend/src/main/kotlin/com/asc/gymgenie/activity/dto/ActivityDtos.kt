package com.asc.gymgenie.activity.dto

import com.asc.gymgenie.activity.entity.ActivityKind
import com.asc.gymgenie.activity.entity.ActivityRing
import com.asc.gymgenie.workout.entity.WorkoutScheduleType
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.util.*

data class ActivityCatalogResponse(
    val id: UUID,
    val name: String,
    val ring: ActivityRing,
    val kind: ActivityKind,
    val presets: List<Int>,
    val unit: String?,
    val defaultGoal: Int?,
    val inverse: Boolean
)

data class ActivityTodayResponse(
    val activityId: UUID,
    val name: String,
    val ring: ActivityRing,
    val kind: ActivityKind,
    val presets: List<Int>,
    val unit: String?,
    val goal: Int?,
    val inverse: Boolean,
    val logValue: Int,
    val scheduleType: WorkoutScheduleType? = null,
    val scheduleDays: List<String> = emptyList(),
    val oneOffDate: LocalDate? = null
)

data class ActivityLogResponse(
    val activityId: UUID,
    val date: LocalDate,
    val value: Int
)

data class ActivityHistoryDayResponse(
    val date: LocalDate,

    val completionPct: Double,
    val logs: List<ActivityLogResponse>
)

data class ActivityCheckinRequest(

    @field:NotNull
    val date: LocalDate?,

    @field:Min(0)
    val value: Int = 0,
)

data class AddActivityToPlanRequest(
    val scheduleType: WorkoutScheduleType? = null,
    val scheduleDays: List<String>? = null,
    val oneOffDate: LocalDate? = null,
    val goal: Int? = null
)

data class UpdateActivityScheduleRequest(
    val scheduleType: WorkoutScheduleType? = null,
    val scheduleDays: List<String> = emptyList(),
    val oneOffDate: LocalDate? = null
)
