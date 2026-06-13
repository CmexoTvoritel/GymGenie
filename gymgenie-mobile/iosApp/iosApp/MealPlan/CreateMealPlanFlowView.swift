import SwiftUI
import Shared

struct CreateMealPlanFlowView: View {
    @StateObject private var vm = CreateMealPlanViewModelWrapper()
    @State private var previousStepIndex: Int32 = 0
    @State private var pendingError: String? = nil
    @State private var didApplyInitialMealType: Bool = false
    @State private var initialLoadCompleted: Bool = false
    @State private var showExitWarning: Bool = false

    var initialMealType: String? = nil
    var initialDate: String? = nil
    var editPlanId: String? = nil
    var onClose: () -> Void
    var onSaved: () -> Void

    private var hasPrefilledInit: Bool {
        editPlanId != nil || (initialMealType != nil && initialDate != nil)
    }

    var body: some View {
        if vm.isInitializing || (hasPrefilledInit && vm.step == .setup && !initialLoadCompleted) {
            ZStack {
                Palette.warmOffWhite.ignoresSafeArea()
                ProgressView()
                    .scaleEffect(1.2)
                    .tint(Palette.coral)
            }
            .onAppear {
                applyInitialMealTypeIfNeeded(isInitializing: vm.isInitializing)
            }
            .onChange(of: vm.isInitializing) { initializing in
                applyInitialMealTypeIfNeeded(isInitializing: initializing)
            }
        } else {
            ZStack {
                Palette.warmOffWhite.ignoresSafeArea()
                content
                    .animation(.easeInOut(duration: 0.25), value: vm.step.index)

                if vm.isSaving {
                    SavingOverlay()
                        .transition(.opacity)
                        .zIndex(20)
                }
            }
            .animation(.easeInOut(duration: 0.2), value: vm.isSaving)
            .sheet(isPresented: Binding(
                get: { vm.gramsFor != nil },
                set: { if !$0 { vm.closeGrams() } }
            )) {
                if let product = vm.gramsFor {
                    GramsSheetContent(product: product, onAdd: { grams in vm.addItem(product, grams: grams) })
                        .presentationDetents([.medium])
                        .presentationDragIndicator(.visible)
                        .presentationBackground(Palette.warmOffWhite)
                }
            }
            .sheet(isPresented: Binding(
                get: { vm.editingItem != nil },
                set: { if !$0 { vm.closeEditGrams() } }
            )) {
                if let item = vm.editingItem {
                    EditGramsSheetContent(item: item, onSave: { grams in vm.updateItemGrams(uid: item.uid, newGrams: grams) })
                        .presentationDetents([.medium])
                        .presentationDragIndicator(.visible)
                        .presentationBackground(Palette.warmOffWhite)
                }
            }
            .onChange(of: vm.step) { step in
                if hasPrefilledInit && step != .setup && !initialLoadCompleted {
                    initialLoadCompleted = true
                }
            }
            .onChange(of: vm.savedPlan?.id) { newValue in
                if newValue != nil { onSaved() }
            }
            .onChange(of: vm.isInitializing) { initializing in
                applyInitialMealTypeIfNeeded(isInitializing: initializing)
            }
            .onAppear {
                if hasPrefilledInit { initialLoadCompleted = true }
                applyInitialMealTypeIfNeeded(isInitializing: vm.isInitializing)
            }
            .onChange(of: vm.errorMessage) { msg in
                if let msg = msg { pendingError = msg }
            }
            .alert(
                "Ошибка",
                isPresented: Binding(
                    get: { pendingError != nil },
                    set: { if !$0 { pendingError = nil; vm.clearError() } }
                ),
                presenting: pendingError
            ) { _ in
                Button("OK") { pendingError = nil; vm.clearError() }
            } message: { msg in
                Text(msg)
            }
            .alert("Выйти без сохранения?", isPresented: $showExitWarning) {
                Button("Остаться", role: .cancel) { }
                Button("Выйти", role: .destructive) {
                    if hasPrefilledInit {
                        onClose()
                    } else {
                        previousStepIndex = vm.step.index
                        vm.goBack()
                    }
                }
            } message: {
                Text("Все добавленные продукты будут потеряны.")
            }
            .interactiveDismissDisabled(vm.step != .setup && !vm.addedItems.isEmpty)
        }
    }

    @ViewBuilder
    private var content: some View {
        switch vm.step {
        case .setup:
            SetupStep(
                vm: vm,
                onClose: onClose,
                onNext: {
                    previousStepIndex = vm.step.index
                    vm.goToEdit()
                }
            )
            .transition(transition(forward: vm.step.index >= previousStepIndex))
        case .edit:
            EditStep(
                vm: vm,
                onBack: {
                    if !vm.addedItems.isEmpty {
                        showExitWarning = true
                    } else if hasPrefilledInit {
                        onClose()
                    } else {
                        previousStepIndex = vm.step.index
                        vm.goBack()
                    }
                },
                onAddProduct: {
                    previousStepIndex = vm.step.index
                    vm.openPicker()
                },
                onSave: { vm.save() },
                isClose: hasPrefilledInit
            )
            .transition(transition(forward: vm.step.index >= previousStepIndex))
        case .picker:
            PickerStep(
                vm: vm,
                onBack: {
                    previousStepIndex = vm.step.index
                    vm.goBack()
                },
                onSelect: { product in
                    previousStepIndex = vm.step.index
                    vm.openInfo(product)
                }
            )
            .transition(transition(forward: vm.step.index >= previousStepIndex))
        case .info:
            InfoStep(
                vm: vm,
                onBack: {
                    previousStepIndex = vm.step.index
                    vm.goBack()
                },
                onChooseGrams: { product in vm.openGrams(product) }
            )
            .transition(transition(forward: vm.step.index >= previousStepIndex))
        default:
            EmptyView()
        }
    }

    private func transition(forward: Bool) -> AnyTransition {
        .asymmetric(
            insertion: .move(edge: forward ? .trailing : .leading),
            removal: .move(edge: forward ? .leading : .trailing)
        )
    }

    private func applyInitialMealTypeIfNeeded(isInitializing: Bool) {
        guard !didApplyInitialMealType else { return }
        if let editId = editPlanId {
            didApplyInitialMealType = true
            vm.loadForEditing(editId)
            return
        }
        guard !isInitializing, let wire = initialMealType else { return }
        if let date = initialDate {
            vm.initWithMealTypeAndDate(wire, date: date)
        } else if let kind = ManualMealKind.companion.fromWireValue(value: wire) {
            vm.setMealKind(kind)
        }
        didApplyInitialMealType = true
    }
}

private struct SetupStep: View {
    @ObservedObject var vm: CreateMealPlanViewModelWrapper
    let onClose: () -> Void
    let onNext: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: "Создать приём пищи",
                showBackNavigation: true,
                useCloseIcon: true,
                onBackTap: onClose
            )

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 20) {
                    Text("Что планируем?")
                        .font(.system(size: 19, weight: .heavy))
                        .foregroundColor(Palette.deepInk)
                        .padding(.horizontal, 20)
                        .padding(.top, 4)

                    VStack(spacing: 10) {
                        ForEach([ManualMealKind.breakfast, .lunch, .dinner], id: \.wireValue) { kind in
                            MealKindCard(
                                kind: kind,
                                isSelected: vm.mealKind == kind,
                                onTap: { vm.setMealKind(kind) }
                            )
                        }
                    }
                    .padding(.horizontal, 20)

                    if vm.mealKind != nil {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Когда?")
                                .font(.system(size: 19, weight: .heavy))
                                .foregroundColor(Palette.deepInk)

                            ScheduleModeToggle(
                                mode: vm.scheduleMode,
                                onSelect: { vm.setScheduleMode($0) }
                            )

                            if vm.scheduleMode == .oneOff {
                                OneOffDateStrip(
                                    bookedDates: vm.bookedOneOffDates,
                                    selectedDate: vm.selectedDate,
                                    onSelect: { vm.selectDate($0) }
                                )
                            } else {
                                WeekdayChipsGrid(
                                    bookedDays: vm.bookedRecurringDays,
                                    selectedDays: vm.selectedWeekdays,
                                    onToggle: { vm.toggleWeekday($0) }
                                )
                            }

                        }
                        .padding(.horizontal, 20)
                    }

                    Spacer().frame(height: 100)
                }
                .padding(.vertical, 12)
            }

            BottomCtaBar {
                PrimaryButton(
                    title: "Далее",
                    enabled: vm.canContinueFromSetup,
                    action: onNext
                )
            }
        }
    }
}

private struct MealKindCard: View {
    let kind: ManualMealKind
    let isSelected: Bool
    let onTap: () -> Void

    private var imageName: String {
        switch kind {
        case .breakfast: return "ic_breakfast"
        case .lunch: return "ic_lunch"
        case .dinner: return "ic_dinner"
        default: return "ic_lunch"
        }
    }

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 16)
                        .fill(isSelected ? Palette.coral.opacity(0.18) : Palette.softCard)
                    Image(imageName)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 26, height: 26)
                }
                .frame(width: 56, height: 56)

                VStack(alignment: .leading, spacing: 4) {
                    Text(kind.displayName)
                        .font(.system(size: 18, weight: .heavy))
                        .foregroundColor(Palette.deepInk)
                    Text(kind.kcalHintRu)
                        .font(.system(size: 16))
                        .foregroundColor(Palette.deepInk)
                }

                Spacer()

                ZStack {
                    Circle().strokeBorder(
                        isSelected ? Palette.coral : Color(red: 0.835, green: 0.835, blue: 0.859),
                        lineWidth: 1.5
                    )
                    if isSelected {
                        Circle().fill(Palette.coral).frame(width: 10, height: 10)
                    }
                }
                .frame(width: 22, height: 22)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 18))
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .strokeBorder(
                        isSelected ? Palette.coral : Color(red: 0.929, green: 0.929, blue: 0.937),
                        lineWidth: isSelected ? 2 : 1.5
                    )
            )
        }
        .buttonStyle(.plain)
    }
}

private struct ScheduleModeToggle: View {
    let mode: ManualScheduleMode
    let onSelect: (ManualScheduleMode) -> Void

    var body: some View {
        HStack(spacing: 0) {
            toggleButton(title: "Разово", isSelected: mode == .oneOff, target: .oneOff)
            toggleButton(title: "По дням", isSelected: mode == .recurring, target: .recurring)
        }
        .padding(4)
        .background(RoundedRectangle(cornerRadius: 14).fill(Palette.softCard))
    }

    private func toggleButton(title: String, isSelected: Bool, target: ManualScheduleMode) -> some View {
        Button(action: { onSelect(target) }) {
            Text(title)
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(isSelected ? .white : Palette.deepInk)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .background(
                    RoundedRectangle(cornerRadius: 11)
                        .fill(isSelected ? Palette.coral : Color.clear)
                )
        }
        .buttonStyle(.plain)
    }
}

private struct OneOffDateStrip: View {
    let bookedDates: [String]
    let selectedDate: String?
    let onSelect: (String) -> Void

    private var dates: [(iso: String, day: Int, weekday: String)] {
        let cal = Calendar(identifier: .gregorian)
        var c = cal
        c.locale = Locale(identifier: "ru_RU")
        let today = c.startOfDay(for: Date())
        let dateFmt = DateFormatter()
        dateFmt.dateFormat = "yyyy-MM-dd"
        dateFmt.calendar = c
        return (0..<14).map { offset in
            let date = c.date(byAdding: .day, value: offset, to: today) ?? today
            let iso = dateFmt.string(from: date)
            let day = c.component(.day, from: date)
            let weekday = dayShortRuFromCalendarIndex(c.component(.weekday, from: date))
            return (iso, day, weekday)
        }
    }

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(dates, id: \.iso) { entry in
                    let isBooked = bookedDates.contains(entry.iso)
                    let isSelected = selectedDate == entry.iso
                    Button(action: {
                        if !isBooked { onSelect(entry.iso) }
                    }) {
                        VStack(spacing: 4) {
                            Text(entry.weekday)
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(isSelected ? .white : Palette.mutedText)
                            Text("\(entry.day)")
                                .font(.system(size: 18, weight: .heavy))
                                .foregroundColor(isSelected ? .white : (isBooked ? Palette.mutedText : Palette.deepInk))
                            if isBooked {
                                Image(systemName: "lock.fill")
                                    .font(.system(size: 9))
                                    .foregroundColor(Palette.mutedText)
                            }
                        }
                        .frame(width: 56, height: 70)
                        .background(
                            RoundedRectangle(cornerRadius: 14)
                                .fill(isSelected ? Palette.coral : (isBooked ? Palette.softCard : Color.white))
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .strokeBorder(
                                    isSelected ? Palette.coral : Color(red: 0.929, green: 0.929, blue: 0.937),
                                    lineWidth: 1.5
                                )
                        )
                        .opacity(isBooked && !isSelected ? 0.55 : 1.0)
                    }
                    .buttonStyle(.plain)
                    .disabled(isBooked)
                }
            }
        }
    }
}

private struct WeekdayChipsGrid: View {
    let bookedDays: [String]
    let selectedDays: [String]
    let onToggle: (String) -> Void

    private let days: [(wire: String, label: String)] = [
        ("MONDAY", "Пн"),
        ("TUESDAY", "Вт"),
        ("WEDNESDAY", "Ср"),
        ("THURSDAY", "Чт"),
        ("FRIDAY", "Пт"),
        ("SATURDAY", "Сб"),
        ("SUNDAY", "Вс"),
    ]

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(days, id: \.wire) { day in
                    let isBooked = bookedDays.contains(day.wire)
                    let isSelected = selectedDays.contains(day.wire)
                    Button(action: { if !isBooked { onToggle(day.wire) } }) {
                        ZStack {
                            Text(day.label)
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(isSelected ? .white : (isBooked ? Palette.mutedText : Palette.deepInk))
                            if isBooked {
                                Image(systemName: "lock.fill")
                                    .font(.system(size: 8))
                                    .foregroundColor(Palette.mutedText)
                                    .offset(y: 14)
                            }
                        }
                        .frame(width: 44, height: 44)
                        .background(
                            Circle().fill(isSelected ? Palette.coral : (isBooked ? Palette.softCard : Color.white))
                        )
                        .overlay(
                            Circle().strokeBorder(
                                isSelected ? Palette.coral : Color(red: 0.929, green: 0.929, blue: 0.937),
                                lineWidth: 1.5
                            )
                        )
                        .opacity(isBooked && !isSelected ? 0.55 : 1.0)
                    }
                    .buttonStyle(.plain)
                    .disabled(isBooked)
                }
            }
        }
    }
}

private struct EditStep: View {
    @ObservedObject var vm: CreateMealPlanViewModelWrapper
    let onBack: () -> Void
    let onAddProduct: () -> Void
    let onSave: () -> Void
    var isClose: Bool = false

    var body: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: vm.mealKind?.displayName ?? "Приём пищи",
                showBackNavigation: true,
                useCloseIcon: isClose,
                onBackTap: onBack
            )

            ScrollView(showsIndicators: false) {
                VStack(spacing: 16) {
                    MacrosSummaryCard(
                        kcal: vm.totalCalories,
                        proteinG: vm.totalProteinG,
                        fatG: vm.totalFatG,
                        carbsG: vm.totalCarbsG
                    )
                    .padding(.horizontal, 20)

                    PlanNameField(
                        name: vm.planName,
                        onChange: { vm.setPlanName($0) }
                    )
                    .padding(.horizontal, 20)

                    if vm.addedItems.isEmpty {
                        EmptyEditState(onAdd: onAddProduct)
                            .padding(.horizontal, 20)
                            .padding(.top, 16)
                    } else {
                        Text("ДОБАВЛЕНО")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(Palette.mutedText)
                            .tracking(0.5)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 20)
                            .padding(.top, 4)

                        VStack(spacing: 8) {
                            ForEach(vm.addedItems, id: \.uid) { item in
                                AddedItemRow(
                                    item: item,
                                    onEdit: { vm.openEditGrams(item) },
                                    onDelete: { vm.removeItem(uid: item.uid) }
                                )
                            }
                        }
                        .padding(.horizontal, 20)

                        Button(action: onAddProduct) {
                            HStack(spacing: 8) {
                                Image(systemName: "plus").font(.system(size: 13, weight: .bold))
                                Text("Добавить продукт").font(.system(size: 16, weight: .bold))
                            }
                            .foregroundColor(Palette.deepInk)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(
                                RoundedRectangle(cornerRadius: 14)
                                    .strokeBorder(
                                        Palette.deepInk.opacity(0.35),
                                        style: StrokeStyle(lineWidth: 1.5, dash: [6, 4])
                                    )
                            )
                        }
                        .buttonStyle(.plain)
                        .padding(.horizontal, 20)
                    }

                    Spacer().frame(height: 100)
                }
                .padding(.vertical, 12)
            }

            BottomCtaBar {
                PrimaryButton(
                    title: "Сохранить",
                    enabled: vm.canSave,
                    action: onSave
                )
            }
        }
    }
}

private struct PlanNameField: View {
    let name: String
    let onChange: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("НАЗВАНИЕ")
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(Palette.deepInk)
                .tracking(0.5)
            TextField("Завтрак · Сегодня", text: Binding(get: { name }, set: { onChange($0) }))
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(Palette.deepInk)
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .overlay(
                    RoundedRectangle(cornerRadius: 14)
                        .strokeBorder(Color(red: 0.929, green: 0.929, blue: 0.937), lineWidth: 1.5)
                )
        }
    }
}

private struct MacrosSummaryCard: View {
    let kcal: Int
    let proteinG: Double
    let fatG: Double
    let carbsG: Double

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("ВСЕГО КАЛОРИЙ").font(.system(size: 15, weight: .bold))
                        .foregroundColor(.white.opacity(0.65))
                        .tracking(0.5)
                    Text("\(kcal) ккал")
                        .font(.system(size: 26, weight: .heavy))
                        .foregroundColor(.white)
                }
                Spacer()
                Image("ic_calories")
                    .renderingMode(.template)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 44, height: 44)
                    .foregroundColor(.red)
            }

            HStack(spacing: 8) {
                MacroPill(label: "Б", grams: proteinG)
                MacroPill(label: "Ж", grams: fatG)
                MacroPill(label: "У", grams: carbsG)
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 22).fill(Palette.deepInk))
    }
}

private struct MacroPill: View {
    let label: String
    let grams: Double

    var body: some View {
        HStack(spacing: 4) {
            Text(label).font(.system(size: 16, weight: .heavy))
                .foregroundColor(.white)
            Text("\(Int(grams.rounded()))г")
                .font(.system(size: 16, weight: .heavy))
                .foregroundColor(.white)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 7)
        .background(Capsule().fill(Color.white.opacity(0.12)))
    }
}

private struct AddedItemRow: View {
    let item: AddedMealItem
    let onEdit: () -> Void
    let onDelete: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Image(foodCategoryImageName(item.product.category))
                .resizable()
                .aspectRatio(1, contentMode: .fill)
                .frame(width: 44, height: 44)
                .clipShape(RoundedRectangle(cornerRadius: 12))

            VStack(alignment: .leading, spacing: 2) {
                Text(item.product.nameRu)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Palette.deepInk)
                    .lineLimit(1)
                Text("\(Int(item.grams.rounded()))г, \(Int(item.portion.calories.rounded())) ккал, Б\(shortMacro(item.portion.proteinG)), Ж\(shortMacro(item.portion.fatG)), У\(shortMacro(item.portion.carbsG))")
                    .font(.system(size: 14))
                    .foregroundColor(Palette.deepInk)
            }

            Spacer(minLength: 8)

            HStack(spacing: 8) {
                Button(action: onEdit) {
                    Image(systemName: "pencil")
                        .font(.system(size: 16))
                        .foregroundColor(Palette.deepInk)
                        .frame(width: 36, height: 36)
                        .background(Circle().fill(Palette.softCard))
                }
                .buttonStyle(.plain)

                Button(action: onDelete) {
                    Image(systemName: "trash.fill")
                        .font(.system(size: 16))
                        .foregroundColor(Color(red: 0.898, green: 0.224, blue: 0.208))
                        .frame(width: 36, height: 36)
                        .background(Circle().fill(Color(red: 1.0, green: 0.918, blue: 0.918)))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .strokeBorder(Color(red: 0.929, green: 0.929, blue: 0.937), lineWidth: 1.5)
        )
    }
}

private struct EmptyEditState: View {
    let onAdd: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Image("ic_lunch")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 40, height: 40)
            Text("Пока пусто")
                .font(.system(size: 18, weight: .heavy))
                .foregroundColor(Palette.deepInk)
            Text("Добавьте продукты, из которых будет состоять приём пищи")
                .font(.system(size: 15))
                .foregroundColor(Palette.mutedText)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)

            Button(action: onAdd) {
                HStack(spacing: 6) {
                    Image(systemName: "plus").font(.system(size: 13, weight: .bold))
                    Text("Добавить продукт").font(.system(size: 16, weight: .bold))
                }
                .foregroundColor(.white)
                .padding(.horizontal, 22)
                .padding(.vertical, 12)
                .background(Capsule().fill(Palette.coral))
            }
            .buttonStyle(.plain)
            .padding(.top, 4)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 32)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 22))
        .overlay(
            RoundedRectangle(cornerRadius: 22)
                .strokeBorder(
                    Color(red: 0.878, green: 0.878, blue: 0.898),
                    style: StrokeStyle(lineWidth: 1.5, dash: [6, 4])
                )
        )
    }
}

private struct PickerScrollOffsetKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

private struct PickerHeaderHeightKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

private struct PickerStep: View {
    @ObservedObject var vm: CreateMealPlanViewModelWrapper
    let onBack: () -> Void
    let onSelect: (FoodProduct) -> Void

    @State private var scrollOffset: CGFloat = 0
    @State private var fullHeaderHeight: CGFloat = 0

    var body: some View {
        let collapseProgress = fullHeaderHeight > 0 ? min(max(scrollOffset / fullHeaderHeight, 0), 1) : 0

        VStack(spacing: 0) {
            GymGenieToolbar(
                title: "Выбор продукта",
                showBackNavigation: true,
                onBackTap: onBack
            )

            if let err = vm.productsError, vm.products.isEmpty {
                CenteredMessage(iconName: "exclamationmark.triangle.fill", title: err, ctaTitle: "Повторить", onCta: {
                    vm.loadProducts()
                })
            } else if vm.isLoadingProducts && vm.products.isEmpty {
                CenteredSpinner()
            } else if vm.filteredProducts.isEmpty && !vm.searchQuery.isEmpty && !vm.isLoadingProducts {
                VStack(spacing: 12) {
                    SearchField(
                        query: vm.searchQuery,
                        onChange: { vm.setSearchQuery($0) }
                    )
                    .padding(.horizontal, 20)

                    CategoryChipsRow(
                        selected: vm.selectedCategory,
                        onSelect: { vm.setCategory($0) }
                    )
                }
                .padding(.bottom, 8)

                CenteredMessage(iconName: "magnifyingglass", title: "Ничего не найдено", ctaTitle: nil, onCta: {})
            } else {
                VStack(spacing: 0) {
                    VStack(spacing: 12) {
                        SearchField(
                            query: vm.searchQuery,
                            onChange: { vm.setSearchQuery($0) }
                        )
                        .padding(.horizontal, 20)

                        CategoryChipsRow(
                            selected: vm.selectedCategory,
                            onSelect: { vm.setCategory($0) }
                        )

                        Spacer().frame(height: 4)
                    }
                    .padding(.top, 8)
                    .background(Palette.warmOffWhite)
                    .background(GeometryReader { geo in
                        Color.clear.preference(
                            key: PickerHeaderHeightKey.self,
                            value: geo.size.height
                        )
                    })
                    .frame(height: fullHeaderHeight > 0 ? max(0, fullHeaderHeight * (1 - collapseProgress)) : nil)
                    .clipped()

                    if vm.isLoadingProducts {
                        VStack {
                            Spacer()
                            ProgressView().scaleEffect(1.2).tint(Palette.coral)
                            Spacer()
                        }
                    } else {
                        ScrollView(showsIndicators: false) {
                            LazyVStack(spacing: 10) {
                                ForEach(vm.filteredProducts, id: \.id) { product in
                                    ProductRow(product: product, onTap: { onSelect(product) })
                                }
                                Color.clear.frame(height: 24)
                            }
                            .padding(.horizontal, 20)
                            .padding(.vertical, 8)
                            .background(GeometryReader { geo in
                                Color.clear.preference(
                                    key: PickerScrollOffsetKey.self,
                                    value: -geo.frame(in: .named("pickerScroll")).minY
                                )
                            })
                        }
                        .coordinateSpace(name: "pickerScroll")
                        .onPreferenceChange(PickerScrollOffsetKey.self) { scrollOffset = $0 }
                    }
                }
                .onPreferenceChange(PickerHeaderHeightKey.self) { newHeight in
                    if newHeight > fullHeaderHeight {
                        fullHeaderHeight = newHeight
                    }
                }
            }
        }
        .onTapGesture {
            UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
        }
    }
}

private struct SearchField: View {
    let query: String
    let onChange: (String) -> Void

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(Palette.mutedText)
            TextField(
                "Поиск продукта",
                text: Binding(get: { query }, set: { onChange($0) })
            )
            .font(.system(size: 15))
            .foregroundColor(Palette.deepInk)
            .autocorrectionDisabled(true)
            .textInputAutocapitalization(.never)
            if !query.isEmpty {
                Button(action: { onChange("") }) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 14))
                        .foregroundColor(Palette.mutedText)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(height: 44)
        .background(Color.white)
        .clipShape(Capsule())
        .shadow(color: Color.black.opacity(0.06), radius: 6, x: 0, y: 2)
    }
}

private struct CategoryChipsRow: View {
    let selected: FoodCategory?
    let onSelect: (FoodCategory?) -> Void

    private var categories: [FoodCategory] { FoodCategory.entries }

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                CategoryChip(label: "Все", isSelected: selected == nil, onTap: { onSelect(nil) })
                ForEach(categories, id: \.name) { cat in
                    CategoryChip(
                        label: cat.displayName(),
                        isSelected: selected == cat,
                        onTap: { onSelect(cat) }
                    )
                }
            }
            .padding(.horizontal, 20)
        }
    }
}

private struct CategoryChip: View {
    let label: String
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(isSelected ? .white : Palette.deepInk)
                .padding(.horizontal, 14)
                .padding(.vertical, 6)
                .background(
                    Capsule().fill(isSelected ? Palette.accentOrange : Palette.softCard)
                )
        }
        .buttonStyle(.plain)
    }
}

private struct ProductRow: View {
    let product: FoodProduct
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                Image(foodCategoryImageName(product.category))
                    .resizable()
                    .aspectRatio(1, contentMode: .fill)
                    .frame(width: 48, height: 48)
                    .clipShape(RoundedRectangle(cornerRadius: 12))

                VStack(alignment: .leading, spacing: 4) {
                    Text(product.nameRu)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .lineLimit(1)

                    HStack(spacing: 6) {
                        MiniMacroChip(label: "ккал", value: "\(Int(product.caloriesPer100g))")
                        MiniMacroChip(label: "Б", value: shortMacro(product.proteinPer100g))
                        MiniMacroChip(label: "Ж", value: shortMacro(product.fatPer100g))
                        MiniMacroChip(label: "У", value: shortMacro(product.carbsPer100g))
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .strokeBorder(Color(red: 0.929, green: 0.929, blue: 0.937), lineWidth: 1.5)
            )
        }
        .buttonStyle(.plain)
    }
}

private struct MiniMacroChip: View {
    let label: String
    let value: String

    var body: some View {
        HStack(spacing: 3) {
            Text(label).font(.system(size: 14, weight: .medium))
                .foregroundColor(Palette.deepInk)
            Text(value).font(.system(size: 14, weight: .medium))
                .foregroundColor(Palette.deepInk)
        }
        .padding(.horizontal, 6)
        .padding(.vertical, 3)
        .background(Capsule().fill(Palette.softCard))
    }
}

private struct InfoStep: View {
    @ObservedObject var vm: CreateMealPlanViewModelWrapper
    let onBack: () -> Void
    let onChooseGrams: (FoodProduct) -> Void

    var body: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: "Выбор продукта",
                showBackNavigation: true,
                onBackTap: onBack
            )

            if let p = vm.infoFor {
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 16) {
                        ZStack {
                            categoryBg(p.category)
                            Image(foodCategoryImageName(p.category))
                                .resizable()
                                .aspectRatio(1, contentMode: .fill)
                                .clipped()
                        }
                        .aspectRatio(1, contentMode: .fit)
                        .frame(maxWidth: .infinity)
                        .clipShape(RoundedRectangle(cornerRadius: 24))
                        .padding(.horizontal, 20)

                        VStack(alignment: .leading, spacing: 4) {
                            Text(p.nameRu)
                                .font(.system(size: 24, weight: .heavy))
                                .foregroundColor(Palette.deepInk)

                            Text("БЖУ продукта на 100г")
                                .font(.system(size: 18, weight: .medium))
                                .foregroundColor(Palette.deepInk)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 20)
                        .padding(.top, 16)

                        VStack(spacing: 10) {
                            HStack(spacing: 10) {
                                BigMacroCell(
                                    label: "Калории",
                                    value: "\(Int(p.caloriesPer100g)) ккал",
                                    fg: Color(red: 0.878, green: 0.482, blue: 0.0)
                                )
                                BigMacroCell(
                                    label: "Белки",
                                    value: "\(shortMacro(p.proteinPer100g)) г",
                                    fg: Color(red: 0.039, green: 0.518, blue: 1.0)
                                )
                            }
                            HStack(spacing: 10) {
                                BigMacroCell(
                                    label: "Жиры",
                                    value: "\(shortMacro(p.fatPer100g)) г",
                                    fg: Color(red: 1.0, green: 0.420, blue: 0.420)
                                )
                                BigMacroCell(
                                    label: "Углеводы",
                                    value: "\(shortMacro(p.carbsPer100g)) г",
                                    fg: Color(red: 0.204, green: 0.78, blue: 0.349)
                                )
                            }
                        }
                        .padding(.horizontal, 20)

                        Spacer().frame(height: 100)
                    }
                    .padding(.vertical, 12)
                }

                BottomCtaBar {
                    PrimaryButton(
                        title: "Выбрать порцию",
                        enabled: true,
                        action: { onChooseGrams(p) }
                    )
                }
            }
        }
    }
}

private struct BigMacroCell: View {
    let label: String
    let value: String
    let fg: Color

    var body: some View {
        VStack(spacing: 6) {
            Text(label)
                .font(.system(size: 15, weight: .heavy))
                .foregroundColor(fg)
                .tracking(0.4)
            Text(value)
                .font(.system(size: 20, weight: .heavy))
                .foregroundColor(Palette.deepInk)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 18)
        .background(Color.clear)
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .strokeBorder(fg, lineWidth: 1.5)
        )
    }
}

private struct GramsSheetContent: View {
    let product: FoodProduct
    let onAdd: (Double) -> Void

    @State private var grams: Double = 100
    @FocusState private var isGramsFieldFocused: Bool

    private var portionMacros: FoodPortionMacros {
        product.macrosForGrams(grams: grams)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 12) {
                Image(foodCategoryImageName(product.category))
                    .resizable()
                    .aspectRatio(1, contentMode: .fill)
                    .frame(width: 48, height: 48)
                    .clipShape(RoundedRectangle(cornerRadius: 14))

                VStack(alignment: .leading, spacing: 2) {
                    Text(product.nameRu)
                        .font(.system(size: 18, weight: .heavy))
                        .foregroundColor(Palette.deepInk)
                        .lineLimit(2)
                    Text("\(Int(product.caloriesPer100g)) ккал, Б\(shortMacro(product.proteinPer100g)), Ж\(shortMacro(product.fatPer100g)), У\(shortMacro(product.carbsPer100g)) / 100г")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(Palette.mutedText)
                }
                Spacer()
            }

            HStack(spacing: 12) {
                StepperButton(symbol: "minus", action: { grams = max(1, grams - 10) })
                GramsValueField(grams: $grams, isFocused: _isGramsFieldFocused)
                StepperButton(symbol: "plus", action: { grams = min(5000, grams + 10) })
            }

            HStack(spacing: 8) {
                ForEach([50, 100, 150, 200], id: \.self) { preset in
                    Button(action: { grams = Double(preset) }) {
                        Text("\(preset) г")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(Int(grams) == preset ? .white : Palette.deepInk)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(Int(grams) == preset ? Palette.coral : Palette.softCard)
                            )
                    }
                    .buttonStyle(.plain)
                }
            }

            LiveMacrosBar(macros: portionMacros)

            Button(action: { onAdd(grams) }) {
                Text("Добавить")
                    .font(.system(size: 18, weight: .heavy))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(RoundedRectangle(cornerRadius: 16).fill(Palette.coral))
            }
            .buttonStyle(.plain)

            Spacer().frame(height: 8)
        }
        .padding(.horizontal, 20)
        .padding(.top, 16)
        .background(Palette.warmOffWhite.ignoresSafeArea())
        .onTapGesture {
            isGramsFieldFocused = false
        }
    }
}

private struct EditGramsSheetContent: View {
    let item: AddedMealItem
    let onSave: (Double) -> Void

    @State private var grams: Double
    @FocusState private var isGramsFieldFocused: Bool

    init(item: AddedMealItem, onSave: @escaping (Double) -> Void) {
        self.item = item
        self.onSave = onSave
        self._grams = State(initialValue: item.grams)
    }

    private var portionMacros: FoodPortionMacros {
        item.product.macrosForGrams(grams: grams)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 12) {
                Image(foodCategoryImageName(item.product.category))
                    .resizable()
                    .aspectRatio(1, contentMode: .fill)
                    .frame(width: 48, height: 48)
                    .clipShape(RoundedRectangle(cornerRadius: 14))

                VStack(alignment: .leading, spacing: 2) {
                    Text(item.product.nameRu)
                        .font(.system(size: 18, weight: .heavy))
                        .foregroundColor(Palette.deepInk)
                        .lineLimit(2)
                    Text("\(Int(item.product.caloriesPer100g)) ккал, Б\(shortMacro(item.product.proteinPer100g)), Ж\(shortMacro(item.product.fatPer100g)), У\(shortMacro(item.product.carbsPer100g)) / 100г")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(Palette.mutedText)
                }
                Spacer()
            }

            HStack(spacing: 12) {
                StepperButton(symbol: "minus", action: { grams = max(1, grams - 10) })
                GramsValueField(grams: $grams, isFocused: _isGramsFieldFocused)
                StepperButton(symbol: "plus", action: { grams = min(5000, grams + 10) })
            }

            HStack(spacing: 8) {
                ForEach([50, 100, 150, 200], id: \.self) { preset in
                    Button(action: { grams = Double(preset) }) {
                        Text("\(preset) г")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(Int(grams) == preset ? .white : Palette.deepInk)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(Int(grams) == preset ? Palette.coral : Palette.softCard)
                            )
                    }
                    .buttonStyle(.plain)
                }
            }

            LiveMacrosBar(macros: portionMacros)

            Button(action: { onSave(grams) }) {
                Text("Сохранить")
                    .font(.system(size: 18, weight: .heavy))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(RoundedRectangle(cornerRadius: 16).fill(Palette.coral))
            }
            .buttonStyle(.plain)

            Spacer().frame(height: 8)
        }
        .padding(.horizontal, 20)
        .padding(.top, 16)
        .background(Palette.warmOffWhite.ignoresSafeArea())
        .onTapGesture {
            isGramsFieldFocused = false
        }
    }
}

private struct GramsValueField: View {
    @Binding var grams: Double
    @FocusState var isFocused: Bool

    @State private var text: String = ""

    var body: some View {
        TextField("100", text: $text)
            .multilineTextAlignment(.center)
            .keyboardType(.numberPad)
            .font(.system(size: 18, weight: .bold))
            .foregroundColor(Palette.deepInk)
            .focused($isFocused)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(Palette.softCard)
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .onAppear { text = "\(Int(grams))" }
            .onChange(of: text) { newValue in
                let cleaned = newValue.filter { $0.isNumber }
                if cleaned != newValue { text = cleaned }
                if let parsed = Double(cleaned), parsed > 0, parsed <= 5000 {
                    grams = parsed
                }
            }
            .onChange(of: grams) { newValue in
                let formatted = "\(Int(newValue))"
                if formatted != text { text = formatted }
            }
    }
}

private struct StepperButton: View {
    let symbol: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Palette.deepInk)
                .frame(width: 52, height: 52)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .overlay(
                    RoundedRectangle(cornerRadius: 14)
                        .strokeBorder(Color(red: 0.929, green: 0.929, blue: 0.937), lineWidth: 1.5)
                )
        }
        .buttonStyle(.plain)
    }
}

private struct LiveMacrosBar: View {
    let macros: FoodPortionMacros

    var body: some View {
        HStack(spacing: 6) {
            macroBlock(label: "ккал", value: "\(Int(macros.calories.rounded()))")
            Divider().frame(height: 24)
            macroBlock(label: "Б", value: "\(shortMacro(macros.proteinG))")
            Divider().frame(height: 24)
            macroBlock(label: "Ж", value: "\(shortMacro(macros.fatG))")
            Divider().frame(height: 24)
            macroBlock(label: "У", value: "\(shortMacro(macros.carbsG))")
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .padding(.horizontal, 14)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .strokeBorder(Color(red: 0.929, green: 0.929, blue: 0.937), lineWidth: 1.5)
        )
    }

    private func macroBlock(label: String, value: String) -> some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.system(size: 18, weight: .medium))
                .foregroundColor(Palette.deepInk)
            Text(label)
                .font(.system(size: 18, weight: .medium))
                .foregroundColor(Palette.deepInk)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct SavingOverlay: View {
    var body: some View {
        ZStack {
            Color.black.opacity(0.35).ignoresSafeArea()
            VStack(spacing: 12) {
                ProgressView().scaleEffect(1.3).tint(Palette.coral)
                Text("Сохраняем рацион...")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
            }
            .padding(24)
            .background(
                RoundedRectangle(cornerRadius: 18).fill(Color.white)
            )
        }
    }
}

private struct BottomCtaBar<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack {
            content()
        }
        .padding(.horizontal, 20)
        .padding(.top, 12)
        .padding(.bottom, 24)
        .background(
            Palette.warmOffWhite
                .ignoresSafeArea(edges: .bottom)
                .shadow(color: Color.black.opacity(0.05), radius: 8, y: -4)
        )
    }
}

private struct PrimaryButton: View {
    let title: String
    let enabled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: { if enabled { action() } }) {
            Text(title)
                .font(.system(size: 18, weight: .heavy))
                .foregroundColor(enabled ? .white : Palette.mutedText)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .background(
                    RoundedRectangle(cornerRadius: 16)
                        .fill(enabled ? Palette.coral : Palette.softCard)
                )
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

private struct CenteredSpinner: View {
    var body: some View {
        VStack {
            Spacer()
            ProgressView().tint(Palette.coral)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct CenteredMessage: View {
    let iconName: String
    let title: String
    let ctaTitle: String?
    let onCta: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Spacer()
            Image(systemName: iconName)
                .font(.system(size: 36))
                .foregroundColor(Palette.accentOrange)
            Text(title)
                .font(.system(size: 14))
                .foregroundColor(Palette.mutedText)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
            if let ctaTitle = ctaTitle {
                Button(action: onCta) {
                    Text(ctaTitle)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 22)
                        .padding(.vertical, 10)
                        .background(Capsule().fill(Palette.coral))
                }
                .buttonStyle(.plain)
            }
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private func shortMacro(_ value: Double) -> String {
    if value == value.rounded() { return "\(Int(value))" }
    return String(format: "%.1f", value)
}

private func categoryBg(_ category: FoodCategory) -> Color {
    if category == FoodCategory.meat { return Color(red: 1.000, green: 0.910, blue: 0.878) }
    if category == FoodCategory.fish { return Color(red: 0.878, green: 0.941, blue: 1.000) }
    if category == FoodCategory.dairy { return Color(red: 1.000, green: 0.973, blue: 0.910) }
    if category == FoodCategory.eggs { return Color(red: 1.000, green: 0.957, blue: 0.839) }
    if category == FoodCategory.grains { return Color(red: 1.000, green: 0.941, blue: 0.839) }
    if category == FoodCategory.legumes { return Color(red: 0.910, green: 0.969, blue: 0.910) }
    if category == FoodCategory.vegetables { return Color(red: 0.878, green: 0.961, blue: 0.910) }
    if category == FoodCategory.fruits { return Color(red: 0.992, green: 0.910, blue: 0.941) }
    if category == FoodCategory.nutsSeeds { return Color(red: 0.961, green: 0.929, blue: 0.878) }
    if category == FoodCategory.oils { return Color(red: 1.000, green: 0.976, blue: 0.878) }
    return Palette.softCard
}
