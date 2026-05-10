import SwiftUI
import Shared

/// Swift-side bridge for [Shared.CreateMealPlanViewModel].
///
/// Mirrors `AiMealViewModelWrapper.swift` exactly: a 50ms polling task drains
/// the Kotlin `StateFlow` into a flat tree of `@Published` properties so
/// SwiftUI can observe them through the standard environment-object machinery.
/// Method calls delegate straight back to the KMM presenter; widening of `Int`
/// to `Int32` happens at this boundary so view code stays free of platform
/// numeric types.
///
/// The presenter is constructed directly here (rather than resolved as a
/// Koin singleton) because it owns flow-local mutable state (added items,
/// search query, the selected meal kind, ...). A singleton would leak that
/// state across separate openings of the create-plan flow; the per-wrapper
/// instance is the same convention used by `MealPlanDetailViewModelWrapper`.
@MainActor
final class CreateMealPlanViewModelWrapper: ObservableObject {
    private let vm: Shared.CreateMealPlanViewModel

    @Published private(set) var step: CreateMealPlanStep = .setup

    // Setup
    @Published private(set) var mealKind: ManualMealKind? = nil
    @Published private(set) var scheduleMode: ManualScheduleMode = .oneOff
    @Published private(set) var selectedDate: String? = nil
    @Published private(set) var selectedWeekdays: [String] = []
    @Published private(set) var bookedRecurringDays: [String] = []
    @Published private(set) var bookedOneOffDates: [String] = []
    @Published private(set) var isInitializing: Bool = true

    // Editor
    @Published private(set) var planName: String = ""
    @Published private(set) var planDescription: String = ""
    @Published private(set) var addedItems: [AddedMealItem] = []

    // Picker
    @Published private(set) var products: [FoodProduct] = []
    @Published private(set) var filteredProducts: [FoodProduct] = []
    @Published private(set) var searchQuery: String = ""
    @Published private(set) var selectedCategory: FoodCategory? = nil
    @Published private(set) var isLoadingProducts: Bool = false
    @Published private(set) var productsError: String? = nil

    // Modal layers
    @Published private(set) var gramsFor: FoodProduct? = nil
    @Published private(set) var infoFor: FoodProduct? = nil

    // Save terminal
    @Published private(set) var isSaving: Bool = false
    @Published private(set) var isSaved: Bool = false
    @Published private(set) var savedPlan: MealPlanDetail? = nil

    @Published private(set) var errorMessage: String? = nil

    // Live totals (computed properties on the KMM state, but `@Published`
    // here so SwiftUI body diffs see them change without observing the
    // backing collection separately).
    @Published private(set) var totalCalories: Int = 0
    @Published private(set) var totalProteinG: Double = 0
    @Published private(set) var totalFatG: Double = 0
    @Published private(set) var totalCarbsG: Double = 0
    @Published private(set) var canContinueFromSetup: Bool = false
    @Published private(set) var canSave: Bool = false

    private var observationTask: Task<Void, Never>?

    init() {
        self.vm = Shared.CreateMealPlanViewModel(
            foodProductApi: KoinHelper.shared.getFoodProductApi(),
            manualMealPlanApi: KoinHelper.shared.getManualMealPlanApi()
        )
        startObserving()
    }

    private func startObserving() {
        observationTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self else { break }
                if let state = self.vm.state.value as? CreateMealPlanUiState {
                    self.step = state.step
                    self.mealKind = state.mealKind
                    self.scheduleMode = state.scheduleMode
                    self.selectedDate = state.selectedDate
                    // KMM exposes typed arrays through the ObjC bridge —
                    // `[String]` and `[FoodProduct]` are valid Swift values
                    // straight off the state object, no NSArray hop needed.
                    self.selectedWeekdays = state.selectedWeekdays
                    self.bookedRecurringDays = state.bookedRecurringDays
                    self.bookedOneOffDates = state.bookedOneOffDates
                    self.isInitializing = state.isInitializing

                    self.planName = state.planName
                    self.planDescription = state.planDescription
                    self.addedItems = state.addedItems

                    self.products = state.products
                    self.filteredProducts = state.filteredProducts
                    self.searchQuery = state.searchQuery
                    self.selectedCategory = state.selectedCategory
                    self.isLoadingProducts = state.isLoadingProducts
                    self.productsError = state.productsError

                    self.gramsFor = state.gramsFor
                    self.infoFor = state.infoFor

                    self.isSaving = state.isSaving
                    self.isSaved = state.isSaved
                    self.savedPlan = state.savedPlan
                    self.errorMessage = state.errorMessage

                    self.totalCalories = Int(state.totalCalories)
                    self.totalProteinG = state.totalProteinG
                    self.totalFatG = state.totalFatG
                    self.totalCarbsG = state.totalCarbsG
                    self.canContinueFromSetup = state.canContinueFromSetup
                    self.canSave = state.canSave
                }
                try? await Task.sleep(nanoseconds: 50_000_000)
            }
        }
    }

    // MARK: - Setup

    func setMealKind(_ kind: ManualMealKind) { vm.setMealKind(kind: kind) }
    func setScheduleMode(_ mode: ManualScheduleMode) { vm.setScheduleMode(mode: mode) }
    func selectDate(_ iso: String) { vm.selectDate(dateKey: iso) }
    func toggleWeekday(_ wire: String) { vm.toggleWeekday(day: wire) }

    // MARK: - Editor

    func setPlanName(_ name: String) { vm.setPlanName(name: name) }
    func setPlanDescription(_ desc: String) { vm.setPlanDescription(desc: desc) }
    func removeItem(uid: Int64) { vm.removeItem(uid: uid) }

    // MARK: - Picker

    func openPicker() { vm.openPicker() }
    func setSearchQuery(_ q: String) { vm.setSearchQuery(q: q) }
    func setCategory(_ c: FoodCategory?) { vm.setCategory(category: c) }

    // MARK: - Modal layers

    func openGrams(_ product: FoodProduct) { vm.openGramsSheet(product: product) }
    func closeGrams() { vm.closeGramsSheet() }
    func openInfo(_ product: FoodProduct) { vm.openInfo(product: product) }
    func closeInfo() { vm.closeInfo() }

    func addItem(_ product: FoodProduct, grams: Double) {
        vm.addItem(product: product, grams: grams)
    }

    // MARK: - Step navigation

    func goToEdit() { vm.goToEdit() }
    func goBack() { vm.goBack() }

    // MARK: - Save

    func save() { vm.save() }
    func clearError() { vm.clearError() }

    deinit {
        observationTask?.cancel()
        vm.onCleared()
    }
}
