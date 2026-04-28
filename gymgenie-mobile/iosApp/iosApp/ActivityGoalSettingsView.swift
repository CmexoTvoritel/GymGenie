import SwiftUI

/// Local-state settings screen for a single activity goal (steps, water intake, etc.).
///
/// Purely client-side for now — no backend persistence. On confirmation the
/// screen calls `onConfirm` with the tuned value and then dismisses; callers
/// decide what to do with the result.
struct ActivityGoalSettingsView: View {
    let emoji: String
    let title: String
    let unit: String
    let defaultValue: Int
    let step: Int

    var onDismiss: () -> Void = {}
    var onConfirm: (Int) -> Void = { _ in }

    // MARK: - Local state

    @State private var value: Int
    @State private var remindersEnabled: Bool = true
    @State private var intervalHours: Int = 2
    @State private var selectedDays: Set<String> = Set(Self.daysOfWeek)

    // MARK: - Palette

    private let accentOrange = Color(red: 0.941, green: 0.439, blue: 0.188)
    private let deepInk = Color(red: 0.161, green: 0.141, blue: 0.125)
    private let mutedText = Color(red: 0.463, green: 0.447, blue: 0.416)
    private let warmOffWhite = Color(red: 0.980, green: 0.976, blue: 0.969)

    // MARK: - Constants

    private static let daysOfWeek: [String] = ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"]
    private static let reminderIntervals: [Int] = [1, 2, 3, 4]

    // MARK: - Init

    init(
        emoji: String,
        title: String,
        unit: String,
        defaultValue: Int,
        step: Int,
        onDismiss: @escaping () -> Void = {},
        onConfirm: @escaping (Int) -> Void = { _ in }
    ) {
        self.emoji = emoji
        self.title = title
        self.unit = unit
        self.defaultValue = defaultValue
        self.step = step
        self.onDismiss = onDismiss
        self.onConfirm = onConfirm
        _value = State(initialValue: defaultValue)
    }

    // MARK: - Derived

    private var maxValue: Int {
        switch unit {
        case "шагов": return 50_000
        case "стаканов": return 20
        case "часов": return 24
        case "минут": return 180
        default: return 999
        }
    }

    // MARK: - Body

    var body: some View {
        ZStack(alignment: .top) {
            warmOffWhite.edgesIgnoringSafeArea(.all)

            ScrollView(showsIndicators: false) {
                VStack(spacing: 16) {
                    topBar
                    heroSection
                        .padding(.top, 8)
                    goalCounterCard
                        .padding(.horizontal, 20)
                    remindersCard
                        .padding(.horizontal, 20)
                    daysOfWeekCard
                        .padding(.horizontal, 20)

                    Spacer(minLength: 16)

                    confirmButton
                        .padding(.horizontal, 20)

                    cancelButton
                        .padding(.horizontal, 20)
                        .padding(.bottom, 24)
                }
                .padding(.top, 8)
            }
        }
    }

    // MARK: - Sections

    private var topBar: some View {
        ZStack {
            Text("Настройка цели")
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(deepInk)

            HStack {
                Button(action: onDismiss) {
                    Text("‹")
                        .font(.system(size: 28, weight: .medium))
                        .foregroundColor(deepInk)
                        .frame(width: 40, height: 40)
                }
                Spacer()
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 12)
    }

    private var heroSection: some View {
        VStack(spacing: 12) {
            Text(emoji)
                .font(.system(size: 72))
            Text(title)
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(deepInk)
        }
        .padding(.top, 16)
    }

    private var goalCounterCard: some View {
        whiteCard {
            VStack(spacing: 12) {
                Text("Цель на день")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(mutedText)

                HStack {
                    circleStepButton(symbol: "−", action: decrement)
                    Spacer()
                    Text("\(value)")
                        .font(.system(size: 36, weight: .bold))
                        .foregroundColor(deepInk)
                    Spacer()
                    circleStepButton(symbol: "+", action: increment)
                }

                Text(unit)
                    .font(.system(size: 14))
                    .foregroundColor(mutedText)
            }
            .padding(20)
        }
    }

    private var remindersCard: some View {
        whiteCard {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Text("Включить напоминания")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(deepInk)
                    Spacer()
                    Toggle("", isOn: $remindersEnabled)
                        .labelsHidden()
                        .toggleStyle(SwitchToggleStyle(tint: accentOrange))
                }

                if remindersEnabled {
                    Text("Интервал")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(mutedText)

                    HStack(spacing: 8) {
                        ForEach(Self.reminderIntervals, id: \.self) { hours in
                            intervalChip(
                                label: "\(hours)ч",
                                isSelected: hours == intervalHours,
                                action: { intervalHours = hours }
                            )
                        }
                    }

                    HStack(spacing: 12) {
                        Text("с 08:00")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(deepInk)
                        Text("до 22:00")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(deepInk)
                    }
                }
            }
            .padding(20)
        }
    }

    private var daysOfWeekCard: some View {
        whiteCard {
            VStack(alignment: .leading, spacing: 12) {
                Text("Дни недели")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(deepInk)

                HStack {
                    ForEach(Self.daysOfWeek, id: \.self) { day in
                        dayChip(
                            label: day,
                            isSelected: selectedDays.contains(day),
                            action: { toggleDay(day) }
                        )
                        if day != Self.daysOfWeek.last {
                            Spacer()
                        }
                    }
                }
            }
            .padding(20)
        }
    }

    private var confirmButton: some View {
        Button(action: handleConfirm) {
            Text("Добавить активность")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(accentOrange)
                )
        }
    }

    private var cancelButton: some View {
        Button(action: onDismiss) {
            Text("Отмена")
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(mutedText)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
        }
    }

    // MARK: - Reusable components

    private func whiteCard<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        content()
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(Color.white)
                    .shadow(color: Color.black.opacity(0.06), radius: 8, x: 0, y: 4)
            )
    }

    private func circleStepButton(symbol: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(symbol)
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(.white)
                .frame(width: 40, height: 40)
                .background(Circle().fill(accentOrange))
        }
        .buttonStyle(.plain)
    }

    private func intervalChip(label: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(isSelected ? .white : deepInk)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(
                    Group {
                        if isSelected {
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .fill(accentOrange)
                        } else {
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .stroke(mutedText.opacity(0.35), lineWidth: 1)
                        }
                    }
                )
        }
        .buttonStyle(.plain)
    }

    private func dayChip(label: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(isSelected ? .white : deepInk)
                .frame(width: 36, height: 36)
                .background(
                    Group {
                        if isSelected {
                            Circle().fill(accentOrange)
                        } else {
                            Circle().stroke(mutedText.opacity(0.35), lineWidth: 1)
                        }
                    }
                )
        }
        .buttonStyle(.plain)
    }

    // MARK: - Actions

    private func decrement() {
        value = max(1, value - step)
    }

    private func increment() {
        value = min(maxValue, value + step)
    }

    private func toggleDay(_ day: String) {
        if selectedDays.contains(day) {
            selectedDays.remove(day)
        } else {
            selectedDays.insert(day)
        }
    }

    private func handleConfirm() {
        onConfirm(value)
        onDismiss()
    }
}

#if DEBUG
struct ActivityGoalSettingsView_Previews: PreviewProvider {
    static var previews: some View {
        ActivityGoalSettingsView(
            emoji: "🏃",
            title: "Физическая активность",
            unit: "шагов",
            defaultValue: 10000,
            step: 500
        )
    }
}
#endif
