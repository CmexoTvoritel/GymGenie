import Foundation
import Shared

/// SwiftUI-facing bridge for the shared `UserProfileStore`.
///
/// Mirrors the polling pattern used by other ViewModel wrappers: a 50ms tick
/// reads the latest snapshot from the underlying KMM `StateFlow` and surfaces
/// it as `@Published` state so SwiftUI views can observe it.
///
/// The underlying store is resolved from the shared Koin container, so all
/// wrappers (and any KMM presenter that asks for `UserProfileStore`) share
/// exactly one instance per process.
@MainActor
final class UserProfileStoreWrapper: ObservableObject {
    let store: Shared.UserProfileStore

    @Published private(set) var profile: UserProfileResponse? = nil

    private var observationTask: Task<Void, Never>?

    init() {
        self.store = KoinHelper.shared.getUserProfileStore()
        startObserving()
    }

    /// Best-effort refresh from the backend. Errors are swallowed inside the
    /// shared store; callers needing user-visible failure handling should keep
    /// using the per-screen `HomeViewModel` instead.
    func load() {
        Task { try? await store.load() }
    }

    private func startObserving() {
        observationTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self else { break }
                self.profile = self.store.profile.value as? UserProfileResponse
                try? await Task.sleep(nanoseconds: 50_000_000)
            }
        }
    }

    deinit {
        observationTask?.cancel()
    }
}
