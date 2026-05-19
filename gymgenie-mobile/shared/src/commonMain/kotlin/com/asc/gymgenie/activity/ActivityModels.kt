package com.asc.gymgenie.activity

import kotlinx.serialization.Serializable

/**
 * Behavioural type of an activity check-in.
 *
 *  - [BINARY] is a single-shot toggle (e.g. "I stretched today"). The semantic
 *    value range is `0` / `1`. For "inverse" activities the meaning is flipped
 *    — `0` means the user has nothing to undo (the goal is upheld by default);
 *    `1` means they explicitly broke the streak.
 *  - [COUNTER] is an incremental tap that adds a fixed unit each time (e.g.
 *    "+1 glass of water").
 *  - [PRESET] is the same as a counter but with discrete preset values picked
 *    from a bottom sheet (e.g. 200 / 250 / 300 ml).
 *
 * The string form is what the backend emits. Use [parseKindOrNull] when
 * mapping a raw activity payload to keep tolerance against unknown values.
 */
enum class ActivityKind { BINARY, COUNTER, PRESET }

/**
 * Logical "ring" the activity belongs to. Drives both the colour scheme and
 * the aggregation in [ActivityRingsCard].
 */
enum class ActivityRing { MOVE, MIND, LIFE }

/**
 * How an activity is scheduled on the user's plan.
 *
 * - `null` / absent — every day (the default).
 * - [RECURRING] — only on specific days of the week ([ActivityTodayResponse.scheduleDays]).
 * - [ONE_TIME] — a single calendar date ([ActivityTodayResponse.oneOffDate]).
 */
enum class ScheduleType { RECURRING, ONE_TIME }

internal fun parseScheduleTypeOrNull(raw: String?): ScheduleType? =
    raw?.let { runCatching { ScheduleType.valueOf(it) }.getOrNull() }

internal fun parseKindOrNull(raw: String?): ActivityKind? =
    raw?.let { runCatching { ActivityKind.valueOf(it) }.getOrNull() }

internal fun parseRingOrNull(raw: String?): ActivityRing? =
    raw?.let { runCatching { ActivityRing.valueOf(it) }.getOrNull() }

/**
 * Today's snapshot of one activity for the current user. The payload is a
 * union shape that covers all three [ActivityKind] cases — fields irrelevant
 * to a given kind are nullable on purpose so the contract stays a single DTO.
 */
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

/**
 * Catalog entry the user can add to their daily plan. Same shape as
 * [ActivityTodayResponse] but without the per-day log state.
 */
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

/**
 * Body of a check-in request. The `value` semantics depend on the activity
 * kind — see [ActivityKind] doc.
 */
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

/**
 * Body for `POST /{activityId}/plan`. When all fields are null the backend
 * falls back to the "every day" schedule (backward compat).
 */
@Serializable
data class AddActivityToPlanRequest(
    val scheduleType: String? = null,
    val scheduleDays: List<String>? = null,
    val oneOffDate: String? = null,
    val goal: Int? = null,
)

/**
 * Body for `PUT /{activityId}/plan/schedule`. Updates the schedule of an
 * activity that is already in the user's plan.
 */
@Serializable
data class UpdateActivityScheduleRequest(
    val scheduleType: String? = null,
    val scheduleDays: List<String>? = null,
    val oneOffDate: String? = null,
)

/**
 * Per-day history bucket returned by `GET /api/v1/activities/history`.
 *
 * - [date] is an ISO-8601 string (`YYYY-MM-DD`); kept as [String] to mirror
 *   the JSON contract and avoid pulling `kotlinx-datetime` serializers into
 *   every consumer.
 * - [completionPct] is a `0.0 .. 1.0` ratio of how many of that day's planned
 *   activities met their goal. Used to render the green progress ring on the
 *   history strip.
 * - [logs] are the raw check-ins recorded for the day.
 */
@Serializable
data class ActivityHistoryDayResponse(
    val date: String,
    val completionPct: Double,
    val logs: List<ActivityLogResponse>,
)

/**
 * UI projection of an [ActivityTodayResponse]. Computed once in the domain
 * layer so neither the iOS nor the Android UI has to duplicate the
 * "is the goal met" / "fraction of progress" logic.
 *
 * For BINARY activities the [fraction] is either `0f` or `1f` and matches
 * [isDone]. For COUNTER/PRESET activities it is the ratio `logValue / goal`,
 * clamped to `[0f, 1f]`.
 */
data class ActivityProgress(
    val activity: ActivityTodayResponse,
    val fraction: Float,
    val isDone: Boolean,
)

/**
 * Computes the canonical [ActivityProgress] for a server-provided activity.
 *
 * Inverse BINARY activities (e.g. "no alcohol today") treat `logValue == 0`
 * as the "done" state — that's the default at the start of every day and
 * the user only logs a breaking event by submitting `1`.
 *
 * COUNTER / PRESET activities fall back to a goal of `1` when the server
 * does not specify one; this keeps the fraction well-defined and avoids
 * a division by zero when the backend sends a malformed payload.
 */
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
