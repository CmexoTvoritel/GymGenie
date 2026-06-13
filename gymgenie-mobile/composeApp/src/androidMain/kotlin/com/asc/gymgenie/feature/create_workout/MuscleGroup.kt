package com.asc.gymgenie.feature.create_workout

import com.asc.gymgenie.R

internal fun muscleGroupDrawable(key: String): Int = when (key.uppercase()) {
    "CHEST" -> R.drawable.ic_chest
    "BACK" -> R.drawable.ic_back
    "SHOULDERS" -> R.drawable.ic_shoulders
    "BICEPS" -> R.drawable.ic_biceps
    "TRICEPS" -> R.drawable.ic_triceps
    "FOREARMS" -> R.drawable.ic_forearms
    "ABS" -> R.drawable.ic_abs
    "QUADRICEPS" -> R.drawable.ic_quadriceps
    "HAMSTRINGS" -> R.drawable.ic_hamstrings
    "GLUTES" -> R.drawable.ic_glutes
    "CALVES" -> R.drawable.ic_calves
    "FULL_BODY" -> R.drawable.ic_fullbody
    "CARDIO" -> R.drawable.ic_cardio
    else -> R.drawable.ic_fullbody
}

internal fun muscleGroupExerciseDrawable(key: String): Int = when (key.uppercase()) {
    "CHEST" -> R.drawable.ic_exercise_chest
    "BACK" -> R.drawable.ic_exercise_back
    "SHOULDERS" -> R.drawable.ic_exercise_shoulders
    "BICEPS", "TRICEPS", "FOREARMS" -> R.drawable.ic_exercise_arms
    "ABS" -> R.drawable.ic_exercise_abs
    "QUADRICEPS", "HAMSTRINGS", "GLUTES", "CALVES" -> R.drawable.ic_exercise_legs
    "FULL_BODY" -> R.drawable.ic_exercise_fullbody
    "CARDIO" -> R.drawable.ic_exercise_cardio
    else -> R.drawable.ic_exercise_fullbody
}
