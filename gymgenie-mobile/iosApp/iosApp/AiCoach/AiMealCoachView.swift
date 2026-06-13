import SwiftUI
import Shared

struct AiMealCoachView: View {
    @EnvironmentObject private var profileStore: UserProfileStoreWrapper
    @StateObject private var vm = AiMealViewModelWrapper()

    @State private var showProfile = false
    @State private var showGoal = false
    @State private var showRestrictions = false
    @State private var showChat = false

    var onClose: (() -> Void)? = nil

    var body: some View {
        NavigationStack {
            AiMealTypeChooseScreen(
                selectedType: vm.selectedMealType,
                onClose: { onClose?() },
                onSelectType: { type in
                    vm.setMealType(type)
                },
                onContinue: {
                    vm.goTo(step: .profile)
                    showProfile = true
                }
            )
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(isPresented: $showProfile) {
                AiMealProfileScreen(
                    profile: vm.profile,
                    onBack: { showProfile = false },
                    onNext: {
                        vm.goTo(step: .goal)
                        showGoal = true
                    },
                    onAge: { vm.setAge($0) },
                    onHeight: { vm.setHeight($0) },
                    onWeight: { vm.setWeight($0) }
                )
                .toolbar(.hidden, for: .navigationBar)
                .navigationDestination(isPresented: $showGoal) {
                    AiMealGoalScreen(
                        selectedGoal: vm.goal,
                        onBack: { showGoal = false },
                        onNext: {
                            vm.goTo(step: .restrictions)
                            showRestrictions = true
                        },
                        onSelect: { vm.setGoal($0) }
                    )
                    .toolbar(.hidden, for: .navigationBar)
                    .navigationDestination(isPresented: $showRestrictions) {
                        AiMealRestrictionsScreen(
                            dietaryRestrictions: vm.dietaryRestrictions,
                            allergies: vm.allergies,
                            onBack: { showRestrictions = false },
                            onNext: {
                                vm.goTo(step: .chat)
                                showChat = true
                            },
                            onDietary: { vm.setDietaryRestrictions($0) },
                            onAllergies: { vm.setAllergies($0) }
                        )
                        .toolbar(.hidden, for: .navigationBar)
                        .navigationDestination(isPresented: $showChat) {
                            AiMealChatScreen(
                                messages: vm.messages,
                                isTyping: vm.isTyping,
                                hasMealPlan: vm.lastMealPlan != nil,
                                savedPlanId: vm.savedPlanId,
                                isSaving: vm.isSaving,
                                isSaved: vm.isSaved,
                                errorMessage: vm.errorMessage,
                                showSchedulePicker: vm.showSchedulePicker,
                                scheduleMode: vm.scheduleMode,
                                selectedDate: vm.selectedDate,
                                selectedWeekdays: vm.selectedWeekdays,
                                bookedOneOffDates: vm.bookedOneOffDates,
                                bookedRecurringDays: vm.bookedRecurringDays,
                                showConflictDialog: vm.showConflictDialog,
                                conflicts: vm.conflicts,
                                onBack: { showChat = false },
                                onSend: { vm.sendMessage($0) },
                                onSave: { vm.onAddPlanTapped() },
                                onScheduleMode: { vm.setScheduleMode($0) },
                                onSelectDate: { vm.setSelectedDate($0) },
                                onToggleWeekday: { vm.toggleWeekday($0) },
                                onSaveWithSchedule: { vm.saveWithSchedule() },
                                onDismissSchedulePicker: { vm.dismissSchedulePicker() },
                                onConfirmReplace: { vm.confirmReplace() },
                                onDismissConflict: { vm.dismissConflictDialog() }
                            )
                            .toolbar(.hidden, for: .navigationBar)
                        }
                    }
                }
            }
        }
        .onAppear {
            vm.refreshFromStore()
        }
        .onChange(of: profileStore.profile?.id) { _ in
            vm.refreshFromStore()
        }
        .onChange(of: showProfile) { showing in
            if !showing {
                showGoal = false
                showRestrictions = false
                showChat = false
            }
        }
        .onChange(of: showGoal) { showing in
            if !showing {
                showRestrictions = false
                showChat = false
            }
        }
        .onChange(of: showRestrictions) { showing in
            if !showing {
                showChat = false
            }
        }
    }
}

struct AiMealTypeChooseScreen: View {
    let selectedType: AiMealType?
    let onClose: () -> Void
    let onSelectType: (AiMealType) -> Void
    let onContinue: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: "План питания",
                showBackNavigation: true,
                onBackTap: onClose
            )

            ScrollView {
                VStack(alignment: .center, spacing: 0) {
                    Spacer().frame(height: 8)
                    Text("Какой приём пищи составить?")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, 20)
                    Spacer().frame(height: 32)

                    VStack(spacing: 12) {
                        ForEach(
                            [AiMealType.breakfast, .lunch, .dinner],
                            id: \.wireValue
                        ) { type in
                            AiMealTypeCard(
                                type: type,
                                isSelected: selectedType == type,
                                onTap: { onSelectType(type) }
                            )
                        }
                    }
                    .padding(.horizontal, 20)
                }
            }

            AiMealPrimaryButton(
                title: "Продолжить",
                enabled: selectedType != nil,
                action: onContinue
            )
        }
        .background(Palette.warmOffWhite)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct AiMealTypeCard: View {
    let type: AiMealType
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(alignment: .center, spacing: 14) {
                Image({
                    switch type.wireValue {
                    case "BREAKFAST": return "ic_breakfast"
                    case "LUNCH": return "ic_lunch"
                    case "DINNER": return "ic_dinner"
                    default: return "ic_lunch"
                    }
                }())
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 28, height: 28)
                VStack(alignment: .leading, spacing: 4) {
                    Text(type.displayName)
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(Palette.deepInk)
                }
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 24))
                        .foregroundColor(Palette.coral)
                }
            }
            .padding(.vertical, 18)
            .padding(.horizontal, 18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white)
            .cornerRadius(16)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(isSelected ? Palette.coral : Color(aiMealHex: "EEEEEE"),
                            lineWidth: isSelected ? 2 : 1.5)
            )
        }
        .buttonStyle(.plain)
    }
}

struct AiMealProfileScreen: View {
    let profile: AiMealProfileData
    let onBack: () -> Void
    let onNext: () -> Void
    let onAge: (Int) -> Void
    let onHeight: (Int) -> Void
    let onWeight: (Int) -> Void

    var canProceed: Bool { profile.isProfileFilled }

    var body: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: "План питания",
                showBackNavigation: true,
                onBackTap: {
                    dismissAiMealFlowKeyboard()
                    onBack()
                }
            )
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer().frame(height: 8)
                    Text("Ваши параметры")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .padding(.horizontal, 20)
                    Spacer().frame(height: 24)
                    AiMealSliderField(
                        label: "Возраст",
                        value: Int(profile.age),
                        min: 15,
                        max: 80,
                        onChange: onAge
                    )
                    AiMealSliderField(
                        label: "Рост (см)",
                        value: Int(profile.height),
                        min: 140,
                        max: 220,
                        onChange: onHeight
                    )
                    AiMealSliderField(
                        label: "Вес (кг)",
                        value: Int(profile.weight),
                        min: 40,
                        max: 180,
                        onChange: onWeight
                    )
                }
            }
            AiMealPrimaryButton(title: "Далее", enabled: canProceed, action: onNext)
        }
        .background(Palette.warmOffWhite)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct AiMealSliderField: View {
    let label: String
    let value: Int
    let min: Int
    let max: Int
    let onChange: (Int) -> Void

    @State private var isEditing: Bool = false
    @State private var inputText: String = ""
    @FocusState private var inputFocused: Bool

    private var pct: CGFloat {
        guard max > min else { return 0 }
        return CGFloat(value - min) / CGFloat(max - min)
    }

    private func commitInput() {
        let digits = inputText.filter { $0.isNumber }
        if let parsed = Int(digits) {
            let clamped = parsed.aiMealClamped(to: min...max)
            onChange(clamped)
        }
        isEditing = false
        inputFocused = false
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .bottom) {
                Text(label)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Color(aiMealHex: "555555"))
                Spacer()
                if isEditing {
                    TextField("", text: $inputText)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.trailing)
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .frame(maxWidth: 120)
                        .focused($inputFocused)
                        .onSubmit { commitInput() }
                        .onChange(of: inputText) { newValue in
                            let filtered = newValue.filter { $0.isNumber }
                            if filtered != newValue { inputText = filtered }
                        }
                        .onChange(of: inputFocused) { focused in
                            if !focused { commitInput() }
                        }
                } else {
                    Text(value == 0 ? "—" : "\(value)")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .contentShape(Rectangle())
                        .onTapGesture {
                            inputText = value == 0 ? "" : "\(value)"
                            isEditing = true
                            DispatchQueue.main.async { inputFocused = true }
                        }
                }
            }
            Spacer().frame(height: 10)

            GeometryReader { geo in
                let w = geo.size.width
                let thumbSize: CGFloat = 24
                let thumbX = pct * (w - thumbSize)

                ZStack(alignment: .leading) {
                    Capsule().fill(Color(aiMealHex: "E0E0E0")).frame(height: 6)
                    Capsule().fill(Palette.coral)
                        .frame(width: Swift.max(0, thumbX + thumbSize / 2), height: 6)
                    Circle()
                        .fill(Palette.coral)
                        .overlay(
                            Circle()
                                .stroke(Color.white, lineWidth: 2.5)
                        )
                        .shadow(color: Palette.coral.opacity(0.35), radius: 3)
                        .frame(width: thumbSize, height: thumbSize)
                        .offset(x: thumbX)
                }
                .contentShape(Rectangle())
                .highPriorityGesture(
                    DragGesture(minimumDistance: 0)
                        .onChanged { drag in
                            let ratio = (drag.location.x / w).aiMealClamped(to: 0...1)
                            let v = Int(Double(min) + Double(ratio) * Double(max - min))
                            onChange(v.aiMealClamped(to: min...max))
                        }
                )
            }
            .frame(height: 24)

            Spacer().frame(height: 4)
            HStack {
                Text("\(min)").font(.system(size: 13)).foregroundColor(Color(aiMealHex: "AAAAAA"))
                Spacer()
                Text("\(max)").font(.system(size: 13)).foregroundColor(Color(aiMealHex: "AAAAAA"))
            }
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 28)
    }
}

struct AiMealGoalScreen: View {
    let selectedGoal: String
    let onBack: () -> Void
    let onNext: () -> Void
    let onSelect: (String) -> Void

    var canProceed: Bool { !selectedGoal.isEmpty }

    private struct GoalOption: Identifiable {
        let id: String
        let imageName: String
        let title: String
        let subtitle: String
    }

    private let options: [GoalOption] = [
        GoalOption(id: MealGoal.loseWeight.wireValue, imageName: "ic_ai_weight_loss",
                   title: MealGoal.loseWeight.displayName,
                   subtitle: "Дефицит калорий, лёгкий рацион"),
        GoalOption(id: MealGoal.maintain.wireValue, imageName: "ic_ai_keeping_fit",
                   title: MealGoal.maintain.displayName,
                   subtitle: "Сбалансированное питание"),
        GoalOption(id: MealGoal.gainMuscle.wireValue, imageName: "ic_ai_muscles",
                   title: MealGoal.gainMuscle.displayName,
                   subtitle: "Профицит и больше белка"),
    ]

    var body: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: "План питания",
                showBackNavigation: true,
                onBackTap: {
                    dismissAiMealFlowKeyboard()
                    onBack()
                }
            )
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer().frame(height: 8)
                    Text("Ваша цель")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .padding(.horizontal, 20)
                    Spacer().frame(height: 24)

                    VStack(spacing: 12) {
                        ForEach(options) { option in
                            AiMealGoalCard(
                                imageName: option.imageName,
                                title: option.title,
                                subtitle: option.subtitle,
                                isSelected: selectedGoal == option.id,
                                action: { onSelect(option.id) }
                            )
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 12)
                }
            }
            AiMealPrimaryButton(title: "Далее", enabled: canProceed, action: onNext)
        }
        .background(Palette.warmOffWhite)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct AiMealGoalCard: View {
    let imageName: String
    let title: String
    let subtitle: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(alignment: .center, spacing: 14) {
                Image(imageName)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 32, height: 32)
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(Palette.deepInk)
                    Text(subtitle)
                        .font(.system(size: 14))
                        .foregroundColor(Color(aiMealHex: "888888"))
                }
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 24))
                        .foregroundColor(Palette.coral)
                }
            }
            .padding(.vertical, 18)
            .padding(.horizontal, 18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white)
            .cornerRadius(16)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(isSelected ? Palette.coral : Color(aiMealHex: "EEEEEE"),
                            lineWidth: isSelected ? 2 : 1.5)
            )
        }
        .buttonStyle(.plain)
    }
}

struct AiMealRestrictionsScreen: View {
    let dietaryRestrictions: String
    let allergies: String
    let onBack: () -> Void
    let onNext: () -> Void
    let onDietary: (String) -> Void
    let onAllergies: (String) -> Void

    @FocusState private var focusedField: Field?

    private enum Field { case dietary, allergies }

    var body: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: "План питания",
                showBackNavigation: true,
                onBackTap: {
                    dismissAiMealFlowKeyboard()
                    onBack()
                }
            )
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer().frame(height: 8)
                    Text("Ограничения и аллергии")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .padding(.horizontal, 20)
                    Text("Можно оставить пустым")
                        .font(.system(size: 15))
                        .foregroundColor(Color(aiMealHex: "888888"))
                        .padding(.horizontal, 20)
                        .padding(.top, 4)
                    Spacer().frame(height: 24)

                    AiMealTextEditorField(
                        label: "Ограничения в питании",
                        placeholder: "вегетарианец, без глютена...",
                        text: dietaryRestrictions,
                        onChange: onDietary,
                        isFocused: focusedField == .dietary,
                        focus: { focusedField = .dietary }
                    )
                    .padding(.horizontal, 20)
                    .padding(.bottom, 20)

                    AiMealTextEditorField(
                        label: "Аллергии",
                        placeholder: "орехи, молочные продукты...",
                        text: allergies,
                        onChange: onAllergies,
                        isFocused: focusedField == .allergies,
                        focus: { focusedField = .allergies }
                    )
                    .padding(.horizontal, 20)
                    .padding(.bottom, 28)
                }
                .contentShape(Rectangle())
                .onTapGesture { focusedField = nil }
            }
            AiMealPrimaryButton(title: "Далее", enabled: true, action: onNext)
        }
        .background(Palette.warmOffWhite)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct AiMealTextEditorField: View {
    let label: String
    let placeholder: String
    let text: String
    let onChange: (String) -> Void
    let isFocused: Bool
    let focus: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(Color(aiMealHex: "555555"))
            ZStack(alignment: .topLeading) {
                if text.isEmpty {
                    Text(placeholder)
                        .font(.system(size: 16))
                        .foregroundColor(Color(aiMealHex: "AAAAAA"))
                        .padding(14)
                        .allowsHitTesting(false)
                }
                TextEditor(text: Binding(
                    get: { text },
                    set: onChange
                ))
                .font(.system(size: 16))
                .foregroundColor(Palette.deepInk)
                .frame(minHeight: 90)
                .padding(10)
                .scrollContentBackground(.hidden)
                .background(Color.clear)
                .onTapGesture { focus() }
            }
            .background(Color.white)
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isFocused ? Palette.coral : Color(aiMealHex: "E0E0E0"), lineWidth: 2)
            )
        }
    }
}

struct AiMealChatScreen: View {
    let messages: [AiMealChatMessage]
    let isTyping: Bool
    let hasMealPlan: Bool
    let savedPlanId: String?
    let isSaving: Bool
    let isSaved: Bool
    let errorMessage: String?
    let showSchedulePicker: Bool
    let scheduleMode: String
    let selectedDate: String?
    let selectedWeekdays: [String]
    let bookedOneOffDates: [String]
    let bookedRecurringDays: [String]
    let showConflictDialog: Bool
    let conflicts: [AiMealConflictPlan]
    let onBack: () -> Void
    let onSend: (String) -> Void
    let onSave: () -> Void
    let onScheduleMode: (String) -> Void
    let onSelectDate: (String?) -> Void
    let onToggleWeekday: (String) -> Void
    let onSaveWithSchedule: () -> Void
    let onDismissSchedulePicker: () -> Void
    let onConfirmReplace: () -> Void
    let onDismissConflict: () -> Void

    @State private var input: String = ""
    @FocusState private var inputFocused: Bool

    private var saveButtonTitle: String {
        if isSaving { return "Сохранение..." }
        return savedPlanId != nil ? "✓ Обновить рацион" : "✓ Сохранить рацион"
    }

    var body: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: "План питания",
                showBackNavigation: true,
                onBackTap: {
                    dismissAiMealFlowKeyboard()
                    onBack()
                }
            )

            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 12) {
                        if messages.isEmpty && !isTyping {
                            VStack(spacing: 0) {
                                Spacer().frame(height: 40)
                                Image("ic_chatbot_preview")
                                    .resizable()
                                    .aspectRatio(contentMode: .fit)
                                    .padding(.horizontal, 32)
                                    .frame(maxWidth: .infinity)
                                Spacer().frame(height: 16)
                                Text("Опишите, какой рацион вы хотите получить")
                                    .font(.system(size: 18, weight: .semibold))
                                    .foregroundColor(Color(aiMealHex: "AAAAAA"))
                                    .multilineTextAlignment(.center)
                                    .padding(.horizontal, 32)
                                Text("Например: «1700 ккал, без молочного, без глютена»")
                                    .font(.system(size: 13))
                                    .foregroundColor(Color(aiMealHex: "888888"))
                                    .multilineTextAlignment(.center)
                                    .padding(.horizontal, 32)
                                    .padding(.top, 8)
                            }
                            .frame(maxWidth: .infinity)
                            .id("empty")
                        }
                        ForEach(Array(messages.enumerated()), id: \.offset) { _, msg in
                            AiMealChatBubbleView(message: msg)
                        }
                        if isTyping { AiMealTypingBubbleView() }
                        Color.clear.frame(height: 1).id("bottom")
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 8)
                    .contentShape(Rectangle())
                    .onTapGesture { inputFocused = false }
                }
                .onChange(of: messages.count) { _ in
                    withAnimation { proxy.scrollTo("bottom", anchor: .bottom) }
                }
                .onChange(of: isTyping) { _ in
                    withAnimation { proxy.scrollTo("bottom", anchor: .bottom) }
                }
            }

            if let err = errorMessage {
                Text(err)
                    .font(.system(size: 13))
                    .foregroundColor(.red)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 4)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            if hasMealPlan && !isSaved && !showSchedulePicker {
                Button(action: {
                    inputFocused = false
                    onSave()
                }) {
                    Text("Добавить план")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(Color(aiMealHex: "22C55E"))
                        .cornerRadius(28)
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 8)
            }

            if showSchedulePicker {
                VStack(spacing: 12) {
                    AiMealScheduleToggle(
                        mode: scheduleMode,
                        onSelect: onScheduleMode
                    )
                    .padding(.horizontal, 20)

                    if scheduleMode == "ONE_OFF" {
                        AiMealDateStrip(
                            bookedDates: bookedOneOffDates,
                            selectedDate: selectedDate,
                            onSelect: onSelectDate
                        )
                        .padding(.horizontal, 20)
                    } else {
                        AiMealWeekdayChips(
                            bookedDays: bookedRecurringDays,
                            selectedDays: selectedWeekdays,
                            onToggle: onToggleWeekday
                        )
                        .padding(.horizontal, 20)
                    }

                    HStack(spacing: 12) {
                        Button(action: onDismissSchedulePicker) {
                            Text("Отмена")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(Color(aiMealHex: "888888"))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .background(Color(aiMealHex: "F3F4F6"))
                                .cornerRadius(24)
                        }

                        let canSaveSchedule = (scheduleMode == "ONE_OFF" && selectedDate != nil)
                            || (scheduleMode == "RECURRING" && !selectedWeekdays.isEmpty)
                        Button(action: onSaveWithSchedule) {
                            Text(isSaving ? "Сохранение..." : "Сохранить")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .background(canSaveSchedule && !isSaving ? Color(aiMealHex: "22C55E") : Color(aiMealHex: "22C55E").opacity(0.4))
                                .cornerRadius(24)
                        }
                        .disabled(!canSaveSchedule || isSaving)
                    }
                    .padding(.horizontal, 20)
                }
                .padding(.vertical, 12)
                .background(Color.white)
                .cornerRadius(16)
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(Color(aiMealHex: "EEEEEE"), lineWidth: 1)
                )
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }

            if isSaved {
                Text("Рацион сохранён! ✓")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Color(aiMealHex: "16A34A"))
                    .frame(maxWidth: .infinity)
                    .padding(14)
                    .background(Color(aiMealHex: "F0FDF4"))
                    .cornerRadius(14)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 8)
            }

            HStack(spacing: 8) {
                ZStack(alignment: .topLeading) {
                    if input.isEmpty {
                        Text("Напишите запрос...")
                            .font(.system(size: 16))
                            .foregroundColor(Color(aiMealHex: "AAAAAA"))
                            .padding(.horizontal, 16)
                            .padding(.vertical, 16)
                    }
                    TextField("", text: $input, axis: .vertical)
                        .font(.system(size: 16))
                        .foregroundColor(Palette.deepInk)
                        .focused($inputFocused)
                        .lineLimit(1...4)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 16)
                }
                .frame(minHeight: 56)
                .background(Color.white)
                .cornerRadius(24)
                .overlay(
                    RoundedRectangle(cornerRadius: 24)
                        .stroke(inputFocused ? Palette.coral : Color(aiMealHex: "E0E0E0"), lineWidth: 2)
                )

                let canSend = !input.trimmingCharacters(in: .whitespaces).isEmpty && !isTyping
                Button(action: {
                    guard canSend else { return }
                    inputFocused = false
                    let text = input
                    input = ""
                    onSend(text)
                }) {
                    Image("ic_send")
                        .resizable()
                        .renderingMode(.template)
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 20, height: 20)
                        .foregroundColor(.white)
                        .frame(width: 44, height: 44)
                        .background(canSend ? Palette.coral : Color(aiMealHex: "E0E0E0"))
                        .clipShape(Circle())
                }
                .disabled(!canSend)
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .background(Color.white)
            .overlay(Rectangle().frame(height: 1).foregroundColor(Color(aiMealHex: "EEEEEE")), alignment: .top)
        }
        .background(Palette.warmOffWhite)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .alert("Заменить существующий план?", isPresented: .constant(showConflictDialog)) {
            Button("Отмена", role: .cancel) { onDismissConflict() }
            Button("Заменить", role: .destructive) { onConfirmReplace() }
        } message: {
            let names = conflicts.map { $0.planName }.joined(separator: ", ")
            Text("На выбранные даты уже есть план: \u{00AB}\(names)\u{00BB}. Заменить его новым?")
        }
    }
}

private struct AiMealChatBubbleView: View {
    let message: AiMealChatMessage
    var isUser: Bool { message.role == AiMealChatMessage.Role.user }

    var body: some View {
        HStack {
            if isUser { Spacer(minLength: 48) }
            Text(message.text)
                .font(.system(size: 14))
                .foregroundColor(isUser ? .white : Palette.deepInk)
                .lineSpacing(3)
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(isUser ? Palette.coral : Color.white)
                .cornerRadius(18)
                .overlay(
                    RoundedRectangle(cornerRadius: 18)
                        .stroke(isUser ? Color.clear : Color(aiMealHex: "EEEEEE"), lineWidth: 1)
                )
            if !isUser { Spacer(minLength: 48) }
        }
        .frame(maxWidth: .infinity, alignment: isUser ? .trailing : .leading)
    }
}

private struct AiMealTypingBubbleView: View {
    @State private var animating = false

    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<3, id: \.self) { idx in
                Circle()
                    .fill(Color(aiMealHex: "AAAAAA"))
                    .frame(width: 8, height: 8)
                    .opacity(animating ? 1.0 : 0.3)
                    .animation(
                        .easeInOut(duration: 0.4)
                            .repeatForever(autoreverses: true)
                            .delay(Double(idx) * 0.15),
                        value: animating
                    )
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .background(Color.white)
        .cornerRadius(18)
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(Color(aiMealHex: "EEEEEE"), lineWidth: 1))
        .onAppear { animating = true }
    }
}

private struct AiMealScheduleToggle: View {
    let mode: String
    let onSelect: (String) -> Void

    var body: some View {
        HStack(spacing: 0) {
            toggleBtn(title: "Разово", target: "ONE_OFF", isSelected: mode == "ONE_OFF")
            toggleBtn(title: "По дням", target: "RECURRING", isSelected: mode == "RECURRING")
        }
        .padding(4)
        .background(RoundedRectangle(cornerRadius: 14).fill(Color(aiMealHex: "F3F4F6")))
    }

    private func toggleBtn(title: String, target: String, isSelected: Bool) -> some View {
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

private struct AiMealDateStrip: View {
    let bookedDates: [String]
    let selectedDate: String?
    let onSelect: (String?) -> Void

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
                        onSelect(isSelected ? nil : entry.iso)
                    }) {
                        VStack(spacing: 4) {
                            Text(entry.weekday)
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(isSelected ? .white : Color(aiMealHex: "888888"))
                            Text("\(entry.day)")
                                .font(.system(size: 18, weight: .heavy))
                                .foregroundColor(isSelected ? .white : (isBooked ? Color(aiMealHex: "888888") : Palette.deepInk))
                            if isBooked && !isSelected {
                                Image(systemName: "lock.fill")
                                    .font(.system(size: 9))
                                    .foregroundColor(Color(aiMealHex: "888888"))
                            }
                        }
                        .frame(width: 56, height: 70)
                        .background(
                            RoundedRectangle(cornerRadius: 14)
                                .fill(isSelected ? Palette.coral : (isBooked ? Color(aiMealHex: "F3F4F6") : Color.white))
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .strokeBorder(
                                    isSelected ? Palette.coral : Color(aiMealHex: "EDEDEF"),
                                    lineWidth: 1.5
                                )
                        )
                        .opacity(isBooked && !isSelected ? 0.55 : 1.0)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}

private struct AiMealWeekdayChips: View {
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
                    Button(action: { onToggle(day.wire) }) {
                        HStack(spacing: 4) {
                            Text(day.label)
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(isSelected ? .white : (isBooked ? Color(aiMealHex: "888888") : Palette.deepInk))
                            if isBooked && !isSelected {
                                Image(systemName: "lock.fill").font(.system(size: 9))
                                    .foregroundColor(Color(aiMealHex: "888888"))
                            }
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(
                            Capsule().fill(isSelected ? Palette.coral : (isBooked ? Color(aiMealHex: "F3F4F6") : Color.white))
                        )
                        .overlay(
                            Capsule().strokeBorder(
                                isSelected ? Palette.coral : Color(aiMealHex: "EDEDEF"),
                                lineWidth: 1.5
                            )
                        )
                        .opacity(isBooked && !isSelected ? 0.55 : 1.0)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}

private struct AiMealPrimaryButton: View {
    let title: String
    let enabled: Bool
    let action: () -> Void
    var color: Color = Palette.coral

    var body: some View {
        Button(action: {
            dismissAiMealFlowKeyboard()
            action()
        }) {
            Text(title)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .background(color.opacity(enabled ? 1.0 : 0.4))
                .cornerRadius(28)
        }
        .disabled(!enabled)
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
    }
}

fileprivate func dismissAiMealFlowKeyboard() {
    UIApplication.shared.sendAction(
        #selector(UIResponder.resignFirstResponder),
        to: nil,
        from: nil,
        for: nil
    )
}

fileprivate extension Comparable {
    func aiMealClamped(to range: ClosedRange<Self>) -> Self {
        Swift.min(Swift.max(self, range.lowerBound), range.upperBound)
    }
}

fileprivate extension Color {
    init(aiMealHex hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default: (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(.sRGB, red: Double(r) / 255, green: Double(g) / 255, blue: Double(b) / 255, opacity: Double(a) / 255)
    }
}
