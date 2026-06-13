import SwiftUI
import Shared

@MainActor
final class AiMealViewModelWrapper: ObservableObject {
    private let vm: Shared.AiMealViewModel

    @Published private(set) var step: AiMealFlowStep = .choose
    @Published private(set) var profile: AiMealProfileData = AiMealProfileData.companion.empty()
    @Published private(set) var goal: String = ""
    @Published private(set) var dietaryRestrictions: String = ""
    @Published private(set) var allergies: String = ""
    @Published private(set) var selectedMealType: AiMealType? = nil
    @Published private(set) var messages: [AiMealChatMessage] = []
    @Published private(set) var isTyping: Bool = false
    @Published private(set) var lastMealPlan: AiMealPlanData? = nil
    @Published private(set) var savedPlanId: String? = nil
    @Published private(set) var isSaving: Bool = false
    @Published private(set) var isSaved: Bool = false
    @Published private(set) var errorMessage: String? = nil

    @Published private(set) var showSchedulePicker: Bool = false
    @Published private(set) var scheduleMode: String = "ONE_OFF"
    @Published private(set) var selectedDate: String? = nil
    @Published private(set) var selectedWeekdays: [String] = []
    @Published private(set) var bookedOneOffDates: [String] = []
    @Published private(set) var bookedRecurringDays: [String] = []
    @Published private(set) var conflicts: [AiMealConflictPlan] = []
    @Published private(set) var showConflictDialog: Bool = false

    private var observationTask: Task<Void, Never>?

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
                    self.selectedMealType = state.selectedMealType
                    self.isTyping = state.isTyping
                    self.lastMealPlan = state.lastMealPlan
                    self.savedPlanId = state.savedPlanId
                    self.isSaving = state.isSaving
                    self.isSaved = state.isSaved
                    self.errorMessage = state.errorMessage
                    self.messages = state.messages as [AiMealChatMessage]
                    self.showSchedulePicker = state.showSchedulePicker
                    self.scheduleMode = state.scheduleMode
                    self.selectedDate = state.selectedDate
                    self.selectedWeekdays = state.selectedWeekdays as [String]
                    self.bookedOneOffDates = state.bookedOneOffDates as [String]
                    self.bookedRecurringDays = state.bookedRecurringDays as [String]
                    self.conflicts = state.conflicts as [AiMealConflictPlan]
                    self.showConflictDialog = state.showConflictDialog
                }
                try? await Task.sleep(nanoseconds: 50_000_000)
            }
        }
    }

    func refreshFromStore() { vm.refreshProfileFromStore() }

    func goTo(step: AiMealFlowStep) { vm.goTo(step: step) }
    func goBack() { vm.goBack() }
    func setMealType(_ type: AiMealType) { vm.setMealType(type: type) }
    func selectMealType(_ type: AiMealType) { vm.selectMealType(type: type) }

    func setAge(_ v: Int) { vm.setAge(value: Int32(v)) }
    func setHeight(_ v: Int) { vm.setHeight(value: Int32(v)) }
    func setWeight(_ v: Int) { vm.setWeight(value: Int32(v)) }
    func setGoal(_ v: String) { vm.setGoal(value: v) }
    func setDietaryRestrictions(_ v: String) { vm.setDietaryRestrictions(value: v) }
    func setAllergies(_ v: String) { vm.setAllergies(value: v) }

    func sendMessage(_ text: String) { vm.sendMessage(text: text) }
    func reset() {
        vm.reset()
    }

    func onAddPlanTapped() { vm.onAddPlanTapped() }
    func setScheduleMode(_ mode: String) { vm.setScheduleMode(mode: mode) }
    func setSelectedDate(_ date: String?) { vm.setSelectedDate(date: date) }
    func toggleWeekday(_ day: String) { vm.toggleWeekday(day: day) }
    func dismissSchedulePicker() { vm.dismissSchedulePicker() }
    func saveWithSchedule() { vm.saveWithSchedule() }
    func confirmReplace() { vm.confirmReplace() }
    func dismissConflictDialog() { vm.dismissConflictDialog() }

    deinit {
        observationTask?.cancel()
        vm.onCleared()
    }
}
