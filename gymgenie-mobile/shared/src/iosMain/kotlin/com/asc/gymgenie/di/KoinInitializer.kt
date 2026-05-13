package com.asc.gymgenie.di

import com.asc.gymgenie.activity.ActivityApi
import com.asc.gymgenie.ai.AiApi
import com.asc.gymgenie.auth.AuthApi
import com.asc.gymgenie.common.SessionManager
import com.asc.gymgenie.exercise.ExerciseApi
import com.asc.gymgenie.nutrition.AiMealApi
import com.asc.gymgenie.nutrition.FoodProductApi
import com.asc.gymgenie.nutrition.ManualMealPlanApi
import com.asc.gymgenie.nutrition.MealPlansApi
import com.asc.gymgenie.nutrition.NutritionApi
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.user.UserApi
import com.asc.gymgenie.user.UserProfileStore
import com.asc.gymgenie.workout.WorkoutApi
import io.ktor.client.HttpClient
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

fun initKoin() {
    runCatching { KoinPlatform.getKoin() }.onSuccess { return }
    startKoin {
        modules(networkModule, profileModule, databaseModule, viewModelModule)
    }
}

object KoinHelper {
    fun getUserProfileStore(): UserProfileStore = KoinPlatform.getKoin().get()
    fun getUserApi(): UserApi = KoinPlatform.getKoin().get()
    fun getAuthApi(): AuthApi = KoinPlatform.getKoin().get()
    fun getTokenStorage(): TokenStorage = KoinPlatform.getKoin().get()
    fun getHttpClient(): HttpClient = KoinPlatform.getKoin().get()
    fun getWorkoutApi(): WorkoutApi = KoinPlatform.getKoin().get()
    fun getExerciseApi(): ExerciseApi = KoinPlatform.getKoin().get()
    fun getAiApi(): AiApi = KoinPlatform.getKoin().get()
    fun getActivityApi(): ActivityApi = KoinPlatform.getKoin().get()
    fun getNutritionApi(): NutritionApi = KoinPlatform.getKoin().get()
    fun getAiMealApi(): AiMealApi = KoinPlatform.getKoin().get()
    fun getMealPlansApi(): MealPlansApi = KoinPlatform.getKoin().get()
    fun getFoodProductApi(): FoodProductApi = KoinPlatform.getKoin().get()
    fun getManualMealPlanApi(): ManualMealPlanApi = KoinPlatform.getKoin().get()
    fun getSessionManager(): SessionManager = KoinPlatform.getKoin().get()
}
