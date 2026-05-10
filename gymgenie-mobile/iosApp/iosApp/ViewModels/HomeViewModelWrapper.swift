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
    @Published private(set) var isLoadingMealPlans: Bool = false
    @Published private(set) var userProfile: UserProfileResponse? = nil
    @Published private(set) var isLoggedOut: Bool = false

    private var observationTask: Task<Void, Never>?

    /// Reference to the app-scoped profile store, kept for legacy
    /// `setProfileStore` callers and to bridge fresh profile snapshots into
    /// the singleton from the home update loop. Now resolved from Koin at
    /// init time, so it is the same instance the rest of the app sees.
    private var sharedProfileStore: Shared.UserProfileStore? = nil

    init() {
        let profileStore = KoinHelper.shared.getUserProfileStore()
        self.sharedProfileStore = profileStore
        weak var weakSelf: HomeViewModelWrapper?
        self.vm = Shared.HomeViewModel(
            userApi: KoinHelper.shared.getUserApi(),
            workoutApi: KoinHelper.shared.getWorkoutApi(),
            activityApi: KoinHelper.shared.getActivityApi(),
            mealPlansApi: KoinHelper.shared.getMealPlansApi(),
            tokenStorage: KoinHelper.shared.getTokenStorage(),
            userProfileStore: profileStore,
            onLogout: {
                Task { @MainActor in weakSelf?.isLoggedOut = true }
            }
        )
        weakSelf = self
        startObserving()
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
                self.isLoadingMealPlans = state.isLoadingMealPlans
                self.userProfile = state.userProfile

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

    func logout() {
        Task {
            let tokenStorage = KoinHelper.shared.getTokenStorage()
            try? await tokenStorage.clearTokens()
            sharedProfileStore?.clear()
            isLoggedOut = true
        }
    }

    deinit {
        observationTask?.cancel()
        vm.onCleared()
    }
}
