import SwiftUI
import Shared

// MARK: - Home screen (premium redesign)

struct HomeView: View {
    @EnvironmentObject private var appState: AppState
    @EnvironmentObject private var profileStore: UserProfileStoreWrapper
    @StateObject private var viewModel = HomeViewModelWrapper()

    @State private var showingSession = false
    @State private var activeSession: ActiveWorkoutSession? = nil

    /// When non-nil, a full-screen meal-plan detail view is presented for that
    /// plan. We use a single optional `String?` (planId) instead of two
    /// separate flags so the presented sheet can never be open with a stale id
    /// after a refetch.
    @State private var openMealPlanId: String? = nil
    @State private var showingAiCoach = false
    @State private var showingNutrition = false
    @State private var showingCreateMealPlan = false

    var body: some View {
        NavigationStack {
            ZStack {
                Palette.warmOffWhite.ignoresSafeArea()

                switch viewModel.screenState {
                case .loading:
                    loadingView
                case .error(let message):
                    errorView(message: message)
                case .content:
                    contentView
                        .refreshable {
                            await refreshAndWait()
                        }
                        // Tints the system pull-to-refresh spinner. Scoped to
                        // the scrollable content so it does not leak into the
                        // accent color of unrelated controls.
                        .tint(Palette.coral)
                        .alert(
                            "Ошибка обновления",
                            isPresented: Binding(
                                get: { viewModel.isContentLoaded && viewModel.errorMessage != nil },
                                set: { if !$0 { viewModel.clearTransientError() } }
                            ),
                            presenting: viewModel.errorMessage
                        ) { _ in
                            Button("OK") { viewModel.clearTransientError() }
                        } message: { message in
                            Text(message)
                        }
                }
            }
            .fullScreenCover(isPresented: $showingSession) {
                if let session = activeSession {
                    WorkoutSessionView(session: session)
                }
            }
            .fullScreenCover(item: Binding(
                get: { openMealPlanId.map { MealPlanIdHolder(value: $0) } },
                set: { openMealPlanId = $0?.value }
            )) { holder in
                MealPlanDetailView(
                    planId: holder.value,
                    onClose: { openMealPlanId = nil }
                )
            }
            .fullScreenCover(isPresented: $showingAiCoach) {
                AiCoachView()
                    .environmentObject(profileStore)
            }
            .fullScreenCover(isPresented: $showingNutrition) {
                NutritionView(onClose: { showingNutrition = false })
                    .environmentObject(profileStore)
            }
            .fullScreenCover(isPresented: $showingCreateMealPlan) {
                // Manual meal-plan creation flow. The save branch dismisses
                // the sheet AND triggers a targeted reload of the home
                // meal-plan section so the new plan appears without a manual
                // pull-to-refresh.
                CreateMealPlanFlowView(
                    onClose: { showingCreateMealPlan = false },
                    onSaved: {
                        showingCreateMealPlan = false
                        viewModel.refreshMealPlans()
                    }
                )
            }
            .onAppear {
                viewModel.setProfileStore(profileStore.store)
                if !viewModel.isContentLoaded {
                    viewModel.loadData()
                }
            }
            .onChange(of: viewModel.isLoggedOut) { loggedOut in
                if loggedOut {
                    appState.navigate(to: .login)
                }
            }
        }
    }

    /// SwiftUI's `.refreshable` keeps the system spinner visible until the
    /// async closure returns. We poll the wrapper's `isRefreshing` flag so the
    /// indicator stays in sync with the underlying KMM state machine.
    private func refreshAndWait() async {
        viewModel.refresh()
        let pollInterval: UInt64 = 50_000_000 // 50ms
        // Wait briefly for the refresh to start (the wrapper observes the
        // shared StateFlow on a 50ms tick, so the flag won't flip in the
        // same run-loop turn).
        let startWaitMaxNanos: UInt64 = 500_000_000 // 0.5s
        var startWaited: UInt64 = 0
        while !viewModel.isRefreshing && startWaited < startWaitMaxNanos {
            try? await Task.sleep(nanoseconds: pollInterval)
            startWaited += pollInterval
        }
        // Then wait for the refresh to complete, capped at a generous timeout
        // so a stalled network call cannot leave the spinner permanently
        // attached to the closure.
        let maxWaitNanos: UInt64 = 15_000_000_000 // 15s safety cap
        var elapsed: UInt64 = 0
        while viewModel.isRefreshing && elapsed < maxWaitNanos {
            try? await Task.sleep(nanoseconds: pollInterval)
            elapsed += pollInterval
        }
    }

    // MARK: - Loading / error

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView().scaleEffect(1.2)
            Text("Загрузка...")
                .font(.system(size: 15))
                .foregroundColor(Palette.mutedText)
        }
    }

    private func errorView(message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 40))
                .foregroundColor(Palette.accentOrange)

            Text(message)
                .font(.system(size: 15))
                .foregroundColor(Palette.mutedText)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            Button(action: viewModel.retry) {
                Text("Повторить")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 32)
                    .padding(.vertical, 12)
                    .background(Capsule().fill(Palette.accentOrange))
            }
        }
    }

    // MARK: - Content

    private var contentView: some View {
        let slot = HomeView.resolveTodaySlot(plans: viewModel.activeWorkoutPlans, today: HomeView.currentDayOfWeek())

        return ScrollView(showsIndicators: false) {
            LazyVStack(alignment: .leading, spacing: 20) {
                HomeHeaderSection(
                    username: viewModel.username.isEmpty ? "друг" : viewModel.username,
                    streakDays: Int(viewModel.streakDays)
                )

                ActivitiesSectionHeader(
                    title: slot.title,
                    subtitle: slot.subtitle,
                    actionTitle: slot.showAll ? "Все" : nil,
                    destination: nil
                )

                workoutSlotView(slot: slot)

                ActivitiesSectionHeader(
                    title: "Активности",
                    subtitle: "Отметить вручную — без умных часов",
                    actionTitle: "Ещё",
                    destination: AnyView(ActivitiesView())
                )

                ActivityRingsCard(activities: viewModel.todayActivities)

                ActivityRowsCard(
                    activities: viewModel.todayActivities,
                    onCheckIn: { id, value in
                        viewModel.checkIn(activityId: id, value: value)
                    }
                )

                AddActivityButton(destination: AnyView(ActivityCatalogView()))

                MealPlanSection(
                    todayPlans: viewModel.todayMealPlans,
                    onPlanTap: { planId in openMealPlanId = planId },
                    // "Create plan" CTA on the home empty-state opens the
                    // manual creation flow directly. The legacy AI-only
                    // surface is reachable from `NutritionView`'s FAB.
                    onCreatePlan: { showingCreateMealPlan = true }
                )

                coursesSection

                Spacer().frame(height: 24)
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)
        }
    }

    // MARK: - Section: workout of the day

    @ViewBuilder
    private func workoutSlotView(slot: TodaySlot) -> some View {
        if slot.plans.isEmpty {
            NoWorkoutPlaceholder(onCreate: { showingAiCoach = true })
        } else {
            WorkoutTodayPager(
                plans: slot.plans,
                isRecommended: slot.isRecommended,
                onView: { _ in
                    // TODO: navigate to plan detail once the route is wired up on iOS.
                },
                onStart: { plan in
                    activeSession = ActiveWorkoutSession(
                        planId: plan.id,
                        planName: plan.name,
                        exercises: [],
                        restSeconds: 60
                    )
                    showingSession = true
                }
            )
        }
    }

    // MARK: - Today-slot resolution

    private static let dayNamesRu: [String: String] = [
        "MONDAY": "понедельник",
        "TUESDAY": "вторник",
        "WEDNESDAY": "среда",
        "THURSDAY": "четверг",
        "FRIDAY": "пятница",
        "SATURDAY": "суббота",
        "SUNDAY": "воскресенье",
    ]

    /// Returns today's weekday using the same `DayOfWeek` enum-name casing the
    /// backend emits in `WorkoutPlanShortResponse.scheduleDays`.
    /// Gregorian: Sunday=1, Monday=2 … Saturday=7.
    static func currentDayOfWeek() -> String {
        let weekday = Calendar.current.component(.weekday, from: Date())
        switch weekday {
        case 1: return "SUNDAY"
        case 2: return "MONDAY"
        case 3: return "TUESDAY"
        case 4: return "WEDNESDAY"
        case 5: return "THURSDAY"
        case 6: return "FRIDAY"
        case 7: return "SATURDAY"
        default: return "MONDAY"
        }
    }

    static func resolveTodaySlot(plans: [WorkoutPlanShortResponse], today: String) -> TodaySlot {
        let upper = today.uppercased()
        let recurringToday = plans.filter { plan in
            plan.scheduleType.uppercased() == "RECURRING" &&
                plan.scheduleDays.contains(where: { $0.uppercased() == upper })
        }
        let oneOff = plans.filter { $0.scheduleType.uppercased() == "ONE_TIME" }
        let dayRu = dayNamesRu[upper] ?? upper.lowercased()

        if !recurringToday.isEmpty {
            return TodaySlot(
                title: "Тренировка на сегодня",
                subtitle: "\(recurringToday.count) запланировано · \(dayRu)",
                showAll: true,
                plans: recurringToday,
                isRecommended: false
            )
        } else if let first = oneOff.first {
            return TodaySlot(
                title: "Рекомендуем сегодня",
                subtitle: "Подобрали разовую под твой день",
                showAll: false,
                plans: [first],
                isRecommended: true
            )
        } else {
            return TodaySlot(
                title: "Рекомендуем сегодня",
                subtitle: "На этот день тренировки не запланированы",
                showAll: false,
                plans: [],
                isRecommended: true
            )
        }
    }

    // MARK: - Section: courses

    private var coursesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Курсы тренеров")
                        .font(.system(size: 20, weight: .heavy))
                        .foregroundColor(Palette.deepInk)
                    Text("От профи под твои цели")
                        .font(.system(size: 13))
                        .foregroundColor(Palette.mutedText)
                }
                Spacer()
                HStack(spacing: 4) {
                    Text("Все")
                    Text("→")
                }
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(Palette.accentOrange)
            }

            CoursesBlock()
        }
    }
}

// MARK: - Header

private struct HomeHeaderSection: View {
    let username: String
    let streakDays: Int

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            // Avatar — coral background to match the redesigned identity.
            ZStack {
                Circle().fill(Palette.coral)
                Text(String(username.prefix(1)).uppercased())
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)
            }
            .frame(width: 44, height: 44)

            VStack(alignment: .leading, spacing: 2) {
                Text(Self.currentDateString())
                    .font(.system(size: 12))
                    .foregroundColor(Palette.mutedText)
                Text("Привет, \(username)!")
                    .font(.system(size: 24, weight: .heavy))
                    .foregroundColor(Palette.deepInk)
                    .lineLimit(1)
            }

            Spacer()

            HStack(spacing: 4) {
                Text("🔥")
                Text("\(streakDays)")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(.white)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(Capsule().fill(Palette.accentOrange))

            Button(action: {}) {
                Image(systemName: "bell")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
                    .frame(width: 40, height: 40)
                    .background(Circle().fill(Palette.softCard))
            }
        }
    }

    private static func currentDateString() -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ru_RU")
        formatter.dateFormat = "EEEE, d MMMM"
        return formatter.string(from: Date())
    }
}

// MARK: - Today slot model

struct TodaySlot {
    let title: String
    let subtitle: String
    let showAll: Bool
    let plans: [WorkoutPlanShortResponse]
    let isRecommended: Bool
}

private let cardBorder = Color(red: 0.929, green: 0.929, blue: 0.937) // #EDEDEF

// MARK: - Workout pager

private struct WorkoutTodayPager: View {
    let plans: [WorkoutPlanShortResponse]
    /// Kept for parity with the today-slot resolution surface; the shared
    /// `WorkoutPlanCard` handles its own visual treatment so this flag is not
    /// currently consumed but is part of the public contract for future use.
    let isRecommended: Bool
    let onView: (WorkoutPlanShortResponse) -> Void
    let onStart: (WorkoutPlanShortResponse) -> Void

    @State private var currentIndex = 0

    var body: some View {
        VStack(spacing: 10) {
            TabView(selection: $currentIndex) {
                ForEach(Array(plans.enumerated()), id: \.element.id) { index, plan in
                    WorkoutPlanCard(
                        plan: plan,
                        onView: { onView(plan) },
                        onStart: { onStart(plan) }
                    )
                    .tag(index)
                    .padding(.horizontal, 1) // avoid clipped shadow on edges
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            // The card itself adapts to content; allocate enough vertical space
            // for the full layout (chips + footer button + dual-line text).
            .frame(height: cardHeight)

            if plans.count > 1 {
                HStack(spacing: 6) {
                    ForEach(0..<plans.count, id: \.self) { i in
                        Capsule()
                            .fill(i == currentIndex ? Palette.coral : Color(red: 0.835, green: 0.835, blue: 0.859))
                            .frame(width: i == currentIndex ? 22 : 6, height: 6)
                            .animation(.easeInOut(duration: 0.2), value: currentIndex)
                    }
                }
            }
        }
    }

    private var cardHeight: CGFloat {
        // Approximate heuristic — keeps the page-style TabView from clipping
        // the chips/footer button. We prefer a fixed value over GeometryReader
        // to avoid introducing layout instability when the page index changes.
        220
    }
}

// MARK: - No workout placeholder

private struct NoWorkoutPlaceholder: View {
    let onCreate: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 16).fill(Color(red: 0.957, green: 0.957, blue: 0.965))
                Text("🏋").font(.system(size: 24))
            }
            .frame(width: 52, height: 52)

            Text("Сегодня выходной")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Palette.deepInk)

            Text("На этот день тренировки не запланированы")
                .font(.system(size: 13))
                .foregroundColor(Palette.mutedText)
                .multilineTextAlignment(.center)

            Button(action: onCreate) {
                Text("Создать тренировку")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 10)
                    .background(RoundedRectangle(cornerRadius: 12).fill(Palette.coral))
            }
            .padding(.top, 4)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 28)
        .padding(.horizontal, 20)
        .background(RoundedRectangle(cornerRadius: 24).fill(.white))
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .strokeBorder(style: StrokeStyle(lineWidth: 1.5, dash: [6, 4]))
                .foregroundColor(cardBorder)
        )
    }
}

// MARK: - Activities section header

struct ActivitiesSectionHeader: View {
    let title: String
    let subtitle: String?
    let actionTitle: String?
    let destination: AnyView?

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 20, weight: .heavy))
                    .foregroundColor(Palette.deepInk)
                if let subtitle = subtitle {
                    Text(subtitle)
                        .font(.system(size: 13))
                        .foregroundColor(Palette.mutedText)
                }
            }

            Spacer()

            if let actionTitle = actionTitle {
                if let destination = destination {
                    NavigationLink {
                        destination
                    } label: {
                        HStack(spacing: 4) {
                            Text(actionTitle)
                            Text("→")
                        }
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(Palette.accentOrange)
                    }
                } else {
                    // Plain label when no destination is wired up yet (the
                    // recurring-today slot intentionally has no list route).
                    Button(action: {}) {
                        HStack(spacing: 4) {
                            Text(actionTitle)
                            Text("→")
                        }
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(Palette.accentOrange)
                    }
                }
            }
        }
    }
}

// MARK: - Activity DTO bridging

/// Conformance hoisted onto the KMM-generated type so SwiftUI bindings such
/// as `sheet(item:)` and `ForEach` can use it directly without wrapping.
/// `activityId` is the canonical primary key of the response.
///
/// The conformance is intentionally retroactive — both the type
/// (`ActivityTodayResponse`, declared in the `Shared` framework) and the
/// protocol (`Identifiable`, declared in the standard library) live in
/// foreign modules. Swift 6 emits a warning unless this is annotated with
/// `@retroactive`; older toolchains tolerate the bare form, so we guard the
/// annotation behind a Swift-version check to keep both happy.
#if swift(>=6.0)
extension Shared.ActivityTodayResponse: @retroactive Identifiable {
    public var id: String { activityId }
}
#else
extension ActivityTodayResponse: Identifiable {
    public var id: String { activityId }
}
#endif

// MARK: - Activity rings card

/// Triple-ring card driven by today's activity feed.
///
/// Each ring averages the per-activity completion fractions over the
/// activities that belong to that ring. An empty ring renders as `–`
/// instead of `0%` to distinguish "nothing planned" from "no progress yet".
struct ActivityRingsCard: View {
    let activities: [ActivityTodayResponse]

    private var totals: RingTotals {
        RingTotals.compute(activities: activities)
    }

    var body: some View {
        HStack(alignment: .center, spacing: 20) {
            ThreeRingsView(
                move: totals.move ?? 0,
                mind: totals.mind ?? 0,
                life: totals.life ?? 0
            )
            .frame(width: 140, height: 140)

            VStack(alignment: .leading, spacing: 12) {
                RingLegendRow(color: Palette.ringMove, title: "ДВИЖЕНИЕ", value: totals.move.formattedPct())
                RingLegendRow(color: Palette.ringMind, title: "РАЗУМ", value: totals.mind.formattedPct())
                RingLegendRow(color: Palette.ringLife, title: "РЕЖИМ", value: totals.life.formattedPct())
            }

            Spacer(minLength: 0)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 24).fill(Palette.softCard))
    }
}

private struct RingTotals {
    let move: CGFloat?
    let mind: CGFloat?
    let life: CGFloat?

    static func compute(activities: [ActivityTodayResponse]) -> RingTotals {
        RingTotals(
            move: average(activities: activities, ring: "MOVE"),
            mind: average(activities: activities, ring: "MIND"),
            life: average(activities: activities, ring: "LIFE")
        )
    }

    private static func average(activities: [ActivityTodayResponse], ring: String) -> CGFloat? {
        let group = activities.filter { $0.ring == ring }
        if group.isEmpty { return nil }
        let sum = group.reduce(CGFloat(0)) { acc, a in
            acc + a.toProgress().fraction.toCGFloat()
        }
        return sum / CGFloat(group.count)
    }
}

private extension Optional where Wrapped == CGFloat {
    func formattedPct() -> String {
        switch self {
        case .none: return "–"
        case .some(let v):
            let clamped = min(max(v, 0), 1)
            return "\(Int(clamped * 100))%"
        }
    }
}

private extension Float {
    func toCGFloat() -> CGFloat { CGFloat(self) }
}

/// Three concentric arc rings — outer = MOVE, middle = MIND, inner = LIFE.
/// Drawn with Circle + trim so the rounded cap sits flush against the
/// progress endpoint, matching the prototype.
struct ThreeRingsView: View {
    let move: CGFloat
    let mind: CGFloat
    let life: CGFloat

    private let strokeWidth: CGFloat = 13
    private let gap: CGFloat = 3

    var body: some View {
        ZStack {
            ring(color: Palette.ringMove, progress: move, inset: 0)
            ring(color: Palette.ringMind, progress: mind, inset: strokeWidth + gap)
            ring(color: Palette.ringLife, progress: life, inset: 2 * (strokeWidth + gap))
        }
    }

    private func ring(color: Color, progress: CGFloat, inset: CGFloat) -> some View {
        let safe = min(max(progress, 0), 1)
        return ZStack {
            Circle()
                .stroke(color.opacity(0.15), lineWidth: strokeWidth)
            if safe > 0 {
                Circle()
                    .trim(from: 0, to: safe)
                    .stroke(
                        color,
                        style: StrokeStyle(lineWidth: strokeWidth, lineCap: .round)
                    )
                    .rotationEffect(.degrees(-90))
            }
        }
        .padding(inset)
    }
}

private struct RingLegendRow: View {
    let color: Color
    let title: String
    let value: String

    var body: some View {
        HStack(spacing: 10) {
            Circle().fill(color).frame(width: 10, height: 10)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(Palette.mutedText)
                    .tracking(0.6)
                Text(value)
                    .font(.system(size: 16, weight: .heavy))
                    .foregroundColor(Palette.deepInk)
            }
        }
    }
}

// MARK: - Activity rows card

/// Filterable list of today's activities with one-tap / long-press / preset
/// check-in interactions. Sheet/filter state is local; data flows top-down
/// from the home screen so the parent stays the single source of truth.
struct ActivityRowsCard: View {
    let activities: [ActivityTodayResponse]
    let onCheckIn: (String, Int) -> Void

    @State private var activeFilter: String = ActivityFilter.all
    @State private var presetTarget: ActivityTodayResponse? = nil

    private var visible: [ActivityTodayResponse] {
        if activeFilter == ActivityFilter.all { return activities }
        return activities.filter { $0.ring == activeFilter }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ActivityFilterChips(
                activities: activities,
                active: activeFilter,
                onSelect: { activeFilter = $0 }
            )

            VStack(spacing: 8) {
                ForEach(visible, id: \.activityId) { activity in
                    ActivityCardRow(
                        activity: activity,
                        onCheckIn: onCheckIn,
                        onOpenPreset: { presetTarget = activity }
                    )
                }
            }
        }
        .sheet(item: $presetTarget) { activity in
            PresetSheetView(
                activity: activity,
                onPick: { id, value in
                    onCheckIn(id, value)
                    presetTarget = nil
                },
                onDismiss: { presetTarget = nil }
            )
            .presentationDetents([.medium])
        }
    }
}

private enum ActivityFilter {
    static let all = "ALL"
}

private func ringColor(for ring: String) -> Color {
    switch ring {
    case "MOVE": return Palette.ringMove
    case "MIND": return Palette.ringMind
    case "LIFE": return Palette.ringLife
    default: return Palette.deepInk
    }
}

private struct ActivityFilterChips: View {
    let activities: [ActivityTodayResponse]
    let active: String
    let onSelect: (String) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                FilterChip(
                    label: "Все",
                    count: activities.count,
                    isActive: active == ActivityFilter.all,
                    activeColor: Palette.deepInk,
                    onTap: { onSelect(ActivityFilter.all) }
                )
                FilterChip(
                    label: "Движение",
                    count: activities.filter { $0.ring == "MOVE" }.count,
                    isActive: active == "MOVE",
                    activeColor: Palette.ringMove,
                    onTap: { onSelect("MOVE") }
                )
                FilterChip(
                    label: "Разум",
                    count: activities.filter { $0.ring == "MIND" }.count,
                    isActive: active == "MIND",
                    activeColor: Palette.ringMind,
                    onTap: { onSelect("MIND") }
                )
                FilterChip(
                    label: "Режим",
                    count: activities.filter { $0.ring == "LIFE" }.count,
                    isActive: active == "LIFE",
                    activeColor: Palette.ringLife,
                    onTap: { onSelect("LIFE") }
                )
            }
        }
    }
}

private struct FilterChip: View {
    let label: String
    let count: Int
    let isActive: Bool
    let activeColor: Color
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 6) {
                Text(label)
                    .font(.system(size: 12.5, weight: .bold))
                    .foregroundColor(isActive ? .white : Palette.deepInk)
                Text("\(count)")
                    .font(.system(size: 12.5, weight: .bold))
                    .foregroundColor(isActive ? .white.opacity(0.85) : Palette.mutedText)
            }
            .padding(.horizontal, 13)
            .padding(.vertical, 7)
            .background(
                Capsule().fill(isActive ? activeColor : .white)
            )
            .overlay(
                Capsule().strokeBorder(
                    isActive ? Color.clear : Palette.activityCardBorder,
                    lineWidth: 1.5
                )
            )
        }
        .buttonStyle(.plain)
    }
}

private struct ActivityCardRow: View {
    let activity: ActivityTodayResponse
    let onCheckIn: (String, Int) -> Void
    let onOpenPreset: () -> Void

    private var progress: ActivityProgress {
        activity.toProgress()
    }

    private var kindEnum: ActivityKindLocal {
        switch activity.kind {
        case "BINARY": return .binary
        case "COUNTER": return .counter
        case "PRESET": return .preset
        default: return .binary
        }
    }

    private var ring: Color { ringColor(for: activity.ring) }

    private var isDone: Bool { progress.isDone }
    private var isPartial: Bool { !progress.isDone && progress.fraction > 0 }
    private var titleColor: Color {
        (isDone || isPartial) ? Palette.deepInk : Color(red: 0.353, green: 0.353, blue: 0.384)
    }
    private var borderColor: Color {
        isDone ? ring : Palette.activityCardBorder
    }

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            EmojiBadge(name: activity.name, ring: ring, isDone: isDone)

            VStack(alignment: .leading, spacing: 4) {
                Text(activity.name)
                    .font(.system(size: 14.5, weight: .bold))
                    .foregroundColor(titleColor)

                ActivitySubtitleView(
                    activity: activity,
                    kind: kindEnum,
                    isDone: isDone,
                    fraction: CGFloat(progress.fraction),
                    ring: ring
                )
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            ActionButtonView(
                activity: activity,
                kind: kindEnum,
                isDone: isDone,
                ring: ring,
                onCheckIn: onCheckIn,
                onOpenPreset: onOpenPreset
            )
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(RoundedRectangle(cornerRadius: 18).fill(.white))
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .strokeBorder(borderColor, lineWidth: 1.5)
        )
        // The done state gets a subtle outer halo via an additional shadow,
        // matching the prototype's `box-shadow: 0 0 0 3px ringColor/8%`.
        .shadow(
            color: isDone ? ring.opacity(0.08) : .clear,
            radius: 0,
            x: 0,
            y: 0
        )
        .contentShape(Rectangle())
        .onTapGesture {
            if kindEnum == .binary {
                toggleBinary(activity: activity, onCheckIn: onCheckIn)
            }
        }
    }
}

/// Swift mirror of the KMM `ActivityKind` enum, kept local so SwiftUI views
/// can switch on a regular Swift enum. Mapping is done in
/// `ActivityCardRow.kindEnum` from the raw string value of the response.
private enum ActivityKindLocal {
    case binary, counter, preset
}

private struct EmojiBadge: View {
    let name: String
    let ring: Color
    let isDone: Bool

    var body: some View {
        ZStack {
            Circle().fill(isDone ? ring : Palette.softCard)
            Text(String(name.prefix(1)).uppercased())
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(isDone ? .white : ring)
        }
        .frame(width: 42, height: 42)
    }
}

private struct ActivitySubtitleView: View {
    let activity: ActivityTodayResponse
    let kind: ActivityKindLocal
    let isDone: Bool
    let fraction: CGFloat
    let ring: Color

    var body: some View {
        switch kind {
        case .binary:
            Text(binaryText())
                .font(.system(size: 12.5, weight: .medium))
                .foregroundColor(isDone ? ring : Palette.mutedText)
        case .counter, .preset:
            VStack(alignment: .leading, spacing: 6) {
                Text(counterLabel())
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundColor(isDone ? ring : Palette.mutedText)
                ProgressBarView(progress: fraction, color: ring)
                    .frame(height: 4)
            }
        }
    }

    private func binaryText() -> String {
        if activity.inverse {
            return isDone ? "Сегодня без алкоголя ✓" : "Снято — сегодня выпил"
        }
        return isDone ? "Выполнено ✓" : "Тапни, чтобы отметить"
    }

    private func counterLabel() -> String {
        // Kotlin `Int?` is exposed as `KotlinInt?` in Swift, so we unbox via
        // `intValue` (matches the convention used elsewhere in the iOS app).
        let goalValue = activity.goal?.intValue ?? 0
        let unit = activity.unit ?? ""
        return "\(activity.logValue) / \(goalValue) \(unit)".trimmingCharacters(in: .whitespaces)
    }
}

private struct ProgressBarView: View {
    let progress: CGFloat
    let color: Color

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(Color(red: 0.933, green: 0.933, blue: 0.945))
                if progress > 0 {
                    Capsule()
                        .fill(color)
                        .frame(width: geo.size.width * min(max(progress, 0), 1))
                }
            }
        }
    }
}

private struct ActionButtonView: View {
    let activity: ActivityTodayResponse
    let kind: ActivityKindLocal
    let isDone: Bool
    let ring: Color
    let onCheckIn: (String, Int) -> Void
    let onOpenPreset: () -> Void

    var body: some View {
        switch kind {
        case .binary:
            Button(action: { toggleBinary(activity: activity, onCheckIn: onCheckIn) }) {
                ZStack {
                    Circle().fill(isDone ? ring : Color.white)
                        .overlay(
                            Circle().strokeBorder(
                                isDone ? Color.clear : Palette.activityCardBorder,
                                lineWidth: 1.5
                            )
                        )
                    if isDone {
                        Image(systemName: "checkmark")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(.white)
                    } else {
                        Circle()
                            .strokeBorder(Palette.mutedText, lineWidth: 1.5)
                            .frame(width: 14, height: 14)
                    }
                }
                .frame(width: 36, height: 36)
            }
            .buttonStyle(.plain)

        case .counter:
            // We render the button manually instead of using `Button` so we
            // can attach both a tap (increment) and a long-press (decrement)
            // gesture without one stealing the events from the other. SwiftUI's
            // `Button` consumes tap gestures internally and would suppress
            // either the tap or the long-press depending on order.
            ZStack {
                Circle().fill(isDone ? ring : Palette.deepInk)
                Image(systemName: "plus")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.white)
            }
            .frame(width: 36, height: 36)
            .contentShape(Circle())
            .onTapGesture {
                onCheckIn(activity.activityId, Int(activity.logValue) + 1)
            }
            .onLongPressGesture(minimumDuration: 0.4) {
                onCheckIn(activity.activityId, max(0, Int(activity.logValue) - 1))
            }

        case .preset:
            Button(action: onOpenPreset) {
                ZStack {
                    Circle().fill(isDone ? ring : Palette.deepInk)
                    Image(systemName: "plus")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.white)
                }
                .frame(width: 36, height: 36)
            }
            .buttonStyle(.plain)
        }
    }
}

/// Toggles a BINARY activity. See KMM `toggleBinary` for full semantics —
/// regular activities flip 0 ↔ 1, inverse activities flip the meaning so
/// `0` is treated as "done" by default.
private func toggleBinary(
    activity: ActivityTodayResponse,
    onCheckIn: (String, Int) -> Void
) {
    let logValue = Int(activity.logValue)
    let currentlyDone = activity.inverse ? (logValue == 0) : (logValue > 0)
    let next: Int
    if currentlyDone {
        next = activity.inverse ? 1 : 0
    } else {
        next = activity.inverse ? 0 : 1
    }
    onCheckIn(activity.activityId, next)
}

// MARK: - Preset bottom sheet

private struct PresetSheetView: View {
    let activity: ActivityTodayResponse
    let onPick: (String, Int) -> Void
    let onDismiss: () -> Void

    private var ring: Color { ringColor(for: activity.ring) }
    private var presets: [Int] {
        guard let raw = activity.presets else { return [] }
        // Kotlin `List<Int>` lands in Swift as `[KotlinInt]`; unbox each
        // entry via `intValue` so the SwiftUI grid sees a plain `[Int]`.
        return raw.map { $0.intValue }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 12) {
                ZStack {
                    Circle().fill(ring.opacity(0.10))
                    Text(String(activity.name.prefix(1)).uppercased())
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(ring)
                }
                .frame(width: 48, height: 48)

                VStack(alignment: .leading, spacing: 2) {
                    Text(activity.name)
                        .font(.system(size: 18, weight: .heavy))
                        .foregroundColor(Palette.deepInk)
                    Text("Сколько сегодня?")
                        .font(.system(size: 13))
                        .foregroundColor(Palette.mutedText)
                }
                Spacer()
            }
            .padding(.top, 8)

            Spacer().frame(height: 20)

            if presets.isEmpty {
                Text("Нет вариантов для быстрого выбора")
                    .font(.system(size: 14))
                    .foregroundColor(Palette.mutedText)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 24)
            } else {
                LazyVGrid(
                    columns: [
                        GridItem(.flexible(), spacing: 10),
                        GridItem(.flexible(), spacing: 10),
                    ],
                    spacing: 10
                ) {
                    ForEach(presets, id: \.self) { value in
                        Button(action: { onPick(activity.activityId, value) }) {
                            VStack(spacing: 2) {
                                Text("\(value)")
                                    .font(.system(size: 24, weight: .heavy))
                                    .foregroundColor(Palette.deepInk)
                                if let unit = activity.unit, !unit.isEmpty {
                                    Text(unit)
                                        .font(.system(size: 12))
                                        .foregroundColor(Palette.mutedText)
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 18)
                            .background(
                                RoundedRectangle(cornerRadius: 16).fill(ring.opacity(0.10))
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: 16)
                                    .strokeBorder(ring.opacity(0.20), lineWidth: 1.5)
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            Spacer().frame(height: 16)

            Button(action: onDismiss) {
                Text("Отмена")
                    .font(.system(size: 14, weight: .bold))
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
    }
}

// MARK: - Add activity button

struct AddActivityButton: View {
    let destination: AnyView

    var body: some View {
        NavigationLink {
            destination
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "plus")
                Text("Добавить активность")
            }
            .font(.system(size: 15, weight: .semibold))
            .foregroundColor(Palette.deepInk)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .strokeBorder(
                        Palette.deepInk.opacity(0.35),
                        style: StrokeStyle(lineWidth: 1.5, dash: [6, 4])
                    )
            )
        }
    }
}

// MARK: - AI tip

struct AiTipCard: View {
    // TODO: replace with AI-generated tip from backend.
    private let tip = "Ты на правильном пути! 3-й день подряд без пропусков. Ещё 4 дня — и новый рекорд твоего стрика. Не сдавайся 💪"

    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            ZStack {
                Circle().fill(Palette.accentOrange)
                Text("✨").font(.system(size: 18))
            }
            .frame(width: 40, height: 40)

            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 8) {
                    Text("AI-подсказка")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.white)
                    Text("PRO")
                        .font(.system(size: 10, weight: .heavy))
                        .foregroundColor(Palette.deepInk)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Capsule().fill(Palette.accentOrange))
                }
                Text(tip)
                    .font(.system(size: 14))
                    .foregroundColor(.white.opacity(0.85))
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: 0)
        }
        .padding(20)
        .background(RoundedRectangle(cornerRadius: 24).fill(Palette.deepInk))
    }
}

// MARK: - Courses block

/// Vertical block of trainer courses for the Home screen:
///  - one full-width featured card,
///  - a 2-column grid of small cards,
///  - a "see all" outline button at the bottom.
///
/// Data is hardcoded for now until the backend exposes a courses endpoint.
private struct CoursesBlock: View {
    private let featured = FeaturedCourseModel(
        title: "Утреннее пробуждение",
        tag: "популярное",
        category: "Йога",
        duration: "25 мин",
        author: "Мария Л.",
        rating: "4.9",
        gradientStart: Color(red: 1.0, green: 0.529, blue: 0.455), // #FF8674
        gradientEnd: Color(red: 1.0, green: 0.353, blue: 0.235)    // #FF5A3C
    )

    private let smallCourses: [SmallCourseModel] = [
        SmallCourseModel(
            title: "Кардио Хит",
            author: "Елена В.",
            duration: "20 мин",
            rating: "5.0",
            badge: "Интенсив",
            gradientStart: Color(red: 1.0, green: 0.667, blue: 0.541),   // #FFAA8A
            gradientEnd: Color(red: 0.914, green: 0.290, blue: 0.173)    // #E94A2C
        ),
        SmallCourseModel(
            title: "Сила и мощь",
            author: "Борис Л.",
            duration: "20 мин",
            rating: "5.0",
            badge: "Интенсив",
            gradientStart: Color(red: 0.239, green: 0.239, blue: 0.271), // #3D3D45
            gradientEnd: Color(red: 0.039, green: 0.039, blue: 0.039)    // #0A0A0A
        ),
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            FeaturedCourseCard(course: featured)

            HStack(alignment: .top, spacing: 12) {
                ForEach(smallCourses) { course in
                    SmallCourseCard(course: course)
                        .frame(maxWidth: .infinity)
                }
            }

            SeeAllCoursesButton(action: {
                // TODO: hook up courses screen once backend exposes the list.
            })
        }
    }
}

private struct FeaturedCourseModel {
    let title: String
    let tag: String
    let category: String
    let duration: String
    let author: String
    let rating: String
    let gradientStart: Color
    let gradientEnd: Color
}

private struct SmallCourseModel: Identifiable {
    let id = UUID()
    let title: String
    let author: String
    let duration: String
    let rating: String
    let badge: String
    let gradientStart: Color
    let gradientEnd: Color
}

private let courseCardBorder = Color(red: 0.929, green: 0.929, blue: 0.937) // #EDEDEF
private let starYellow = Color(red: 0.831, green: 0.627, blue: 0.090)
private let ratingPillBg = Color(red: 1.0, green: 0.965, blue: 0.839)       // #FFF6D6
private let smallCourseBadgeBg = Color(red: 0.882, green: 0.945, blue: 1.0) // #E1F1FF
private let smallCourseBadgeFg = Color(red: 0.039, green: 0.518, blue: 1.0) // #0A84FF

private struct FeaturedCourseCard: View {
    let course: FeaturedCourseModel

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack(alignment: .topLeading) {
                LinearGradient(
                    gradient: Gradient(colors: [course.gradientStart, course.gradientEnd]),
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .frame(height: 170)

                // Title + category overlay — bottom-left
                VStack(alignment: .leading, spacing: 2) {
                    Spacer()
                    Text(course.title)
                        .font(.system(size: 18, weight: .heavy))
                        .foregroundColor(.white)
                        .lineLimit(2)
                    Text("\(course.category) · \(course.duration)")
                        .font(.system(size: 12))
                        .foregroundColor(.white.opacity(0.9))
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .frame(height: 170, alignment: .bottomLeading)

                // "популярное" frosted badge — top-left
                Text(course.tag)
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(.black)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(
                        RoundedRectangle(cornerRadius: 14)
                            .fill(Color.white.opacity(0.85))
                    )
                    .padding(12)
            }
            .frame(height: 170)
            .clipped()

            // Author row
            HStack(spacing: 8) {
                Circle()
                    .fill(
                        LinearGradient(
                            gradient: Gradient(colors: [
                                Color(red: 1.0, green: 0.667, blue: 0.541),
                                Color(red: 1.0, green: 0.494, blue: 0.373),
                            ]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 28, height: 28)

                Text(course.author)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)

                RatingPill(rating: course.rating)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
        }
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .strokeBorder(courseCardBorder, lineWidth: 1.5)
        )
    }
}

private struct RatingPill: View {
    let rating: String

    var body: some View {
        HStack(spacing: 3) {
            Image(systemName: "star.fill")
                .font(.system(size: 11))
                .foregroundColor(starYellow)
            Text(rating)
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(Palette.deepInk)
        }
        .padding(.horizontal, 9)
        .padding(.vertical, 4)
        .background(RoundedRectangle(cornerRadius: 10).fill(ratingPillBg))
    }
}

private struct SmallCourseCard: View {
    let course: SmallCourseModel

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack(alignment: .bottomTrailing) {
                LinearGradient(
                    gradient: Gradient(colors: [course.gradientStart, course.gradientEnd]),
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .frame(height: 110)

                Text(course.duration)
                    .font(.system(size: 10.5, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(
                        RoundedRectangle(cornerRadius: 10)
                            .fill(Color.black.opacity(0.6))
                    )
                    .padding(8)
            }
            .frame(height: 110)
            .clipped()

            VStack(alignment: .leading, spacing: 4) {
                Text(course.title)
                    .font(.system(size: 14, weight: .heavy))
                    .foregroundColor(Palette.deepInk)
                    .lineLimit(1)

                Text(course.author)
                    .font(.system(size: 11.5))
                    .foregroundColor(Palette.mutedText)
                    .lineLimit(1)

                HStack(spacing: 4) {
                    Text(course.badge)
                        .font(.system(size: 10.5, weight: .bold))
                        .foregroundColor(smallCourseBadgeFg)
                        .padding(.horizontal, 7)
                        .padding(.vertical, 3)
                        .background(
                            RoundedRectangle(cornerRadius: 8).fill(smallCourseBadgeBg)
                        )

                    Spacer()

                    HStack(spacing: 3) {
                        Image(systemName: "star.fill")
                            .font(.system(size: 10))
                            .foregroundColor(starYellow)
                        Text(course.rating)
                            .font(.system(size: 11.5, weight: .bold))
                            .foregroundColor(Palette.deepInk)
                    }
                }
                .padding(.top, 4)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
        }
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .strokeBorder(courseCardBorder, lineWidth: 1.5)
        )
    }
}

private struct SeeAllCoursesButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text("Посмотреть все курсы")
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(Palette.deepInk)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 13)
                .background(
                    RoundedRectangle(cornerRadius: 14)
                        .strokeBorder(courseCardBorder, lineWidth: 1.5)
                )
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Meal plan section

private let mealCardBorder = Color(red: 0.929, green: 0.929, blue: 0.937) // #EDEDEF

/// Identifiable wrapper around the planId String — `fullScreenCover(item:)`
/// requires an `Identifiable` payload, so we lift the raw id into a tiny
/// holder rather than coloring the public KMM `String` with a conformance.
struct MealPlanIdHolder: Identifiable {
    let value: String
    var id: String { value }
}

/// Home meal-plan section.
///
/// Two rendering branches driven entirely by [todayPlans]:
///  - empty list → empty-state CTA that opens the manual creation flow,
///  - non-empty   → one row per (meal × plan) card; tap raises [onPlanTap]
///    so the parent can present the detail screen.
///
/// The section is intentionally state-free; the real source of truth is the
/// shared `HomeViewModel`. Everything beyond layout flows top-down from the
/// parent.
struct MealPlanSection: View {
    let todayPlans: [TodayMealPlanCard]
    let onPlanTap: (String) -> Void
    let onCreatePlan: () -> Void

    private var totalKcal: Int {
        // `estimatedCalories` is `KotlinInt?` in Swift (Kotlin `Int?`); unbox
        // via `intValue` (returns `Int32`) and widen to `Int` so the
        // accumulator stays in the platform-native integer type. Null
        // estimates count as zero so a card without one does not break the
        // running sum.
        todayPlans.reduce(0) { acc, card in
            acc + Int(card.estimatedCalories?.intValue ?? 0)
        }
    }

    private var subtitle: String {
        if todayPlans.isEmpty {
            return "Сегодня нет запланированных приёмов пищи"
        }
        return "\(todayPlans.count) \(Self.pluralizeMeals(todayPlans.count)) · \(totalKcal) ккал"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text("План питания")
                    .font(.system(size: 20, weight: .heavy))
                    .foregroundColor(Palette.deepInk)
                Text(subtitle)
                    .font(.system(size: 13))
                    .foregroundColor(Palette.mutedText)
                    .lineLimit(1)
            }

            if todayPlans.isEmpty {
                EmptyMealPlanCard(onCreate: onCreatePlan)
            } else {
                VStack(spacing: 10) {
                    ForEach(todayPlans, id: \.planId) { card in
                        TodayMealPlanRow(
                            card: card,
                            onTap: { onPlanTap(card.planId) }
                        )
                    }
                }
            }
        }
    }

    /// Russian noun pluralization for "приём пищи". Public-static so the
    /// section header and the list summary stay consistent.
    private static func pluralizeMeals(_ count: Int) -> String {
        let mod10 = count % 10
        let mod100 = count % 100
        if mod10 == 1 && mod100 != 11 { return "приём пищи" }
        if (2...4).contains(mod10) && !(12...14).contains(mod100) { return "приёма пищи" }
        return "приёмов пищи"
    }
}

private struct EmptyMealPlanCard: View {
    let onCreate: () -> Void

    var body: some View {
        VStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 16).fill(Color(red: 0.957, green: 0.957, blue: 0.965))
                Text("🥗").font(.system(size: 28))
            }
            .frame(width: 56, height: 56)

            Text("Нет плана питания на сегодня")
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(Palette.deepInk)

            Text("Создайте рацион — он появится в нужный день недели или дату")
                .font(.system(size: 13))
                .foregroundColor(Palette.mutedText)
                .multilineTextAlignment(.center)

            Button(action: onCreate) {
                Text("Создать план")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 10)
                    .background(RoundedRectangle(cornerRadius: 12).fill(Palette.coral))
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 24)
        .padding(.horizontal, 20)
        .background(RoundedRectangle(cornerRadius: 24).fill(.white))
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .strokeBorder(style: StrokeStyle(lineWidth: 1.5, dash: [6, 4]))
                .foregroundColor(Color(red: 0.878, green: 0.878, blue: 0.898))
        )
    }
}

/// Single home-meal row backed by a [TodayMealPlanCard].
///
/// Renders meal-type emoji + display name, the parent plan's name, a short
/// dish summary, and the meal's estimated calorie count when available.
/// Tap surface is the entire row; the parent handles navigation to the
/// per-plan detail view.
private struct TodayMealPlanRow: View {
    let card: TodayMealPlanCard
    let onTap: () -> Void

    private var palette: MealPalette { MealPalette.forType(card.mealType) }
    private var displayName: String { MealPalette.displayName(card.mealType) }

    private var dishSummary: String {
        let dishes = card.dishes
        if dishes.isEmpty {
            return card.mealName.isEmpty ? "Без описания" : card.mealName
        }
        return dishes.prefix(3)
            .map { $0.name }
            .joined(separator: " · ")
    }

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12).fill(palette.iconBg)
                    Text(palette.emoji).font(.system(size: 18))
                }
                .frame(width: 42, height: 42)

                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 8) {
                        Text(displayName)
                            .font(.system(size: 14.5, weight: .bold))
                            .foregroundColor(Palette.deepInk)
                        Text(card.planName)
                            .font(.system(size: 11.5))
                            .foregroundColor(Palette.mutedText)
                            .lineLimit(1)
                    }
                    Text(dishSummary)
                        .font(.system(size: 12))
                        .foregroundColor(Palette.mutedText)
                        .lineLimit(1)
                }

                Spacer(minLength: 8)

                if let kcal = card.estimatedCalories {
                    VStack(alignment: .trailing, spacing: 0) {
                        Text("\(kcal.intValue)")
                            .font(.system(size: 14, weight: .heavy))
                            .foregroundColor(Palette.deepInk)
                        Text("ККАЛ")
                            .font(.system(size: 10, weight: .semibold))
                            .foregroundColor(Palette.mutedText)
                    }
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(RoundedRectangle(cornerRadius: 18).fill(.white))
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .strokeBorder(mealCardBorder, lineWidth: 1.5)
            )
        }
        .buttonStyle(.plain)
    }
}

/// Static palette + label resolution for a meal type. Kept in the view layer
/// because palettes / Russian display names are pure presentation concerns —
/// the KMM domain model only carries the wire string.
private struct MealPalette {
    let iconBg: Color
    let iconFg: Color
    let emoji: String

    static func forType(_ wireValue: String) -> MealPalette {
        switch wireValue.uppercased() {
        case "BREAKFAST":
            return MealPalette(
                iconBg: Color(red: 1.0, green: 0.965, blue: 0.839),
                iconFg: Color(red: 0.831, green: 0.627, blue: 0.090),
                emoji: "☀️"
            )
        case "LUNCH":
            return MealPalette(
                iconBg: Color(red: 1.0, green: 0.933, blue: 0.867),
                iconFg: Color(red: 0.878, green: 0.482, blue: 0.0),
                emoji: "🥗"
            )
        case "DINNER":
            return MealPalette(
                iconBg: Color(red: 0.902, green: 0.914, blue: 1.0),
                iconFg: Color(red: 0.231, green: 0.357, blue: 0.859),
                emoji: "🌙"
            )
        case "SNACK":
            return MealPalette(
                iconBg: Color(red: 0.910, green: 0.969, blue: 0.910),
                iconFg: Color(red: 0.184, green: 0.620, blue: 0.267),
                emoji: "🍎"
            )
        default:
            return MealPalette(
                iconBg: Color(red: 0.953, green: 0.949, blue: 0.937),
                iconFg: Color(red: 0.431, green: 0.431, blue: 0.463),
                emoji: "🍽️"
            )
        }
    }

    static func displayName(_ wireValue: String) -> String {
        switch wireValue.uppercased() {
        case "BREAKFAST": return "Завтрак"
        case "LUNCH": return "Обед"
        case "DINNER": return "Ужин"
        case "SNACK": return "Перекус"
        default: return wireValue
        }
    }
}

// MARK: - Legacy mock detail view (retired)
//
// The previous file shipped a hardcoded `MealDetailView` + supporting mock
// models (`MealRow`, `MealDetailModel`, `DishItem`, `MacroItem`, `lunchModel`,
// `DishRow`, `MacroCard`). They were the placeholder used while the real
// meal-plan API was not exposed; the new home wiring opens the real
// `MealPlanDetailView(planId:)` instead, so all of that mock surface has been
// removed in this file. Search history (`git log -- HomeView.swift`) for the
// old implementation if a reference is needed.

