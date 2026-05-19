import SwiftUI
import Shared

struct ActivityCatalogView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var tabBarState: TabBarState
    @StateObject private var viewModel = ActivityCatalogViewModelWrapper()

    @State private var searchQuery: String = ""
    @State private var scheduleTarget: CatalogScheduleTarget? = nil

    var body: some View {
        ZStack {
            Palette.warmOffWhite.ignoresSafeArea()

            VStack(spacing: 0) {
                navigationBar
                searchBar

                if viewModel.isLoading {
                    spinner
                } else if let error = viewModel.error, viewModel.catalog.isEmpty {
                    ErrorBanner(message: error, onRetry: { viewModel.load() })
                } else if filteredItems.isEmpty {
                    EmptyHint(
                        icon: "magnifyingglass",
                        message: "Ничего не найдено",
                        hint: "Попробуй изменить запрос"
                    )
                } else {
                    ScrollView(showsIndicators: false) {
                        VStack(alignment: .leading, spacing: 8) {
                            ForEach(ringOrder, id: \.self) { ring in
                                let items = grouped[ring] ?? []
                                if !items.isEmpty {
                                    Text(ringLabel(ring))
                                        .font(.system(size: 18, weight: .medium))
                                        .foregroundColor(Palette.deepInk)
                                        .padding(.top, 8)
                                        .padding(.bottom, 4)
                                    ForEach(items, id: \.id) { activity in
                                        CatalogActivityCard(
                                            activity: activity,
                                            isInPlan: viewModel.planIds.contains(activity.id),
                                            onToggle: {
                                                if viewModel.planIds.contains(activity.id) {
                                                    viewModel.togglePlan(activityId: activity.id)
                                                } else {
                                                    scheduleTarget = CatalogScheduleTarget(
                                                        activityId: activity.id,
                                                        activityName: activity.name,
                                                        defaultGoal: activity.defaultGoal?.intValue
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer().frame(height: 24)
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 16)
                    }
                }
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .sheet(item: $scheduleTarget) { target in
            CatalogScheduleSheet(
                activityName: target.activityName,
                onAdd: { scheduleType, scheduleDays, oneOffDate in
                    viewModel.addToPlanWithSchedule(
                        activityId: target.activityId,
                        scheduleType: scheduleType,
                        scheduleDays: scheduleDays,
                        oneOffDate: oneOffDate,
                        goal: target.defaultGoal.map { Int($0) }
                    )
                    scheduleTarget = nil
                },
                onDismiss: { scheduleTarget = nil }
            )
            .presentationDetents([.medium, .large])
        }
        .onAppear {
            tabBarState.isVisible = false
            viewModel.load()
        }
        .onDisappear {
            tabBarState.isVisible = true
        }
    }

    private var filteredItems: [ActivityCatalogResponse] {
        if searchQuery.isEmpty { return viewModel.catalog }
        let query = searchQuery.lowercased()
        return viewModel.catalog.filter { $0.name.lowercased().contains(query) }
    }

    private var grouped: [String: [ActivityCatalogResponse]] {
        Dictionary(grouping: filteredItems, by: { $0.ring })
    }

    private let ringOrder: [String] = ["MOVE", "MIND", "LIFE"]

    private func ringLabel(_ ring: String) -> String {
        switch ring {
        case "MOVE": return "Движение"
        case "MIND": return "Разум"
        case "LIFE": return "Режим"
        default: return ring
        }
    }

    private var navigationBar: some View {
        HStack {
            Button(action: { dismiss() }) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
                    .frame(width: 40, height: 40)
                    .background(Circle().fill(Palette.softCard))
            }

            Spacer()

            Text("Добавить активность")
                .font(.system(size: 17, weight: .heavy))
                .foregroundColor(Palette.deepInk)

            Spacer()

            Spacer().frame(width: 40, height: 40)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 8)
    }

    private var searchBar: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .foregroundColor(Palette.mutedText)
            TextField("Найти активность...", text: $searchQuery)
                .font(.system(size: 15))
                .foregroundColor(Palette.deepInk)
            if !searchQuery.isEmpty {
                Button(action: { searchQuery = "" }) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 16))
                        .foregroundColor(Palette.mutedText.opacity(0.6))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(RoundedRectangle(cornerRadius: 16).fill(Palette.softCard))
        .padding(.horizontal, 20)
        .padding(.top, 8)
    }

    private var spinner: some View {
        ProgressView()
            .progressViewStyle(.circular)
            .tint(Palette.accentOrange)
            .frame(maxWidth: .infinity)
            .frame(maxHeight: .infinity)
    }
}

private struct CatalogActivityCard: View {
    let activity: ActivityCatalogResponse
    let isInPlan: Bool
    let onToggle: () -> Void

    private var ring: Color { ringColor(for: activity.ring) }

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle().fill(ring.opacity(0.15))
                Text(String(activity.name.prefix(1)).uppercased())
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(ring)
            }
            .frame(width: 36, height: 36)

            VStack(alignment: .leading, spacing: 2) {
                Text(activity.name)
                    .font(.system(size: 18, weight: .medium))
                    .foregroundColor(Palette.deepInk)
                Text(kindLabel(activity.kind))
                    .font(.system(size: 16))
                    .foregroundColor(Palette.mutedText)
            }

            Spacer()

            Button(action: onToggle) {
                ZStack {
                    Circle().fill(isInPlan ? ring : Palette.softCard)
                    Image(systemName: isInPlan ? "checkmark" : "plus")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(isInPlan ? .white : ring)
                }
                .frame(width: 32, height: 32)
            }
            .buttonStyle(.plain)
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 14).fill(Color.white))
    }

    private func ringColor(for ring: String) -> Color {
        switch ring {
        case "MOVE": return Palette.ringMove
        case "MIND": return Palette.ringMind
        case "LIFE": return Palette.ringLife
        default: return Palette.accentOrange
        }
    }

    private func kindLabel(_ kind: String) -> String {
        switch kind {
        case "BINARY": return "Да/Нет"
        case "COUNTER": return "Счётчик"
        case "PRESET": return "Пресеты"
        default: return kind
        }
    }
}

private struct EmptyHint: View {
    let icon: String
    let message: String
    let hint: String

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 32))
                .foregroundColor(Palette.accentOrange.opacity(0.7))
            Text(message)
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(Palette.deepInk)
            Text(hint)
                .font(.system(size: 13))
                .foregroundColor(Palette.mutedText)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(.horizontal, 32)
    }
}

private struct ErrorBanner: View {
    let message: String
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 32))
                .foregroundColor(Palette.accentOrange.opacity(0.8))
            Text("Не удалось загрузить")
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(Palette.deepInk)
            Text(message)
                .font(.system(size: 13))
                .foregroundColor(Palette.mutedText)
                .multilineTextAlignment(.center)
            Button(action: onRetry) {
                Text("Повторить")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 10)
                    .background(RoundedRectangle(cornerRadius: 12).fill(Palette.accentOrange))
            }
            .buttonStyle(.plain)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(.horizontal, 32)
    }
}

// MARK: - Schedule selection when adding from catalog

struct CatalogScheduleTarget: Identifiable {
    let id: String
    let activityId: String
    let activityName: String
    let defaultGoal: Int32?

    init(activityId: String, activityName: String, defaultGoal: Int32? = nil) {
        self.id = activityId
        self.activityId = activityId
        self.activityName = activityName
        self.defaultGoal = defaultGoal
    }
}

private enum CatalogScheduleMode: CaseIterable {
    case everyDay
    case recurring
    case oneTime

    var label: String {
        switch self {
        case .everyDay: return "Каждый день"
        case .recurring: return "По дням недели"
        case .oneTime: return "На конкретную дату"
        }
    }
}

/// Sheet presented when the user adds an activity from the catalog,
/// allowing them to choose a schedule before the activity is added to
/// their plan.
private struct CatalogScheduleSheet: View {
    let activityName: String
    let onAdd: (_ scheduleType: String?, _ scheduleDays: [String], _ oneOffDate: String?) -> Void
    let onDismiss: () -> Void

    private static let daysOfWeek: [String] = ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"]

    @State private var scheduleMode: CatalogScheduleMode = .everyDay
    @State private var selectedDays: Set<String> = Set(["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"])
    @State private var oneOffDate: String = ""
    @State private var showDatePicker: Bool = false
    @State private var pickerDate: Date = Date()

    private static let dayLabelToBackend: [String: String] = [
        "Пн": "MONDAY", "Вт": "TUESDAY", "Ср": "WEDNESDAY",
        "Чт": "THURSDAY", "Пт": "FRIDAY", "Сб": "SATURDAY", "Вс": "SUNDAY"
    ]

    private var isFormValid: Bool {
        switch scheduleMode {
        case .everyDay: return true
        case .recurring: return !selectedDays.isEmpty
        case .oneTime: return !oneOffDate.isEmpty
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Header
            HStack(spacing: 12) {
                ZStack {
                    Circle().fill(Palette.accentOrange.opacity(0.12))
                    Text(String(activityName.prefix(1)).uppercased())
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(Palette.accentOrange)
                }
                .frame(width: 48, height: 48)

                VStack(alignment: .leading, spacing: 2) {
                    Text(activityName)
                        .font(.system(size: 18, weight: .heavy))
                        .foregroundColor(Palette.deepInk)
                    Text("Выбери расписание")
                        .font(.system(size: 13))
                        .foregroundColor(Palette.mutedText)
                }
                Spacer()
            }
            .padding(.top, 8)

            Spacer().frame(height: 20)

            // Schedule mode radio buttons
            VStack(alignment: .leading, spacing: 4) {
                ForEach(CatalogScheduleMode.allCases, id: \.self) { mode in
                    Button(action: { scheduleMode = mode }) {
                        HStack(spacing: 12) {
                            ZStack {
                                Circle()
                                    .stroke(
                                        mode == scheduleMode
                                            ? Palette.accentOrange
                                            : Palette.mutedText.opacity(0.4),
                                        lineWidth: 1.5
                                    )
                                    .frame(width: 22, height: 22)
                                if mode == scheduleMode {
                                    Circle()
                                        .fill(Palette.accentOrange)
                                        .frame(width: 22, height: 22)
                                    Circle()
                                        .fill(.white)
                                        .frame(width: 8, height: 8)
                                }
                            }
                            Text(mode.label)
                                .font(.system(size: 15, weight: mode == scheduleMode ? .semibold : .regular))
                                .foregroundColor(mode == scheduleMode ? Palette.deepInk : Palette.mutedText)
                        }
                        .padding(.vertical, 8)
                    }
                    .buttonStyle(.plain)
                }
            }

            // Day chips for recurring
            if scheduleMode == .recurring {
                Spacer().frame(height: 12)
                HStack {
                    ForEach(Self.daysOfWeek, id: \.self) { day in
                        Button(action: { toggleDay(day) }) {
                            Text(day)
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(selectedDays.contains(day) ? .white : Palette.deepInk)
                                .frame(width: 36, height: 36)
                                .background(
                                    Group {
                                        if selectedDays.contains(day) {
                                            Circle().fill(Palette.accentOrange)
                                        } else {
                                            Circle().stroke(Palette.mutedText.opacity(0.35), lineWidth: 1)
                                        }
                                    }
                                )
                        }
                        .buttonStyle(.plain)
                        if day != Self.daysOfWeek.last {
                            Spacer()
                        }
                    }
                }
            }

            // Date picker for one-time
            if scheduleMode == .oneTime {
                Spacer().frame(height: 12)
                Button(action: { showDatePicker = true }) {
                    HStack {
                        Text(displayOneOffDate)
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(oneOffDate.isEmpty ? Palette.mutedText : Palette.deepInk)
                        Spacer()
                        Image(systemName: "calendar")
                            .foregroundColor(Palette.mutedText)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 14)
                    .background(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .stroke(Palette.mutedText.opacity(0.35), lineWidth: 1)
                    )
                }
                .buttonStyle(.plain)
            }

            Spacer().frame(height: 24)

            // Add button
            Button(action: handleAdd) {
                Text("Добавить")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(
                        RoundedRectangle(cornerRadius: 14)
                            .fill(isFormValid ? Palette.accentOrange : Palette.accentOrange.opacity(0.5))
                    )
            }
            .disabled(!isFormValid)
            .buttonStyle(.plain)

            Spacer().frame(height: 8)

            // Cancel button
            Button(action: onDismiss) {
                Text("Отмена")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(Palette.deepInk)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(RoundedRectangle(cornerRadius: 14).fill(Palette.softCard))
            }
            .buttonStyle(.plain)

            Spacer().frame(height: 8)
        }
        .padding(.horizontal, 20)
        .padding(.top, 8)
        .background(Palette.warmOffWhite.ignoresSafeArea())
        .sheet(isPresented: $showDatePicker) {
            NavigationStack {
                DatePicker("", selection: $pickerDate, in: Date()..., displayedComponents: .date)
                    .datePickerStyle(.graphical)
                    .environment(\.locale, Locale(identifier: "ru_RU"))
                    .padding()
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Готово") {
                                let formatter = DateFormatter()
                                formatter.calendar = Calendar(identifier: .iso8601)
                                formatter.dateFormat = "yyyy-MM-dd"
                                formatter.locale = Locale(identifier: "en_US_POSIX")
                                oneOffDate = formatter.string(from: pickerDate)
                                showDatePicker = false
                            }
                        }
                    }
            }
        }
    }

    private var displayOneOffDate: String {
        if oneOffDate.isEmpty { return "Выбрать дату" }
        let parser = DateFormatter()
        parser.calendar = Calendar(identifier: .iso8601)
        parser.dateFormat = "yyyy-MM-dd"
        parser.locale = Locale(identifier: "en_US_POSIX")
        guard let date = parser.date(from: oneOffDate) else { return oneOffDate }
        let display = DateFormatter()
        display.locale = Locale(identifier: "ru_RU")
        display.dateFormat = "d MMMM yyyy"
        return display.string(from: date)
    }

    private func toggleDay(_ day: String) {
        if selectedDays.contains(day) && selectedDays.count > 1 {
            selectedDays.remove(day)
        } else {
            selectedDays.insert(day)
        }
    }

    private func handleAdd() {
        let scheduleType: String?
        var days: [String] = []
        var date: String? = nil

        switch scheduleMode {
        case .everyDay:
            scheduleType = nil
        case .recurring:
            scheduleType = "RECURRING"
            let ordered = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]
            let backendDays = selectedDays.compactMap { Self.dayLabelToBackend[$0] }
            days = backendDays.sorted { ordered.firstIndex(of: $0)! < ordered.firstIndex(of: $1)! }
        case .oneTime:
            scheduleType = "ONE_TIME"
            date = oneOffDate
        }

        onAdd(scheduleType, days, date)
    }
}
