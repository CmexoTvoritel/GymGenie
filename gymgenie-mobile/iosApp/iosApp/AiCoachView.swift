import SwiftUI
import Shared

// MARK: - Root container

struct AiCoachView: View {
    @EnvironmentObject private var profileStore: UserProfileStoreWrapper
    @StateObject private var vm = AiViewModelWrapper()
    @State private var previousStepIndex: Int32 = 0

    var body: some View {
        ZStack {
            Palette.warmOffWhite.ignoresSafeArea()
            content
        }
        .animation(.easeInOut(duration: 0.3), value: vm.step.index)
        .onAppear {
            if let profile = profileStore.profile {
                vm.prefillProfile(profile)
            }
        }
        // Tracks profile identity (id) so a re-login or fresh profile load
        // re-triggers the pre-fill exactly once. The wrapper itself guards
        // against overwriting user edits.
        .onChange(of: profileStore.profile?.id) { _ in
            if let profile = profileStore.profile {
                vm.prefillProfile(profile)
            }
        }
    }

    private func transition(for step: AiFlowStep) -> AnyTransition {
        let forward = step.index >= previousStepIndex
        return .asymmetric(
            insertion: .move(edge: forward ? .trailing : .leading),
            removal: .move(edge: forward ? .leading : .trailing)
        )
    }

    @ViewBuilder
    private var content: some View {
        switch vm.step {
        case .choose:
            AiChooseScreen {
                previousStepIndex = vm.step.index
                vm.goTo(step: .profile)
            }
            .transition(transition(for: .choose))
        case .profile:
            AiProfileScreen(
                profile: vm.profile,
                onBack: {
                    previousStepIndex = vm.step.index
                    vm.goBack()
                },
                onNext: {
                    previousStepIndex = vm.step.index
                    vm.goTo(step: .experience)
                },
                onAge: { vm.setAge($0) },
                onHeight: { vm.setHeight($0) },
                onWeight: { vm.setWeight($0) }
            )
            .transition(transition(for: .profile))
        case .experience:
            AiExperienceScreen(
                profile: vm.profile,
                onBack: {
                    previousStepIndex = vm.step.index
                    vm.goBack()
                },
                onNext: {
                    previousStepIndex = vm.step.index
                    vm.goTo(step: .health)
                },
                onExperience: { vm.setExperience($0) },
                onFrequency: { vm.setFrequency($0) }
            )
            .transition(transition(for: .experience))
        case .health:
            AiHealthScreen(
                profile: vm.profile,
                onBack: {
                    previousStepIndex = vm.step.index
                    vm.goBack()
                },
                onNext: {
                    previousStepIndex = vm.step.index
                    vm.goTo(step: .chat)
                },
                onHasLimitations: { vm.setHasLimitations($0) },
                onLimitationsDesc: { vm.setLimitationsDesc($0) }
            )
            .transition(transition(for: .health))
        case .chat:
            AiChatScreen(
                messages: vm.messages,
                isTyping: vm.isTyping,
                hasWorkout: vm.hasWorkout,
                savedPlanId: vm.savedPlanId,
                isSaving: vm.isSaving,
                isSaved: vm.isSaved,
                errorMessage: vm.errorMessage,
                onBack: {
                    previousStepIndex = vm.step.index
                    vm.goBack()
                },
                onSend: { vm.sendMessage($0) },
                onSave: { vm.saveWorkout() }
            )
            .transition(transition(for: .chat))
        default:
            EmptyView()
        }
    }
}

// MARK: - Screen 1: Choose

private struct AiChooseScreen: View {
    let onNext: () -> Void

    /// Drives the full-screen presentation of the AI meal flow. Kept local
    /// to this screen so the workout flow's `AiViewModelWrapper` doesn't
    /// need to know about meal-flow navigation.
    @State private var showMealCoach = false

    var body: some View {
        VStack(spacing: 0) {
            Text("AI")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(Palette.deepInk)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer().frame(height: 40)
                    Text("Что вы хотите сгенерировать?")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, 20)

                    Spacer().frame(height: 32)

                    VStack(spacing: 14) {
                        GenerateCard(
                            emoji: "🏋️",
                            title: "План тренировки",
                            subtitle: "Персональная программа на основе ваших данных",
                            enabled: true,
                            action: onNext
                        )
                        GenerateCard(
                            emoji: "🥗",
                            title: "План питания",
                            subtitle: "Персональный рацион на основе ваших целей",
                            enabled: true,
                            action: { showMealCoach = true }
                        )
                    }
                    .padding(.horizontal, 20)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.warmOffWhite)
        // The meal flow lives in its own self-contained surface
        // (`AiMealCoachView`) and inherits `profileStore` from the tab
        // hierarchy via @EnvironmentObject — no explicit injection needed.
        .fullScreenCover(isPresented: $showMealCoach) {
            AiMealCoachView(onClose: { showMealCoach = false })
        }
    }
}

private struct GenerateCard: View {
    let emoji: String
    let title: String
    let subtitle: String
    let enabled: Bool
    let action: () -> Void

    @State private var pressed = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(emoji).font(.system(size: 28))
            Text(title)
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(Palette.deepInk)
            Text(subtitle)
                .font(.system(size: 13))
                .foregroundColor(Color(hex: "888888"))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(24)
        .background(Color.white)
        .cornerRadius(16)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(pressed && enabled ? Palette.coral : Color(hex: "EEEEEE"), lineWidth: 2)
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

// MARK: - Screen 2: Profile

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
            FlowHeader(title: "План тренировки", onBack: onBack)
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer().frame(height: 8)
                    Text("Введите данные профиля")
                        .font(.system(size: 22, weight: .bold))
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
                    .font(.system(size: 14, weight: .medium))
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
                let thumbSize: CGFloat = 20
                let thumbX = pct * (w - thumbSize)

                ZStack(alignment: .leading) {
                    Capsule().fill(Color(hex: "E0E0E0")).frame(height: 4)
                    Capsule().fill(Palette.coral)
                        .frame(width: Swift.max(0, thumbX + thumbSize / 2), height: 4)
                    Circle()
                        .fill(Palette.coral)
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
            .frame(height: 20)

            Spacer().frame(height: 4)
            HStack {
                Text("\(min)").font(.system(size: 11)).foregroundColor(Color(hex: "AAAAAA"))
                Spacer()
                Text("\(max)").font(.system(size: 11)).foregroundColor(Color(hex: "AAAAAA"))
            }
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 28)
    }
}

// MARK: - Screen 3: Experience

private struct AiExperienceScreen: View {
    let profile: AiProfileData
    let onBack: () -> Void
    let onNext: () -> Void
    let onExperience: (String) -> Void
    let onFrequency: (String) -> Void

    var canProceed: Bool { profile.isExperienceFilled }

    var body: some View {
        VStack(spacing: 0) {
            FlowHeader(title: "План тренировки", onBack: onBack)
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer().frame(height: 8)
                    Text("Ваш опыт")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .padding(.horizontal, 20)
                    Spacer().frame(height: 24)

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Как давно вы занимаетесь спортом?")
                            .font(.system(size: 14, weight: .medium))
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
                            .font(.system(size: 14, weight: .medium))
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

// MARK: - Screen 4: Health

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
            FlowHeader(title: "План тренировки", onBack: onBack)
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer().frame(height: 8)
                    Text("Здоровье")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .padding(.horizontal, 20)
                    Spacer().frame(height: 24)

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Есть ли у вас ограничения по здоровью?")
                            .font(.system(size: 14, weight: .medium))
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
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(Color(hex: "555555"))
                            ZStack(alignment: .topLeading) {
                                if profile.limitationsDesc.isEmpty {
                                    Text("Например: грыжа позвоночника")
                                        .font(.system(size: 14))
                                        .foregroundColor(Color(hex: "AAAAAA"))
                                        .padding(14)
                                        .allowsHitTesting(false)
                                }
                                TextEditor(text: Binding(
                                    get: { profile.limitationsDesc },
                                    set: onLimitationsDesc
                                ))
                                .font(.system(size: 14))
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
                // Tapping any empty space in the scroll content (paddings,
                // labels, gaps) clears focus from the TextEditor. Interactive
                // children (ChipGroup, TextEditor) handle their own taps.
                .contentShape(Rectangle())
                .onTapGesture { textFocused = false }
            }
            PrimaryButton(title: "Далее", enabled: canProceed, action: onNext)
        }
        .background(Palette.warmOffWhite)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Screen 5: Chat

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

    /// Switches between insert and update copy. We rely on `savedPlanId` —
    /// not `isSaved` — because once a plan is persisted the user keeps
    /// iterating with the AI; every new message clears `isSaved` but the
    /// saved id remains so subsequent saves overwrite the same plan.
    private var saveButtonTitle: String {
        if isSaving { return "Сохранение..." }
        return savedPlanId != nil ? "✓ Обновить тренировку" : "✓ Добавить тренировку"
    }

    var body: some View {
        VStack(spacing: 0) {
            FlowHeader(title: "AI Тренер", onBack: onBack)

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
                                    .foregroundColor(Palette.deepInk)
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
                    // Tapping anywhere in the messages list (empty area or
                    // around bubbles) clears focus from the input field.
                    // Bubbles render as plain Text so they don't intercept.
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
                    // Saving may transition out of input mode (success card,
                    // disabled state). Drop focus so the keyboard doesn't
                    // float on top of the success state.
                    inputFocused = false
                    onSave()
                }) {
                    Text(saveButtonTitle)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(isSaving ? Color.green.opacity(0.6) : Color.green)
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
                    // Drop focus so the keyboard collapses on send. The user
                    // can tap the field again to compose a follow-up.
                    inputFocused = false
                    let text = input
                    input = ""
                    onSend(text)
                }) {
                    Text("↑")
                        .font(.system(size: 18, weight: .bold))
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

// MARK: - Shared components

private struct FlowHeader: View {
    let title: String
    let onBack: () -> Void

    var body: some View {
        HStack(spacing: 4) {
            Button(action: {
                // Tapping back must always dismiss the keyboard so the user
                // doesn't navigate away with a still-floating keyboard.
                dismissAiFlowKeyboard()
                onBack()
            }) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
                    .frame(width: 44, height: 44)
            }
            Text(title)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(Palette.deepInk)
            Spacer()
        }
        .padding(.leading, 4)
        .padding(.trailing, 16)
        .padding(.top, 12)
        .padding(.bottom, 4)
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
                    .font(.system(size: 14, weight: .medium))
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
                        // Picking a chip should also clear focus from any
                        // sibling text input (e.g. health limitations field).
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
            // CTA fires after dismissing the keyboard so transitions out of
            // the screen don't leave the keyboard hanging mid-animation.
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

// MARK: - Helpers

/// File-scoped keyboard dismiss used by every shared control in the AI flow
/// (chips, primary CTA, header back button, chat send/save). Resigns the
/// current first responder so any focused TextEditor/TextField loses focus
/// alongside the user's actual gesture target.
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
