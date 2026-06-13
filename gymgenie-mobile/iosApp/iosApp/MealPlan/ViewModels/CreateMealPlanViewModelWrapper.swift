import SwiftUI
import Shared

@MainActor
final class CreateMealPlanViewModelWrapper: ObservableObject {
    private let vm: Shared.CreateMealPlanViewModel

    @Published private(set) var step: CreateMealPlanStep = .setup

    @Published private(set) var mealKind: ManualMealKind? = nil
    @Published private(set) var scheduleMode: ManualScheduleMode = .oneOff
    @Published private(set) var selectedDate: String? = nil
    @Published private(set) var selectedWeekdays: [String] = []
    @Published private(set) var bookedRecurringDays: [String] = []
    @Published private(set) var bookedOneOffDates: [String] = []
    @Published private(set) var isInitializing: Bool = true

    @Published private(set) var planName: String = ""
    @Published private(set) var planDescription: String = ""
    @Published private(set) var addedItems: [AddedMealItem] = []

    @Published private(set) var products: [FoodProduct] = []
    @Published private(set) var filteredProducts: [FoodProduct] = []
    @Published private(set) var searchQuery: String = ""
    @Published private(set) var selectedCategory: FoodCategory? = nil
    @Published private(set) var isLoadingProducts: Bool = false
    @Published private(set) var productsError: String? = nil

    @Published private(set) var gramsFor: FoodProduct? = nil
    @Published private(set) var infoFor: FoodProduct? = nil
    @Published private(set) var editingItem: AddedMealItem? = nil

    @Published private(set) var isSaving: Bool = false
    @Published private(set) var isSaved: Bool = false
    @Published private(set) var savedPlan: MealPlanDetail? = nil

    @Published private(set) var errorMessage: String? = nil

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
            manualMealPlanApi: KoinHelper.shared.getManualMealPlanApi(),
            mealPlansApi: KoinHelper.shared.getMealPlansApi()
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
                    self.editingItem = state.editingItem

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

    func initWithMealTypeAndDate(_ mealType: String, date: String) {
        vm.doInitWithMealTypeAndDate(mealType: mealType, date: date)
    }

    func loadForEditing(_ planId: String) {
        vm.loadForEditing(planId: planId)
    }

    func setMealKind(_ kind: ManualMealKind) { vm.setMealKind(kind: kind) }
    func setScheduleMode(_ mode: ManualScheduleMode) { vm.setScheduleMode(mode: mode) }
    func selectDate(_ iso: String) { vm.selectDate(dateKey: iso) }
    func toggleWeekday(_ wire: String) { vm.toggleWeekday(day: wire) }

    func setPlanName(_ name: String) { vm.setPlanName(name: name) }
    func setPlanDescription(_ desc: String) { vm.setPlanDescription(desc: desc) }
    func removeItem(uid: Int64) { vm.removeItem(uid: uid) }

    func openPicker() { vm.openPicker() }
    func loadProducts() { vm.loadProducts() }
    func setSearchQuery(_ q: String) { vm.setSearchQuery(q: q) }
    func setCategory(_ c: FoodCategory?) { vm.setCategory(category: c) }

    func openGrams(_ product: FoodProduct) { vm.openGramsSheet(product: product) }
    func closeGrams() { vm.closeGramsSheet() }
    func openInfo(_ product: FoodProduct) { vm.openInfo(product: product) }
    func closeInfo() { vm.closeInfo() }
    func openEditGrams(_ item: AddedMealItem) { vm.openEditGrams(item: item) }
    func closeEditGrams() { vm.closeEditGrams() }
    func updateItemGrams(uid: Int64, newGrams: Double) { vm.updateItemGrams(uid: uid, newGrams: newGrams) }

    func addItem(_ product: FoodProduct, grams: Double) {
        vm.addItem(product: product, grams: grams)
    }

    func goToEdit() { vm.goToEdit() }
    func goBack() { vm.goBack() }

    func save() { vm.save() }
    func clearError() { vm.clearError() }

    deinit {
        observationTask?.cancel()
        vm.onCleared()
    }
}
