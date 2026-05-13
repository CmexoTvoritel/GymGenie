package com.asc.gymgenie.navigation.tabs.home

import com.asc.gymgenie.feature.activities.GoalCategory
import kotlinx.serialization.Serializable

@Serializable
sealed class HomeConfig {

    @Serializable
    data object Main : HomeConfig()

    @Serializable
    data object Activities : HomeConfig()

    @Serializable
    data object ActivityCatalog : HomeConfig()

    @Serializable
    data class ActivityGoalSettings(val category: GoalCategory) : HomeConfig()

    @Serializable
    data object Nutrition : HomeConfig()

    @Serializable
    data class CreateMealPlan(val initialMealType: String? = null) : HomeConfig()
}
