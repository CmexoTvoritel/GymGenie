import SwiftUI
import Shared

@MainActor
final class PaywallViewModelWrapper: ObservableObject {
    private let vm: Shared.PaywallViewModel

    @Published private(set) var selectedPlan: Shared.PlanType = .yearly
    @Published private(set) var isPurchasing: Bool = false
    @Published private(set) var purchaseSuccess: Bool = false

    private var observationTask: Task<Void, Never>?

    init() {
        let userApi = KoinHelper.shared.getUserApi()
        let userProfileStore = KoinHelper.shared.getUserProfileStore()
        self.vm = Shared.PaywallViewModel(
            userApi: userApi,
            userProfileStore: userProfileStore
        )
        startObserving()
    }

    private func startObserving() {
        observationTask = Task { [weak self] in
            guard let self = self else { return }
            while !Task.isCancelled {
                let state = self.vm.state.value as! PaywallUiState
                self.selectedPlan = state.selectedPlan
                self.isPurchasing = state.isPurchasing
                self.purchaseSuccess = state.purchaseSuccess

                try? await Task.sleep(nanoseconds: 50_000_000) // 50ms
            }
        }
    }

    func selectPlan(_ plan: Shared.PlanType) {
        vm.selectPlan(plan: plan)
    }

    func purchase() {
        vm.purchase()
    }

    func consumePurchaseSuccess() {
        vm.consumePurchaseSuccess()
    }

    deinit {
        observationTask?.cancel()
        vm.onCleared()
    }
}
