package com.asc.gymgenie.navigation.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainTab(
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    HOME("Главная", Icons.Outlined.Home, Icons.Filled.Home),
    AI_COACH("ИИ", Icons.Outlined.AutoAwesome, Icons.Filled.AutoAwesome),
    WORKOUTS("Тренировки", Icons.Outlined.FitnessCenter, Icons.Filled.FitnessCenter),
    PROFILE("Профиль", Icons.Outlined.Person, Icons.Filled.Person),
}
