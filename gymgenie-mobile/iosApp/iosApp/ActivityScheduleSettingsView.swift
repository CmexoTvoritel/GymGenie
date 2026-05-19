import SwiftUI
import Shared

/// Screen for editing the schedule of an activity that is already in the
/// user's plan. Communicates with the backend via `ActivitiesViewModelWrapper.updateSchedule`.
struct ActivityScheduleSettingsView: View {
    let activityId: String
    let activityName: String
    let initialScheduleType: String?
    let initialScheduleDays: [String]
    let initialOneOffDate: String?

    var onDismiss: () -> Void = {}
    @ObservedObject var viewModel: ActivitiesViewModelWrapper

    // MARK: - Local state

    @State private var scheduleMode: ScheduleMode
    @State private var selectedDays: Set<String>
    @State private var oneOffDate: String
    @State private var showDatePicker: Bool = false
    @State private var pickerDate: Date = Date()

    // MARK: - Palette

    private let accentOrange = Color(red: 0.941, green: 0.439, blue: 0.188)
    private let deepInk = Color(red: 0.161, green: 0.141, blue: 0.125)
    private let mutedText = Color(red: 0.463, green: 0.447, blue: 0.416)
    private let warmOffWhite = Color(red: 0.980, green: 0.976, blue: 0.969)

    // MARK: - Constants

    private static let daysOfWeek: [String] = ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"]

    private static let dayLabelToBackend: [String: String] = [
        "Пн": "MONDAY", "Вт": "TUESDAY", "Ср": "WEDNESDAY",
        "Чт": "THURSDAY", "Пт": "FRIDAY", "Сб": "SATURDAY", "Вс": "SUNDAY"
    ]

    private static let backendToDayLabel: [String: String] = {
        var map: [String: String] = [:]
        for (k, v) in dayLabelToBackend { map[v] = k }
        return map
    }()

    // MARK: - Init

    init(
        activityId: String,
        activityName: String,
        initialScheduleType: String?,
        initialScheduleDays: [String],
        initialOneOffDate: String?,
        viewModel: ActivitiesViewModelWrapper,
        onDismiss: @escaping () -> Void = {}
    ) {
        self.activityId = activityId
        self.activityName = activityName
        self.initialScheduleType = initialScheduleType
        self.initialScheduleDays = initialScheduleDays
        self.initialOneOffDate = initialOneOffDate
        self.viewModel = viewModel
        self.onDismiss = onDismiss

        let mode: ScheduleMode
        switch initialScheduleType {
        case "RECURRING": mode = .recurring
        case "ONE_TIME": mode = .oneTime
        default: mode = .everyDay
        }
        _scheduleMode = State(initialValue: mode)

        let labels = initialScheduleDays.compactMap { Self.backendToDayLabel[$0] }
        _selectedDays = State(initialValue: labels.isEmpty ? Set(Self.daysOfWeek) : Set(labels))
        _oneOffDate = State(initialValue: initialOneOffDate ?? "")
    }

    // MARK: - Body

    var body: some View {
        ZStack(alignment: .top) {
            ScrollView(showsIndicators: false) {
                VStack(spacing: 16) {
                    topBar
                    heroSection
                        .padding(.top, 8)
                    scheduleModeCard
                        .padding(.horizontal, 20)

                    switch scheduleMode {
                    case .everyDay:
                        EmptyView()
                    case .recurring:
                        daysOfWeekCard
                            .padding(.horizontal, 20)
                    case .oneTime:
                        oneOffDateCard
                            .padding(.horizontal, 20)
                    }

                    if let error = viewModel.scheduleUpdateError {
                        Text(error)
                            .font(.system(size: 13))
                            .foregroundColor(.red.opacity(0.8))
                            .padding(.horizontal, 24)
                    }

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
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(warmOffWhite.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
        .sheet(isPresented: $showDatePicker) {
            datePickerSheet
        }
    }

    // MARK: - Sections

    private var topBar: some View {
        ZStack {
            Text("Расписание")
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
            Text(activityName)
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(deepInk)
        }
        .padding(.top, 16)
    }

    private var scheduleModeCard: some View {
        whiteCard {
            VStack(alignment: .leading, spacing: 12) {
                Text("Тип расписания")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(deepInk)

                ForEach(ScheduleMode.allCases, id: \.self) { mode in
                    Button(action: { scheduleMode = mode }) {
                        HStack(spacing: 12) {
                            ZStack {
                                Circle()
                                    .stroke(mode == scheduleMode ? accentOrange : mutedText.opacity(0.4), lineWidth: 1.5)
                                    .frame(width: 22, height: 22)
                                if mode == scheduleMode {
                                    Circle()
                                        .fill(accentOrange)
                                        .frame(width: 22, height: 22)
                                    Circle()
                                        .fill(.white)
                                        .frame(width: 8, height: 8)
                                }
                            }
                            Text(mode.label)
                                .font(.system(size: 15, weight: mode == scheduleMode ? .semibold : .regular))
                                .foregroundColor(mode == scheduleMode ? deepInk : mutedText)
                        }
                        .padding(.vertical, 6)
                    }
                    .buttonStyle(.plain)
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

    private var oneOffDateCard: some View {
        whiteCard {
            VStack(alignment: .leading, spacing: 12) {
                Text("Дата")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(deepInk)

                Button(action: { showDatePicker = true }) {
                    HStack {
                        Text(displayOneOffDate)
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(oneOffDate.isEmpty ? mutedText : deepInk)
                        Spacer()
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 14)
                    .background(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .stroke(mutedText.opacity(0.35), lineWidth: 1)
                    )
                }
                .buttonStyle(.plain)
            }
            .padding(20)
        }
    }

    private var datePickerSheet: some View {
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

    private var confirmButton: some View {
        Button(action: handleConfirm) {
            Group {
                if viewModel.isScheduleUpdating {
                    ProgressView()
                        .progressViewStyle(.circular)
                        .tint(.white)
                } else {
                    Text("Сохранить расписание")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.white)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(isFormValid ? accentOrange : accentOrange.opacity(0.5))
            )
        }
        .disabled(!isFormValid || viewModel.isScheduleUpdating)
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

    // MARK: - Computed

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

    private var isFormValid: Bool {
        switch scheduleMode {
        case .everyDay: return true
        case .recurring: return !selectedDays.isEmpty
        case .oneTime: return !oneOffDate.isEmpty
        }
    }

    // MARK: - Actions

    private func toggleDay(_ day: String) {
        if selectedDays.contains(day) && selectedDays.count > 1 {
            selectedDays.remove(day)
        } else {
            selectedDays.insert(day)
        }
    }

    private func handleConfirm() {
        let type: String?
        var days: [String] = []
        var date: String? = nil

        switch scheduleMode {
        case .everyDay:
            type = nil
        case .recurring:
            type = "RECURRING"
            let ordered = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]
            let backendDays = selectedDays.compactMap { Self.dayLabelToBackend[$0] }
            days = backendDays.sorted { ordered.firstIndex(of: $0)! < ordered.firstIndex(of: $1)! }
        case .oneTime:
            type = "ONE_TIME"
            date = oneOffDate
        }

        viewModel.updateSchedule(
            activityId: activityId,
            scheduleType: type,
            scheduleDays: days,
            oneOffDate: date
        )
        onDismiss()
    }
}

// MARK: - ScheduleMode

private enum ScheduleMode: CaseIterable {
    case everyDay
    case recurring
    case oneTime

    var label: String {
        switch self {
        case .everyDay: return "Каждый день"
        case .recurring: return "По дням недели"
        case .oneTime: return "Один раз"
        }
    }
}
