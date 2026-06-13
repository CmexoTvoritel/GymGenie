package com.asc.gymgenie.activity

import kotlinx.serialization.Serializable

enum class ActivityKind { BINARY, COUNTER, PRESET }

enum class ActivityRing { MOVE, MIND, LIFE }

enum class ScheduleType { RECURRING, ONE_TIME }

internal fun parseScheduleTypeOrNull(raw: String?): ScheduleType? =
    raw?.let { runCatching { ScheduleType.valueOf(it) }.getOrNull() }

internal fun parseKindOrNull(raw: String?): ActivityKind? =
    raw?.let { runCatching { ActivityKind.valueOf(it) }.getOrNull() }

internal fun parseRingOrNull(raw: String?): ActivityRing? =
    raw?.let { runCatching { ActivityRing.valueOf(it) }.getOrNull() }

@Serializable
data class ActivityTodayResponse(
    val activityId: String,
    val name: String,
    val ring: String,
    val kind: String,
    val presets: List<Int>? = null,
    val unit: String? = null,
    val goal: Int? = null,
    val inverse: Boolean = false,
    val logValue: Int = 0,
    val scheduleType: String? = null,
    val scheduleDays: List<String> = emptyList(),
    val oneOffDate: String? = null,
)

@Serializable
data class ActivityCatalogResponse(
    val id: String,
    val name: String,
    val ring: String,
    val kind: String,
    val presets: List<Int>? = null,
    val unit: String? = null,
    val defaultGoal: Int? = null,
    val inverse: Boolean = false,
)

@Serializable
data class ActivityCheckinRequest(
    val date: String,
    val value: Int,
)

@Serializable
data class ActivityLogResponse(
    val activityId: String,
    val date: String,
    val value: Int,
)

@Serializable
data class AddActivityToPlanRequest(
    val scheduleType: String? = null,
    val scheduleDays: List<String>? = null,
    val oneOffDate: String? = null,
    val goal: Int? = null,
)

@Serializable
data class UpdateActivityScheduleRequest(
    val scheduleType: String? = null,
    val scheduleDays: List<String>? = null,
    val oneOffDate: String? = null,
)

@Serializable
data class ActivityHistoryDayResponse(
    val date: String,
    val completionPct: Double,
    val logs: List<ActivityLogResponse>,
)

data class ActivityProgress(
    val activity: ActivityTodayResponse,
    val fraction: Float,
    val isDone: Boolean,
)

fun ActivityTodayResponse.toProgress(): ActivityProgress {
    val kindEnum = parseKindOrNull(kind) ?: ActivityKind.BINARY
    return when (kindEnum) {
        ActivityKind.BINARY -> {
            val done = if (inverse) logValue == 0 else logValue > 0
            ActivityProgress(this, if (done) 1f else 0f, done)
        }

        ActivityKind.COUNTER, ActivityKind.PRESET -> {
            val effectiveGoal = goal?.takeIf { it > 0 } ?: 1
            val fraction = (logValue.toFloat() / effectiveGoal.toFloat()).coerceIn(0f, 1f)
            ActivityProgress(this, fraction, logValue >= effectiveGoal)
        }
    }
}
