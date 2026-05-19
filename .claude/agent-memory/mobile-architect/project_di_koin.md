---
name: Koin DI integration scope
description: Koin now covers network/profile singletons AND every ViewModel; logout flows centrally through SessionManager.
type: project
---

GymGenie KMM uses Koin in three modules wired in `shared/src/commonMain/kotlin/com/asc/gymgenie/di/AppModule.kt`:

- `networkModule` — `TokenStorage`, `AuthApi`, `SessionManager`, the shared authenticated `HttpClient`, and every API class (`WorkoutApi`, `ExerciseApi`, `AiApi`, `ActivityApi`, `MealPlansApi`, `FoodProductApi`, `NutritionApi`, `AiMealApi`, `ManualMealPlanApi`).
- `profileModule` — `UserApi`, `UserProfileStore`.
- `viewModelModule` — every shared ViewModel as `factory { ... }`. Two factories take a runtime parameter via `parametersOf`: `WorkoutDetailViewModel(planId)` and `MealPlanDetailViewModel(planId)`.

**Why:** Removing prop drilling and unifying ViewModel construction. Before this, screens manually constructed VMs and passed `TokenStorage`/`UserProfileStore`/`onLogout` from `App.kt` down through `MainScreen` and into every leaf — hidden coupling and brittle wiring.

**How to apply:**
- Resolve any singleton or VM with `koin.get<X>()` in Compose, or `KoinHelper.shared.getX()` in Swift.
- For VMs with a runtime key (planId): `koin.get<WorkoutDetailViewModel> { parametersOf(planId) }`. iOS does NOT have a wrapper for these yet.
- ViewModels never know about navigation. Forced/explicit logout flows through `SessionManager.triggerLogout()` only. The composition root (Android `App.kt`, iOS `AppState.startObservingLogout`) is the SINGLE place that clears tokens, clears the profile store, and routes back to login.
- `SessionManager.observeLogout(onLogout:)` is a Swift bridge — Kotlin `SharedFlow.collect` cannot be polled by `.value` so iOS subscribes through this helper, which returns a `SessionSubscription` to cancel in `deinit`.

Bootstrap:
- Android: `GymGenieApplication.onCreate()` calls `startKoin { modules(networkModule, profileModule, viewModelModule) }` AFTER `AndroidTokenStorageContext.init(this)` — the Android `TokenStorage` impl reads context lazily.
- iOS: `KoinInitializerKt.doInitKoin()` from `iOSApp.init()`. `KoinHelper` exposes typed accessors.
