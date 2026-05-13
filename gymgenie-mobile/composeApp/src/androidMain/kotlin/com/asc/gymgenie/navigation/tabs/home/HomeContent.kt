package com.asc.gymgenie.navigation.tabs.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.asc.gymgenie.feature.activities.ActivitiesScreen
import com.asc.gymgenie.feature.activities.ActivityCatalogScreen
import com.asc.gymgenie.feature.activities.ActivityGoalSettingsScreen
import com.asc.gymgenie.feature.home.HomeScreen
import com.asc.gymgenie.feature.nutrition.CreateMealPlanFlowScreen
import com.asc.gymgenie.feature.nutrition.NutritionScreen
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.workout.ActiveWorkoutSession
import com.asc.gymgenie.workout.WorkoutPlanShortResponse

@Composable
fun HomeContent(
    component: HomeComponent,
    onOpenWorkoutPlan: (WorkoutPlanShortResponse) -> Unit,
    onSessionReady: (ActiveWorkoutSession) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mealPlansReloadKey by component.mealPlansReloadKey.subscribeAsState()
    val activitiesRefreshSignal by component.activitiesRefreshSignal.subscribeAsState()

    Box(modifier = modifier.fillMaxSize().background(WarmOffWhite)) {
        Children(
            stack = component.stack,
            animation = stackAnimation(slide()),
        ) { child ->
            when (val instance = child.instance) {
                HomeComponent.Child.Main -> HomeScreen(
                    onOpenActivities = component::openActivities,
                    onOpenCatalog = component::openCatalog,
                    onViewPlan = onOpenWorkoutPlan,
                    onSessionReady = onSessionReady,
                    onCreateMealPlan = { mealType -> component.openCreateMealPlan(mealType) },
                    onViewMealPlan = { component.openNutrition() },
                    mealPlansReloadKey = mealPlansReloadKey,
                )

                HomeComponent.Child.Activities -> ActivitiesScreen(
                    onBack = component::pop,
                    onOpenCatalog = component::openCatalog,
                    refreshSignal = activitiesRefreshSignal,
                )

                HomeComponent.Child.ActivityCatalog -> ActivityCatalogScreen(
                    onBack = component::pop,
                )

                is HomeComponent.Child.ActivityGoalSettings -> ActivityGoalSettingsScreen(
                    category = instance.category,
                    onBack = component::pop,
                )

                HomeComponent.Child.Nutrition -> NutritionScreen(
                    onBack = component::pop,
                )

                is HomeComponent.Child.CreateMealPlan -> CreateMealPlanFlowScreen(
                    initialMealType = instance.initialMealType,
                    onDismiss = component::pop,
                    onSaved = component::onMealPlanSaved,
                )
            }
        }
    }
}
