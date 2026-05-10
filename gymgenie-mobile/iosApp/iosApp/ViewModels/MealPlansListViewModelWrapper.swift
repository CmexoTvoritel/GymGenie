import SwiftUI
import Shared

/// Swift-side bridge for [Shared.MealPlansListViewModel].
///
/// Drives the saved meal plans list (`NutritionView`). The polling pattern
/// matches the other wrappers in this project (50ms tick on the main actor)
/// so the iOS app stays consistent with the workouts/AI flows.
@MainActor
final class MealPlansListViewModelWrapper: ObservableObject {
    private let vm: Shared.MealPlansListViewModel

    @Published private(set) var isLoading: Bool = false
    @Published private(set) var isLoadingMore: Bool = false
    @Published private(set) var isRefreshing: Bool = false
    @Published private(set) var plans: [MealPlanShortInfo] = []
    @Published private(set) var plansLoaded: Bool = false
    @Published private(set) var hasMore: Bool = true
    @Published private(set) var errorMessage: String? = nil
    @Published private(set) var deletingPlanId: String? = nil

    private var observationTask: Task<Void, Never>?

    init() {
        self.vm = Shared.MealPlansListViewModel(
            mealPlansApi: KoinHelper.shared.getMealPlansApi(),
            pageSize: 20
        )
        startObserving()
    }

    private func startObserving() {
        observationTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self else { break }
                if let state = self.vm.state.value as? MealPlansListUiState {
                    self.isLoading = state.isLoading
                    self.isLoadingMore = state.isLoadingMore
                    self.isRefreshing = state.isRefreshing
                    self.plansLoaded = state.plansLoaded
                    self.hasMore = state.hasMore
                    self.errorMessage = state.errorMessage
                    self.deletingPlanId = state.deletingPlanId
                    if let list = state.plans as? [MealPlanShortInfo] {
                        self.plans = list
                    } else {
                        self.plans = (state.plans as NSArray).compactMap { $0 as? MealPlanShortInfo }
                    }
                }
                try? await Task.sleep(nanoseconds: 50_000_000)
            }
        }
    }

    func load() { vm.load() }
    func loadMore() { vm.loadMore() }
    func refresh() { vm.refresh() }
    func retry() { vm.retry() }
    func delete(planId: String) { vm.deletePlan(planId: planId) }
    func clearError() { vm.clearError() }

    deinit {
        observationTask?.cancel()
        vm.onCleared()
    }
}
