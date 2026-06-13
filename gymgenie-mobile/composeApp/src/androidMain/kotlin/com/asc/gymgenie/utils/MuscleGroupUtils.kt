package com.asc.gymgenie.utils

/**
 * Centralized muscle-group and exercise-category Russian translations.
 */

/** Standard Russian name for a backend muscle group wire value (e.g. "CHEST" -> "Грудь"). */
fun muscleGroupNameRu(group: String): String = when (group.uppercase()) {
    "CHEST" -> "Грудь"
    "BACK" -> "Спина"
    "SHOULDERS" -> "Плечи"
    "BICEPS" -> "Бицепс"
    "TRICEPS" -> "Трицепс"
    "FOREARMS" -> "Предплечья"
    "ABS" -> "Пресс"
    "QUADRICEPS" -> "Квадрицепс"
    "HAMSTRINGS" -> "Бицепс бедра"
    "CALVES" -> "Икры"
    "GLUTES" -> "Ягодицы"
    "CARDIO" -> "Кардио"
    "FULL_BODY" -> "Всё тело"
    else -> group
}

/**
 * Verbose Russian name for history/summary contexts.
 * Handles additional alias groups (CORE, ARMS, LEGS, SHOULDER) and uses
 * more descriptive labels where appropriate.
 */
fun muscleGroupNameVerboseRu(group: String?): String = when (group?.uppercase()) {
    "CHEST" -> "Грудные мышцы"
    "BACK" -> "Спина"
    "SHOULDERS", "SHOULDER" -> "Плечи"
    "BICEPS" -> "Бицепс"
    "TRICEPS" -> "Трицепс"
    "FOREARMS" -> "Предплечья"
    "ARMS" -> "Руки"
    "ABS", "CORE" -> "Пресс"
    "QUADRICEPS" -> "Квадрицепсы"
    "HAMSTRINGS" -> "Задняя поверхность бедра"
    "GLUTES" -> "Ягодицы"
    "CALVES" -> "Икроножные"
    "LEGS" -> "Ноги"
    "CARDIO" -> "Кардио"
    "FULL_BODY" -> "Все тело"
    else -> "Смешанная"
}

/** Russian name for exercise category wire value (e.g. "STRENGTH" -> "Сила"). */
fun categoryNameRu(category: String): String = when (category.uppercase()) {
    "STRENGTH" -> "Сила"
    "CARDIO" -> "Кардио"
    "FLEXIBILITY" -> "Гибкость"
    "BALANCE" -> "Баланс"
    "PLYOMETRIC" -> "Плиометрика"
    "FUNCTIONAL" -> "Функционал"
    else -> category
}
