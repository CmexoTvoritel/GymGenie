package com.asc.gymgenie.di

import com.asc.gymgenie.activity.ActivityApi
import com.asc.gymgenie.ai.AiApi
import com.asc.gymgenie.auth.AuthApi
import com.asc.gymgenie.common.SessionManager
import com.asc.gymgenie.common.createAuthenticatedClient
import com.asc.gymgenie.exercise.ExerciseApi
import com.asc.gymgenie.nutrition.AiMealApi
import com.asc.gymgenie.nutrition.FoodProductApi
import com.asc.gymgenie.nutrition.ManualMealPlanApi
import com.asc.gymgenie.nutrition.MealPlansApi
import com.asc.gymgenie.nutrition.NutritionApi
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.storage.createTokenStorage
import com.asc.gymgenie.user.UserApi
import com.asc.gymgenie.user.UserProfileStore
import com.asc.gymgenie.workout.WorkoutApi
import io.ktor.client.HttpClient
import org.koin.dsl.module

/**
 * Network-layer singletons.
 *
 * - [TokenStorage] is a process-wide singleton because credentials must be
 *   read/written from a single source of truth across all auth flows.
 * - [AuthApi] owns its own internal `HttpClient`; making it a singleton avoids
 *   spawning a new client on every login/refresh.
 * - [SessionManager] is the bridge that lets the network layer signal a
 *   forced logout (refresh failed) up to the UI layer without taking a
 *   navigation dependency.
 * - The authenticated [HttpClient] depends on the three above and is shared
 *   by all API classes (UserApi, WorkoutApi, AiApi, ...). Sharing this
 *   instance is critical: every additional client carries its own Ktor
 *   `BearerTokens` cache, and concurrent clients race on every refresh —
 *   one wins, the others retry with a now-invalidated refresh token and
 *   sign the user out.
 */
val networkModule = module {
    single<TokenStorage> { createTokenStorage() }
    single { AuthApi() }
    single { SessionManager() }
    single<HttpClient> { createAuthenticatedClient(get(), get(), get()) }
    single { WorkoutApi(get()) }
    single { ExerciseApi(get()) }
    single { AiApi(get()) }
    single { ActivityApi(get()) }
    single { FoodProductApi(get()) }
    single { NutritionApi(get()) }
    single { AiMealApi(get()) }
    single { MealPlansApi(get()) }
    single { ManualMealPlanApi(get()) }
}

val profileModule = module {
    single { UserApi(get()) }
    single { UserProfileStore(get()) }
}
