import SwiftUI
import Shared

/// Swift mirror of the shared `ScreenState` sealed class.
///
/// Mapping happens in the observation loop so the rest of the SwiftUI layer
/// can rely on a strongly-typed enum instead of casting `Any` payloads.
enum HomeScreenState: Equatable {
    case loading
    case error(String)
    case content
}

@MainActor
final class HomeViewModelWrapper: ObservableObject {
    private let vm: Shared.HomeViewModel

    @Published private(set) var screenState: HomeScreenState = .loading
    @Published private(set) var isContentLoaded: Bool = false
    @Published private(set) var isRefreshing: Bool = false
    @Published private(set) var errorMessage: String? = nil
    @Published private(set) var username: String = ""
    @Published private(set) var subscriptionType: String = "FREE"
    @Published private(set) var streakDays: Int32 = 0
    @Published private(set) var activeWorkoutPlans: [WorkoutPlanShortResponse] = []
    @Published private(set) var todayActivities: [ActivityTodayResponse] = []
    @Published private(set) var todayMealPlans: [TodayMealPlanCard] = []
    @Published private(set) var selectedMealDate: Date = Calendar.current.startOfDay(for: Date())
    @Published private(set) var isLoadingMealPlans: Bool = false
    @Published private(set) var userProfile: UserProfileResponse? = nil
    @Published private(set) var isLoadingSession: Bool = false
    @Published private(set) var pendingSession: ActiveWorkoutSession? = nil
    @Published private(set) var sessionError: String? = nil
    @Published private(set) var activityError: String? = nil
    @Published private(set) var isLoggedOut: Bool = false

    private var observationTask: Task<Void, Never>?
    private var logoutSubscription: SessionSubscription?

    /// Reference to the app-scoped profile store, kept for legacy
    /// `setProfileStore` callers and to bridge fresh profile snapshots into
    /// the singleton from the home update loop. Now resolved from Koin at
    /// init time, so it is the same instance the rest of the app sees.
    private var sharedProfileStore: Shared.UserProfileStore? = nil

    init() {
        let profileStore = KoinHelper.shared.getUserProfileStore()
        self.sharedProfileStore = profileStore
        self.vm = Shared.HomeViewModel(
            userApi: KoinHelper.shared.getUserApi(),
            workoutApi: KoinHelper.shared.getWorkoutApi(),
            activityApi: KoinHelper.shared.getActivityApi(),
            mealPlansApi: KoinHelper.shared.getMealPlansApi(),
            tokenStorage: KoinHelper.shared.getTokenStorage(),
            userProfileStore: profileStore,
            sessionManager: KoinHelper.shared.getSessionManager(),
            pendingSessionUploader: KoinHelper.shared.getPendingSessionUploader()
        )
        startObserving()

        // Mirrors the Android `App.kt` listener: every `SessionManager`
        // emission flips `isLoggedOut` so SwiftUI surfaces can route back
        // to login. Both forced (network 401) and explicit (Profile screen)
        // logouts now flow through a single signal.
        let sessionManager = KoinHelper.shared.getSessionManager()
        logoutSubscription = sessionManager.observeLogout { [weak self] in
            Task { @MainActor in self?.isLoggedOut = true }
        }
    }

    /// Source-compatibility shim — callers (e.g. `HomeView.onAppear`) still
    /// invoke this, but both `init` and every caller resolve the same Koin
    /// singleton, so this is always a no-op. Do NOT rely on passing a different
    /// store here expecting a behavior change; it will have no effect on the
    /// underlying `HomeViewModel`, which is already bound to the Koin singleton.
    func setProfileStore(_ store: Shared.UserProfileStore) {
        sharedProfileStore = store
        if let profile = userProfile {
            store.update(profile: profile)
        }
    }

    private func startObserving() {
        observationTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self = self else { break }
                guard let state = self.vm.state.value as? HomeUiState else { continue }

                self.screenState = Self.mapScreenState(state.screenState)
                self.isContentLoaded = state.isContentLoaded
                self.isRefreshing = state.isRefreshing
                self.errorMessage = state.errorMessage
                self.username = state.username
                self.subscriptionType = state.subscriptionType
                self.streakDays = state.streakDays
                self.activeWorkoutPlans = state.activeWorkoutPlans as [WorkoutPlanShortResponse]
                self.todayActivities = state.todayActivities as [ActivityTodayResponse]
                self.todayMealPlans = state.todayMealPlans as [TodayMealPlanCard]
                self.selectedMealDate = Self.toDate(state.selectedMealDate)
                self.isLoadingMealPlans = state.isLoadingMealPlans
                self.userProfile = state.userProfile
                self.isLoadingSession = state.isLoadingSession
                self.pendingSession = state.pendingSession
                self.sessionError = state.sessionError
                self.activityError = state.activityError

                if let profile = state.userProfile {
                    self.sharedProfileStore?.update(profile: profile)
                }

                try? await Task.sleep(nanoseconds: 50_000_000) // 50ms
            }
        }
    }

    /// Maps the KMM `ScreenState` sealed hierarchy into the local Swift enum.
    /// Kotlin sealed objects are exposed as singletons in Swift, while
    /// `ScreenState.Error` is a regular class with a `message` property.
    private static func mapScreenState(_ state: ScreenState) -> HomeScreenState {
        if state.isLoading { return .loading }
        if let error = state.asError { return .error(error.message) }
        return .content
    }

    func loadData() {
        vm.load()
    }

    func refresh() {
        vm.refresh()
    }

    /// Targeted reload of just the meal-plan section. Triggered by SwiftUI
    /// after the manual creation flow saves successfully — rerunning the full
    /// dashboard load would also refetch the workout/activity panes for no
    /// reason.
    func refreshMealPlans() {
        vm.refreshMealPlans()
    }

    func retry() {
        vm.retry()
    }

    func clearTransientError() {
        vm.clearTransientError()
    }

    /// Forwards an activity check-in to the underlying KMM view model.
    ///
    /// The KMM side performs the optimistic update on `state.todayActivities`
    /// before the network call resolves, so the SwiftUI layer sees the new
    /// value on the next 50ms observation tick — no additional plumbing
    /// is needed here.
    func checkIn(activityId: String, value: Int) {
        vm.checkIn(activityId: activityId, value: Int32(value))
    }

    func startWorkout(planId: String, planName: String) {
        vm.startWorkout(planId: planId, planName: planName)
    }

    func clearPendingSession() {
        vm.clearPendingSession()
    }

    func clearActivityError() {
        vm.clearActivityError()
    }

    /// Switches the meal-plan section to a different calendar day.
    ///
    /// SwiftUI works with `Date`; the KMM presenter expects
    /// `kotlinx.datetime.LocalDate`. The mapping lives here so call sites
    /// remain free of KMM date types.
    func selectMealDate(_ date: Date) {
        let local = Self.toLocalDate(date)
        vm.selectMealDate(date: local)
    }

    private static func toLocalDate(_ date: Date) -> Kotlinx_datetimeLocalDate {
        let components = Calendar.current.dateComponents([.year, .month, .day], from: date)
        return Kotlinx_datetimeLocalDate(
            year: Int32(components.year ?? 1970),
            monthNumber: Int32(components.month ?? 1),
            dayOfMonth: Int32(components.day ?? 1)
        )
    }

    private static func toDate(_ local: Kotlinx_datetimeLocalDate) -> Date {
        var components = DateComponents()
        components.year = Int(local.year)
        components.month = Int(local.monthNumber)
        components.day = Int(local.dayOfMonth)
        return Calendar.current.date(from: components) ?? Date()
    }

    func logout() {
        // Token clearance and profile-store reset are now centralised in
        // the SessionManager listener at the App level. This method only
        // signals the intent — the same path used for forced (network 401)
        // logouts.
        KoinHelper.shared.getSessionManager().triggerLogout()
    }

    deinit {
        observationTask?.cancel()
        logoutSubscription?.cancel()
        vm.onCleared()
    }
}
