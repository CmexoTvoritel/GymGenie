@file:OptIn(com.arkivanov.decompose.DelicateDecomposeApi::class)

package com.asc.gymgenie.navigation.tabs.home

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.popWhile
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.asc.gymgenie.feature.activities.GoalCategory

class DefaultHomeComponent(
    componentContext: ComponentContext,
) : HomeComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<HomeConfig>()

    override val stack: Value<ChildStack<*, HomeComponent.Child>> =
        childStack(
            source = navigation,
            serializer = HomeConfig.serializer(),
            initialConfiguration = HomeConfig.Main,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    private val _mealPlansReloadKey = MutableValue(0)
    override val mealPlansReloadKey: Value<Int> = _mealPlansReloadKey

    private val _activitiesRefreshSignal = MutableValue(0)
    override val activitiesRefreshSignal: Value<Int> = _activitiesRefreshSignal

    private fun createChild(
        config: HomeConfig,
        @Suppress("UNUSED_PARAMETER") childContext: ComponentContext,
    ): HomeComponent.Child = when (config) {
        HomeConfig.Main -> HomeComponent.Child.Main
        HomeConfig.Activities -> HomeComponent.Child.Activities
        HomeConfig.ActivityCatalog -> HomeComponent.Child.ActivityCatalog
        is HomeConfig.ActivityGoalSettings -> HomeComponent.Child.ActivityGoalSettings(config.category)
        is HomeConfig.ActivityScheduleSettings -> HomeComponent.Child.ActivityScheduleSettings(
            activityId = config.activityId,
            activityName = config.activityName,
            scheduleType = config.scheduleType,
            scheduleDays = config.scheduleDays,
            oneOffDate = config.oneOffDate,
        )
        is HomeConfig.MealPlanDetail -> HomeComponent.Child.MealPlanDetail(config.planId)
        is HomeConfig.CreateMealPlan -> HomeComponent.Child.CreateMealPlan(config.initialMealType, config.initialDate, config.editPlanId)
        is HomeConfig.WorkoutDetail -> HomeComponent.Child.WorkoutDetail(config.planId)
        HomeConfig.Notifications -> HomeComponent.Child.Notifications
    }

    override fun openActivities() {
        navigation.push(HomeConfig.Activities)
    }

    override fun openCatalog() {
        navigation.push(HomeConfig.ActivityCatalog)
    }

    override fun openGoalSettings(category: GoalCategory) {
        navigation.push(HomeConfig.ActivityGoalSettings(category))
    }

    override fun openActivityScheduleSettings(
        activityId: String,
        activityName: String,
        scheduleType: String?,
        scheduleDays: List<String>,
        oneOffDate: String?,
    ) {
        navigation.push(
            HomeConfig.ActivityScheduleSettings(
                activityId = activityId,
                activityName = activityName,
                scheduleType = scheduleType,
                scheduleDays = scheduleDays,
                oneOffDate = oneOffDate,
            )
        )
    }

    override fun openMealPlanDetail(planId: String) {
        navigation.push(HomeConfig.MealPlanDetail(planId))
    }

    override fun openCreateMealPlan(initialMealType: String?, initialDate: String?, editPlanId: String?) {
        navigation.push(HomeConfig.CreateMealPlan(initialMealType, initialDate, editPlanId))
    }

    override fun openNotifications() {
        navigation.push(HomeConfig.Notifications)
    }

    override fun openWorkoutDetail(planId: String) {
        if (stack.value.active.configuration !is HomeConfig.WorkoutDetail) {
            navigation.push(HomeConfig.WorkoutDetail(planId))
        }
    }

    override fun pop() {
        val current = stack.value.active.configuration
        if (current is HomeConfig.ActivityCatalog || current is HomeConfig.ActivityScheduleSettings) {
            _activitiesRefreshSignal.value = _activitiesRefreshSignal.value + 1
        }
        navigation.pop()
    }

    override fun popBackToActivities() {
        navigation.popWhile { it !is HomeConfig.Activities && it !is HomeConfig.Main }
        _activitiesRefreshSignal.value = _activitiesRefreshSignal.value + 1
    }

    override fun resetToMain() {
        navigation.replaceAll(HomeConfig.Main)
    }

    override fun onMealPlanSaved() {
        _mealPlansReloadKey.value = _mealPlansReloadKey.value + 1
        navigation.popWhile { it !is HomeConfig.Main }
    }

    override fun onMealPlanDeleted() {
        _mealPlansReloadKey.value = _mealPlansReloadKey.value + 1
        navigation.pop()
    }
}
