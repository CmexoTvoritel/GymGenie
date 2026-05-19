import SwiftUI
import Shared

@MainActor
final class ActivitiesViewModelWrapper: ObservableObject {
    private let vm: Shared.ActivitiesViewModel

    @Published private(set) var isLoading: Bool = true
    @Published private(set) var todayActivities: [ActivityTodayResponse] = []
    @Published private(set) var history: [ActivityHistoryDayResponse] = []
    @Published private(set) var isHistoryLoading: Bool = false
    @Published private(set) var error: String? = nil
    @Published private(set) var isScheduleUpdating: Bool = false
    @Published private(set) var scheduleUpdateError: String? = nil
    @Published private(set) var isLoggedOut: Bool = false
    @Published private(set) var selectedDate: String = {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .iso8601)
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f.string(from: Date())
    }()

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
                self.selectedDate = state.selectedDate as String
                self.isScheduleUpdating = state.isScheduleUpdating
                self.scheduleUpdateError = state.scheduleUpdateError

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

    func selectDate(date: String) {
        vm.selectDate(date: date)
    }

    func updateSchedule(
        activityId: String,
        scheduleType: String?,
        scheduleDays: [String],
        oneOffDate: String?
    ) {
        vm.updateSchedule(
            activityId: activityId,
            scheduleType: scheduleType,
            scheduleDays: scheduleDays,
            oneOffDate: oneOffDate
        )
    }

    func clearScheduleUpdateError() {
        vm.clearScheduleUpdateError()
    }

    deinit {
        observationTask?.cancel()
        logoutSubscription?.cancel()
        vm.onCleared()
    }
}
