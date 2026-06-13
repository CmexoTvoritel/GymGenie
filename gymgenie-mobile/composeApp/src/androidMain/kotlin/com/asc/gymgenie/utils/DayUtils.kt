package com.asc.gymgenie.utils

import java.util.Calendar

/**
 * Centralized weekday name translations and ordering constants.
 * All backend wire values are uppercase English (e.g. "MONDAY").
 */

/** Canonical backend ordering. */
val WeekdayOrder: List<String> = listOf(
    "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY",
)

/** Ordered pairs of (backend wire, short Russian label). */
val WeekdayPairs: List<Pair<String, String>> = listOf(
    "MONDAY" to "Пн",
    "TUESDAY" to "Вт",
    "WEDNESDAY" to "Ср",
    "THURSDAY" to "Чт",
    "FRIDAY" to "Пт",
    "SATURDAY" to "Сб",
    "SUNDAY" to "Вс",
)

/** Ordered short Russian day labels: ["Пн", "Вт", ...] */
val WeekdayLabelsRu: List<String> = WeekdayPairs.map { it.second }

/** Full Russian day name from backend wire value (e.g. "MONDAY" -> "понедельник"). */
fun weekdayNameRu(wire: String): String = when (wire.uppercase()) {
    "MONDAY" -> "понедельник"
    "TUESDAY" -> "вторник"
    "WEDNESDAY" -> "среда"
    "THURSDAY" -> "четверг"
    "FRIDAY" -> "пятница"
    "SATURDAY" -> "суббота"
    "SUNDAY" -> "воскресенье"
    else -> wire.lowercase()
}

/** Short Russian day abbreviation from backend wire value (e.g. "MONDAY" -> "Пн"). */
fun weekdayShortRu(wire: String): String = when (wire.uppercase()) {
    "MONDAY" -> "Пн"
    "TUESDAY" -> "Вт"
    "WEDNESDAY" -> "Ср"
    "THURSDAY" -> "Чт"
    "FRIDAY" -> "Пт"
    "SATURDAY" -> "Сб"
    "SUNDAY" -> "Вс"
    else -> wire
}

/** Short Russian day abbreviation from [Calendar] day constant (e.g. [Calendar.MONDAY] -> "Пн"). */
fun weekdayShortFromCalendar(calendarDay: Int): String = when (calendarDay) {
    Calendar.MONDAY -> "Пн"
    Calendar.TUESDAY -> "Вт"
    Calendar.WEDNESDAY -> "Ср"
    Calendar.THURSDAY -> "Чт"
    Calendar.FRIDAY -> "Пт"
    Calendar.SATURDAY -> "Сб"
    Calendar.SUNDAY -> "Вс"
    else -> ""
}

/** Short Russian day abbreviation from [java.time.DayOfWeek]. */
fun weekdayShortFromDayOfWeek(day: java.time.DayOfWeek): String = when (day) {
    java.time.DayOfWeek.MONDAY -> "Пн"
    java.time.DayOfWeek.TUESDAY -> "Вт"
    java.time.DayOfWeek.WEDNESDAY -> "Ср"
    java.time.DayOfWeek.THURSDAY -> "Чт"
    java.time.DayOfWeek.FRIDAY -> "Пт"
    java.time.DayOfWeek.SATURDAY -> "Сб"
    java.time.DayOfWeek.SUNDAY -> "Вс"
}

/** Short Russian day abbreviation from [kotlinx.datetime.DayOfWeek]. */
fun weekdayShortFromKotlinxDayOfWeek(day: kotlinx.datetime.DayOfWeek): String = when (day) {
    kotlinx.datetime.DayOfWeek.MONDAY -> "Пн"
    kotlinx.datetime.DayOfWeek.TUESDAY -> "Вт"
    kotlinx.datetime.DayOfWeek.WEDNESDAY -> "Ср"
    kotlinx.datetime.DayOfWeek.THURSDAY -> "Чт"
    kotlinx.datetime.DayOfWeek.FRIDAY -> "Пт"
    kotlinx.datetime.DayOfWeek.SATURDAY -> "Сб"
    kotlinx.datetime.DayOfWeek.SUNDAY -> "Вс"
}

/** Reverse mapping: short Russian label to backend wire value (e.g. "Пн" -> "MONDAY"). */
val dayLabelToBackend: LinkedHashMap<String, String> = linkedMapOf(
    "Пн" to "MONDAY",
    "Вт" to "TUESDAY",
    "Ср" to "WEDNESDAY",
    "Чт" to "THURSDAY",
    "Пт" to "FRIDAY",
    "Сб" to "SATURDAY",
    "Вс" to "SUNDAY",
)

/** Reverse mapping: backend wire value to short Russian label (e.g. "MONDAY" -> "Пн"). */
val backendToDayLabel: Map<String, String> = dayLabelToBackend.entries.associate { (k, v) -> v to k }
