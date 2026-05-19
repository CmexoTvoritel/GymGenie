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
    data class ActivityScheduleSettings(
        val activityId: String,
        val activityName: String,
        val scheduleType: String? = null,
        val scheduleDays: List<String> = emptyList(),
        val oneOffDate: String? = null,
    ) : HomeConfig()

    @Serializable
    data class MealPlanDetail(val planId: String) : HomeConfig()

    @Serializable
    data class CreateMealPlan(
        val initialMealType: String? = null,
        val initialDate: String? = null,
        val editPlanId: String? = null,
    ) : HomeConfig()

    @Serializable
    data class WorkoutDetail(val planId: String) : HomeConfig()

    @Serializable
    data object Notifications : HomeConfig()
}
