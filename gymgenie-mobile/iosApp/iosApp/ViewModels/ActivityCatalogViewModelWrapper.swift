import SwiftUI
import Shared

/// SwiftUI bridge over the KMM `ActivityCatalogViewModel`.
///
/// Same observation contract as `ActivitiesViewModelWrapper` — see that file
/// for the rationale behind the 50ms polling loop.
@MainActor
final class ActivityCatalogViewModelWrapper: ObservableObject {
    private let vm: Shared.ActivityCatalogViewModel

    @Published private(set) var isLoading: Bool = true
    @Published private(set) var catalog: [ActivityCatalogResponse] = []
    @Published private(set) var planIds: Set<String> = []
    @Published private(set) var error: String? = nil
    @Published private(set) var isLoggedOut: Bool = false

    private var observationTask: Task<Void, Never>?

    init() {
        weak var weakSelf: ActivityCatalogViewModelWrapper?
        self.vm = Shared.ActivityCatalogViewModel(
            activityApi: KoinHelper.shared.getActivityApi(),
            onLogout: {
                Task { @MainActor in weakSelf?.isLoggedOut = true }
            }
        )
        weakSelf = self
        startObserving()
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
                // Kotlin `Set<String>` lands on the Swift side as `Set<AnyHashable>`
                // — convert it explicitly so consumers get a typed `Set<String>`.
                self.planIds = Set(state.planIds.compactMap { $0 as? String })
                self.error = state.error

                try? await Task.sleep(nanoseconds: 50_000_000)
            }
        }
    }

    func load() { vm.load() }
    func togglePlan(activityId: String) { vm.togglePlan(activityId: activityId) }

    deinit {
        observationTask?.cancel()
        vm.onCleared()
    }
}
