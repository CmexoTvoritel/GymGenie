package com.asc.gymgenie.utils

import java.time.Month

/**
 * Centralized Russian month name translations.
 *
 * On JVM/Android, [kotlinx.datetime.Month] is a typealias for [java.time.Month],
 * so a single overload covers both.
 */

/** Nominative case month names, 1-indexed (index 0 is empty). */
val MonthNamesNominative: List<String> = listOf(
    "", "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
)

/** Genitive case month names, 1-indexed (index 0 is empty). */
val MonthNamesGenitive: List<String> = listOf(
    "", "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

/** Genitive case month name from [Month] enum (e.g. Month.JANUARY -> "января"). */
fun monthNameGenitive(month: Month): String = when (month) {
    Month.JANUARY -> "января"
    Month.FEBRUARY -> "февраля"
    Month.MARCH -> "марта"
    Month.APRIL -> "апреля"
    Month.MAY -> "мая"
    Month.JUNE -> "июня"
    Month.JULY -> "июля"
    Month.AUGUST -> "августа"
    Month.SEPTEMBER -> "сентября"
    Month.OCTOBER -> "октября"
    Month.NOVEMBER -> "ноября"
    Month.DECEMBER -> "декабря"
}

/** Genitive case month name from month number (1-12). */
fun monthNameGenitiveFromInt(month: Int): String =
    MonthNamesGenitive.getOrElse(month) { "" }
