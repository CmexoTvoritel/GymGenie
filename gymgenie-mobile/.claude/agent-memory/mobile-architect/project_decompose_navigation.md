---
name: Decompose navigation architecture (Android)
description: Android composeApp uses Decompose ChildStack/ChildSlot navigation; tabs are 4 retained children, workout-session is a global ChildSlot, all configs are @Serializable
type: project
---

The Android `composeApp` uses Decompose 3.3.0 with Essenty 2.5.0 for the entire navigation tree. iOS is unaffected and continues to use SwiftUI navigation directly.

**Why:** Decompose provides predictable, testable, lifecycle-aware navigation that survives configuration changes and process death. It cleanly replaces the prior ad-hoc `var selectedTab` / `mutableStateListOf` / `Boolean` flag pattern that had grown unmaintainable.

**How to apply:**
- All navigation logic lives under `composeApp/src/androidMain/kotlin/com/asc/gymgenie/navigation/{root,main,tabs/{home,workouts,profile,ai}}/`. Keep new flows there.
- Each component has a triplet: `XComponent` (interface), `DefaultXComponent` (implementation, `ComponentContext by componentContext`), `XContent` (Compose renderer using `Children(stack = ...)`). Tab-stack components also have `XConfig.kt` for `@Serializable` configurations.
- Bottom bar visibility is driven by per-tab stack depth in `MainContent.shouldShowBottomBar`. When introducing a new top-level subscreen, ensure the bottom bar gets hidden there (stack size > 1 hides it).
- The active workout session is a global `ChildSlot` on `MainComponent`, not a stack child. Its config (`WorkoutSessionConfig(session)`) carries the `ActiveWorkoutSession` directly and uses `serializer = null` because the session is not `@Serializable`. Do not introduce side-channel state to feed the slot.
- ViewModels owned by components (`CreateWorkoutViewModel`, `ProfileViewModel`) are provided as `() -> VM` factories from `MainActivity` via Koin and instantiated via `by lazy { provider().also { lifecycle.doOnDestroy { vm.onCleared() } } }`. Do not call `koin.get()` directly inside components.
- `componentScope()` extension on `Lifecycle` (`navigation/util/ComponentScope.kt`) gives a `CoroutineScope` that auto-cancels on destroy. Use it for component-side coroutines (DataStore reads, session manager listening).
- `MainActivity` is the only Android entry point — no `App.kt`. It builds `DefaultRootComponent` with `defaultComponentContext()`, which auto-wires back handling.
