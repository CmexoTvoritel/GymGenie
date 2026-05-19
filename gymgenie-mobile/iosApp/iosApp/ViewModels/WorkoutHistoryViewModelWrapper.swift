import SwiftUI
import Shared

@MainActor
final class WorkoutHistoryViewModelWrapper: ObservableObject {
    private let vm: Shared.WorkoutHistoryViewModel

    @Published private(set) var selectedDate: Kotlinx_datetimeLocalDate
    @Published private(set) var weekDates: [Kotlinx_datetimeLocalDate] = []
    @Published private(set) var sessions: [WorkoutSessionHistoryItem] = []
    @Published private(set) var weekSessions: [String: [WorkoutSessionHistoryItem]] = [:]
    @Published private(set) var isLoading: Bool = false
    @Published private(set) var isRefreshing: Bool = false
    @Published private(set) var error: String? = nil

    private var observationTask: Task<Void, Never>?

    init() {
        self.vm = KoinHelper.shared.getWorkoutHistoryViewModel()
        self.selectedDate = (vm.state.value as! WorkoutHistoryState).selectedDate
        startObserving()
    }

    private func startObserving() {
        observationTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self = self else { break }
                guard let state = self.vm.state.value as? WorkoutHistoryState else { continue }

                self.selectedDate = state.selectedDate
                self.weekDates = state.weekDates as [Kotlinx_datetimeLocalDate]
                self.sessions = state.sessions as [WorkoutSessionHistoryItem]
                self.isLoading = state.isLoading
                self.isRefreshing = state.isRefreshing
                self.error = state.error

                var mapped: [String: [WorkoutSessionHistoryItem]] = [:]
                let dict = state.weekSessions as NSDictionary
                dict.enumerateKeysAndObjects { key, value, _ in
                    if let k = key as? String,
                       let v = value as? [WorkoutSessionHistoryItem] {
                        mapped[k] = v
                    }
                }
                self.weekSessions = mapped

                try? await Task.sleep(nanoseconds: 50_000_000)
            }
        }
    }

    func selectDate(_ date: Kotlinx_datetimeLocalDate) {
        vm.selectDate(date: date)
    }

    func selectSwiftDate(_ date: Date) {
        let local = Self.toLocalDate(date)
        vm.selectDate(date: local)
    }

    func shiftWeek(_ offset: Int) {
        vm.shiftWeek(offset: Int32(offset))
    }

    func refresh() {
        vm.refresh()
    }

    func refreshAsync() async {
        vm.refresh()
        try? await Task.sleep(nanoseconds: 100_000_000)
        while isRefreshing {
            try? await Task.sleep(nanoseconds: 50_000_000)
        }
    }

    static func toLocalDate(_ date: Date) -> Kotlinx_datetimeLocalDate {
        let components = Calendar.current.dateComponents([.year, .month, .day], from: date)
        return Kotlinx_datetimeLocalDate(
            year: Int32(components.year ?? 1970),
            monthNumber: Int32(components.month ?? 1),
            dayOfMonth: Int32(components.day ?? 1)
        )
    }

    static func toSwiftDate(_ local: Kotlinx_datetimeLocalDate) -> Date {
        var components = DateComponents()
        components.year = Int(local.year)
        components.month = Int(local.monthNumber)
        components.day = Int(local.dayOfMonth)
        return Calendar.current.date(from: components) ?? Date()
    }

    static func localDateString(_ local: Kotlinx_datetimeLocalDate) -> String {
        return "\(local.year)-\(String(format: "%02d", local.monthNumber))-\(String(format: "%02d", local.dayOfMonth))"
    }

    deinit {
        observationTask?.cancel()
        vm.onCleared()
    }
}
