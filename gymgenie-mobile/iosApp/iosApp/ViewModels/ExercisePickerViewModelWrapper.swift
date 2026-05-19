import SwiftUI
import Shared

/// Swift-side bridge over the shared `ExercisePickerViewModel`.
///
/// A distinct, short-lived wrapper used inside the create-workout flow. It
/// deliberately does NOT reuse `WorkoutsViewModelWrapper` so browsing the
/// catalog while composing a workout cannot clobber the user's previous
/// catalog filter / search state.
@MainActor
final class ExercisePickerViewModelWrapper: ObservableObject {
    private let vm: Shared.ExercisePickerViewModel

    @Published private(set) var exercises: [Shared.ExerciseShortResponse] = []
    @Published private(set) var isLoading: Bool = false
    @Published private(set) var isLoadingMore: Bool = false
    @Published private(set) var hasMore: Bool = true
    @Published private(set) var errorMessage: String? = nil

    private var observationTask: Task<Void, Never>?

    init() {
        self.vm = Shared.ExercisePickerViewModel(
            exerciseApi: KoinHelper.shared.getExerciseApi()
        )
        startObserving()
    }

    private func startObserving() {
        observationTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self = self else { break }
                guard let state = self.vm.state.value as? Shared.ExercisePickerUiState else { continue }
                self.exercises = state.exercises as [Shared.ExerciseShortResponse]
                self.isLoading = state.isLoading
                self.isLoadingMore = state.isLoadingMore
                self.hasMore = state.hasMore
                self.errorMessage = state.errorMessage

                try? await Task.sleep(nanoseconds: 50_000_000) // 50ms
            }
        }
    }

    func load(muscleGroup: String) {
        vm.load(muscleGroup: muscleGroup)
    }

    func loadMore() {
        vm.loadMore()
    }

    func retry() {
        vm.retry()
    }

    func clearError() {
        vm.clearError()
    }

    deinit {
        observationTask?.cancel()
        vm.onCleared()
    }
}
