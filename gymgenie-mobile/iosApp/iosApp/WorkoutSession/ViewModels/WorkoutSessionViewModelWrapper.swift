import SwiftUI
import Shared

@MainActor
final class WorkoutSessionViewModelWrapper: ObservableObject {
    let vm: WorkoutSessionViewModel
    let sessionStartDate: Date = Date()

    @Published private(set) var phase: WorkoutSessionViewModel.Phase = WorkoutSessionViewModel.Phase.exercise
    @Published private(set) var currentExerciseIndex: Int32 = 0
    @Published private(set) var currentSetIndex: Int32 = 0
    @Published private(set) var restDurationSeconds: Int32 = 0
    @Published private(set) var currentWeight: Double = 60.0
    @Published private(set) var currentReps: Int32 = 12
    @Published private(set) var isFinished: Bool = false
    @Published private(set) var currentExercise: ActiveExercise? = nil
    @Published private(set) var nextExercise: ActiveExercise? = nil
    @Published private(set) var totalSets: Int32 = 0
    @Published private(set) var displaySetNumber: Int32 = 1
    @Published private(set) var totalExercises: Int32 = 0
    @Published private(set) var requiresWeight: Bool = false
    @Published private(set) var isSubmitting: Bool = false
    @Published private(set) var isSubmitted: Bool = false
    @Published private(set) var submitError: String? = nil

    private var submitObservationTask: Task<Void, Never>?

    var sessionDurationSeconds: Int {
        Int(Date().timeIntervalSince(sessionStartDate))
    }

    init(
        session: ActiveWorkoutSession,
        localRepository: LocalWorkoutRepository
    ) {
        self.vm = WorkoutSessionViewModel(
            session: session,
            localRepository: localRepository,
            workoutApi: KoinHelper.shared.getWorkoutApi(),
            sessionIdSeed: Self.generateSessionId()
        )
        syncState()
    }

    private func syncState() {
        guard let state = vm.state.value as? WorkoutSessionViewModel.State else { return }
        if self.phase != state.phase { self.phase = state.phase }
        if self.currentExerciseIndex != state.currentExerciseIndex { self.currentExerciseIndex = state.currentExerciseIndex }
        if self.currentSetIndex != state.currentSetIndex { self.currentSetIndex = state.currentSetIndex }
        if self.restDurationSeconds != state.restDurationSeconds { self.restDurationSeconds = state.restDurationSeconds }
        if self.currentWeight != state.currentWeight { self.currentWeight = state.currentWeight }
        if self.currentReps != state.currentReps { self.currentReps = state.currentReps }
        if self.isFinished != state.isFinished { self.isFinished = state.isFinished }
        if self.currentExercise?.exerciseId != state.currentExercise?.exerciseId { self.currentExercise = state.currentExercise }
        if self.nextExercise?.exerciseId != state.nextExercise?.exerciseId { self.nextExercise = state.nextExercise }
        if self.totalSets != state.totalSets { self.totalSets = state.totalSets }
        if self.displaySetNumber != state.displaySetNumber { self.displaySetNumber = state.displaySetNumber }
        let newTotalExercises = Int32(state.totalExercises)
        if self.totalExercises != newTotalExercises { self.totalExercises = newTotalExercises }
        if self.requiresWeight != state.requiresWeight { self.requiresWeight = state.requiresWeight }
        if self.isSubmitting != state.isSubmitting { self.isSubmitting = state.isSubmitting }
        if self.isSubmitted != state.isSubmitted { self.isSubmitted = state.isSubmitted }
        if self.submitError != state.submitError { self.submitError = state.submitError }
    }

    func completeSet(elapsedSeconds: Int32) {
        vm.completeSet(elapsedSeconds: elapsedSeconds)
        syncState()
        if isFinished { startSubmitObservation() }
    }

    func restComplete() {
        vm.restComplete()
        syncState()
    }

    func skipRest() { vm.skipRest(); syncState() }
    func skipSet() { vm.skipSet(); syncState() }
    func adjustRest(_ delta: Int32) { vm.adjustRest(deltaSeconds: delta); syncState() }
    func adjustWeight(_ delta: Double) { vm.adjustWeight(delta: delta); syncState() }
    func adjustReps(_ delta: Int32) { vm.adjustReps(delta: delta); syncState() }

    func cancelWorkout() {
        let duration = Int32(sessionDurationSeconds)
        vm.cancelWorkout(totalDurationSeconds: duration)
        syncState()
        startSubmitObservation()
    }

    func retrySubmit() { vm.retrySubmit(); syncState(); startSubmitObservation() }

    func totalVolumeKg() -> Int {
        guard let state = vm.state.value as? WorkoutSessionViewModel.State else { return 0 }
        let volume = state.completedSets.reduce(0.0) { acc, set in
            let reps = Int(set.repsActual)
            let weight = set.weightActual
            if reps == 0 && weight == 0.0 { return acc }
            return acc + (weight * Double(reps))
        }
        return Int(volume)
    }

    private func startSubmitObservation() {
        submitObservationTask?.cancel()
        submitObservationTask = Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 500_000_000)
                guard let self = self else { break }
                guard let state = self.vm.state.value as? WorkoutSessionViewModel.State else { continue }
                if self.isSubmitting != state.isSubmitting { self.isSubmitting = state.isSubmitting }
                if self.isSubmitted != state.isSubmitted { self.isSubmitted = state.isSubmitted }
                if self.submitError != state.submitError { self.submitError = state.submitError }
                if state.isSubmitted { break }
                if !state.isSubmitting && state.submitError != nil { break }
            }
        }
    }

    deinit {
        submitObservationTask?.cancel()
        vm.dispose()
    }

    private static func generateSessionId() -> String {
        let now = Int(Date().timeIntervalSince1970 * 1000)
        let random = (0..<10).map { _ in
            String(Int.random(in: 0..<36), radix: 36)
        }.joined()
        return "ws-\(now)-\(random)"
    }
}
