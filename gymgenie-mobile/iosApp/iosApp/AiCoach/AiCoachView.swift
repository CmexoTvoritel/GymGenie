import SwiftUI
import Shared

struct AiCoachView: View {
    @EnvironmentObject private var appState: AppState
    @EnvironmentObject private var profileStore: UserProfileStoreWrapper
    @EnvironmentObject private var tabBarState: TabBarState
    @StateObject private var vm = AiViewModelWrapper()

    @State private var showProfile = false
    @State private var showExperience = false
    @State private var showHealth = false
    @State private var showChat = false

    @State private var showMealFlow = false
    @StateObject private var mealVm = AiMealViewModelWrapper()
    @State private var showMealProfile = false
    @State private var showMealGoal = false
    @State private var showMealRestrictions = false
    @State private var showMealChat = false

    @State private var showWorkoutExitAlert = false
    @State private var showMealExitAlert = false

    private var isPremium: Bool {
        guard let sub = profileStore.profile?.subscriptionType else { return false }
        return sub != "FREE"
    }

    private var needsWorkoutExitConfirmation: Bool {
        !vm.isSaved && (!vm.messages.isEmpty || vm.isTyping)
    }

    private var needsMealExitConfirmation: Bool {
        !mealVm.isSaved && (!mealVm.messages.isEmpty || mealVm.isTyping)
    }

    var body: some View {
        ZStack {
            Palette.warmOffWhite.ignoresSafeArea()
            if isPremium {
                AiChooseScreen(
                    onNext: {
                        vm.goTo(step: .profile)
                        showProfile = true
                    },
                    onMealNext: {
                        showMealFlow = true
                    }
                )
            } else {
                PremiumLockedOverlay(onUnlock: { appState.navigate(to: .paywall) })
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .onAppear {
            vm.refreshFromStore()
        }
        .onChange(of: profileStore.profile?.id) { _ in
            vm.refreshFromStore()
        }
        .onChange(of: showProfile) { showing in
            if !showing {
                showExperience = false
                showHealth = false
                showChat = false
                vm.reset()
            }
            tabBarState.isVisible = !showing
        }
        .onChange(of: showExperience) { showing in
            if !showing {
                showHealth = false
                showChat = false
            }
        }
        .onChange(of: showHealth) { showing in
            if !showing {
                showChat = false
            }
        }
        .onChange(of: showMealFlow) { showing in
            if !showing {
                showMealProfile = false
                showMealGoal = false
                showMealRestrictions = false
                showMealChat = false
                mealVm.reset()
            }
            tabBarState.isVisible = !showing
        }
        .onChange(of: showMealProfile) { showing in
            if !showing {
                showMealGoal = false
                showMealRestrictions = false
                showMealChat = false
            }
        }
        .onChange(of: showMealGoal) { showing in
            if !showing {
                showMealRestrictions = false
                showMealChat = false
            }
        }
        .onChange(of: showMealRestrictions) { showing in
            if !showing {
                showMealChat = false
            }
        }
        .navigationDestination(isPresented: $showProfile) {
            AiProfileScreen(
                profile: vm.profile,
                onBack: { showProfile = false },
                onNext: {
                    vm.goTo(step: .experience)
                    showExperience = true
                },
                onAge: { vm.setAge($0) },
                onHeight: { vm.setHeight($0) },
                onWeight: { vm.setWeight($0) }
            )
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(isPresented: $showExperience) {
                AiExperienceScreen(
                    profile: vm.profile,
                    onBack: { showExperience = false },
                    onNext: {
                        vm.goTo(step: .health)
                        showHealth = true
                    },
                    onExperience: { vm.setExperience($0) },
                    onFrequency: { vm.setFrequency($0) }
                )
                .toolbar(.hidden, for: .navigationBar)
                .navigationDestination(isPresented: $showHealth) {
                    AiHealthScreen(
                        profile: vm.profile,
                        onBack: { showHealth = false },
                        onNext: {
                            vm.goTo(step: .chat)
                            showChat = true
                        },
                        onHasLimitations: { vm.setHasLimitations($0) },
                        onLimitationsDesc: { vm.setLimitationsDesc($0) }
                    )
                    .toolbar(.hidden, for: .navigationBar)
                    .navigationDestination(isPresented: $showChat) {
                        AiChatScreen(
                            messages: vm.messages,
                            isTyping: vm.isTyping,
                            hasWorkout: vm.hasWorkout,
                            savedPlanId: vm.savedPlanId,
                            isSaving: vm.isSaving,
                            isSaved: vm.isSaved,
                            errorMessage: vm.errorMessage,
                            onBack: {
                                if needsWorkoutExitConfirmation {
                                    showWorkoutExitAlert = true
                                } else {
                                    showProfile = false
                                }
                            },
                            onSend: { vm.sendMessage($0) },
                            onSave: { vm.saveWorkout() }
                        )
                        .toolbar(.hidden, for: .navigationBar)
                        .alert("Выйти из чата?", isPresented: $showWorkoutExitAlert) {
                            Button("Остаться", role: .cancel) { }
                            Button("Выйти", role: .destructive) {
                                showProfile = false
                            }
                        } message: {
                            Text("Весь прогресс будет потерян. Убедитесь, что план сохранён.")
                        }
                    }
                }
            }
        }
        .navigationDestination(isPresented: $showMealFlow) {
            mealFlowDestination
        }
    }

    @ViewBuilder
    private var mealFlowDestination: some View {
        AiMealTypeChooseScreen(
            selectedType: mealVm.selectedMealType,
            onClose: { showMealFlow = false },
            onSelectType: { type in
                mealVm.setMealType(type)
            },
            onContinue: {
                mealVm.refreshFromStore()
                mealVm.goTo(step: .profile)
                showMealProfile = true
            }
        )
        .toolbar(.hidden, for: .navigationBar)
        .navigationDestination(isPresented: $showMealProfile) {
            mealProfileDestination
        }
    }

    @ViewBuilder
    private var mealProfileDestination: some View {
        AiMealProfileScreen(
            profile: mealVm.profile,
            onBack: { showMealProfile = false },
            onNext: {
                mealVm.goTo(step: .goal)
                showMealGoal = true
            },
            onAge: { mealVm.setAge($0) },
            onHeight: { mealVm.setHeight($0) },
            onWeight: { mealVm.setWeight($0) }
        )
        .toolbar(.hidden, for: .navigationBar)
        .navigationDestination(isPresented: $showMealGoal) {
            mealGoalDestination
        }
    }

    @ViewBuilder
    private var mealGoalDestination: some View {
        AiMealGoalScreen(
            selectedGoal: mealVm.goal,
            onBack: { showMealGoal = false },
            onNext: {
                mealVm.goTo(step: .restrictions)
                showMealRestrictions = true
            },
            onSelect: { mealVm.setGoal($0) }
        )
        .toolbar(.hidden, for: .navigationBar)
        .navigationDestination(isPresented: $showMealRestrictions) {
            mealRestrictionsDestination
        }
    }

    @ViewBuilder
    private var mealRestrictionsDestination: some View {
        AiMealRestrictionsScreen(
            dietaryRestrictions: mealVm.dietaryRestrictions,
            allergies: mealVm.allergies,
            onBack: { showMealRestrictions = false },
            onNext: {
                mealVm.goTo(step: .chat)
                showMealChat = true
            },
            onDietary: { mealVm.setDietaryRestrictions($0) },
            onAllergies: { mealVm.setAllergies($0) }
        )
        .toolbar(.hidden, for: .navigationBar)
        .navigationDestination(isPresented: $showMealChat) {
            mealChatDestination
        }
    }

    @ViewBuilder
    private var mealChatDestination: some View {
        AiMealChatScreen(
            messages: mealVm.messages,
            isTyping: mealVm.isTyping,
            hasMealPlan: mealVm.lastMealPlan != nil,
            savedPlanId: mealVm.savedPlanId,
            isSaving: mealVm.isSaving,
            isSaved: mealVm.isSaved,
            errorMessage: mealVm.errorMessage,
            showSchedulePicker: mealVm.showSchedulePicker,
            scheduleMode: mealVm.scheduleMode,
            selectedDate: mealVm.selectedDate,
            selectedWeekdays: mealVm.selectedWeekdays,
            bookedOneOffDates: mealVm.bookedOneOffDates,
            bookedRecurringDays: mealVm.bookedRecurringDays,
            showConflictDialog: mealVm.showConflictDialog,
            conflicts: mealVm.conflicts,
            onBack: {
                if needsMealExitConfirmation {
                    showMealExitAlert = true
                } else {
                    showMealFlow = false
                }
            },
            onSend: { mealVm.sendMessage($0) },
            onSave: { mealVm.onAddPlanTapped() },
            onScheduleMode: { mealVm.setScheduleMode($0) },
            onSelectDate: { mealVm.setSelectedDate($0) },
            onToggleWeekday: { mealVm.toggleWeekday($0) },
            onSaveWithSchedule: { mealVm.saveWithSchedule() },
            onDismissSchedulePicker: { mealVm.dismissSchedulePicker() },
            onConfirmReplace: { mealVm.confirmReplace() },
            onDismissConflict: { mealVm.dismissConflictDialog() }
        )
        .toolbar(.hidden, for: .navigationBar)
        .alert("Выйти из чата?", isPresented: $showMealExitAlert) {
            Button("Остаться", role: .cancel) { }
            Button("Выйти", role: .destructive) {
                showMealFlow = false
            }
        } message: {
            Text("Весь прогресс будет потерян. Убедитесь, что план сохранён.")
        }
    }
}

private struct AiChooseScreen: View {
    let onNext: () -> Void
    let onMealNext: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(title: "ИИ тренер")

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer().frame(height: 40)
                    Text("Что вы хотите сгенерировать?")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, 20)

                    Spacer().frame(height: 32)

                    VStack(spacing: 14) {
                        GenerateCard(
                            imageName: "ic_ai_workout",
                            title: "План тренировки",
                            subtitle: "Персональная программа на основе ваших данных",
                            enabled: true,
                            action: onNext
                        )
                        GenerateCard(
                            imageName: "ic_ai_meal",
                            title: "План питания",
                            subtitle: "Персональный рацион на основе ваших целей",
                            enabled: true,
                            action: onMealNext
                        )
                    }
                    .padding(.horizontal, 20)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.warmOffWhite)
    }
}

private struct GenerateCard: View {
    let imageName: String
    let title: String
    let subtitle: String
    let enabled: Bool
    let action: () -> Void

    @State private var pressed = false

    var body: some View {
        HStack(alignment: .center, spacing: 16) {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
                Text(subtitle)
                    .font(.system(size: 15))
                    .foregroundColor(Color(hex: "888888"))
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Image(imageName)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 72, height: 72)
        }
        .padding(24)
        .background(Color.white)
        .cornerRadius(16)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(pressed && enabled ? Palette.coral : Color(hex: "E0E0E0"), lineWidth: 2)
        )
        .scaleEffect(pressed && enabled ? 1.02 : 1.0)
        .opacity(enabled ? 1.0 : 0.45)
        .animation(.easeInOut(duration: 0.15), value: pressed)
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in if enabled { pressed = true } }
                .onEnded { _ in
                    pressed = false
                    if enabled { action() }
                }
        )
    }
}

private struct AiProfileScreen: View {
    let profile: AiProfileData
    let onBack: () -> Void
    let onNext: () -> Void
    let onAge: (Int) -> Void
    let onHeight: (Int) -> Void
    let onWeight: (Int) -> Void

    var canProceed: Bool { profile.isProfileFilled }

    var body: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: "План тренировки",
                showBackNavigation: true,
                onBackTap: {
                    dismissAiFlowKeyboard()
                    onBack()
                }
            )
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer().frame(height: 8)
                    Text("Введите данные профиля")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .padding(.horizontal, 20)
                    Spacer().frame(height: 24)
                    SliderField(label: "Возраст", value: Int(profile.age), min: 10, max: 80, onChange: onAge)
                    SliderField(label: "Рост (см)", value: Int(profile.height), min: 100, max: 220, onChange: onHeight)
                    SliderField(label: "Вес (кг)", value: Int(profile.weight), min: 30, max: 150, onChange: onWeight)
                }
            }
            PrimaryButton(title: "Далее", enabled: canProceed, action: onNext)
        }
        .background(Palette.warmOffWhite)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct SliderField: View {
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
            let clamped = parsed.clamped(to: min...max)
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
                    .foregroundColor(Color(hex: "555555"))
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
                    Capsule().fill(Color(hex: "E0E0E0")).frame(height: 6)
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
                            let ratio = (drag.location.x / w).clamped(to: 0...1)
                            let v = Int(Double(min) + Double(ratio) * Double(max - min))
                            onChange(v.clamped(to: min...max))
                        }
                )
            }
            .frame(height: 24)

            Spacer().frame(height: 4)
            HStack {
                Text("\(min)").font(.system(size: 13)).foregroundColor(Color(hex: "AAAAAA"))
                Spacer()
                Text("\(max)").font(.system(size: 13)).foregroundColor(Color(hex: "AAAAAA"))
            }
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 28)
    }
}

private struct AiExperienceScreen: View {
    let profile: AiProfileData
    let onBack: () -> Void
    let onNext: () -> Void
    let onExperience: (String) -> Void
    let onFrequency: (String) -> Void

    var canProceed: Bool { profile.isExperienceFilled }

    var body: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: "План тренировки",
                showBackNavigation: true,
                onBackTap: {
                    dismissAiFlowKeyboard()
                    onBack()
                }
            )
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer().frame(height: 8)
                    Text("Ваш опыт")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .padding(.horizontal, 20)
                    Spacer().frame(height: 24)

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Как давно вы занимаетесь спортом?")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(Color(hex: "555555"))
                        ChipGroup(
                            options: ["Давно", "Недавно", "Не занимался"],
                            selected: profile.experience,
                            onSelect: onExperience
                        )
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 28)

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Как часто вы занимаетесь спортом?")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(Color(hex: "555555"))
                        ChipGroup(
                            options: ["Часто", "Редко", "Не занимался"],
                            selected: profile.frequency,
                            onSelect: onFrequency
                        )
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 28)
                }
            }
            PrimaryButton(title: "Далее", enabled: canProceed, action: onNext)
        }
        .background(Palette.warmOffWhite)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct AiHealthScreen: View {
    let profile: AiProfileData
    let onBack: () -> Void
    let onNext: () -> Void
    let onHasLimitations: (String) -> Void
    let onLimitationsDesc: (String) -> Void

    var canProceed: Bool { profile.isHealthFilled }
    @FocusState private var textFocused: Bool

    var body: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: "План тренировки",
                showBackNavigation: true,
                onBackTap: {
                    dismissAiFlowKeyboard()
                    onBack()
                }
            )
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer().frame(height: 8)
                    Text("Здоровье")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .padding(.horizontal, 20)
                    Spacer().frame(height: 24)

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Есть ли у вас ограничения по здоровью?")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(Color(hex: "555555"))
                        ChipGroup(
                            options: ["Да", "Нет"],
                            selected: profile.hasLimitations,
                            onSelect: { choice in
                                textFocused = false
                                onHasLimitations(choice)
                            }
                        )
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 24)

                    if profile.hasLimitations == "Да" {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Опишите ваши ограничения")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(Color(hex: "555555"))
                            ZStack(alignment: .topLeading) {
                                if profile.limitationsDesc.isEmpty {
                                    Text("Например: грыжа позвоночника")
                                        .font(.system(size: 16))
                                        .foregroundColor(Color(hex: "AAAAAA"))
                                        .padding(14)
                                        .allowsHitTesting(false)
                                }
                                TextEditor(text: Binding(
                                    get: { profile.limitationsDesc },
                                    set: onLimitationsDesc
                                ))
                                .font(.system(size: 16))
                                .foregroundColor(Palette.deepInk)
                                .focused($textFocused)
                                .frame(minHeight: 90)
                                .padding(10)
                                .scrollContentBackground(.hidden)
                                .background(Color.clear)
                            }
                            .background(Color.white)
                            .cornerRadius(12)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(textFocused ? Palette.coral : Color(hex: "E0E0E0"), lineWidth: 2)
                            )
                        }
                        .padding(.horizontal, 20)
                        .transition(.opacity.combined(with: .move(edge: .top)))
                        .animation(.easeInOut(duration: 0.25), value: profile.hasLimitations)
                    }
                }
                .contentShape(Rectangle())
                .onTapGesture { textFocused = false }
            }
            PrimaryButton(title: "Далее", enabled: canProceed, action: onNext)
        }
        .background(Palette.warmOffWhite)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct AiChatScreen: View {
    let messages: [AiChatMessage]
    let isTyping: Bool
    let hasWorkout: Bool
    let savedPlanId: String?
    let isSaving: Bool
    let isSaved: Bool
    let errorMessage: String?
    let onBack: () -> Void
    let onSend: (String) -> Void
    let onSave: () -> Void

    @State private var input: String = ""
    @FocusState private var inputFocused: Bool

    private var saveButtonTitle: String {
        if isSaving { return "Сохранение..." }
        return savedPlanId != nil ? "✓ Обновить тренировку" : "✓ Добавить тренировку"
    }

    var body: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: "AI Тренер",
                showBackNavigation: true,
                onBackTap: {
                    dismissAiFlowKeyboard()
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
                                Text("Опишите какую тренировку вы хотите получить")
                                    .font(.system(size: 18, weight: .semibold))
                                    .foregroundColor(Color(hex: "AAAAAA"))
                                    .multilineTextAlignment(.center)
                                    .padding(.horizontal, 32)
                            }
                            .frame(maxWidth: .infinity)
                            .id("empty")
                        }
                        ForEach(Array(messages.enumerated()), id: \.offset) { _, msg in
                            ChatBubbleView(message: msg)
                        }
                        if isTyping { TypingBubbleView() }
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

            if hasWorkout && !isSaved {
                Button(action: {
                    inputFocused = false
                    onSave()
                }) {
                    Text(saveButtonTitle)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(isSaving ? Color(hex: "22C55E").opacity(0.6) : Color(hex: "22C55E"))
                        .cornerRadius(28)
                }
                .disabled(isSaving)
                .padding(.horizontal, 20)
                .padding(.vertical, 8)
            }

            if isSaved {
                Text("Тренировка добавлена! ✓")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Color(hex: "16A34A"))
                    .frame(maxWidth: .infinity)
                    .padding(14)
                    .background(Color(hex: "F0FDF4"))
                    .cornerRadius(14)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 8)
            }

            HStack(spacing: 8) {
                ZStack(alignment: .topLeading) {
                    if input.isEmpty {
                        Text("Напишите запрос...")
                            .font(.system(size: 16))
                            .foregroundColor(Color(hex: "AAAAAA"))
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
                        .stroke(inputFocused ? Palette.coral : Color(hex: "E0E0E0"), lineWidth: 2)
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
                        .background(canSend ? Palette.coral : Color(hex: "E0E0E0"))
                        .clipShape(Circle())
                }
                .disabled(!canSend)
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .background(Color.white)
            .overlay(Rectangle().frame(height: 1).foregroundColor(Color(hex: "EEEEEE")), alignment: .top)
        }
        .background(Palette.warmOffWhite)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct ChatBubbleView: View {
    let message: AiChatMessage
    var isUser: Bool { message.role == AiChatMessage.Role.user }

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
                        .stroke(isUser ? Color.clear : Color(hex: "EEEEEE"), lineWidth: 1)
                )
            if !isUser { Spacer(minLength: 48) }
        }
        .frame(maxWidth: .infinity, alignment: isUser ? .trailing : .leading)
    }
}

private struct TypingBubbleView: View {
    @State private var animating = false

    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<3, id: \.self) { idx in
                Circle()
                    .fill(Color(hex: "AAAAAA"))
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
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(Color(hex: "EEEEEE"), lineWidth: 1))
        .onAppear { animating = true }
    }
}

private struct ChipGroup: View {
    let options: [String]
    let selected: String
    let onSelect: (String) -> Void

    var body: some View {
        HStack(spacing: 8) {
            ForEach(options, id: \.self) { opt in
                let active = selected == opt
                Text(opt)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(active ? .white : Palette.deepInk)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 12)
                    .background(active ? Palette.coral : Color.white)
                    .cornerRadius(24)
                    .overlay(
                        RoundedRectangle(cornerRadius: 24)
                            .stroke(active ? Palette.coral : Color(hex: "E0E0E0"), lineWidth: 2)
                    )
                    .onTapGesture {
                        dismissAiFlowKeyboard()
                        onSelect(opt)
                    }
            }
            Spacer()
        }
    }
}

private struct PrimaryButton: View {
    let title: String
    let enabled: Bool
    let action: () -> Void
    var color: Color = Palette.coral

    var body: some View {
        Button(action: {
            dismissAiFlowKeyboard()
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

fileprivate func dismissAiFlowKeyboard() {
    UIApplication.shared.sendAction(
        #selector(UIResponder.resignFirstResponder),
        to: nil,
        from: nil,
        for: nil
    )
}

extension Comparable {
    fileprivate func clamped(to range: ClosedRange<Self>) -> Self {
        Swift.min(Swift.max(self, range.lowerBound), range.upperBound)
    }
}

extension Color {
    init(hex: String) {
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
