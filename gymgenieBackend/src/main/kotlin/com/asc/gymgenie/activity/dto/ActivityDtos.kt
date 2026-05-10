package com.asc.gymgenie.activity.dto

import com.asc.gymgenie.activity.entity.ActivityKind
import com.asc.gymgenie.activity.entity.ActivityRing
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.util.*

// ===== Responses =====

/** Catalog entry — returned by `GET /catalog`. */
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

/**
 * A planned activity merged with the current day's log — returned by `GET /today`.
 *
 * [goal] is the effective goal: user override if set, otherwise the catalog [defaultGoal-equivalent].
 * [logValue] is 0 when the user has not yet checked in for today.
 */
data class ActivityTodayResponse(
    val activityId: UUID,
    val name: String,
    val ring: ActivityRing,
    val kind: ActivityKind,
    val presets: List<Int>,
    val unit: String?,
    val goal: Int?,
    val inverse: Boolean,
    val logValue: Int
)

/** Single log row — returned by `POST /{activityId}/checkin` and embedded in history responses. */
data class ActivityLogResponse(
    val activityId: UUID,
    val date: LocalDate,
    val value: Int
)

/** History bucket: per-day completion summary plus the raw logs for that day. */
data class ActivityHistoryDayResponse(
    val date: LocalDate,
    /** 0.0 .. 1.0 — share of the user's planned activities that met their goal that day. */
    val completionPct: Double,
    val logs: List<ActivityLogResponse>
)

// ===== Requests =====

/** Body for `POST /{activityId}/checkin`. */
data class ActivityCheckinRequest(

    @field:NotNull
    val date: LocalDate?,

    /** Raw value to record. For BINARY the service forces this to 1. For COUNTER/PRESET, must be >= 0. */
    @field:Min(0)
    val value: Int = 0,
)
