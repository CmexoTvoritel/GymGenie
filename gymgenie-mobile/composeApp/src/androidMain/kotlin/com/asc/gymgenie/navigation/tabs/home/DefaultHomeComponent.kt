// Required because StackNavigator.push (used by openActivities /
// openCatalog / openGoalSettings / openNutrition / openCreateMealPlan) is
// @DelicateDecomposeApi. Pushes are driven by explicit user actions, so
// the duplicate-configuration risk that motivates the marker does not
// apply.
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
        HomeConfig.Nutrition -> HomeComponent.Child.Nutrition
        is HomeConfig.CreateMealPlan -> HomeComponent.Child.CreateMealPlan(config.initialMealType)
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

    override fun openNutrition() {
        navigation.push(HomeConfig.Nutrition)
    }

    override fun openCreateMealPlan(initialMealType: String?) {
        navigation.push(HomeConfig.CreateMealPlan(initialMealType))
    }

    override fun pop() {
        val current = stack.value.active.configuration
        if (current is HomeConfig.ActivityCatalog) {
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
        navigation.pop()
    }
}
