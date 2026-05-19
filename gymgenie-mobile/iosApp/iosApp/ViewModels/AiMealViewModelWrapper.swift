import SwiftUI
import Shared

/// Swift-side bridge for [Shared.AiMealViewModel].
///
/// Mirrors `AiViewModelWrapper.swift` exactly: a 50ms polling task drains the
/// Kotlin `StateFlow` into a flat tree of `@Published` properties so SwiftUI
/// can observe them with the standard environment-object machinery. Method
/// calls delegate straight back to the KMM presenter; widening of `Int` to
/// `Int32` happens at this boundary so view code stays free of platform
/// numeric types.
@MainActor
final class AiMealViewModelWrapper: ObservableObject {
    private let vm: Shared.AiMealViewModel

    @Published private(set) var step: AiMealFlowStep = .choose
    @Published private(set) var profile: AiMealProfileData = AiMealProfileData.companion.empty()
    @Published private(set) var goal: String = ""
    @Published private(set) var dietaryRestrictions: String = ""
    @Published private(set) var allergies: String = ""
    @Published private(set) var messages: [AiMealChatMessage] = []
    @Published private(set) var isTyping: Bool = false
    @Published private(set) var lastMealPlan: AiMealPlanData? = nil
    @Published private(set) var savedPlanId: String? = nil
    @Published private(set) var isSaving: Bool = false
    @Published private(set) var isSaved: Bool = false
    @Published private(set) var errorMessage: String? = nil

    private var observationTask: Task<Void, Never>?
    private var didPrefill: Bool = false

    init() {
        self.vm = Shared.AiMealViewModel(
            aiMealApi: KoinHelper.shared.getAiMealApi(),
            userProfileStore: KoinHelper.shared.getUserProfileStore()
        )
        startObserving()
    }

    private func startObserving() {
        observationTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self else { break }
                if let state = self.vm.state.value as? AiMealUiState {
                    self.step = state.step
                    self.profile = state.profile
                    self.goal = state.goal
                    self.dietaryRestrictions = state.dietaryRestrictions
                    self.allergies = state.allergies
                    self.isTyping = state.isTyping
                    self.lastMealPlan = state.lastMealPlan
                    self.savedPlanId = state.savedPlanId
                    self.isSaving = state.isSaving
                    self.isSaved = state.isSaved
                    self.errorMessage = state.errorMessage
                    self.messages = state.messages as [AiMealChatMessage]
                }
                try? await Task.sleep(nanoseconds: 50_000_000)
            }
        }
    }

    /// Pushes the cached user profile (loaded by `HomeViewModel` and shared
    /// through `UserProfileStoreWrapper`) into the AI meal flow so the user
    /// does not have to re-enter age / height / weight on every visit.
    /// Idempotent and safe even if the user has already started editing —
    /// the wrapper guards on `didPrefill` here, and the KMM presenter
    /// additionally only fills empty slots.
    func prefillProfile(_ profile: UserProfileResponse) {
        guard !didPrefill else { return }
        didPrefill = true

        let current = self.profile
        if current.isProfileFilled { return }

        if let age = profile.ageYears { vm.setAge(value: age.int32Value) }
        if let height = profile.heightCm { vm.setHeight(value: Int32(height.doubleValue)) }
        if let weight = profile.weightKg { vm.setWeight(value: Int32(weight.doubleValue)) }
    }

    func goTo(step: AiMealFlowStep) { vm.goTo(step: step) }
    func goBack() { vm.goBack() }

    func setAge(_ v: Int) { vm.setAge(value: Int32(v)) }
    func setHeight(_ v: Int) { vm.setHeight(value: Int32(v)) }
    func setWeight(_ v: Int) { vm.setWeight(value: Int32(v)) }
    func setGoal(_ v: String) { vm.setGoal(value: v) }
    func setDietaryRestrictions(_ v: String) { vm.setDietaryRestrictions(value: v) }
    func setAllergies(_ v: String) { vm.setAllergies(value: v) }

    func sendMessage(_ text: String) { vm.sendMessage(text: text) }
    func saveMealPlan() { vm.saveMealPlan() }
    func reset() { vm.reset() }

    deinit {
        observationTask?.cancel()
        vm.onCleared()
    }
}
