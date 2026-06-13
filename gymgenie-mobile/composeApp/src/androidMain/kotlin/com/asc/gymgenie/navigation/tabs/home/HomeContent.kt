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
import com.asc.gymgenie.feature.activities.ActivityScheduleSettingsScreen
import com.asc.gymgenie.feature.create_workout.CreateWorkoutFlowScreen
import com.asc.gymgenie.feature.home.HomeScreen
import com.asc.gymgenie.feature.meal_plan_detail.MealPlanDetailScreen
import com.asc.gymgenie.feature.notifications.NotificationsScreen
import com.asc.gymgenie.feature.nutrition.CreateMealPlanFlowScreen
import com.asc.gymgenie.feature.workouts.WorkoutDetailScreen
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.workout.ActiveWorkoutSession
import com.asc.gymgenie.workout.WorkoutPlanShortResponse

@Composable
fun HomeContent(
    component: HomeComponent,
    onOpenWorkoutPlan: (WorkoutPlanShortResponse) -> Unit,
    onStartPlan: (String, String) -> Unit,
    onSessionReady: (ActiveWorkoutSession) -> Unit,
    onOpenPaywall: () -> Unit,
    onSwitchToProfile: () -> Unit = {},
    onSwitchToWorkouts: () -> Unit = {},
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
                    onCreateMealPlan = { mealType, date -> component.openCreateMealPlan(mealType, date) },
                    onViewMealPlan = { planId -> component.openMealPlanDetail(planId) },
                    onCreateWorkout = component::openCreateWorkout,
                    mealPlansReloadKey = mealPlansReloadKey,
                    activitiesRefreshSignal = activitiesRefreshSignal,
                    onOpenPaywall = onOpenPaywall,
                    onNotificationsClick = component::openNotifications,
                    onSwitchToProfile = onSwitchToProfile,
                    onSwitchToWorkouts = onSwitchToWorkouts,
                    onOpenActivityScheduleSettings = { activityId, name, scheduleType, scheduleDays, oneOffDate ->
                        component.openActivityScheduleSettings(
                            activityId = activityId,
                            activityName = name,
                            scheduleType = scheduleType,
                            scheduleDays = scheduleDays,
                            oneOffDate = oneOffDate,
                        )
                    },
                )

                HomeComponent.Child.Activities -> ActivitiesScreen(
                    onBack = component::pop,
                    onOpenCatalog = component::openCatalog,
                    refreshSignal = activitiesRefreshSignal,
                    onOpenScheduleSettings = { activityId, name, scheduleType, scheduleDays, oneOffDate ->
                        component.openActivityScheduleSettings(
                            activityId = activityId,
                            activityName = name,
                            scheduleType = scheduleType,
                            scheduleDays = scheduleDays,
                            oneOffDate = oneOffDate,
                        )
                    },
                )

                HomeComponent.Child.ActivityCatalog -> ActivityCatalogScreen(
                    onBack = component::pop,
                )

                is HomeComponent.Child.ActivityGoalSettings -> ActivityGoalSettingsScreen(
                    category = instance.category,
                    onBack = component::pop,
                )

                is HomeComponent.Child.ActivityScheduleSettings -> ActivityScheduleSettingsScreen(
                    activityId = instance.activityId,
                    activityName = instance.activityName,
                    initialScheduleType = instance.scheduleType,
                    initialScheduleDays = instance.scheduleDays,
                    initialOneOffDate = instance.oneOffDate,
                    onBack = component::pop,
                )

                is HomeComponent.Child.MealPlanDetail -> MealPlanDetailScreen(
                    planId = instance.planId,
                    isPastDate = instance.isPastDate,
                    onBack = component::pop,
                    onDeleted = component::onMealPlanDeleted,
                    onEdit = { component.openCreateMealPlan(editPlanId = instance.planId) },
                )

                is HomeComponent.Child.CreateWorkout -> CreateWorkoutFlowScreen(
                    viewModel = instance.viewModel,
                    onDismiss = {
                        instance.viewModel.reset()
                        component.pop()
                    },
                    onSaved = component::onWorkoutCreated,
                )

                is HomeComponent.Child.CreateMealPlan -> CreateMealPlanFlowScreen(
                    initialMealType = instance.initialMealType,
                    initialDate = instance.initialDate,
                    editPlanId = instance.editPlanId,
                    onDismiss = component::pop,
                    onDiscardToHome = component::resetToMain,
                    onSaved = component::onMealPlanSaved,
                )

                is HomeComponent.Child.WorkoutDetail -> WorkoutDetailScreen(
                    planId = instance.planId,
                    onBack = component::pop,
                    onStartPlan = onStartPlan,
                )

                HomeComponent.Child.Notifications -> NotificationsScreen(
                    onBack = component::pop,
                )
            }
        }
    }
}
