package com.asc.gymgenie.navigation.tabs.home

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.asc.gymgenie.feature.activities.GoalCategory

interface HomeComponent {

    val stack: Value<ChildStack<*, Child>>
    val mealPlansReloadKey: Value<Int>
    val activitiesRefreshSignal: Value<Int>

    fun openActivities()
    fun openCatalog()
    fun openGoalSettings(category: GoalCategory)
    fun openNutrition()
    fun openCreateMealPlan(initialMealType: String? = null)
    fun popBackToActivities()
    fun pop()
    fun resetToMain()
    fun onMealPlanSaved()

    sealed class Child {
        data object Main : Child()
        data object Activities : Child()
        data object ActivityCatalog : Child()
        data class ActivityGoalSettings(val category: GoalCategory) : Child()
        data object Nutrition : Child()
        data class CreateMealPlan(val initialMealType: String? = null) : Child()
    }
}
