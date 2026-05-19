---
name: GymGenie project context
description: Key architectural decisions, known issues, and patterns in the GymGenie mobile codebase
type: project
---

GymGenie is an AI-powered sports app: Kotlin/Spring Boot backend, KMM shared logic, SwiftUI iOS, Jetpack Compose Android, Decompose navigation.

Architecture note: As of March 2026, the mobile side uses a flat state-machine navigation (AppState on iOS, sealed Screen class on Android) rather than full Decompose component trees. This was an intentional starting point.

Known contract mismatches identified in first review (2026-03-21):
- Backend WorkoutPlanShortResponse has `daysCount` (not `dayCount`) and `createdBy` is a non-null enum `CreatedBy` (not nullable String)
- Backend ExerciseShortResponse fields `muscleGroup`, `category`, `difficultyLevel`, `nameEn` are all non-null enums/strings; shared KMM model makes them nullable
- Backend UserProfileResponse has `subscriptionType: String` (non-null); shared KMM model has it as nullable
- Backend PagedResponse has a `page` field; shared KMM PagedResponse does not include it (minor, unused)

**Why:** These mismatches will cause deserialization failures at runtime even though `ignoreUnknownKeys = true` is set, because non-null backend fields mapped to nullable KMM fields will silently default, and field name mismatches (daysCount vs dayCount) will cause silent null deserialization.

**How to apply:** Always verify KMM shared models against backend DTOs field by field before marking API integration tasks complete.

BASE_URL: Centralized as AppConfig.BASE_URL = "https://api.gymgenie.ru:8443" in shared/commonMain (moved in 2026-03-24 refactor). All API classes (AuthApi, UserApi, WorkoutApi, ExerciseApi) use it as constructor default. Old per-class hardcoded URLs (10.0.2.2 and localhost) are gone.

Token storage (2026-03-24 refactor): Android uses SharedPreferences (gymgenie_tokens) via AndroidTokenStorage in shared/androidMain. iOS uses NSUserDefaults via IosTokenStorage in shared/iosMain. Both created via expect/actual `createTokenStorage()`. Android requires `AndroidTokenStorageContext.init(context)` in Application.onCreate() — wired via GymGenieApplication.

Shared ViewModels (2026-03-24 refactor): AuthViewModel, HomeViewModel, WorkoutsViewModel, PaywallViewModel are in shared/commonMain/presentation. AuthViewModel, HomeViewModel, WorkoutsViewModel accept TokenStorage via constructor. Platform-specific ViewModel files are now empty placeholder comments. Android screens either receive VM by parameter (LoginScreen, RegisterScreen get AuthViewModel from App.kt) or create their own via `remember` + `createTokenStorage()` (HomeScreen, WorkoutsScreen, PaywallScreen).

iOS wrappers: Thin ObservableObject wrappers polling shared VM state every 50ms via Task.sleep loop. No business logic in wrappers. Located in iosApp/ViewModels/.

Backend/connectivity notes (identified 2026-03-24):
- server.address is bound to 127.0.0.1 (loopback only) — NOT reachable from Android emulator or physical device without changing bind address.
- `JwtProperties.secret` has a hardcoded default — missing JWT_SECRET env var will silently use insecure default.
- `ddl-auto: update` in use — non-nullable column additions without migration will cause runtime constraint failures.

Known remaining issues as of 2026-03-24:
- `streakDays` in HomeUiState is never populated — UserProfileResponse has no streak field, always stays 0.
- UserApi endpoint `/users/me` and WorkoutApi/ExerciseApi endpoints lack `/api/v1` prefix that AuthApi has. Will cause 404s on real backend unless routing is configured differently.
- iOS polling (50ms loop) is functional but not reactive — state can lag up to 50ms.
- iOS wrapper `deinit` calls `vm.onCleared()` but deinit is not guaranteed on MainActor (low risk in practice).
- Android PaywallViewModel.purchase() race: mock sets purchaseSuccess synchronously so it works, but will silently fail when real async purchase is wired.
- Each API class creates its own HttpClient. Wasteful but acceptable at prototype stage.

Token refresh / logout detection added 2026-03-27:
- `markAsRefreshTokenRequest()` is now the first call in the `refreshTokens` lambda — correct placement.
- `sendWithoutRequest` narrowed to `Url(AppConfig.BASE_URL).host` — correct, avoids sending Bearer to third-party hosts.
- `HomeViewModel` and `WorkoutsViewModel` now take `tokenStorage: TokenStorage` and `onLogout: () -> Unit`; check `getAccessToken() == null` in failure branches to detect post-refresh-failure state. **Critical known issue**: `getAccessToken()` is a `suspend` fun, but is called without `suspend` context — this compiles only because both concrete implementations (Android SharedPreferences, iOS NSUserDefaults) happen to be non-blocking synchronous reads. The interface contract is `suspend` so future async implementations (e.g. Keychain) would break silently. Should be noted as an open risk.
- iOS: `HomeViewModelWrapper` and `WorkoutsViewModelWrapper` expose `@Published isLoggedOut: Bool`; set via `Task { @MainActor in ... }` in the `onLogout` closure — correct threading.
- iOS `ProfileView` observes `homeVM.isLoggedOut` and calls `appState.navigate(to: .login)` — correct.
- Android `ProfileScreen` does NOT have a `DisposableEffect` to call `homeViewModel.onCleared()` — coroutine scope leaks when the screen is destroyed. Open bug.
- Android `onLogout` lambda is set from `App.kt` as `{ currentScreen = Screen.Login }` — this `currentScreen` is a Compose `MutableState` variable captured in the composable lambda. The lambda is called from a coroutine on `Dispatchers.Main` (ViewModel scope). Since `currentScreen` is a Compose state write and the ViewModel runs on `Dispatchers.Main`, this is safe.

New features added 2026-03-26 (workout execution flow + exercise detail + profile + create workout):
- WorkoutSessionViewModel: shared KMM ViewModel, fully self-contained with rest timer and set tracking. Known issue: restTimerJob races against state reads (see findings).
- WorkoutSessionViewModelWrapper: polling-based iOS ObservableObject (50ms). Correct pattern consistent with existing wrappers.
- ExerciseApi (new): ExerciseDetailResponse KMM model drops secondaryMuscleGroups and videoUrl vs backend — intentional subset, safe with ignoreUnknownKeys.
- SQL seed data adds 18 exercises. Uses gen_random_uuid() which requires pgcrypto extension (implicit in most Postgres 13+ setups but not guaranteed in all environments).
- RestTimerView (iOS) and RestTimerScreen (Android): both duplicate "- 10 сек" labels for both adjust buttons — the second button should be "+ 10 сек". This is a UI bug present on both platforms.
- iOS ExerciseDetailView: back button padding top=52 is hardcoded; works for most iPhones but may mis-align on unusual notch sizes. Non-blocking.
- iOS safe area fix: applied via .background(backgroundColor.ignoresSafeArea()) on HomeView and background screens. MainView uses .edgesIgnoringSafeArea(.bottom) for tab bar. Correct approach.
- ProfileView (iOS): instantiates its own HomeViewModelWrapper which starts its own 50ms polling loop — duplicates the HomeScreen's VM instance. Acceptable for stub stage.
- ExerciseDetailScreen (Android): error state is silent (only sets isLoading=false, no error message shown). Non-blocking for prototype.
- CreateWorkoutView (iOS) and CreateWorkoutScreen (Android): "Create" button just calls dismiss()/onBack — does NOT save workout. Acceptable as stub per task scope.

Server-driven subscription added 2026-04-29:
- Backend: AuthUserResponse and UserProfileResponse now both include subscriptionType: String (enum name). generateTokenResponse() maps user.subscriptionType.name — correct.
- activateSubscription() in UserService is @Transactional, calls userRepository.save(user) explicitly — correct (entity is detached after findById in a new @Transactional call).
- UserController endpoint PUT /api/v1/users/me/subscription takes no body — correct, no content-type needed. Ktor client call sends an empty PUT — Spring accepts this without a request body.
- KMM UserResponse has subscriptionType with default "FREE" — safe for any old token that lacks the field.
- AuthViewModel propagates subscriptionType from tokenResponse.user.subscriptionType into AuthUiState on both login and register — correct.
- PaywallViewModel.purchase() always flips purchaseSuccess=true regardless of API result — intentional "paid user should never be blocked" contract, documented in code.
- UserProfileStore.markPremium() is gone — no longer exists. update(profile) replaces it. No leftover references found.
- IS_PREMIUM_KEY / isPremiumKey / DataStore premium writes: fully absent from App.kt and AppState.swift.
- Android App.kt: login routes by authViewModel.state.value.subscriptionType == "PREMIUM" — reads state synchronously after onAuthSuccess, which fires from within the LaunchedEffect that already set the state. Race-free.
- iOS AppState.completeLogin(isPremium:): routes to .main or .paywall correctly. No UserDefaults premium writes present.
- iOS AuthView.onChange(loginSuccess/registerSuccess): passes viewModel.subscriptionType == "PREMIUM" to completeLogin — correct.
- iOS PaywallViewModelWrapper: resolves KoinHelper.shared.getUserApi() and getUserProfileStore() — KoinHelper is a Kotlin object, exposed as .shared in Swift. Correct Kotlin/Native interop.
- PurchaseSuccessScreen (Android): single Column with weight(1f) spacers, no BottomCenter overlay. Image+texts grouped in inner Column with CenterHorizontally. Correct layout.
- PurchaseSuccessView (iOS): VStack with two Spacer() instances flanking content VStack, buttons inside same VStack. Correct layout, no overlay.
- open risk: PaywallViewModelWrapper.startObserving() uses force-cast `as! PaywallUiState` (line 28). AuthViewModelWrapper uses safe cast `as?` with `continue` guard. Inconsistency — if the KMM state type ever changes, the force-cast will crash on iOS. Low risk in practice because the type is fixed, but worth noting.

Auth screen keyboard/scroll/image fix reviewed 2026-05-19:
- Android imePadding is on the outer Box (not the scrollable Column), which is correct: imePadding shrinks the Box, Column fills the shrunk space, image scrolls off top when keyboard opens.
- Android windowSoftInputMode is not set in AndroidManifest (defaults to adjustResize). enableEdgeToEdge() + imePadding() is the correct Compose pattern — no manifest flag needed.
- Android image height computed via BoxWithConstraints + painterResource.intrinsicSize. Correct; painterResource is cached so recomposition cost is negligible.
- Android navigationBarsPadding: NOT on the outer Box — applied via a Spacer at the bottom of the card column. This is correct because it needs to scroll with the content.
- iOS keyboard avoidance: NotificationCenter UIKeyboardWillShow/Hide + .offset(y: -keyboardOffset). No ScrollView. Works for most devices.
- iOS KNOWN OPEN RISK (small screen clipping): on iPhone SE (375×667pt) with a ~336pt keyboard, only ~331pt of vertical space remains for the card. The card content (title + fields + button + divider + social row + mode-switch link + safe bottom padding) totals well over 331pt. The bottom portion (social buttons, mode-switch link) will be clipped above the top edge. No scroll fallback exists. Acceptable tradeoff for the target device range (SE is the only affected size), but worth tracking.
- iOS image area height: safeTop + 16 + maxImageHeight + 32 with .edgesIgnoringSafeArea(.all). safeTop from GeometryReader.safeAreaInsets is correct because edgesIgnoringSafeArea extends the view to fill under the status bar.
- Deleted files (LoginScreen.kt, RegisterScreen.kt, LoginView.swift, RegisterView.swift): zero remaining references confirmed across entire codebase.
- SocialButtonsRow (Android) and SocialIconButton (iOS) migrated into AuthScreen.kt and AuthView.swift respectively. Both are referenced only from their containing file — correct scope.

Home screen state/tab-preservation overhaul added 2026-04-29:
- ScreenState sealed class added at shared/commonMain/ui/ScreenState.kt. Three variants: Loading, Error(message), Content. Used only by HomeViewModel so far; intended as cross-screen contract.
- HomeUiState now carries screenState: ScreenState (replaces isLoading flag), isContentLoaded: Boolean (guards against wiping to full-screen loader on refreshes), isRefreshing: Boolean (pull-to-refresh affordance), and clearTransientError() public function.
- HomeViewModel.load() guards with loadJob?.isActive check (explicit Job field). First load sets screenState=Loading; subsequent loads when isContentLoaded=true fall through to refresh path (isRefreshing=true). load() guard line 74 has a dead condition: `screenState == Loading && isContentLoaded` can never be true simultaneously because Loading is only set when !isContentLoaded. The real concurrent-call guard is `loadJob?.isActive == true` on line 75. Minor dead code, not a runtime bug.
- Android MainScreen: uses TabContent composable (Box with Modifier.size(0.dp) + clipToBounds + pointerInput swallow when hidden). All 4 tabs stay in composition tree simultaneously. Hidden tabs get size 0dp + swallowed gestures. Home secondary stack rendered in a separate HomeSecondaryStack composable. Tab switch re-entering HOME clears homeStack.
- iOS MainView: uses tabView() helper with .opacity(isActive ? 1 : 0) + .allowsHitTesting(isActive) + .accessibilityHidden(!isActive). All 4 views stay in ZStack. Correct back-stack preservation.
- iOS HomeView: refreshable applied only to contentView (inside switch branch). .tint(Palette.coral) scoped to contentView. refreshAndWait() polls isRefreshing flag with 50ms tick; 0.5s start-wait cap and 15s timeout cap to avoid stuck spinner. onAppear guard uses isContentLoaded (not userProfile==nil as before).
- iOS HomeViewModelWrapper: adds screenState (HomeScreenState enum), isContentLoaded, isRefreshing properties. mapScreenState() casts to ScreenStateLoading / ScreenStateError — correct KMM naming convention. Exposes refresh() function. No userProfile property removed from old worktree version (it is still present in the wrapper and used for guard logic).
- Known open issue (2026-04-29): Android HomeScreen LaunchedEffect guard `!state.isContentLoaded && state.screenState != ScreenState.Loading` — the second condition is redundant since isContentLoaded is false only on first load which starts in Loading state; but not harmful.
- Known open issue (2026-04-29): transientError (refresh error banner) in Android HomeContent is displayed indefinitely — clearTransientError() is never called from the UI. The error persists until next successful refresh. No dismiss button provided.
- Known open issue (2026-04-29): iOS counterpart for transient refresh errors is also silent — errorMessage is observed in wrapper but HomeView shows no toast/snackbar for the refresh-error case; the error just goes nowhere since the screen stays in .content state.

AI Workout Flow mobile UI added 2026-04-28:
- AiViewModel (KMM shared): individual field setters (setAge, setHeight, setWeight, setExperience, setFrequency, setHasLimitations, setLimitationsDesc) bridge nicely to iOS where lambda-based updateProfile() is inaccessible. Clean.
- AiFlowScreen (Android Compose): SliderField uses a custom gesture detector (detectHorizontalDragGestures) rather than Material Slider. The `trackWidthPx` is initialized to 1f as default and is set from `size.width` inside the `pointerInput` block — this means it is 1f on first composition until the user touches the slider. However, this only affects the `onDragStart` ratio calculation; `onHorizontalDrag` uses `value` (already correct) so in practice only tap-to-seek would be wrong until first layout. Low risk in practice.
- AiFlowScreen (Android): the `hovered` state in GenerateTypeCard is set on clickable but never set to `true` on press — hover highlight never appears. Minor UI defect, non-blocking.
- iOS AiViewModelWrapper: 50ms polling loop, consistent with existing wrappers. State cast `vm.state.value as? AiUiState` is safe — KMM StateFlow.value is typed AiUiState. Known risk: `@MainActor final class` with `deinit` calling `vm.onCleared()` — deinit is not guaranteed to run on MainActor; vm.onCleared() calls scope.cancel() which is safe from any thread. Acceptable.
- iOS AiCoachView: `default:` case in switch over KMM enum (AiFlowStep) returns EmptyView. This is required because Swift doesn't know KMM enums are exhaustive. Safe.
- iOS AiCoachView: animation transition is always `forwardTransition` (insert=trailing, remove=leading) regardless of direction. Going back will look wrong (slides in from right when it should slide in from left). Tracked as open issue.
- iOS AiCoachView: ForEach in chat uses `id: \.offset` — safe for append-only transcripts but will cause full re-render on any insertion except at the end. Acceptable for this use case.
- healthIssues field is correctly threaded: AiProfileData.healthContext() → sendMessage() → AiChatRequest.healthIssues → POST /api/v1/ai/chat. Backend uses it only on first message (context already established). Correct.
- AiApi.clearSession() uses DELETE /api/v1/ai/chat/session. Backend confirms this endpoint exists via clearSession(). Contract is correct.
- SaveWorkoutRequest fields match backend exactly. estimatedDurationMinutes intentionally excluded (backend drops it). Correct.
- Session reset: reset() sets _state.value = AiUiState() (resets local) and fires clearSession() best-effort. Correct.
- HealthScreen in Compose: `focused` state for textarea border is declared inside the `if` block — each time the block recomposes, focused resets to false. Tracked as open issue (focus highlight may not work correctly on recomposition).

Koin 4.0.0 DI integration added 2026-04-29 (network + profile layer only):
- AppModule.kt (commonMain/di): networkModule (TokenStorage, AuthApi, authenticated HttpClient) + profileModule (UserApi, UserProfileStore). All singletons. Correct.
- KoinInitializer.kt (iosMain/di): `fun initKoin()` with GlobalContext.getOrNull() guard. Object KoinHelper exposes getUserProfileStore() for Swift. Correct.
- iOS calls `KoinInitializerKt.doInitKoin()` — the `doInit` prefix is the standard Kotlin/Native Obj-C name-mangling for top-level functions starting with `init` (confirmed by community KMM/Koin examples).
- Android: AndroidTokenStorageContext.init(this) called BEFORE startKoin{} in GymGenieApplication.onCreate(). Correct order — Android TokenStorage reads context lazily (on first use), so Koin can create the singleton before any read happens.
- `api(libs.koin.core)` used in shared/build.gradle.kts commonMain — correct, avoids requiring composeApp to re-declare koin-core separately.
- `koin-android` added only to composeApp androidMain — correct, not leaked into shared.
- App.kt resolves TokenStorage and UserProfileStore from Koin via GlobalContext.get() inside remember{} — ensures same singleton as GymGenieApplication. AuthViewModel is constructed manually with Koin-resolved tokenStorage, which is correct and consistent.
- HomeViewModelWrapper (iOS) still constructs its own TokenStorage + HttpClient manually (not from Koin). This is intentional — partial migration. The UserProfileStore is now resolved from Koin (correct singleton). Two separate TokenStorage instances exist on iOS: one in Koin, one manually created by each wrapper. NSUserDefaults-backed, so they read/write the same data — functionally correct but architecturally inconsistent. Accepted as known open migration item.
- setProfileStore(_:) on HomeViewModelWrapper: still called from HomeView.onAppear(). The implementation now does store.update(profile: profile) if a profile is already loaded, then swaps sharedProfileStore reference. Since Koin already wired the correct singleton at init, this call is a no-op in steady state. The call site is still present in HomeView — not removed. This is source-compatible and low-risk.
- KoinHelper.shared.getUserProfileStore() in Swift: `object KoinHelper` → `.shared` static property. Correct Kotlin/Native obj-c interop for companion/object.
- AuthApi creates its own internal HttpClient (not the Koin singleton). AuthApi is itself a Koin singleton, so only one internal HttpClient is spawned per process. Correct.
- Multiple HttpClient instances remain: Koin authenticated client (shared by UserApi/UserProfileStore), AuthApi's internal one, and manually-created ones in HomeViewModelWrapper/AiViewModelWrapper/WorkoutsViewModelWrapper (iOS). The profile layer is now clean; the rest is acknowledged open migration debt.

New profile fields + UserProfileStore + replaceWorkout added 2026-04-29:
- New columns on UserEntity: ageYears (Int?), experience (String? len=100), frequency (String? len=100), healthIssues (String? len=2000). Schema managed by ddl-auto:update — acceptable for dev, risky in prod without migration.
- UserProfileStore (KMM): thin MutableStateFlow<UserProfileResponse?> cache. load() swallows errors intentionally. Thread-safe: MutableStateFlow write from Kotlin coroutine is atomic. Android: created in App.kt remember{}, passed down through MainScreen → AiFlowScreen. Correct.
- iOS UserProfileStoreWrapper: creates its own UserApi/HttpClient (does NOT share with HomeViewModelWrapper). Wasteful but consistent with existing pattern. MainView creates it as @StateObject and passes via .environmentObject(). Correct.
- AiViewModel.init{}: pre-fills AiProfileData from UserProfileStore.profile.value snapshot. If profile is loaded, hasLimitations is pre-filled as "Нет" for users who have no health issues. This is correct — HEALTH_NO is the fallback for null/blank healthIssues, enabling the Health screen's proceed button without user interaction. This IS a behavioral shortcut: the user skips a conscious health confirmation. Tracked as known accepted tradeoff (not a bug).
- savedPlanId reset: AiUiState() on reset() clears savedPlanId. Correct. During sendMessage(), isSaved is cleared but savedPlanId is preserved — correct intent (subsequent saves overwrite the same plan).
- replaceWorkout ownership check: plan.user.id != userId — correct. Uses findById (not findByIdAndUserId) first, which means a 404 throws NotFoundException before the ownership check. The two-step approach is correct and clear.
- Cascade delete for replaceWorkout: workoutPlanDayRepository.deleteAllByWorkoutPlan(plan). WorkoutPlanDayEntity has @OneToMany(cascade=[CascadeType.ALL], orphanRemoval=true) on exercises — correct, day deletion cascades to exercises. CRITICAL: deleteAllByWorkoutPlan is a derived-query bulk delete that bypasses JPA cascade. Exercises will NOT be deleted by JPA cascade. This is a data integrity bug — orphan exercises remain in workout_plan_exercises table after day bulk delete.
- iOS AiViewModelWrapper creates its own internal UserProfileStore (empty, never loaded) but pre-fill is done via prefillProfile() called from AiCoachView.onAppear + onChange(profileStore.profile?.id). This correctly avoids requiring env dependency at init time. Pattern is consistent and correct.
- AiViewModelWrapper.prefillProfile(): guarded by didPrefill flag. On re-login, onChange(profileStore.profile?.id) fires only if id changes — edge case: if the same user logs back in, profileStore.profile?.id stays the same and prefill does not re-run. Acceptable.
- Profile fields are re-sent on EVERY chat message (not just first). Backend ignores them after first message because session is non-empty. Wire redundancy is intentional and documented. Correct.
- @Transactional on chat(): covers both the user profile save and session initialization. The profileUpdated flag pattern avoids an unnecessary save() call when no profile fields are provided. Correct.
- ConversationSessionStore.initializeIfEmpty() is @Synchronized — prevents concurrent first-message race. chat() checks isEmpty() outside the lock, then calls initializeIfEmpty() atomically. If two threads pass isEmpty() simultaneously, only one will initialize; the other hits the `if (!initialized)` path and falls through to addMessages(). This is correct TOCTOU handling.

AI workout DTO split + workout-level restSeconds + setWeightsKg fix added 2026-05-17:
- Backend: AiWorkoutExerciseParsedDto (lenient, for GigaChat deserialization, retains nullable reps/restSeconds/setWeightsKg) split from AiWorkoutExerciseDto (strict, mobile-facing, non-null reps, no restSeconds, optional setWeightsKg). This resolves the critical DTO-sharing design issue logged 2026-05-03.
- AiWorkoutDto.restSeconds: Int? = null added at workout level (backend and mobile). Mobile AiWorkoutExerciseDto.restSeconds removed, setWeightsKg: List<Double?>? = null added.
- SaveWorkoutRequest.restSeconds: Int = 60 added (backend and mobile). saveWorkout()/replaceWorkout() now apply workout.restSeconds to every exercise entity — correct uniform rest.
- Fallback chain for workoutRest: workout.restSeconds ?: normalized.firstOrNull()?.restSeconds ?: 60. The middle leg uses per-exercise restSeconds from the parsed (not normalized) exercises. This is a safety net for old-format responses; in normal operation the prompt always produces workout-level restSeconds.
- PROMPT LAYOUT BUG (minor): the restSeconds description line ("restSeconds — время отдыха…") is placed BETWEEN Вариант 1 and Вариант 2 sections in the SYSTEM_PROMPT — not inside the Вариант 2 example. This is structurally ambiguous but the JSON example immediately below it shows restSeconds in the correct workout-level position, so GigaChat should still follow the example correctly.
- No iOS or Android caller references AiWorkoutExerciseDto.restSeconds — field removal causes no compilation break on either platform.
- normalizeAndValidateSetWeights() correctly operates on AiWorkoutExerciseDto (strict, mobile-facing). reconcileAiExerciseSetWeights() correctly operates on AiWorkoutExerciseParsedDto (lenient, AI-facing). No cross-contamination.

GigaChat escapeControlCharsInStrings added 2026-05-03:
- New private function `escapeControlCharsInStrings(json: String)` inserted between markdown-strip and `repairGigaChatJson` in the `chat()` pipeline.
- Walks char-by-char with `inString`/`escaped` flags. Correctly handles `\"`, `\\`, `\uXXXX` pass-through, Cyrillic (code ≥ 0x410 bypasses < 0x20 guard), and already-escaped sequences.
- Pipeline order (strip → escapeControlChars → repairGigaChatJson → parseAiResponse) is correct.
- Residual known issues (pre-existing, not introduced by this fix): `repairGigaChatJson` regex `[^"]*` does not handle `\"` inside the matched string value — structural repair may miss that edge case. Control chars in non-string structural positions are not covered (not a known GigaChat failure mode).

Workout plan detail/edit flow added 2026-05-04:
- Backend WorkoutPlanShortResponse extended: scheduleDays (List<DayOfWeek>), restSeconds (first exercise of first day, default 60), primaryMuscleGroup (most-common across all exercises, nullable), exercisesCount (first day only), totalSets (first day only). These are "first day" heuristics — correct for simple plans, slightly misleading for complex multi-day plans.
- WorkoutPlanService.update() uses saveAndFlush(plan) after plan.days.clear() to force orphan DELETE before re-inserting new days. Correct atomic replace.
- @EntityGraph on findByIdAndUserId now eagerly loads days+exercises+exercise for short-response mapping. N+1 avoided. The same graph is applied to findByUserId (pagination) which triggers HHH90003004 (in-memory pagination warning). Acknowledged in comment; acceptable for small page sizes.
- KMM UpdateWorkoutPlanRequest.scheduleDays: List<String>? — backend expects List<DayOfWeek> (enum). Spring Jackson deserializes DayOfWeek from uppercase strings — correct.
- WorkoutDetailViewModel.load() guard (line 77): `isLoading && plan != null` — this condition is always false on first call (plan is null initially) so the guard is never triggered. The guard reads wrong: it should be `isLoading && plan == null` to prevent double-loading during in-flight requests. The actual concurrent-call protection is implicit in the coroutine scope (no explicit Job field). Duplicate load() calls may fire concurrent runLoad() invocations. Low risk in practice (UI only calls it once), but the guard is logically inverted.
- WorkoutDetailViewModel.saveEdit(): when scheduleType is ONE_TIME, sends scheduleDays=emptyList() explicitly. Backend's UpdateWorkoutPlanRequest.scheduleDays nullable means null=keep-existing, emptyList=replace. Sending emptyList for ONE_TIME is semantically correct.
- WorkoutDetailViewModel.collectScheduleDays(): uses plan.days.map { it.dayOfWeek } — reads all day entities from WorkoutPlanResponse. Correct for view→edit hydration.
- WorkoutDetailScreen: delete icon only shown in view mode (not edit mode) — correct, prevents accidental delete during edit.
- WorkoutDetailScreen: BackHandler in ExercisePickerOverlay fires before the outer BackHandler because it is lower in the composition tree — correct Compose BackHandler stacking behavior.
- MainScreen: WorkoutDetailScreen rendered as a Box overlay above the tab content. onBack bumps workoutsReloadKey unconditionally (even if no edits were made) — minor unnecessary refresh on plain view-only visit. Acceptable.
- onStartPlan from WorkoutDetailScreen: closes the detail screen (selectedWorkoutPlanId = null) before setting activeWorkoutSession — correct sequencing.
- FeaturedWorkoutCard.kt and WorkoutCardSmall.kt confirmed deleted (no longer present).
- WorkoutDetailScreen.ExercisePickerOverlay: instantiates a full CreateWorkoutViewModel just to drive the muscle group list. The save path of CreateWorkoutViewModel is never reached — intentional isolation, no risk.
- WorkoutDetailScreen comment on line 137: "The `runBlocking` here is intentional" — this comment is incorrect/misleading. There is no runBlocking; the navigation call is inside LaunchedEffect which runs as a coroutine. The comment should be removed.

GigaChat prompt engineering update added 2026-05-03 (hallucination reduction):
- Exercise catalog keys renamed: `id`→`exerciseId`, `n`→`name`, `m`→`muscleGroup`, `c`→`category`, `d`→`difficulty`. Change is self-contained in buildContextMessage() — catalog is a Kotlin Map serialized inline, no external parser or DTO touches those keys.
- SYSTEM_PROMPT updated: old `"n"` key reference changed to `"name"` inside the existing ПРАВИЛА block; new КРИТИЧЕСКИЕ ПРАВИЛА ДЛЯ ID УПРАЖНЕНИЙ section with 6 constraints added. No stale abbreviated key references remain.
- A runtime defensive filter already strips hallucinated IDs post-parse (added previously). Prompt rules are an upstream complementary layer.
- No tests break: only one context-loads spring test exists; no unit tests for WorkoutAiService.
- Known open prompt risk: Rule 3 (fallback to nearest exercise) conflicts with Rule 4 (self-check and replace if no exact match) — both are correct but may cause the model to pick a semantically poor fallback rather than omitting the exercise, which is undetectable at runtime. Acceptable tradeoff; runtime filter still strips genuinely hallucinated IDs.

Singleton HttpClient + SessionManager added 2026-05-05:
- Root cause fixed: each screen was calling `createAuthenticatedClient()` independently, creating 6+ HttpClient instances each with its own Ktor BearerTokens cache. When one refreshed, the others raced on the now-invalidated refresh token and signed the user out.
- Fix: `createAuthenticatedClient` is now called exactly once via `AppModule.networkModule` as a Koin `single<HttpClient>`. All API classes (WorkoutApi, ExerciseApi, AiApi, UserApi) now receive the same shared client.
- `SessionManager` is a KMM class (commonMain) with a `MutableSharedFlow(replay=0, extraBufferCapacity=1)`. `triggerLogout()` uses `tryEmit` — non-blocking, never drops because extraBufferCapacity=1. No replay prevents stale logout on fresh subscription. Correct design.
- `AuthenticatedHttpClient.refreshTokens`: reads `tokenStorage.getRefreshToken()` first, falls back to `oldTokens?.refreshToken`. This is intentional — storage is the single source of truth, avoiding race with Ktor's stale in-memory cache.
- `App.kt`: `LaunchedEffect(sessionManager)` collects `logoutEvent`, calls `userProfileStore.clear()` and sets `currentScreen = Screen.Login`. The `userProfileStore.clear()` method exists (confirmed in UserProfileStore.kt).
- Double-logout is idempotent: `SessionManager.triggerLogout()` → `App.kt` sets `Screen.Login`. Simultaneously, a ViewModel's `onLogout` callback also sets `Screen.Login`. Both writes go to the same `MutableState<Screen>` on the main thread — composable recomposition is idempotent.
- No manual `createAuthenticatedClient()` or `AuthApi()` instantiation found anywhere in `composeApp/` — grep confirmed zero matches.
- `ExercisePickerOverlay` (inside WorkoutDetailScreen): now uses `koin.get<ExerciseApi>()` and `koin.get<WorkoutApi>()` — confirmed clean.
- `ExercisePickerScreen` (create_workout): uses `koin.get<WorkoutApi>()` and `koin.get<ExerciseApi>()` — confirmed clean.
- Known open risk carried forward: iOS wrappers (HomeViewModelWrapper, AiViewModelWrapper, WorkoutsViewModelWrapper) still create their own HttpClients manually — NOT migrated in this PR. iOS still has the original multi-client problem. This is an open migration item.
- `AuthApi` owns its own internal HttpClient (not the singleton) and is itself a Koin singleton — correct, only one internal client per process for auth flows.

AI Nutrition meal-planning feature added 2026-05-08:
- Backend: new meal_plans/meals/dishes entity tree (plan→meal→dish). Old nutrition backend (NutritionService/NutritionController/MealPlanDayEntity/MealItemEntity) fully removed. BREAKING: backend no longer serves /api/v1/nutrition/... routes.
- KMM NutritionApi (the old client) is still live in the DI module and HomeViewModel still calls nutritionApi.getActivePlan(), createPlan(), addItemToMeal() — these hit the now-deleted backend and will 404 at runtime. This is a pre-existing HomeViewModel feature, not introduced by this PR, but the backend removal makes it a hard runtime break.
- KMM: AiMealChatRequest.heightCm/weightKg are Int? but backend AiMealChatRequest expects Double?. JSON serialization is Int→Double compatible (no runtime parse error), but fractional values are truncated at the mobile boundary. Low-severity but a type contract divergence.
- MealAiService.repairGigaChatJson: regex anchored to end-of-string ($) — only repairs the very last malformed array element. If GigaChat drops a closing } before an interior array close, the repair is a no-op. Pre-existing approach from workout flow.
- KMM SaveMealPlanRequest.description is non-optional (String), but MealPlanEntity.description is nullable. If the AI returns null description, AiMealPlanData.description defaults to an empty String "" in the KMM model, which persists as an empty string rather than null. Cosmetic inconsistency only.
- No @PreAuthorize or subscription gate on the new /api/v1/ai/meal endpoints — consistent with existing AI chat endpoints.

Android nutrition screens reviewed 2026-05-08:
- AiMealFlowScreen: all step enum names, state fields, and VM method calls verified correct against KMM AiMealViewModel. Step transition direction uses targetState.index vs initialState.index — correct.
- AiMealFlowScreen: tokenStorage parameter is accepted but never used (VM takes only AiMealApi + UserProfileStore via Koin). Dead parameter — low priority but slightly misleading.
- NutritionScreen: MealPlansListViewModel wiring correct. load()/refresh()/loadMore()/deletePlan()/retry() all called correctly. Infinite-scroll via snapshotFlow is correct.
- NutritionScreen: isExpectedEmptyState helper has a tautological condition (errorMessage == null redundant because outer branch already checks errorMessage != null) — functional no-op but confusing.
- NutritionScreen: refresh() on AI coach dismiss is correct — fires against the list VM, not the flow VM.
- NutritionScreen: AnimatedVisibility wrapping AiMealFlowScreen means the flow VM stays alive through exit animation (~300ms) — acceptable, DisposableEffect fires after composition leaves.
- MainScreen: Nutrition added to HomeStackDestination sealed class, rendered in HomeSecondaryStack. BackHandler pops via homeStack.removeAt — correct.
- MainScreen: BackHandler does NOT include the Nutrition overlay in isOverlayActive (that flag is for workout overlays). Nutrition back navigation is handled exclusively via homeStack.isNotEmpty() branch — correct, consistent with Activities/Catalog.
- HomeScreen: onCreateMealPlan threaded from signature → ContentWithPullToRefresh → HomeContent → MealPlanSection.onCreatePlan — correct chain with no breaks.
- All Koin registrations confirmed: AiMealApi and MealPlansApi are singletons in networkModule.

Ktor bearer token cache cleared on logout reviewed 2026-05-18:
- clearBearerTokens() uses plugin(Auth).providers.filterIsInstance<BearerAuthProvider>().firstOrNull()?.clearToken(). The Auth.providers property WAS removed in Ktor 3.0.0 (KTOR-6382) and replaced with the authProvider<T>() extension function in io.ktor.client.plugins.auth. The implementation as written will fail to compile against Ktor 3.1.3 unless Auth.providers was re-added. The idiomatic Ktor 3 API is: client.authProvider<BearerAuthProvider>()?.clearToken(). This is a compilation-breaking bug.
- clearToken() method on BearerAuthProvider confirmed present in Ktor 3.x — correct API to call.
- Android: httpClient.clearBearerTokens() called BEFORE tokenStorage.clearTokens() in logoutEvent collector — correct ordering.
- iOS bridge: clearHttpClientBearerTokens(client:) top-level function in AuthenticatedHttpClient.kt. ObjC name mangling for a top-level function named clearHttpClientBearerTokens in file AuthenticatedHttpClient.kt → AuthenticatedHttpClientKt.clearHttpClientBearerTokens(client:). This is correct Kotlin/Native interop naming.
- KoinHelper.getHttpClient() confirmed present in KoinInitializer.kt line 35. iOS gets the same singleton.
- After clearToken(), the next request triggers loadTokens again — this is how BearerAuthProvider cache works (clearToken sets internal holder to null, next addRequestHeaders call re-invokes loadTokens). Correct: new user's tokens from tokenStorage will be picked up on next request after login.
- Thread safety: clearToken() calls tokensHolder.clearToken() which uses a Mutex internally. If a request is in-flight when clearToken fires, the in-flight request holds its own token snapshot; clearToken takes effect on the next load cycle. This is safe — logout navigation fires immediately after, so no new requests will be made with the old token.
- iOS logout does NOT reset ViewModels beyond what AppState does (no explicit VM reset calls). Acceptable: iOS MainView is torn down on navigate(.login), and all @StateObject VMs are recreated on re-entry to .main.
- No other in-memory user-data caches identified that would survive logout: UserProfileStore.clear() is called, HomeViewModel.reset() called on Android via RootComponent, and iOS gets fresh VMs on re-entry.

Logout/login flow bugfixes reviewed 2026-05-18:
- Bug 1 fixed: AiContent.kt null premium check — `profile?.subscriptionType?.let { it != "FREE" } ?: false`. Correct.
- Bug 2 fixed: AiCoachView.swift null premium check — `guard let sub = ...`. Correct.
- Bug 3 fixed: HomeViewModel.reset() now cancels entire scope and recreates it. `_state` is `val` (MutableStateFlow), separate from scope — not cancelled on scope recreation, still emits. Observers (`collectAsState()`, iOS wrapper polling) remain valid. Correct.
- Bug 4 fixed: DefaultRootComponent.onAuthSuccess() now calls `scope.launch { userProfileStore.load() }` before navigation. This is fire-and-forget: load() is async; navigation fires synchronously on the line below. AiContent reads from UserProfileStore directly, so it will show as locked until load() completes (~one network RTT). Acceptable transient state.
- HomeViewModel.reset() race: scope.cancel() kills in-flight coroutines. Any response arriving after cancel() will have its `_state.update` callbacks run on a dead scope (they execute in the coroutine body after the `fold`), so they become no-ops — correct.
- WorkoutsViewModel is a Koin `factory` (not singleton): each WorkoutsViewModelHolder gets a fresh instance. No stale state on re-login. No reset() needed.
- Other singleton VMs: only HomeViewModel is `single` in viewModelModule. All others are `factory`. HomeViewModel.reset() is the only one needed.
- Profile screen premium check (`profile?.subscriptionType == "PREMIUM"` in Android ProfileScreen.kt and iOS ProfileView.swift): reads from UserProfileStore via `collectAsState()`/polling. After logout+login, clear() sets null, then load() sets new user's profile. The `== "PREMIUM"` check on a nullable String: in Kotlin `null == "PREMIUM"` is false (safe). In Swift `nil == "PREMIUM"` is false (safe). No bug.
- HomeScreen/HomeView `subscriptionType != "FREE"` check: reads from HomeUiState which defaults to "FREE". After reset(), state is HomeUiState() — subscriptionType="FREE" → isPremium=false. Safe.
- iOS logout: MainView is a switch-branch in ContentView. When appState.currentScreen changes to .login, MainView is torn down. @StateObject HomeViewModelWrapper.deinit fires vm.onCleared(). On re-entry to .main, a fresh MainView (and fresh HomeViewModelWrapper with fresh HomeViewModel) is created. No stale state. The Koin singleton UserProfileStore is cleared in AppState.startObservingLogout(). Correct and safe.
- iOS UserProfileStoreWrapper (@StateObject in MainView): also torn down with MainView on logout. On re-entry a fresh wrapper is created. Its `load()` is called from MainView.onAppear. Correct.

Nutrition/workout session improvements reviewed 2026-05-18 (multi-file batch):
- HomeView.swift isoDateString fix: replaced ISO8601DateFormatter(.withFullDate) with DateFormatter("yyyy-MM-dd") — correct, avoids "+03:00" timezone suffix. Missing `locale = Locale(identifier: "en_US_POSIX")` on DateFormatter (same gap in OneOffDateStrip in CreateMealPlanFlowView). Low risk in practice since "yyyy-MM-dd" doesn't use locale-sensitive symbols.
- CreateMealPlanFlowView.swift infinite loading fix: initialLoadCompleted=true now set in applyInitialMealTypeIfNeeded AND in onChange(of: vm.step) — double-cover against 50ms polling race. Correct.
- CreateMealPlanFlowView.swift back navigation fix: EditStep.onBack uses `hasPrefilledInit` (covers both edit + prefilled-create) instead of `editPlanId != nil`. Correct.
- CRITICAL: ManualMealItemRequest.foodProductId remains non-nullable String in shared/ManualMealPlanModels.kt. save() in CreateMealPlanViewModel always maps ALL addedItems to ManualMealItemRequest(foodProductId = it.product.id, grams = it.grams). For AI-generated dishes, product.id = dish.id (a valid UUID string) but NOT a foodProductId in the catalog. The backend now accepts null foodProductId, but the mobile always sends a non-null string (the dish.id). Backend will attempt a catalog lookup for that UUID and throw BadRequestException("Food product not found"). AI-generated dishes CANNOT be resaved because the wire DTO has not been updated to allow null foodProductId on mobile. hasCatalogProduct flag is tracked in AddedMealItem but is never consulted during save() to send null vs non-null. This is an incomplete implementation.
- Backend MealPlanService.kt: correctly handles null foodProductId items as inline AI dishes — backend side is ready. Frontend wire format is the missing piece.
- WorkoutsViewModel.startWorkout(): fetches full plan via getPlanById before creating session. Correctly guards isLoadingSession to prevent concurrent calls. Logout detection (401) wired correctly. iOS wrapper exposes isLoadingSession/pendingSession/sessionError. Both HomeView and WorkoutsView show loading overlay + error alert. Correct and complete.

AI tab paywall overlay + logout state clearing reviewed 2026-05-18:
- Paywall: moved from HomeComponent in-stack push (tab-switching hack) to a SlotNavigation overlay on DefaultMainComponent, mirroring workoutSessionSlot. workoutSessionSlot omits a key= parameter; paywallSlot uses key="paywall_slot" — prevents key collision on the same ComponentContext. Correct.
- PaywallChild is a sealed class with a single data object Active — clean minimal contract.
- handleBackButton=true on paywallSlot: hardware back closes the paywall overlay and returns to whichever tab was active — correct.
- Concurrent slot concern: if both paywallSlot and workoutSessionSlot are active simultaneously (theoretically possible since they are independent SlotNavigations), both overlays will be rendered in MainContent's Box. The workout session renders first, paywall second (drawn on top). In practice this path cannot be reached because the AI tab shows PremiumLockedOverlay (no workout access for free users) and the HomeTab onOpenPaywall is exposed as a passthrough. Low-risk edge case, acceptable.
- openPaywall() calls paywallNavigation.activate(PaywallConfig) unconditionally — if already active, Decompose SlotNavigation.activate re-activates the same config, which is a no-op for childSlot (already active). Safe.
- HomeViewModel.reset(): cancels loadJob and sets _state.value = HomeUiState(). Does NOT cancel the CoroutineScope itself (scope.cancel() is only in onCleared()). All outstanding fetch coroutines launched without storing a Job reference (refresh(), checkIn(), etc.) will continue running after reset(). These are fire-and-forget coroutines; they will update _state after reset() has already written default HomeUiState() — causing a transient state clobber. In practice the window is small (network response arriving during the ~instant of logout transition), but it is a documented race. Accepted as low risk.
- Android logout: tokenStorage.clearTokens() + userProfileStore.clear() + authViewModel.resetState() + homeViewModel.reset() + navigation.replaceAll(Login) — correct and complete.
- iOS logout: AppState.startObservingLogout() calls tokenStorage.clearTokens() + userProfileStore.clear() + navigate(.login). Does NOT call homeViewModel.reset(). This is NOT a bug: iOS HomeViewModelWrapper creates its own Shared.HomeViewModel instance (not the Koin singleton). When .main screen is replaced by .login, HomeView is unmounted, HomeViewModelWrapper deinit fires vm.onCleared(). On re-entry to .main, a fresh HomeViewModelWrapper and HomeViewModel are created. Stale state does not persist on iOS.
- iOS ContentView .onChange(of: appState.currentScreen == .login): calls authViewModel.resetState() to clear the auth form. The .onChange(of: Bool) overload (two-argument form with newValue) is the iOS 15 API. This is correct SwiftUI syntax and is not a SourceKit false positive concern.
- Profile tab paywall inconsistency: ProfileComponent uses in-stack push (ProfileConfig.Paywall) while AI tab now uses the main-level slot overlay. These are two intentionally different navigation patterns: the profile paywall appears as a "page" in the profile stack (same treatment as profile edit screens), while the AI tab paywall is a full-screen overlay that blocks the entire UI. Both are correct for their respective UX flows.

iOS UI parity batch reviewed 2026-05-19 (10 fixes):
- ExerciseFilterSheet: .large detent only — correct.
- Scroll-direction header collapse: thresholds 10/5pt — correct, asymmetric debounce is intentional.
- EditProfileView: profileStore.load() + form.initialized=false on save success — correct.
- ConfirmAccountSheet: .lineLimit(nil) — correct fix.
- ProfileView: sheet height 270→300 — cosmetic, sufficient.
- WorkoutDetailView: Start button added, drag-to-reorder via offset simulation, spacing 8pt, save button full-width — all correct.
- WorkoutBuilderView: drag-to-reorder via offset simulation — correct.
- MuscleGroupPickerView: image-only cell (text removed, scaledToFit + padding(12)) — correct.
- AiCoachView: GymGenieToolbar on each sub-screen, tabBarState.isVisible toggled on step change and onAppear — correct, with one known stale-state risk (see below).
- CreateWorkoutViewModelWrapper: moveExercise implemented client-side (iterate updateExerciseAt) — functionally correct but bypasses KMM atomic moveExercise; WorkoutDetailViewModelWrapper delegates to vm.moveExercise directly (correct).
KNOWN OPEN RISK (2026-05-19): AiCoachView tab bar stale state — if the user is on a non-.choose step and switches away from the AI tab (OS tab switch, not onBack), tabBarState.isVisible remains false and the tab bar stays hidden on the other tab. No .onDisappear reset exists. Low-probability UX bug.
KNOWN OPEN: moveExercise in CreateWorkoutViewModelWrapper does not call vm.moveExercise(fromIndex:toIndex:) — it calls vm.updateExerciseAt for every index, which is N writes instead of 1 atomic move. Functional but inconsistent with how WorkoutDetailViewModelWrapper does it.
drag-to-reorder rowHeight initialized to 68pt (hardcoded), updated only on .onAppear of the first rendered row. If rows differ in height, the drag offset calculation will be off by the delta. Acceptable for uniform-height rows (current design).

AiCoachView ZStack→NavigationStack rewrite reviewed 2026-05-19:
- Each AI flow step now pushed onto the outer NavigationStack (from MainView.tabView) via chained .navigationDestination(isPresented:). Real native push/pop, native swipe-back gesture works.
- Cascade reset via onChange: showProfile=false resets showExperience+showHealth+showChat; showExperience=false resets showHealth+showChat; showHealth=false resets showChat. Prevents stale pushed screens on re-entry.
- tabBarState.isVisible driven by showProfile (onChange + onAppear). onDisappear resets to true (correct tab-switching case).
- KNOWN OPEN ISSUE: vm.reset() is never called when the user back-navigates to the root. If user starts flow, generates a chat, then backs out and re-enters, the VM retains messages/isSaved/savedPlanId/hasWorkout state. AiViewModel.reset() exists and would clear this, but it is not wired to any back-to-root event in the new navigation model. The old ZStack-based model may or may not have reset on back (not verified from diff). Low-impact for typical use but means stale chat state persists across re-entries within the same session.
- KNOWN OPEN ISSUE: UINavigationController+SwipeBack.swift enables swipe-back globally (gestureRecognizerShouldBegin returns true when viewControllers.count > 1). When user swipes back natively on AiProfileScreen, iOS pops the NavigationStack but does NOT call onBack() (the callback only fires on the GymGenieToolbar back button). This means showProfile stays true while the view is gone — until the onChange cascade fires. In SwiftUI NavigationStack, the isPresented binding IS set to false by the system when the stack pops (whether by back button or swipe), so showProfile will be correctly set to false and the cascade will fire. This is safe: SwiftUI NavigationStack contracts guarantee that the binding is updated on programmatic or gesture pop.
- Chained .navigationDestination(isPresented:) attached to child views: This is the documented SwiftUI pattern but carries a known platform caveat — in some iOS 16.x versions, nesting .navigationDestination inside a presented destination caused navigation glitches (double-push, lost pop). iOS 17+ resolved these issues. If the app targets iOS 16, this warrants verification.

Native timer refactor (workout session CPU fix) reviewed 2026-05-19:
- Root cause eliminated: 200ms polling loop in WorkoutSessionViewModelWrapper replaced with event-driven syncState(). Zero polling during exercise/rest phases.
- KMM State: sessionDurationSeconds, restSecondsRemaining, restTotalSeconds removed; restDurationSeconds added. completeSet/cancelWorkout now take platform-supplied durations.
- iOS: restEndDate-based Task.sleep for rest completion (scheduleRestCompletion). Submit-only polling (500ms) fires only after isFinished=true. sessionDurationSeconds is a computed property (Date.timeIntervalSince). No active timer properties remain in KMM State.
- Android: LaunchedEffect(Unit) tick-down loop in RestTimerScreen. Adjustment delta tracking via prevDuration. completeSet passes elapsed from per-set native timer. cancelWorkout computes from sessionStartMillis.
- KNOWN OPEN (Android): LaunchedEffect(Unit) loop in RestTimerScreen captures restRemaining var at launch. The loop reads restRemaining directly each iteration (it is a MutableState so reads are live). However, the loop runs unconditionally and does NOT restart when the set index changes — it's possible for the loop to still be running when `LaunchedEffect(currentExerciseIndex, currentSetIndex)` resets restRemaining. Compose cancels LaunchedEffect(Unit) only on composition exit, not on set change. Result: both the reset AND the running loop write to restRemaining. The loop runs its next decrement after the reset, potentially dropping 1 second off the new countdown. Net effect is at most a 1-second drift on the very first tick after a set transition. Not a correctness bug but a minor timer accuracy issue.
- KNOWN OPEN (iOS): If restEndDate - now < 0 when scheduleRestCompletion fires (e.g. user was in background), remaining is ≤ 0. The guard `if remaining > 0` is present — if negative, Task.sleep is skipped and restComplete() fires immediately. Correct behavior.
- KNOWN OPEN (iOS): completeSet(elapsedSeconds: 0) always passes 0. KMM stores elapsedSeconds nowhere in current State or DB (durationSeconds is always null in persistSet). The parameter is vestigial on iOS. Android correctly passes the per-set elapsed value. This discrepancy is benign for now but the API surface implies the value is used.
- restAdjustCount @Published in WorkoutSessionViewModelWrapper is incremented on every adjustRest call but never observed in RestTimerView. Dead published property — harmless.

RestTimerView CPU fix reviewed 2026-05-19 (second pass — current change):
- TimelineView(.animation) replaced by Task.sleep(1s) countdown + restEndDate-based completion scheduling. Correct fix for the 120fps re-evaluation root cause.
- Two separate Tasks: countdownTask (1s ticks, drives remainingSeconds @State) and restCompletionTask (sleeps exactly until restEndDate, then fires sessionVM.restComplete()). No polling conflict — both are @MainActor-isolated.
- Adjust ±10s: both handlers cancel+reschedule restCompletionTask (via scheduleRestCompletion()) and recalculate remainingSeconds via ceil(restEndDate.timeIntervalSince(Date())). Correct.
- onDisappear cancels both tasks. Correct lifecycle management.
- KNOWN MINOR (iOS): onAppear sets restEndDate then calls scheduleRestCompletion(), then sets remainingSeconds, then calls startCountdown(). The ordering is correct — restEndDate is fully set before either Task reads it.
- KNOWN MINOR (iOS): startCountdown() does NOT cancel+reschedule on adjustRest — it just keeps ticking. After an adjust, countdownTask will read the updated restEndDate on its next iteration and sync remainingSeconds correctly. At worst, one tick is skipped but the ceil() recalculation in the button handler already corrects the displayed value immediately. No visual artifact.
- Android side: untouched. Confirmed correct — only RestTimerView.swift was modified.
- Pattern is fully consistent with ElapsedTimerView.swift (same @State + Task.sleep(@MainActor) idiom).

iOS workout session Android-parity changes reviewed 2026-05-18:
- totalSets bug fixed: `exerciseSummaries` in WorkoutSessionView now uses `setsForExercise.count` (all sets) instead of `nonSkipped.count` for the `sets` field — correctly matches Android `setsForExercise.size`. This was a real correctness bug; skipped sets were disappearing from the summary count.
- ExerciseHeroView info button: frame 36→40pt, icon 18→22pt — matches Android `Modifier.size(40.dp)` / `Modifier.size(22.dp)`. Correct.
- WorkoutControlCard minus button: coral stroke removed, plain `Circle().fill(Color.white)` retained — matches Android `background(Color.White)` (no border). Correct.
- RestTimerView nextExerciseCard info button: frame 36→40pt, icon 18→22pt — matches Android. Correct.
- RestTimerView skipSetButton: inner `.fill(.white)` removed, only gray stroke remains — matches Android `OutlinedButton` (transparent fill). Correct.
- WorkoutSummaryView submissionStatusView: rewritten to match Android SubmissionStatusBanner (13sp, .gray for spinner text, Color(0xFFE53935) for error, outlined retry button with coral). Correct.
- Known remaining issue (pre-existing, not introduced here): iOS RestTimerView nextExerciseCard name logic uses `sessionVM.totalSets` for "remaining within exercise" guard, while Android uses `current.sets`. These should be equivalent if totalSets is sourced from currentExercise.sets — but the iOS path goes through the VM wrapper which exposes `totalSets` as a computed property. Functionally equivalent as long as the wrapper is correct; the pre-existing nextExerciseName bug (showing wrong exercise name mid-exercise) is still present and tracked separately.
- Structural layout divergence (intentional/known): In WorkoutSummaryView, `submissionStatusView` appears AFTER the exercise list. In Android WorkoutSummaryScreen, `SubmissionStatusBanner` appears BEFORE the exercise list (between stat cards and the exercise rows). This divergence was NOT addressed in this change set. It affects UX: on Android the submission status is prominently placed above exercises; on iOS it is buried below. This is a low-priority discrepancy — the component still renders and functions correctly — but is not fully aligned with Android.
- ExerciseHero placeholder icon divergence (pre-existing, platform-appropriate): Android uses `🏋️` emoji in RestTimerScreen's nextExerciseCard circle; iOS uses `Image(systemName: "dumbbell")`. Platform-native choice, not a bug.

Workout session redesign reviewed 2026-05-16:
- Bug fix: ALTER TABLE PendingWorkoutSession ADD COLUMN finished_at INTEGER in try-catch on both Android and iOS DatabaseDriverFactory — correct pattern.
- Android BackHandler: present in both ExerciseScreen and RestTimerScreen — correct.
- iOS back disabled: .toolbar(.hidden), .navigationBarBackButtonHidden(true), .interactiveDismissDisabled(true), .gesture(DragGesture()) all applied on the Group — correct.
- iOS RestTimerView nextExerciseCard name text bug: always shows sessionVM.nextExercise?.exerciseName (exercise after current by index+1), but when resting between sets of the SAME exercise, Android correctly shows current exercise name via nextExerciseName() logic. iOS shows wrong exercise name (or "Финиш") in this case. The iOS info button for the card also opens the wrong exercise's sheet in this scenario (shows the exercise after current, not the current exercise).
- Android weight display: currentWeight.toInt().toString() truncates; iOS uses "%.0f" which rounds. Minor display inconsistency for 2.5 increments (e.g. 62.5 shows "62" Android vs "63" iOS).
- ExerciseInfoSheet Android: koin.get<ExerciseDetailViewModel>() as factory, remember(exerciseId), DisposableEffect cleanup — correct pattern.
- ExerciseInfoSheet iOS: ExerciseDetailViewModelWrapper as @StateObject with onAppear load — correct.
- No comments in any new files — requirement met.
- Component extraction complete: 6 Android components, 4 iOS components all in correct subdirectories.

durationMinutes → secondsPer10Reps rename completed 2026-05-16:
- ExerciseEntity, all 4 ExerciseDtos, ExerciseService, WorkoutPlanDtos, WorkoutPlanService, exercises.sql fully renamed. KMM ExerciseModels and WorkoutModels updated. iOS and Android detail/card screens updated.
- WorkoutPlanShortResponse.estimatedMinutes is now server-computed; clients removed their own client-side formulas.
- WorkoutPlanExerciseResponse.secondsPer10Reps added (nullable) so the detail-screen formula can work when browsing the full plan.
- Remaining durationMinutes references: WorkoutSessionService, WorkoutSessionDtos (backend), WorkoutHistoryScreen/View (mobile), WorkoutSummaryScreen/View (mobile) — all correctly untouched as they refer to actual session elapsed time.
- Formula deviation: backend uses maxOf(1,...), iOS returns 0 when exercises list is empty (returns before formula runs), Android also returns 0 via `return 0` guard. The 0-vs-1 divergence only appears on the empty-exercises edge case; the plan cards always use the server value.

Meal plan UI overhaul reviewed 2026-05-17:
- MealPlanSection.kt: title +1sp (17→?), subtitle +2sp reviewed. Typo "сам" → "само" confirmed fixed at line 329.
- CreateMealPlanFlowScreen.kt: FlowHeader fully removed from Android side (iOS still uses its own SwiftUI FlowHeader — intentionally separate). GymGenieToolbar used on all 4 steps (SETUP, EDIT, PICKER, INFO). No double statusBarsPadding — toolbar carries its own .statusBarsPadding(), no outer padding wrapping it.
- EditStep toolbar: showBackNavigation=true + showCloseIcon=true + onBackClick=onRequestDiscard. This shows X icon and triggers the dialog, not goBack(). Correct.
- BackHandler in CreateMealPlanFlowScreen: SETUP→onDismiss (component::pop), EDIT→showDiscardDialog=true, else→viewModel.goBack(). Correct.
- DismissCreateMealPlanDialog: triggers onDiscardToHome (component::resetToMain) on confirm, onCancel on dismiss. Correct.
- MacroPill: label and grams both 16sp ExtraBold White — unified styling. Correct.
- PlanNameField: "НАЗВАНИЕ" label 13sp Bold DeepInk. TextField textStyle 18sp. pointerInput detectTapGestures clears focus on outer tap. Correct.
- Shared ViewModel (CreateMealPlanViewModel): generatedPlanName() now returns "Завтрак 17 мая" format (space separator, numeric day + monthShort). Old KDoc still says "Завтрак · Сегодня / Обед · Пн, Ср" — stale doc, minor.
- HomeContent.kt: onDiscardToHome = component::resetToMain wired at line 71. Correct.
- EmptyEditState "+" removal: the empty-state CTA button correctly has no "+" prefix (text = "Добавить продукт" only). The AddProductButton (shown when items exist) still has "+" at line 911 — this is the non-empty state add button. User request was to remove "+" from the empty state only — confirmed correct.
- Known stale KDoc: generatedPlanName() still documents "Завтрак · Сегодня" format — should be updated.

Per-day activity scheduling feature reviewed 2026-05-19:
- Backend: UserActivityEntity now mirrors MealPlanEntity scheduling pattern exactly — WorkoutScheduleType enum, @ElementCollection scheduleDays, oneOffDate LocalDate. Null = every day (backward compat). Correct.
- isScheduledFor() in ActivityService is the canonical filter — clean single-responsibility helper.
- normalizeScheduleDays() sorts via DayOfWeek.value before toMutableSet(). Kotlin's toMutableSet() returns LinkedHashSet — insertion order preserved. Sort is cosmetic (affects response ordering, not correctness). Correct.
- History computation in getHistory() correctly applies isScheduledFor() per-day — no regression for backdated activity views.
- Repository JOIN FETCH: LEFT JOIN FETCH ua.scheduleDays is correct for @ElementCollection — avoids N+1 without requiring non-null membership.
- KMM ActivitiesViewModel.updateSchedule() sends scheduleDays.ifEmpty { null } — backend UpdateActivityScheduleRequest has scheduleDays: List<String> = emptyList() default, so null deserialized as emptyList. Safe.
- Android ActivityScheduleSettingsScreen: calls koin.get<ActivitiesViewModel>() — creates a separate factory instance from ActivitiesScreen's instance. This is intentional by design; DefaultHomeComponent.pop() increments activitiesRefreshSignal on ActivityScheduleSettings pop, triggering a server reload in ActivitiesScreen. The `isScheduleUpdating` spinner correctly reflects the in-flight API call for the duration the settings screen is open (own factory instance is fine for this).
- iOS ActivityScheduleSettingsView: uses sheet(item: $scheduleTarget) on ActivityTodayResponse which is Identifiable via @retroactive extension in HomeView.swift. Correct.
- One-time date picker in iOS: uses Date()... range (no past dates). Android uses datePicker.minDate = System.currentTimeMillis(). Consistent behavior.
- Optimistic rollback in ActivitiesViewModel: rolls back to `current` (captured before applySchedule). Correct — current is a val snapshot, not a reference.
- KNOWN ISSUE (minor): the `current` capture in updateSchedule() is done BEFORE applySchedule(), so rollback restores correctly. However, if the user opens settings a second time before the first request completes (race condition), `current` would reflect an already-optimistically-updated state. The refresh-on-back signal from Decompose mitigates this by reloading from server.

GigaChat AI integration added 2026-04-26:
- New `ai` package under `com.asc.gymgenie.ai` in the backend.
- Trust-all SSLContext used for GigaChat-specific RestClient bean — scoped correctly, does not affect default RestClient.
- Auth token cached in-memory with @Synchronized + @Volatile — thread-safe pattern is correct.
- Conversation history in ConcurrentHashMap<UUID, MutableList> — addMessages() is @Synchronized which serializes per-JVM, not per-user. Acceptable for MVP.
- ConversationSessionStore.getHistory() copies list to prevent external mutation — correct.
- GigaChat expiresAt is Unix epoch in **milliseconds** (GigaChat API returns ms). Compared against System.currentTimeMillis() — consistent.
- WorkoutPlanEntity.name has @Column(length=100). GigaChat-generated names could exceed 100 chars — silent truncation or DB error risk.
- saveWorkout always creates a single day with dayOfWeek=MONDAY and name="Тренировка" (hardcoded Russian string). Schema forces a day model that does not fit AI-generated one-off plans well. Acknowledged as MVP tradeoff.
- `estimatedDurationMinutes` in SaveWorkoutRequest is not stored anywhere (WorkoutPlanEntity has no such field) — it is silently discarded on save.
- Jackson duplicate dependency: `tools.jackson.module:jackson-module-kotlin` (Spring Boot 4 BOM) AND `com.fasterxml.jackson.module:jackson-module-kotlin` (legacy). Both resolve to Jackson 2.x com.fasterxml namespace — the tools.jackson groupId is just a coordinate alias in the SB4 BOM. ObjectMapper injection works correctly; the double entry is redundant but harmless.
- `healthIssues` is only used on first message. If sent on subsequent messages it is silently ignored — this is correct behaviour (context already established) but is undocumented at the API level.
- The `chat()` method is not @Transactional — correct, it does not write to DB. `saveWorkout()` is @Transactional — correct.
- No @PreAuthorize or subscription gate on the AI endpoints — any authenticated user can call them. This may or may not be intentional for MVP.
