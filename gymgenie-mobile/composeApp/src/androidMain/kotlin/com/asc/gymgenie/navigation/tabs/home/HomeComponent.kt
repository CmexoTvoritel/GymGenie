package com.asc.gymgenie.navigation.tabs.home

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.asc.gymgenie.feature.activities.GoalCategory
import com.asc.gymgenie.presentation.CreateWorkoutViewModel

interface HomeComponent {

    val stack: Value<ChildStack<*, Child>>
    val mealPlansReloadKey: Value<Int>
    val activitiesRefreshSignal: Value<Int>

    fun openActivities()
    fun openCatalog()
    fun openGoalSettings(category: GoalCategory)
    fun openActivityScheduleSettings(
        activityId: String,
        activityName: String,
        scheduleType: String?,
        scheduleDays: List<String>,
        oneOffDate: String?,
    )
    fun openMealPlanDetail(planId: String)
    fun openCreateMealPlan(initialMealType: String? = null, initialDate: String? = null, editPlanId: String? = null)
    fun openCreateWorkout()
    fun openWorkoutDetail(planId: String)
    fun openNotifications()
    fun popBackToActivities()
    fun pop()
    fun resetToMain()
    fun onMealPlanSaved()
    fun onMealPlanDeleted()
    fun onWorkoutCreated()

    sealed class Child {
        data object Main : Child()
        data object Activities : Child()
        data object ActivityCatalog : Child()
        data class ActivityGoalSettings(val category: GoalCategory) : Child()
        data class ActivityScheduleSettings(
            val activityId: String,
            val activityName: String,
            val scheduleType: String?,
            val scheduleDays: List<String>,
            val oneOffDate: String?,
        ) : Child()
        data class MealPlanDetail(val planId: String, val isPastDate: Boolean = false) : Child()
        data class CreateMealPlan(
            val initialMealType: String? = null,
            val initialDate: String? = null,
            val editPlanId: String? = null,
        ) : Child()
        data class CreateWorkout(val viewModel: CreateWorkoutViewModel) : Child()
        data class WorkoutDetail(val planId: String) : Child()
        data object Notifications : Child()
    }
}
