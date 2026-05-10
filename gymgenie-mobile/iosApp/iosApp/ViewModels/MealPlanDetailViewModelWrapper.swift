import SwiftUI
import Shared

/// Swift-side bridge for [Shared.MealPlanDetailViewModel].
///
/// Owns the saved meal plan's id at construction time so the underlying KMM
/// presenter does not need a separate "set id" intent. The view triggers
/// `load()` on first appear; `retry()` re-runs the same fetch on demand.
@MainActor
final class MealPlanDetailViewModelWrapper: ObservableObject {
    private let vm: Shared.MealPlanDetailViewModel

    @Published private(set) var isLoading: Bool = false
    @Published private(set) var plan: MealPlanDetail? = nil
    @Published private(set) var errorMessage: String? = nil

    private var observationTask: Task<Void, Never>?

    init(planId: String) {
        self.vm = Shared.MealPlanDetailViewModel(
            mealPlansApi: KoinHelper.shared.getMealPlansApi(),
            planId: planId
        )
        startObserving()
    }

    private func startObserving() {
        observationTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self else { break }
                if let state = self.vm.state.value as? MealPlanDetailUiState {
                    self.isLoading = state.isLoading
                    self.plan = state.plan
                    self.errorMessage = state.errorMessage
                }
                try? await Task.sleep(nanoseconds: 50_000_000)
            }
        }
    }

    func load() { vm.load() }
    func retry() { vm.retry() }

    deinit {
        observationTask?.cancel()
        vm.onCleared()
    }
}
