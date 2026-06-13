import SwiftUI
import Shared

private enum EditColors {
    static let black = Color(red: 0.039, green: 0.039, blue: 0.039)
    static let border = Color(red: 0.929, green: 0.929, blue: 0.937)
    static let muted = Color(red: 0.545, green: 0.545, blue: 0.573)
    static let errorRed = Color(red: 0.898, green: 0.282, blue: 0.302)
    static let trackInactive = Color(red: 0.878, green: 0.878, blue: 0.878)
    static let rangeLabel = Color(red: 0.667, green: 0.667, blue: 0.667)
    static let sliderLabel = Color(red: 0.333, green: 0.333, blue: 0.333)
}

@MainActor
final class EditProfileFormState: ObservableObject {
    @Published var firstName: String = ""
    @Published var lastName: String = ""
    @Published var weightKg: Int = 70
    @Published var heightCm: Int = 175
    @Published var ageYears: Int = 25
    @Published var experience: String = "Недавно"
    @Published var frequency: String = "Редко"
    @Published var hasHealthIssues: Bool = false
    @Published var healthIssues: String = ""
    var initialized: Bool = false

    func seed(from profile: UserProfileResponse) {
        guard !initialized else { return }
        firstName = profile.firstName ?? ""
        lastName = profile.lastName ?? ""
        weightKg = profile.weightKg.map { Int(truncating: $0) } ?? 70
        heightCm = profile.heightCm.map { Int(truncating: $0) } ?? 175
        ageYears = profile.ageYears.map { $0.intValue } ?? 25
        experience = profile.experience ?? "Недавно"
        frequency = profile.frequency ?? "Редко"
        hasHealthIssues = !(profile.healthIssues ?? "").isEmpty
        healthIssues = profile.healthIssues ?? ""
        initialized = true
    }
}

struct EditProfileView: View {
    let onBack: () -> Void

    @EnvironmentObject private var profileStore: UserProfileStoreWrapper
    @StateObject private var vm = ProfileViewModelWrapper()
    @StateObject private var form = EditProfileFormState()

    @State private var showMetrics = false
    @State private var showExperience = false
    @State private var showHealth = false

    private let backgroundColor = Palette.warmOffWhite

    var body: some View {
        VStack(spacing: 0) {

            GymGenieToolbar(
                title: "Редактировать",
                showBackNavigation: true,
                onBackTap: { if !vm.isLoading { onBack() } },
                actions: [
                    ToolbarAction(
                        content: AnyView(toolbarSaveContent),
                        action: { if !vm.isLoading { save() } }
                    ),
                ]
            )

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {

                    HStack {
                        Spacer()
                        editAvatar(
                            name: "\(form.firstName) \(form.lastName)".trimmingCharacters(in: .whitespaces)
                        )
                        Spacer()
                    }
                    .padding(.vertical, 24)

                    editFieldLabel("ИМЯ")
                    editInputField(
                        value: $form.firstName,
                        placeholder: "Введите имя"
                    )

                    editFieldLabel("ФАМИЛИЯ")
                    editInputField(
                        value: $form.lastName,
                        placeholder: "Введите фамилию"
                    )

                    editFieldLabel("EMAIL")
                    editInputField(
                        value: .constant(profileStore.profile?.email ?? ""),
                        placeholder: "email@example.com",
                        enabled: false
                    )

                    editFieldLabel("БЕЗОПАСНОСТЬ")
                    SettingsGroupView {
                        SettingsRowView(label: "Сменить пароль", icon: "lock.fill")
                    }

                    editFieldLabel("ПАРАМЕТРЫ")
                    SettingsGroupView {
                        SettingsRowView(
                            label: "Вес, рост, возраст",
                            icon: "person.fill",
                            value: "\(form.weightKg) \u{00B7} \(form.heightCm) \u{00B7} \(form.ageYears)",
                            action: { showMetrics = true }
                        )
                        Divider().padding(.horizontal, 16)
                        SettingsRowView(
                            label: "Опыт и частота",
                            icon: "dumbbell.fill",
                            value: form.experience,
                            action: { showExperience = true }
                        )
                        Divider().padding(.horizontal, 16)
                        SettingsRowView(
                            label: "Здоровье",
                            icon: "questionmark.circle.fill",
                            value: form.hasHealthIssues ? "Указано" : "Нет огранич.",
                            action: { showHealth = true }
                        )
                    }

                    Spacer().frame(height: 20)
                }
                .padding(.horizontal, 20)
            }
            .disabled(vm.isLoading)

            VStack(spacing: 0) {
                if let error = vm.error {
                    Text(error)
                        .font(.system(size: 13))
                        .foregroundColor(EditColors.errorRed)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.bottom, 8)
                }

                Button(action: save) {
                    Group {
                        if vm.isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        } else {
                            Text("Сохранить изменения")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(.white)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(
                        RoundedRectangle(cornerRadius: 28, style: .continuous)
                            .fill(Palette.coral)
                    )
                }
                .buttonStyle(.plain)
                .disabled(vm.isLoading)
            }
            .padding(.horizontal, 20)
            .padding(.top, 10)
            .padding(.bottom, 12)
        }
        .contentShape(Rectangle())
        .onTapGesture {
            UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
        }
        .background(backgroundColor.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
        .navigationDestination(isPresented: $showMetrics) {
            EditMetricsView(form: form, onBack: { showMetrics = false })
        }
        .navigationDestination(isPresented: $showExperience) {
            EditExperienceView(form: form, onBack: { showExperience = false })
        }
        .navigationDestination(isPresented: $showHealth) {
            EditHealthView(form: form, onBack: { showHealth = false })
        }
        .onAppear { seedForm() }
        .onChange(of: profileStore.profile?.id) { _ in seedForm() }
        .onChange(of: vm.success) { didSucceed in
            if didSucceed {
                vm.consumeSuccess()
                profileStore.refresh()
                form.initialized = false
                onBack()
            }
        }
    }

    @ViewBuilder
    private var toolbarSaveContent: some View {
        if vm.isLoading {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle(tint: Palette.coral))
                .scaleEffect(0.8)
        } else {
            Text("Сохранить")
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(EditColors.black)
        }
    }

    private func seedForm() {
        guard let profile = profileStore.profile else { return }
        form.initialized = false
        form.seed(from: profile)
    }

    private func save() {
        guard form.initialized else { return }
        let request = UpdateUserProfileRequest(
            firstName: form.firstName.isEmpty ? nil : form.firstName,
            lastName: form.lastName.isEmpty ? nil : form.lastName,
            gender: nil,
            birthDate: nil,
            weightKg: KotlinDouble(double: Double(form.weightKg)),
            heightCm: KotlinDouble(double: Double(form.heightCm)),
            profilePhotoUrl: nil,
            ageYears: KotlinInt(int: Int32(form.ageYears)),
            experience: form.experience,
            frequency: form.frequency,
            healthIssues: (form.hasHealthIssues && !form.healthIssues.isEmpty)
                ? form.healthIssues
                : nil
        )
        vm.updateProfile(request: request)
    }
}

private func editFieldLabel(_ text: String) -> some View {
    Text(text)
        .font(.system(size: 13, weight: .semibold))
        .foregroundColor(EditColors.black)
        .tracking(0.8)
        .padding(.top, 16)
        .padding(.bottom, 4)
}

private func editInputField(
    value: Binding<String>,
    placeholder: String,
    enabled: Bool = true
) -> some View {
    TextField(placeholder, text: value)
        .font(.system(size: 17))
        .disabled(!enabled)
        .foregroundColor(enabled ? EditColors.black : EditColors.muted)
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(EditColors.border, lineWidth: 1)
        )
}

private func editAvatar(name: String, size: CGFloat = 92) -> some View {
    let initials = name
        .split(separator: " ")
        .prefix(2)
        .compactMap { $0.first.map { String($0).uppercased() } }
        .joined()
    let display = initials.isEmpty ? "?" : initials

    return ZStack {
        Circle()
            .fill(
                LinearGradient(
                    colors: [Palette.coral, Color(red: 1.0, green: 0.541, blue: 0.431)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .frame(width: size, height: size)
        Text(display)
            .font(.system(size: size * 0.36, weight: .bold))
            .foregroundColor(.white)
    }
}

struct EditMetricsView: View {
    @ObservedObject var form: EditProfileFormState
    let onBack: () -> Void

    private let backgroundColor = Palette.warmOffWhite

    var body: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: "Параметры",
                showBackNavigation: true,
                onBackTap: onBack
            )

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    SliderFieldView(
                        label: "Возраст",
                        value: $form.ageYears,
                        range: 10...80
                    )
                    SliderFieldView(
                        label: "Рост (см)",
                        value: $form.heightCm,
                        range: 100...220
                    )
                    SliderFieldView(
                        label: "Вес (кг)",
                        value: $form.weightKg,
                        range: 30...150
                    )
                    Spacer().frame(height: 20)
                }
                .padding(.horizontal, 20)
            }

            stepBottomButton(text: "Готово", action: onBack)
        }
        .background(backgroundColor.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
    }
}

struct EditExperienceView: View {
    @ObservedObject var form: EditProfileFormState
    let onBack: () -> Void

    private let backgroundColor = Palette.warmOffWhite

    var body: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: "Опыт",
                showBackNavigation: true,
                onBackTap: onBack
            )

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    editFieldLabel("КАК ДАВНО ВЫ ЗАНИМАЕТЕСЬ")
                    PickerGroupView(
                        options: ["Давно", "Недавно", "Не занимался"],
                        selected: form.experience,
                        onSelect: { form.experience = $0 }
                    )

                    Spacer().frame(height: 24)

                    editFieldLabel("КАК ЧАСТО ВЫ ЗАНИМАЕТЕСЬ")
                    PickerGroupView(
                        options: ["Часто", "Редко", "Не занимался"],
                        selected: form.frequency,
                        onSelect: { form.frequency = $0 }
                    )

                    Spacer().frame(height: 20)
                }
                .padding(.horizontal, 20)
            }

            stepBottomButton(text: "Готово", action: onBack)
        }
        .background(backgroundColor.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
    }
}

struct EditHealthView: View {
    @ObservedObject var form: EditProfileFormState
    let onBack: () -> Void

    private let backgroundColor = Palette.warmOffWhite

    var body: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: "Здоровье",
                showBackNavigation: true,
                onBackTap: onBack
            )

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    editFieldLabel("ОГРАНИЧЕНИЯ ПО ЗДОРОВЬЮ?")
                    PickerGroupView(
                        options: ["Да", "Нет"],
                        selected: form.hasHealthIssues ? "Да" : "Нет",
                        onSelect: { selected in
                            let enabled = selected == "Да"
                            form.hasHealthIssues = enabled
                            if !enabled { form.healthIssues = "" }
                        }
                    )

                    if form.hasHealthIssues {
                        Spacer().frame(height: 24)
                        editFieldLabel("ОПИШИТЕ ПОДРОБНЕЕ")
                        healthTextField
                    }

                    Spacer().frame(height: 20)
                }
                .padding(.horizontal, 20)
            }

            stepBottomButton(text: "Готово", action: onBack)
        }
        .background(backgroundColor.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
    }

    private var healthTextField: some View {
        TextEditor(text: $form.healthIssues)
            .font(.system(size: 17))
            .frame(minHeight: 80)
            .padding(12)
            .scrollContentBackground(.hidden)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(EditColors.border, lineWidth: 1)
            )
    }
}

private struct SliderFieldView: View {
    let label: String
    @Binding var value: Int
    let range: ClosedRange<Int>

    @State private var isEditing = false
    @State private var inputText = ""
    @FocusState private var textFocused: Bool

    private var pct: Double {
        guard range.upperBound > range.lowerBound else { return 0 }
        return Double(value - range.lowerBound) / Double(range.upperBound - range.lowerBound)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .bottom) {
                Text(label)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(EditColors.sliderLabel)
                Spacer()

                if isEditing {
                    TextField("", text: $inputText)
                        .keyboardType(.numberPad)
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .multilineTextAlignment(.trailing)
                        .frame(maxWidth: 100)
                        .focused($textFocused)
                        .onSubmit { commitInput() }
                        .onAppear { textFocused = true }
                        .onChange(of: textFocused) { focused in
                            if !focused { commitInput() }
                        }
                } else {
                    Text(value == 0 ? "\u{2014}" : "\(value)")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .frame(minWidth: 60, alignment: .trailing)
                        .contentShape(Rectangle())
                        .onTapGesture {
                            inputText = value == 0 ? "" : "\(value)"
                            isEditing = true
                        }
                }
            }

            Spacer().frame(height: 10)

            GeometryReader { geo in
                let trackWidth = geo.size.width
                let thumbSize: CGFloat = 24

                ZStack(alignment: .leading) {

                    Capsule()
                        .fill(EditColors.trackInactive)
                        .frame(height: 6)

                    Capsule()
                        .fill(Palette.coral)
                        .frame(width: max(0, trackWidth * pct), height: 6)

                    Circle()
                        .fill(Palette.coral)
                        .frame(width: thumbSize, height: thumbSize)
                        .overlay(
                            Circle().stroke(Color.white, lineWidth: 2.5)
                        )
                        .offset(x: max(0, (trackWidth - thumbSize) * pct))
                }
                .frame(height: 32)
                .contentShape(Rectangle())
                .gesture(
                    DragGesture(minimumDistance: 0)
                        .onChanged { drag in
                            let ratio = min(max(drag.location.x / trackWidth, 0), 1)
                            let newValue = Int(Double(range.lowerBound) + ratio * Double(range.upperBound - range.lowerBound))
                            value = min(max(newValue, range.lowerBound), range.upperBound)
                        }
                )
            }
            .frame(height: 32)

            Spacer().frame(height: 4)

            HStack {
                Text("\(range.lowerBound)")
                    .font(.system(size: 13))
                    .foregroundColor(EditColors.rangeLabel)
                Spacer()
                Text("\(range.upperBound)")
                    .font(.system(size: 13))
                    .foregroundColor(EditColors.rangeLabel)
            }
        }
        .padding(.bottom, 28)
    }

    private func commitInput() {
        let digits = inputText.filter(\.isNumber)
        if let parsed = Int(digits) {
            value = min(max(parsed, range.lowerBound), range.upperBound)
        }
        isEditing = false
    }
}

private struct PickerGroupView: View {
    let options: [String]
    let selected: String
    let onSelect: (String) -> Void

    var body: some View {
        FlowLayout(spacing: 8) {
            ForEach(options, id: \.self) { option in
                let isSelected = option == selected
                Button(action: { onSelect(option) }) {
                    Text(option)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(isSelected ? .white : EditColors.black)
                        .padding(.horizontal, 18)
                        .padding(.vertical, 12)
                        .background(
                            RoundedRectangle(cornerRadius: 24, style: .continuous)
                                .fill(isSelected ? Palette.coral : Color.white)
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 24, style: .continuous)
                                .stroke(
                                    isSelected ? Palette.coral : EditColors.border,
                                    lineWidth: isSelected ? 2 : 1.5
                                )
                        )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.top, 6)
    }
}

private struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = computeLayout(proposal: proposal, subviews: subviews)
        return CGSize(width: proposal.width ?? result.size.width, height: result.size.height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = computeLayout(proposal: proposal, subviews: subviews)
        for (index, offset) in result.offsets.enumerated() {
            subviews[index].place(
                at: CGPoint(x: bounds.minX + offset.x, y: bounds.minY + offset.y),
                proposal: .unspecified
            )
        }
    }

    private struct LayoutResult {
        var offsets: [CGPoint]
        var size: CGSize
    }

    private func computeLayout(proposal: ProposedViewSize, subviews: Subviews) -> LayoutResult {
        let maxWidth = proposal.width ?? .infinity
        var offsets: [CGPoint] = []
        var currentX: CGFloat = 0
        var currentY: CGFloat = 0
        var lineHeight: CGFloat = 0
        var totalWidth: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if currentX + size.width > maxWidth, currentX > 0 {
                currentX = 0
                currentY += lineHeight + spacing
                lineHeight = 0
            }
            offsets.append(CGPoint(x: currentX, y: currentY))
            lineHeight = max(lineHeight, size.height)
            currentX += size.width + spacing
            totalWidth = max(totalWidth, currentX - spacing)
        }

        return LayoutResult(
            offsets: offsets,
            size: CGSize(width: totalWidth, height: currentY + lineHeight)
        )
    }
}

private func stepBottomButton(text: String, action: @escaping () -> Void) -> some View {
    Button(action: action) {
        Text(text)
            .font(.system(size: 18, weight: .bold))
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(
                RoundedRectangle(cornerRadius: 28, style: .continuous)
                    .fill(Palette.coral)
            )
    }
    .buttonStyle(.plain)
    .padding(.horizontal, 20)
    .padding(.bottom, 12)
}
