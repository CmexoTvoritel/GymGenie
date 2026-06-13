import SwiftUI
import Shared

@MainActor
final class AiViewModelWrapper: ObservableObject {
    private let vm: Shared.AiViewModel

    @Published private(set) var step: AiFlowStep = .choose
    @Published private(set) var profile: AiProfileData = AiProfileData.companion.empty()
    @Published private(set) var messages: [AiChatMessage] = []
    @Published private(set) var isTyping: Bool = false
    @Published private(set) var hasWorkout: Bool = false
    @Published private(set) var savedPlanId: String? = nil
    @Published private(set) var isSaving: Bool = false
    @Published private(set) var isSaved: Bool = false
    @Published private(set) var errorMessage: String? = nil

    private var observationTask: Task<Void, Never>?

    init() {
        self.vm = Shared.AiViewModel(
            aiApi: KoinHelper.shared.getAiApi(),
            userProfileStore: KoinHelper.shared.getUserProfileStore()
        )
        startObserving()
    }

    private func startObserving() {
        observationTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self else { break }
                if let state = self.vm.state.value as? AiUiState {
                    self.step = state.step
                    self.profile = state.profile
                    self.isTyping = state.isTyping
                    self.hasWorkout = state.lastWorkout != nil
                    self.savedPlanId = state.savedPlanId
                    self.isSaving = state.isSaving
                    self.isSaved = state.isSaved
                    self.errorMessage = state.errorMessage
                    self.messages = state.messages as [AiChatMessage]
                }
                try? await Task.sleep(nanoseconds: 50_000_000)
            }
        }
    }

    func refreshFromStore() { vm.refreshProfileFromStore() }

    func goTo(step: AiFlowStep) { vm.goTo(step: step) }
    func goBack() { vm.goBack() }

    func setAge(_ v: Int) { vm.setAge(value: Int32(v)) }
    func setHeight(_ v: Int) { vm.setHeight(value: Int32(v)) }
    func setWeight(_ v: Int) { vm.setWeight(value: Int32(v)) }
    func setExperience(_ v: String) { vm.setExperience(value: v) }
    func setFrequency(_ v: String) { vm.setFrequency(value: v) }
    func setHasLimitations(_ v: String) { vm.setHasLimitations(value: v) }
    func setLimitationsDesc(_ v: String) { vm.setLimitationsDesc(value: v) }

    func sendMessage(_ text: String) { vm.sendMessage(text: text) }
    func saveWorkout() { vm.saveWorkout() }

    func reset() {
        vm.reset()
    }

    deinit {
        observationTask?.cancel()
        vm.onCleared()
    }
}
