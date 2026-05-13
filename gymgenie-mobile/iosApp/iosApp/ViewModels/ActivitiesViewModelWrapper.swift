import SwiftUI
import Shared

/// SwiftUI bridge over the KMM `ActivitiesViewModel`.
///
/// Mirrors the polling pattern used by `HomeViewModelWrapper`: a 50ms tick
/// reads the latest `state.value` snapshot and republishes individual fields
/// as `@Published` properties, so SwiftUI views can bind to them without
/// dealing with `Any` casts on every read.
@MainActor
final class ActivitiesViewModelWrapper: ObservableObject {
    private let vm: Shared.ActivitiesViewModel

    @Published private(set) var isLoading: Bool = true
    @Published private(set) var todayActivities: [ActivityTodayResponse] = []
    @Published private(set) var history: [ActivityHistoryDayResponse] = []
    @Published private(set) var isHistoryLoading: Bool = false
    @Published private(set) var error: String? = nil
    @Published private(set) var isLoggedOut: Bool = false

    private var observationTask: Task<Void, Never>?
    private var logoutSubscription: SessionSubscription?

    init() {
        self.vm = Shared.ActivitiesViewModel(
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
                guard let state = self.vm.state.value as? ActivitiesUiState else {
                    try? await Task.sleep(nanoseconds: 50_000_000)
                    continue
                }
                self.isLoading = state.isLoading
                self.todayActivities = state.todayActivities as [ActivityTodayResponse]
                self.history = state.history as [ActivityHistoryDayResponse]
                self.isHistoryLoading = state.isHistoryLoading
                self.error = state.error

                try? await Task.sleep(nanoseconds: 50_000_000)
            }
        }
    }

    func load() { vm.load() }

    func loadHistory(startDate: String, endDate: String) {
        vm.loadHistory(startDate: startDate, endDate: endDate)
    }

    func checkIn(activityId: String, value: Int) {
        vm.checkIn(activityId: activityId, value: Int32(value))
    }

    deinit {
        observationTask?.cancel()
        logoutSubscription?.cancel()
        vm.onCleared()
    }
}
