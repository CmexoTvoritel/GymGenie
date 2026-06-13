import SwiftUI
import Shared

@MainActor
final class MealPlanDetailViewModelWrapper: ObservableObject {
    private let vm: Shared.MealPlanDetailViewModel

    @Published private(set) var isLoading: Bool = false
    @Published private(set) var plan: MealPlanDetail? = nil
    @Published private(set) var errorMessage: String? = nil
    @Published private(set) var isDeleting: Bool = false
    @Published private(set) var isDeleted: Bool = false

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
                    self.isDeleting = state.isDeleting
                    self.isDeleted = state.isDeleted
                }
                try? await Task.sleep(nanoseconds: 50_000_000)
            }
        }
    }

    func load() { vm.load() }
    func retry() { vm.retry() }
    func delete() { vm.delete() }

    deinit {
        observationTask?.cancel()
        vm.onCleared()
    }
}
