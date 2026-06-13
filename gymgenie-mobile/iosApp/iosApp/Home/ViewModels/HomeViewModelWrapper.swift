import SwiftUI
import Shared

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
    @Published private(set) var name: String = ""
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

        let sessionManager = KoinHelper.shared.getSessionManager()
        logoutSubscription = sessionManager.observeLogout { [weak self] in
            Task { @MainActor in self?.isLoggedOut = true }
        }
    }

    func setProfileStore(_ store: Shared.UserProfileStore) {
        sharedProfileStore = store
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
                self.name = state.name
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

                try? await Task.sleep(nanoseconds: 50_000_000)
            }
        }
    }

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

    func refreshMealPlans() {
        vm.refreshMealPlans()
    }

    func retry() {
        vm.retry()
    }

    func clearTransientError() {
        vm.clearTransientError()
    }

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

    func removeFromPlan(activityId: String) {
        vm.removeFromPlan(activityId: activityId)
    }

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

        KoinHelper.shared.getSessionManager().triggerLogout()
    }

    deinit {
        observationTask?.cancel()
        logoutSubscription?.cancel()
        vm.onCleared()
    }
}
