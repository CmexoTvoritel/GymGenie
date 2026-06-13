package com.asc.gymgenie.utils

/**
 * Shared formatting utilities for the Android UI layer.
 */

/** Compact rest duration label: "30с", "1м", "1м 30с". */
fun formatRestDuration(seconds: Int): String {
    if (seconds < 60) return "${seconds}с"
    val minutes = seconds / 60
    val remainder = seconds % 60
    return if (remainder == 0) "${minutes}м" else "${minutes}м ${remainder}с"
}

/** Verbose rest duration label: "30 сек", "1 мин", "1 мин 30 сек". */
fun formatRestDurationLong(seconds: Int): String {
    if (seconds < 60) return "$seconds сек"
    val minutes = seconds / 60
    val remainder = seconds % 60
    return if (remainder == 0) "$minutes мин" else "$minutes мин $remainder сек"
}
