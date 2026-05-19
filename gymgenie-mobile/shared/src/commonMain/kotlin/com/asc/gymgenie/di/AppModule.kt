package com.asc.gymgenie.di

import com.asc.gymgenie.activity.ActivityApi
import com.asc.gymgenie.ai.AiApi
import com.asc.gymgenie.ai.AiViewModel
import com.asc.gymgenie.auth.AuthApi
import com.asc.gymgenie.common.SessionManager
import com.asc.gymgenie.common.createAuthenticatedClient
import com.asc.gymgenie.exercise.ExerciseApi
import com.asc.gymgenie.nutrition.AiMealApi
import com.asc.gymgenie.nutrition.AiMealViewModel
import com.asc.gymgenie.nutrition.CreateMealPlanViewModel
import com.asc.gymgenie.nutrition.FoodProductApi
import com.asc.gymgenie.nutrition.ManualMealPlanApi
import com.asc.gymgenie.nutrition.MealPlanDetailViewModel
import com.asc.gymgenie.nutrition.MealPlansApi
import com.asc.gymgenie.nutrition.MealPlansListViewModel
import com.asc.gymgenie.nutrition.NutritionApi
import com.asc.gymgenie.presentation.ActivitiesViewModel
import com.asc.gymgenie.presentation.ActivityCatalogViewModel
import com.asc.gymgenie.presentation.AuthViewModel
import com.asc.gymgenie.presentation.CreateWorkoutViewModel
import com.asc.gymgenie.presentation.ExerciseDetailViewModel
import com.asc.gymgenie.presentation.ExercisePickerViewModel
import com.asc.gymgenie.presentation.FoodPickerViewModel
import com.asc.gymgenie.presentation.HomeViewModel
import com.asc.gymgenie.presentation.PaywallViewModel
import com.asc.gymgenie.presentation.ProfileViewModel
import com.asc.gymgenie.presentation.WorkoutDetailViewModel
import com.asc.gymgenie.presentation.WorkoutHistoryViewModel
import com.asc.gymgenie.presentation.WorkoutsViewModel
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.storage.createTokenStorage
import com.asc.gymgenie.user.UserApi
import com.asc.gymgenie.user.UserProfileStore
import com.asc.gymgenie.workout.PendingSessionUploader
import com.asc.gymgenie.workout.WorkoutApi
import io.ktor.client.HttpClient
import org.koin.core.parameter.parametersOf
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
    single { PendingSessionUploader(get(), get()) }
}

val profileModule = module {
    single { UserApi(get()) }
    single { UserProfileStore(get()) }
}

/**
 * Presentation-layer view models.
 *
 * View models are registered as factories: each screen owns a fresh instance
 * for the duration of its composition / SwiftUI scene. The shared coroutine
 * scope they hold is cancelled by the platform layer (`onCleared` /
 * `deinit`) so a factory does not leak across navigations.
 *
 * View models that need a runtime parameter — currently [WorkoutDetailViewModel]
 * keyed by `planId` and [MealPlanDetailViewModel] keyed by `planId` — read
 * the value from [parametersOf]; callers resolve them via
 * `koin.get { parametersOf(planId) }`.
 */
val viewModelModule = module {
    factory { AuthViewModel(get(), get()) }
    single { HomeViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { ActivitiesViewModel(get(), get()) }
    factory { ActivityCatalogViewModel(get(), get()) }
    factory { WorkoutsViewModel(get(), get(), get(), get()) }
    factory { CreateWorkoutViewModel(get(), get()) }
    factory { ExerciseDetailViewModel(get()) }
    factory { ExercisePickerViewModel(get()) }
    factory { FoodPickerViewModel(get()) }
    factory { PaywallViewModel(get(), get()) }
    factory { ProfileViewModel(get(), get()) }
    factory { AiViewModel(get(), get()) }
    factory { AiMealViewModel(get(), get()) }
    factory { CreateMealPlanViewModel(get(), get(), get()) }
    factory { MealPlansListViewModel(get()) }
    factory { WorkoutHistoryViewModel(get(), get()) }
    factory { (planId: String) -> WorkoutDetailViewModel(planId, get(), get(), get()) }
    factory { (planId: String) -> MealPlanDetailViewModel(get(), planId) }
}
