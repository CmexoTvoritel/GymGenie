import Foundation
import Shared

@MainActor
final class UserProfileStoreWrapper: ObservableObject {
    let store: Shared.UserProfileStore

    @Published private(set) var profile: UserProfileResponse? = nil

    private var observationTask: Task<Void, Never>?

    init() {
        self.store = KoinHelper.shared.getUserProfileStore()
        startObserving()
    }

    func load() {
        Task { try? await store.load() }
    }

    func refresh() {
        profile = store.profile.value as? UserProfileResponse
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
