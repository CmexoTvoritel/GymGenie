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
    private var didPrefill: Bool = false

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

    /// Pushes the cached user profile (loaded by `HomeViewModel` and shared
    /// through `UserProfileStoreWrapper`) into the AI flow so the user does
    /// not have to re-enter age / height / weight / experience / health on
    /// every visit. Idempotent and safe even if the user has already started
    /// editing — we only fill empty slots.
    func prefillProfile(_ profile: UserProfileResponse) {
        guard !didPrefill else { return }
        didPrefill = true

        // Skip if the user has already filled (or partially edited) the
        // profile in the AI flow — preserve their explicit input.
        let current = self.profile
        if current.isProfileFilled { return }

        if let age = profile.ageYears { vm.setAge(value: age.int32Value) }
        if let height = profile.heightCm { vm.setHeight(value: Int32(height.doubleValue)) }
        if let weight = profile.weightKg { vm.setWeight(value: Int32(weight.doubleValue)) }
        if let exp = profile.experience, !exp.isEmpty { vm.setExperience(value: exp) }
        if let freq = profile.frequency, !freq.isEmpty { vm.setFrequency(value: freq) }
        if let health = profile.healthIssues, !health.isEmpty {
            vm.setHasLimitations(value: AiProfileData.companion.HEALTH_YES)
            vm.setLimitationsDesc(value: health)
        }
    }

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
        didPrefill = false
    }

    deinit {
        observationTask?.cancel()
        vm.onCleared()
    }
}
