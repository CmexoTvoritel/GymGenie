import SwiftUI
import Shared

@MainActor
final class ActivityCatalogViewModelWrapper: ObservableObject {
    private let vm: Shared.ActivityCatalogViewModel

    @Published private(set) var isLoading: Bool = true
    @Published private(set) var catalog: [ActivityCatalogResponse] = []
    @Published private(set) var planIds: Set<String> = []
    @Published private(set) var error: String? = nil
    @Published private(set) var isLoggedOut: Bool = false

    private var observationTask: Task<Void, Never>?
    private var logoutSubscription: SessionSubscription?

    init() {
        self.vm = Shared.ActivityCatalogViewModel(
            activityApi: KoinHelper.shared.getActivityApi(),
            sessionManager: KoinHelper.shared.getSessionManager()
        )
        startObserving()
        let sessionManager = KoinHelper.shared.getSessionManager()
        logoutSubscription = sessionManager.observeLogout { [weak self] in
            Task { @MainActor in self?.isLoggedOut = true }
        }
    }

    private func startObserving() {
        observationTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self = self else { break }
                guard let state = self.vm.state.value as? ActivityCatalogUiState else {
                    try? await Task.sleep(nanoseconds: 50_000_000)
                    continue
                }
                self.isLoading = state.isLoading
                self.catalog = state.catalog as [ActivityCatalogResponse]
                self.planIds = Set(state.planIds)
                self.error = state.error

                try? await Task.sleep(nanoseconds: 50_000_000)
            }
        }
    }

    func load() { vm.load() }
    func togglePlan(activityId: String) { vm.togglePlan(activityId: activityId) }

    func addToPlanWithSchedule(
        activityId: String,
        scheduleType: String?,
        scheduleDays: [String],
        oneOffDate: String?,
        goal: Int?
    ) {
        vm.addToPlanWithSchedule(
            activityId: activityId,
            scheduleType: scheduleType,
            scheduleDays: scheduleDays,
            oneOffDate: oneOffDate,
            goal: goal.map { KotlinInt(int: Int32($0)) }
        )
    }

    deinit {
        observationTask?.cancel()
        logoutSubscription?.cancel()
        vm.onCleared()
    }
}
