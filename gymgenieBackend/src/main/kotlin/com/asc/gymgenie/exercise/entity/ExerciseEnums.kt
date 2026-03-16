package com.asc.gymgenie.exercise.entity

enum class MuscleGroup {
    CHEST,       // Грудные мышцы
    BACK,        // Спина
    SHOULDERS,   // Плечи
    BICEPS,      // Бицепс
    TRICEPS,     // Трицепс
    FOREARMS,    // Предплечья
    ABS,         // Пресс
    QUADRICEPS,  // Квадрицепсы
    HAMSTRINGS,  // Бицепс бедра
    GLUTES,      // Ягодицы
    CALVES,      // Икры
    FULL_BODY,   // Всё тело
    CARDIO       // Кардио
}

enum class ExerciseCategory {
    STRENGTH,    // Силовая
    CARDIO,      // Кардио
    FLEXIBILITY, // Растяжка
    BALANCE,     // Баланс
    PLYOMETRIC,  // Плиометрика
    CALISTHENICS // Калистеника
}

enum class DifficultyLevel {
    BEGINNER,    // Начинающий
    INTERMEDIATE,// Средний
    ADVANCED     // Продвинутый
}
