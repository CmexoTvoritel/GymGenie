import SwiftUI
import Shared

// MARK: - Root container

/// AI-powered meal-planning flow.
///
/// 5 in-feature steps (mirrors `AiCoachView` for the workout flow):
///  - 0 Choose      — single welcome card, "Составить рацион на день"
///  - 1 Profile     — age / height / weight sliders
///  - 2 Goal        — three-card goal picker (lose / maintain / gain)
///  - 3 Restrictions — diet + allergies free-text fields
///  - 4 Chat        — meal-plan chat with save CTA
///
/// All visual primitives (header, sliders, chips, primary button, chat
/// bubbles, typing indicator, hex color helper) are defined inline and
/// `fileprivate` so they cannot collide with the workout-flow versions in
/// `AiCoachView.swift`.
struct AiMealCoachView: View {
    @EnvironmentObject private var profileStore: UserProfileStoreWrapper
    @StateObject private var vm = AiMealViewModelWrapper()
    @State private var previousStepIndex: Int32 = 0

    var onClose: (() -> Void)? = nil

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
        .onChange(of: profileStore.profile?.id) { _ in
            if let profile = profileStore.profile {
                vm.prefillProfile(profile)
            }
        }
    }

    private func transition(for step: AiMealFlowStep) -> AnyTransition {
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
            EmptyView()
        case .profile:
            AiMealProfileScreen(
                profile: vm.profile,
                onBack: {
                    previousStepIndex = vm.step.index
                    onClose?()
                },
                onNext: {
                    previousStepIndex = vm.step.index
                    vm.goTo(step: .goal)
                },
                onAge: { vm.setAge($0) },
                onHeight: { vm.setHeight($0) },
                onWeight: { vm.setWeight($0) }
            )
            .transition(transition(for: .profile))
        case .goal:
            AiMealGoalScreen(
                selectedGoal: vm.goal,
                onBack: {
                    previousStepIndex = vm.step.index
                    vm.goBack()
                },
                onNext: {
                    previousStepIndex = vm.step.index
                    vm.goTo(step: .restrictions)
                },
                onSelect: { vm.setGoal($0) }
            )
            .transition(transition(for: .goal))
        case .restrictions:
            AiMealRestrictionsScreen(
                dietaryRestrictions: vm.dietaryRestrictions,
                allergies: vm.allergies,
                onBack: {
                    previousStepIndex = vm.step.index
                    vm.goBack()
                },
                onNext: {
                    previousStepIndex = vm.step.index
                    vm.goTo(step: .chat)
                },
                onDietary: { vm.setDietaryRestrictions($0) },
                onAllergies: { vm.setAllergies($0) }
            )
            .transition(transition(for: .restrictions))
        case .chat:
            AiMealChatScreen(
                messages: vm.messages,
                isTyping: vm.isTyping,
                hasMealPlan: vm.lastMealPlan != nil,
                savedPlanId: vm.savedPlanId,
                isSaving: vm.isSaving,
                isSaved: vm.isSaved,
                errorMessage: vm.errorMessage,
                onBack: {
                    previousStepIndex = vm.step.index
                    vm.goBack()
                },
                onSend: { vm.sendMessage($0) },
                onSave: { vm.saveMealPlan() }
            )
            .transition(transition(for: .chat))
        default:
            EmptyView()
        }
    }
}

// MARK: - Step 0: Choose

private struct AiMealChooseScreen: View {
    let onClose: (() -> Void)?
    let onNext: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                if let onClose = onClose {
                    Button(action: onClose) {
                        Image(systemName: "xmark")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundColor(Palette.deepInk)
                            .frame(width: 44, height: 44)
                    }
                }
                Spacer()
                Text("AI Питание")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
                Spacer()
                Color.clear.frame(width: 44, height: 44)
            }
            .padding(.top, 4)

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer().frame(height: 40)
                    Text("Что хотите получить от ИИ-нутрициолога?")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, 20)

                    Spacer().frame(height: 32)

                    VStack(spacing: 14) {
                        AiMealGenerateCard(
                            emoji: "🥗",
                            title: "Составить рацион на день",
                            subtitle: "Завтрак, обед и ужин с учётом ваших целей",
                            enabled: true,
                            action: onNext
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

private struct AiMealGenerateCard: View {
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
                .foregroundColor(Color(aiMealHex: "888888"))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(24)
        .background(Color.white)
        .cornerRadius(16)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(pressed && enabled ? Palette.coral : Color(aiMealHex: "EEEEEE"), lineWidth: 2)
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

// MARK: - Step 1: Profile

private struct AiMealProfileScreen: View {
    let profile: AiMealProfileData
    let onBack: () -> Void
    let onNext: () -> Void
    let onAge: (Int) -> Void
    let onHeight: (Int) -> Void
    let onWeight: (Int) -> Void

    var canProceed: Bool { profile.isProfileFilled }

    var body: some View {
        VStack(spacing: 0) {
            AiMealFlowHeader(title: "Рацион на день", onBack: onBack)
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer().frame(height: 8)
                    Text("Ваши параметры")
                        .font(.system(size: 22, weight: .bold))
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
                    .font(.system(size: 14, weight: .medium))
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
                let thumbSize: CGFloat = 20
                let thumbX = pct * (w - thumbSize)

                ZStack(alignment: .leading) {
                    Capsule().fill(Color(aiMealHex: "E0E0E0")).frame(height: 4)
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
                            let ratio = (drag.location.x / w).aiMealClamped(to: 0...1)
                            let v = Int(Double(min) + Double(ratio) * Double(max - min))
                            onChange(v.aiMealClamped(to: min...max))
                        }
                )
            }
            .frame(height: 20)

            Spacer().frame(height: 4)
            HStack {
                Text("\(min)").font(.system(size: 11)).foregroundColor(Color(aiMealHex: "AAAAAA"))
                Spacer()
                Text("\(max)").font(.system(size: 11)).foregroundColor(Color(aiMealHex: "AAAAAA"))
            }
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 28)
    }
}

// MARK: - Step 2: Goal

private struct AiMealGoalScreen: View {
    let selectedGoal: String
    let onBack: () -> Void
    let onNext: () -> Void
    let onSelect: (String) -> Void

    var canProceed: Bool { !selectedGoal.isEmpty }

    private struct GoalOption: Identifiable {
        let id: String
        let emoji: String
        let title: String
        let subtitle: String
    }

    private let options: [GoalOption] = [
        GoalOption(id: MealGoal.loseWeight.wireValue, emoji: "🔥",
                   title: MealGoal.loseWeight.displayName,
                   subtitle: "Дефицит калорий, лёгкий рацион"),
        GoalOption(id: MealGoal.maintain.wireValue, emoji: "⚖️",
                   title: MealGoal.maintain.displayName,
                   subtitle: "Сбалансированное питание"),
        GoalOption(id: MealGoal.gainMuscle.wireValue, emoji: "💪",
                   title: MealGoal.gainMuscle.displayName,
                   subtitle: "Профицит и больше белка"),
    ]

    var body: some View {
        VStack(spacing: 0) {
            AiMealFlowHeader(title: "Рацион на день", onBack: onBack)
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer().frame(height: 8)
                    Text("Ваша цель")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .padding(.horizontal, 20)
                    Spacer().frame(height: 24)

                    VStack(spacing: 12) {
                        ForEach(options) { option in
                            AiMealGoalCard(
                                emoji: option.emoji,
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
    let emoji: String
    let title: String
    let subtitle: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(alignment: .center, spacing: 14) {
                Text(emoji).font(.system(size: 28))
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(Palette.deepInk)
                    Text(subtitle)
                        .font(.system(size: 12))
                        .foregroundColor(Color(aiMealHex: "888888"))
                }
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 22))
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

// MARK: - Step 3: Restrictions

private struct AiMealRestrictionsScreen: View {
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
            AiMealFlowHeader(title: "Рацион на день", onBack: onBack)
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer().frame(height: 8)
                    Text("Ограничения и аллергии")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .padding(.horizontal, 20)
                    Text("Можно оставить пустым")
                        .font(.system(size: 13))
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
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(Color(aiMealHex: "555555"))
            ZStack(alignment: .topLeading) {
                if text.isEmpty {
                    Text(placeholder)
                        .font(.system(size: 14))
                        .foregroundColor(Color(aiMealHex: "AAAAAA"))
                        .padding(14)
                        .allowsHitTesting(false)
                }
                TextEditor(text: Binding(
                    get: { text },
                    set: onChange
                ))
                .font(.system(size: 14))
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

// MARK: - Step 4: Chat

private struct AiMealChatScreen: View {
    let messages: [AiMealChatMessage]
    let isTyping: Bool
    let hasMealPlan: Bool
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
        return savedPlanId != nil ? "✓ Обновить рацион" : "✓ Сохранить рацион"
    }

    var body: some View {
        VStack(spacing: 0) {
            AiMealFlowHeader(title: "AI Нутрициолог", onBack: onBack)

            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 12) {
                        if messages.isEmpty && !isTyping {
                            VStack(spacing: 0) {
                                Spacer().frame(height: 40)
                                Text("🥗")
                                    .font(.system(size: 64))
                                Spacer().frame(height: 20)
                                Text("Опишите, какой рацион вы хотите получить")
                                    .font(.system(size: 18, weight: .semibold))
                                    .foregroundColor(Palette.deepInk)
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

            if hasMealPlan && !isSaved {
                Button(action: {
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
                    Text("↑")
                        .font(.system(size: 18, weight: .bold))
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

// MARK: - Shared in-flow controls

private struct AiMealFlowHeader: View {
    let title: String
    let onBack: () -> Void

    var body: some View {
        HStack(spacing: 4) {
            Button(action: {
                dismissAiMealFlowKeyboard()
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

// MARK: - Helpers

/// File-scoped keyboard dismiss used by every shared control in the AI meal
/// flow (cards, primary CTA, header back, chat send/save). Resigns the
/// current first responder so any focused TextEditor/TextField loses focus
/// alongside the user's actual gesture target.
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

/// Hex initializer scoped to the AI meal flow. The workout-flow file already
/// declares a public `Color(hex:)` extension; redeclaring it here would
/// duplicate the symbol. Naming the parameter `aiMealHex:` keeps the local
/// helper unambiguous from the project-wide one.
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
