---
name: Shared ScreenState contract
description: Canonical loading/error/content phase contract for KMM screens, with paired refresh/content-loaded flags
type: project
---

GymGenie KMM uses a shared sealed class `ScreenState` (Loading / Error(message) / Content) at `shared/src/commonMain/kotlin/com/asc/gymgenie/ui/ScreenState.kt`. Each screen's UI state pairs `screenState: ScreenState` with two booleans: `isContentLoaded` (true once a successful payload has rendered) and `isRefreshing` (true during pull-to-refresh).

**Why:** Avoids the common bug where a refresh flips the screen back to a full-page spinner. Background refresh stays inside `ScreenState.Content` while `isRefreshing = true`; only the very first load surfaces `Loading`. Refresh failures keep `Content` and surface a non-blocking `errorMessage`.

**How to apply:**
- New screens should reuse `ScreenState` instead of inventing their own loading flag.
- ViewModels expose `load()` (initial / blocking) and `refresh()` (background). `refresh()` must never collapse to full-screen Loading.
- iOS wrappers mirror the sealed class with a Swift enum (e.g. `HomeScreenState`) and map via `is ScreenStateLoading` / `as? ScreenStateError` (Kotlin sealed classes export to ObjC as `<Type><Variant>` flat names).
- Android Compose: wrap `ScreenState.Content` in `PullToRefreshBox` (material3 `pulltorefresh` is available — `material3 = 1.10.0-alpha05` in libs.versions.toml). iOS: use `.refreshable` on the ScrollView.
- The Kotlin guard `loadJob: Job?` should serialize concurrent loads to prevent stacking refreshes.
