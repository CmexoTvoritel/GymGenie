import SwiftUI
import Shared

struct HomeView: View {
    @EnvironmentObject private var appState: AppState
    @EnvironmentObject private var profileStore: UserProfileStoreWrapper
    @EnvironmentObject private var tabBarState: TabBarState
    @StateObject private var viewModel = HomeViewModelWrapper()

    @State private var showingSession = false
    @State private var activeSession: ActiveWorkoutSession? = nil

    @State private var openMealPlanId: String? = nil
    @State private var mealPlanDetailIsPast: Bool = false
    @State private var showingAiCoach = false
    @State private var showingCreateMealPlan = false
    @State private var createMealPlanInitialType: String? = nil
    @State private var createMealPlanInitialDate: String? = nil
    @State private var editMealPlanId: String? = nil
    @State private var showingNotifications = false
    @State private var selectedPlanId: String? = nil
    @State private var showCreateWorkout = false
    @State private var scheduleTarget: ActivityTodayResponse? = nil
    @State private var snackbarMessage: String? = nil
    @State private var showingActivities = false
    @State private var showingActivityCatalog = false
    @StateObject private var activitiesViewModel = ActivitiesViewModelWrapper()

    var body: some View {
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
                    .onChange(of: viewModel.pendingSession?.planId) { _ in
                        if let session = viewModel.pendingSession {
                            activeSession = session
                            showingSession = true
                            viewModel.clearPendingSession()
                        }
                    }
                    .alert(
                        "Ошибка",
                        isPresented: Binding(
                            get: { viewModel.sessionError != nil },
                            set: { if !$0 { viewModel.clearPendingSession() } }
                        )
                    ) {
                        Button("OK") { viewModel.clearPendingSession() }
                    } message: {
                        Text(viewModel.sessionError ?? "Не удалось загрузить тренировку")
                    }
                    .alert(
                        "Ошибка",
                        isPresented: Binding(
                            get: { viewModel.activityError != nil },
                            set: { if !$0 { viewModel.clearActivityError() } }
                        )
                    ) {
                        Button("OK") { viewModel.clearActivityError() }
                    } message: {
                        Text(viewModel.activityError ?? "Не удалось сохранить активность")
                    }
            }

            if viewModel.isLoadingSession {
                Color.black.opacity(0.25).ignoresSafeArea()
                ProgressView()
                    .scaleEffect(1.4)
                    .tint(.white)
            }

            if let message = snackbarMessage {
                VStack {
                    Spacer()
                    Text(message)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(Color.black.opacity(0.82))
                        )
                        .padding(.horizontal, 20)
                        .padding(.bottom, 100)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
                .animation(.easeInOut(duration: 0.25), value: snackbarMessage)
                .onAppear {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                        withAnimation { snackbarMessage = nil }
                    }
                }
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .navigationDestination(isPresented: Binding(
            get: { openMealPlanId != nil },
            set: { if !$0 { dismissMealPlanDetail() } }
        )) {
            if let id = openMealPlanId {
                MealPlanDetailView(
                    planId: id,
                    onClose: { dismissMealPlanDetail() },
                    onDeleted: { dismissMealPlanDetail() },
                    onEdit: {
                        editMealPlanId = id
                        openMealPlanId = nil
                        showingCreateMealPlan = true
                    },
                    isPastDate: mealPlanDetailIsPast
                )
                .toolbar(.hidden, for: .navigationBar)
            }
        }
        .navigationDestination(isPresented: Binding(
            get: { selectedPlanId != nil },
            set: { if !$0 { dismissPlanDetail() } }
        )) {
            if let id = selectedPlanId {
                WorkoutDetailView(
                    planId: id,
                    onBack: { dismissPlanDetail() },
                    onStartPlan: { planId, planName in
                        dismissPlanDetail()
                        viewModel.startWorkout(planId: planId, planName: planName)
                    }
                )
                .toolbar(.hidden, for: .navigationBar)
            }
        }
        .fullScreenCover(isPresented: $showingSession) {
            if let session = activeSession {
                WorkoutSessionView(session: session)
            }
        }
        .fullScreenCover(isPresented: $showingAiCoach) {
            AiCoachView()
                .environmentObject(profileStore)
        }
        .fullScreenCover(isPresented: $showCreateWorkout) {
            CreateWorkoutFlowView(onDismiss: {
                showCreateWorkout = false
                viewModel.refresh()
            })
        }
        .sheet(item: $scheduleTarget) { activity in
            ActivityScheduleSettingsView(
                activityId: activity.activityId,
                activityName: activity.name,
                initialScheduleType: activity.scheduleType,
                initialScheduleDays: activity.scheduleDays as [String],
                initialOneOffDate: activity.oneOffDate,
                viewModel: activitiesViewModel,
                onDismiss: {
                    scheduleTarget = nil
                    viewModel.refresh()
                }
            )
        }
        .navigationDestination(isPresented: $showingCreateMealPlan) {
            CreateMealPlanFlowView(
                initialMealType: createMealPlanInitialType,
                initialDate: createMealPlanInitialDate,
                editPlanId: editMealPlanId,
                onClose: {
                    showingCreateMealPlan = false
                    editMealPlanId = nil
                },
                onSaved: {
                    showingCreateMealPlan = false
                    editMealPlanId = nil
                    viewModel.refreshMealPlans()
                }
            )
            .toolbar(.hidden, for: .navigationBar)
        }
        .navigationDestination(isPresented: $showingNotifications) {
            NotificationsView(onClose: { showingNotifications = false })
                .toolbar(.hidden, for: .navigationBar)
        }
        .navigationDestination(isPresented: $showingActivities) {
            ActivitiesView()
                .toolbar(.hidden, for: .navigationBar)
        }
        .navigationDestination(isPresented: $showingActivityCatalog) {
            ActivityCatalogView()
                .toolbar(.hidden, for: .navigationBar)
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
        .onChange(of: openMealPlanId) { id in
            tabBarState.isVisible = (id == nil && selectedPlanId == nil)
        }
        .onChange(of: selectedPlanId) { id in
            tabBarState.isVisible = (id == nil && openMealPlanId == nil)
        }
        .onChange(of: showingCreateMealPlan) { showing in
            if !showing {
                tabBarState.isVisible = true
            } else {
                tabBarState.isVisible = false
            }
        }
        .onChange(of: showingNotifications) { showing in
            tabBarState.isVisible = !showing
        }
        .onChange(of: showingActivities) { showing in
            if showing {
                tabBarState.isVisible = false
            } else {
                tabBarState.isVisible = true
                viewModel.refresh()
            }
        }
        .onChange(of: showingActivityCatalog) { showing in
            if showing {
                tabBarState.isVisible = false
            } else {
                tabBarState.isVisible = true
                viewModel.refresh()
            }
        }
    }

    private func dismissPlanDetail() {
        selectedPlanId = nil
        viewModel.refresh()
    }

    private func dismissMealPlanDetail() {
        openMealPlanId = nil
        viewModel.refreshMealPlans()
    }

    private func refreshAndWait() async {
        viewModel.refresh()
        let pollInterval: UInt64 = 50_000_000
        let startWaitMaxNanos: UInt64 = 500_000_000
        var startWaited: UInt64 = 0
        while !viewModel.isRefreshing && startWaited < startWaitMaxNanos {
            try? await Task.sleep(nanoseconds: pollInterval)
            startWaited += pollInterval
        }
        let maxWaitNanos: UInt64 = 15_000_000_000
        var elapsed: UInt64 = 0
        while viewModel.isRefreshing && elapsed < maxWaitNanos {
            try? await Task.sleep(nanoseconds: pollInterval)
            elapsed += pollInterval
        }
    }

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

    private func buildHomeDisplayName() -> String {
        guard let p = profileStore.profile else { return "друг" }
        if let first = p.firstName, !first.isEmpty { return first }
        return "друг"
    }

    private var contentView: some View {
        let slot = HomeView.resolveTodaySlot(plans: viewModel.activeWorkoutPlans, today: HomeView.currentDayOfWeek())

        return ScrollView(showsIndicators: false) {
            LazyVStack(alignment: .leading, spacing: 12) {
                HomeHeaderSection(
                    username: buildHomeDisplayName(),
                    onNotificationsClick: { showingNotifications = true },
                    onProfileClick: { tabBarState.switchTo(.profile) }
                )

                ActivitiesSectionHeader(
                    title: slot.title,
                    subtitle: slot.subtitle,
                    actionTitle: slot.showAll ? "Все" : nil,
                    destination: nil,
                    onAction: slot.showAll ? { tabBarState.switchTo(.workouts) } : nil
                )
                .padding(.top, 24)

                workoutSlotView(slot: slot)

                ActivitiesSectionHeader(
                    title: "Активности",
                    subtitle: "Отметить вручную — без умных часов",
                    actionTitle: "Ещё",
                    destination: nil,
                    onAction: { showingActivities = true }
                )
                .padding(.top, 12)

                ActivityRingsCard(activities: viewModel.todayActivities)

                ActivityRowsCard(
                    activities: viewModel.todayActivities,
                    onCheckIn: { id, value in
                        viewModel.checkIn(activityId: id, value: value)
                    },
                    onOpenScheduleSettings: { activity in
                        scheduleTarget = activity
                    },
                    onRemoveFromPlan: { activityId in
                        viewModel.removeFromPlan(activityId: activityId)
                    }
                )

                Button(action: { showingActivityCatalog = true }) {
                    HStack(spacing: 8) {
                        Image(systemName: "plus")
                        Text("Добавить активность")
                    }
                    .font(.system(size: 18, weight: .semibold))
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
                .buttonStyle(.plain)

                MealPlanSection(
                    todayPlans: viewModel.todayMealPlans,
                    isLoading: viewModel.isLoadingMealPlans,
                    selectedDate: viewModel.selectedMealDate,
                    onDateSelected: { date in viewModel.selectMealDate(date) },
                    onPlanTap: { planId in
                        let calendar = Calendar.current
                        let today = calendar.startOfDay(for: Date())
                        let target = calendar.startOfDay(for: viewModel.selectedMealDate)
                        mealPlanDetailIsPast = target < today
                        openMealPlanId = planId
                    },
                    onCreatePlan: { mealType, date in
                        createMealPlanInitialType = mealType
                        createMealPlanInitialDate = date
                        showingCreateMealPlan = true
                    },
                    onPastDateBlocked: {
                        withAnimation { snackbarMessage = "Создание на вчерашний день недоступно" }
                    },
                    isPremium: viewModel.subscriptionType != "FREE",
                    onOpenPaywall: { appState.navigate(to: .paywall) }
                )
                .padding(.top, 12)

                coursesSection
                    .padding(.top, 12)

                Spacer().frame(height: 24)
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)
        }
    }

    @ViewBuilder
    private func workoutSlotView(slot: TodaySlot) -> some View {
        if slot.plans.isEmpty {
            NoWorkoutPlaceholder(onCreate: { showCreateWorkout = true })
        } else {
            WorkoutTodayPager(
                plans: slot.plans,
                isRecommended: slot.isRecommended,
                onView: { plan in selectedPlanId = plan.id },
                onStart: { plan in
                    viewModel.startWorkout(planId: plan.id, planName: plan.name)
                }
            )
        }
    }

    static func currentDayOfWeek() -> String {
        calendarWeekdayToBackend(Calendar.current.component(.weekday, from: Date()))
    }

    static func resolveTodaySlot(plans: [WorkoutPlanShortResponse], today: String) -> TodaySlot {
        let upper = today.uppercased()
        let recurringToday = plans.filter { plan in
            plan.scheduleType.uppercased() == "RECURRING" &&
                plan.scheduleDays.contains(where: { $0.uppercased() == upper })
        }
        let oneOff = plans.filter { $0.scheduleType.uppercased() == "ONE_TIME" }
        let dayRu = dayNameRu(upper)

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

    private var coursesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .center, spacing: 16) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Курсы тренеров")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(Palette.deepInk)
                    Text("От профи под твои цели")
                        .font(.system(size: 16, weight: .regular))
                        .foregroundColor(Palette.mutedText)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Text("Все")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundColor(Palette.accentOrange)
            }

            CoursesBlock()
        }
    }
}

private struct HomeHeaderSection: View {
    let username: String
    var onNotificationsClick: () -> Void = {}
    var onProfileClick: () -> Void = {}

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            Button(action: onProfileClick) {
                HStack(alignment: .center, spacing: 12) {
                    ZStack {
                        Circle().fill(Palette.coral)
                        Text(String(username.prefix(1)).uppercased())
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.white)
                    }
                    .frame(width: 48, height: 48)

                    VStack(alignment: .leading, spacing: 4) {
                        Text(Self.currentDateString())
                            .font(.system(size: 16, weight: .regular))
                            .foregroundColor(Palette.mutedText)
                        Text("Привет, \(username)!")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(Palette.deepInk)
                            .lineLimit(1)
                    }
                }
            }
            .buttonStyle(.plain)

            Spacer()

            Button(action: onNotificationsClick) {
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

struct TodaySlot {
    let title: String
    let subtitle: String
    let showAll: Bool
    let plans: [WorkoutPlanShortResponse]
    let isRecommended: Bool
}

private let cardBorder = Color(red: 0.929, green: 0.929, blue: 0.937)

private struct WorkoutTodayPager: View {
    let plans: [WorkoutPlanShortResponse]
    let isRecommended: Bool
    let onView: (WorkoutPlanShortResponse) -> Void
    let onStart: (WorkoutPlanShortResponse) -> Void

    @State private var currentIndex = 0

    var body: some View {
        VStack(spacing: 10) {
            if plans.count == 1 {

                WorkoutPlanCard(
                    plan: plans[0],
                    onView: { onView(plans[0]) },
                    onStart: { onStart(plans[0]) }
                )
            } else {
                TabView(selection: $currentIndex) {
                    ForEach(Array(plans.enumerated()), id: \.element.id) { index, plan in
                        WorkoutPlanCard(
                            plan: plan,
                            onView: { onView(plan) },
                            onStart: { onStart(plan) }
                        )
                        .tag(index)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .frame(height: 270)

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
}

private struct NoWorkoutPlaceholder: View {
    let onCreate: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Image("ic_weekend_day")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 100, height: 100)

            Text("Сегодня выходной")
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(Palette.deepInk)

            Text("На этот день тренировки не запланированы")
                .font(.system(size: 16))
                .foregroundColor(Palette.mutedText)
                .multilineTextAlignment(.center)

            Button(action: onCreate) {
                Text("Создать тренировку")
                    .font(.system(size: 18, weight: .bold))
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

struct ActivitiesSectionHeader: View {
    let title: String
    let subtitle: String?
    let actionTitle: String?
    let destination: AnyView?
    var onAction: (() -> Void)? = nil

    var body: some View {
        HStack(alignment: .center, spacing: 16) {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
                if let subtitle = subtitle {
                    Text(subtitle)
                        .font(.system(size: 16, weight: .regular))
                        .foregroundColor(Palette.mutedText)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if let actionTitle = actionTitle {
                if let destination = destination {
                    NavigationLink {
                        destination
                    } label: {
                        Text(actionTitle)
                            .font(.system(size: 18, weight: .medium))
                            .foregroundColor(Palette.accentOrange)
                    }
                } else {
                    Button(action: { onAction?() }) {
                        Text(actionTitle)
                            .font(.system(size: 18, weight: .medium))
                            .foregroundColor(Palette.accentOrange)
                    }
                }
            }
        }
    }
}

#if compiler(>=6.0)
extension Shared.ActivityTodayResponse: @retroactive Identifiable {
    public var id: String { activityId }
}
#else
extension ActivityTodayResponse: Identifiable {
    public var id: String { activityId }
}
#endif

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
        HStack(alignment: .center, spacing: 12) {
            Circle().fill(color).frame(width: 24, height: 24)
            VStack(alignment: .leading, spacing: 8) {
                Text(title)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(Palette.mutedText)
                    .tracking(0.6)
                Text(value)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
            }
        }
    }
}

struct ActivityRowsCard: View {
    let activities: [ActivityTodayResponse]
    let onCheckIn: (String, Int) -> Void
    var onOpenScheduleSettings: ((ActivityTodayResponse) -> Void)? = nil
    var onRemoveFromPlan: ((String) -> Void)? = nil

    @State private var activeFilter: String = ActivityFilter.all
    @State private var presetTarget: ActivityTodayResponse? = nil
    @State private var deleteTarget: ActivityTodayResponse? = nil

    private var visible: [ActivityTodayResponse] {
        if activeFilter == ActivityFilter.all { return activities }
        return activities.filter { $0.ring == activeFilter }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ActivityFilterChips(
                active: activeFilter,
                onSelect: { activeFilter = $0 }
            )

            VStack(spacing: 8) {
                ForEach(visible, id: \.activityId) { activity in
                    ActivityCardRow(
                        activity: activity,
                        onCheckIn: onCheckIn,
                        onOpenPreset: { presetTarget = activity },
                        onOpenScheduleSettings: onOpenScheduleSettings != nil
                            ? { onOpenScheduleSettings?(activity) }
                            : nil
                    )
                    .onLongPressGesture(minimumDuration: 0.5) {
                        deleteTarget = activity
                    }
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
        .sheet(item: $deleteTarget) { target in
            VStack(alignment: .leading, spacing: 16) {
                Text(target.name)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Palette.deepInk)

                Button(action: {
                    onRemoveFromPlan?(target.activityId)
                    deleteTarget = nil
                }) {
                    HStack(spacing: 12) {
                        Image(systemName: "trash")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 22, height: 22)
                        Text("Удалить из плана")
                            .font(.system(size: 16, weight: .medium))
                    }
                    .foregroundColor(Color(red: 0.851, green: 0.267, blue: 0.267))
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 14)
                    .background(
                        RoundedRectangle(cornerRadius: 14)
                            .fill(Color(red: 1.0, green: 0.941, blue: 0.933))
                    )
                }
                .buttonStyle(.plain)
            }
            .padding(.top, 12)
            .padding(.horizontal, 20)
            .padding(.bottom, 24)
            .presentationDetents([.height(170)])
            .presentationBackground(Color.white)
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

private func formatScheduleLabel(activity: ActivityTodayResponse) -> String? {
    guard let scheduleType = activity.scheduleType as String? else { return nil }
    switch scheduleType {
    case "RECURRING":
        let days = activity.scheduleDays as [String]
        if days.isEmpty { return nil }
        let sorted = days.sorted { (weekdayOrder.firstIndex(of: $0) ?? 99) < (weekdayOrder.firstIndex(of: $1) ?? 99) }
        return sorted.compactMap { backendDayToShort[$0] }.joined(separator: " ")
    case "ONE_TIME":
        guard let raw = activity.oneOffDate as String? else { return nil }
        let parser = DateFormatter()
        parser.calendar = Calendar(identifier: .iso8601)
        parser.dateFormat = "yyyy-MM-dd"
        parser.locale = Locale(identifier: "en_US_POSIX")
        guard let date = parser.date(from: raw) else { return raw }
        let display = DateFormatter()
        display.locale = Locale(identifier: "ru_RU")
        display.dateFormat = "d MMM"
        return display.string(from: date)
    default:
        return nil
    }
}

private struct ActivityFilterChips: View {
    let active: String
    let onSelect: (String) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                FilterChip(
                    label: "Все",
                    isActive: active == ActivityFilter.all,
                    activeColor: Palette.deepInk,
                    onTap: { onSelect(ActivityFilter.all) }
                )
                FilterChip(
                    label: "Движение",
                    isActive: active == "MOVE",
                    activeColor: Palette.ringMove,
                    onTap: { onSelect("MOVE") }
                )
                FilterChip(
                    label: "Разум",
                    isActive: active == "MIND",
                    activeColor: Palette.ringMind,
                    onTap: { onSelect("MIND") }
                )
                FilterChip(
                    label: "Режим",
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
    let isActive: Bool
    let activeColor: Color
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(isActive ? .white : Palette.deepInk)
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
    var onOpenScheduleSettings: (() -> Void)? = nil

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

    private var scheduleLabel: String? {
        formatScheduleLabel(activity: activity)
    }

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            EmojiBadge(ringName: activity.ring, ring: ring, isDone: isDone)

            VStack(alignment: .leading, spacing: 4) {
                Text(activity.name)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(titleColor)

                ActivitySubtitleView(
                    activity: activity,
                    kind: kindEnum,
                    isDone: isDone,
                    fraction: CGFloat(progress.fraction),
                    ring: ring
                )

                if let label = scheduleLabel {
                    Text(label)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(Palette.mutedText.opacity(0.7))
                        .onTapGesture { onOpenScheduleSettings?() }
                }
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

private enum ActivityKindLocal {
    case binary, counter, preset
}

private struct EmojiBadge: View {
    let ringName: String
    let ring: Color
    let isDone: Bool

    private var iconName: String {
        switch ringName {
        case "MOVE": return "ic_activity_run"
        case "MIND": return "ic_activity_mind"
        default: return "ic_activity_schedule"
        }
    }

    var body: some View {
        ZStack {
            Circle().fill(isDone ? ring : Palette.softCard)
            Image(iconName)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 22, height: 22)
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
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(isDone ? ring : Palette.mutedText)
        case .counter, .preset:
            VStack(alignment: .leading, spacing: 6) {
                Text(counterLabel())
                    .font(.system(size: 16, weight: .medium))
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
            let atMin = activity.logValue <= 0
            let goalValue = activity.goal?.intValue ?? Int(Int32.max)
            let atMax = Int(activity.logValue) >= goalValue
            HStack(spacing: 8) {
                Button(action: {
                    onCheckIn(activity.activityId, max(0, Int(activity.logValue) - 1))
                }) {
                    ZStack {
                        Circle().fill(atMin ? Palette.softCard : Palette.deepInk)
                        Image(systemName: "minus")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(atMin ? Palette.mutedText : .white)
                    }
                    .frame(width: 36, height: 36)
                }
                .buttonStyle(.plain)
                .disabled(atMin)

                Button(action: {
                    onCheckIn(activity.activityId, min(goalValue, Int(activity.logValue) + 1))
                }) {
                    ZStack {
                        Circle().fill(atMax ? Palette.softCard : (isDone ? ring : Palette.deepInk))
                        Image(systemName: "plus")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(atMax ? Palette.mutedText : .white)
                    }
                    .frame(width: 36, height: 36)
                }
                .buttonStyle(.plain)
                .disabled(atMax)
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

private struct PresetSheetView: View {
    let activity: ActivityTodayResponse
    let onPick: (String, Int) -> Void
    let onDismiss: () -> Void

    private var ring: Color { ringColor(for: activity.ring) }
    private var maxValue: Int {
        let goal = activity.goal?.intValue ?? 0
        return goal > 0 ? goal : 120
    }
    private var unit: String { activity.unit ?? "" }

    @State private var sliderValue: Double

    init(activity: ActivityTodayResponse, onPick: @escaping (String, Int) -> Void, onDismiss: @escaping () -> Void) {
        self.activity = activity
        self.onPick = onPick
        self.onDismiss = onDismiss
        self._sliderValue = State(initialValue: Double(activity.logValue))
    }

    private var chips: [Int] {
        [maxValue / 4, maxValue / 2, maxValue * 3 / 4, maxValue]
    }

    private static func activityIconName(for ring: String) -> String {
        switch ring {
        case "MOVE": return "ic_activity_run"
        case "MIND": return "ic_activity_mind"
        default: return "ic_activity_schedule"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 12) {
                ZStack {
                    Circle().fill(ring.opacity(0.10))
                    Image(Self.activityIconName(for: activity.ring))
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 24, height: 24)
                }
                .frame(width: 48, height: 48)

                VStack(alignment: .leading, spacing: 2) {
                    Text(activity.name)
                        .font(.system(size: 20, weight: .medium))
                        .foregroundColor(Palette.deepInk)
                    Text("Сколько сегодня?")
                        .font(.system(size: 18, weight: .regular))
                        .foregroundColor(Palette.mutedText)
                }
                Spacer()
            }
            .padding(.top, 8)

            Spacer().frame(height: 24)

            HStack(alignment: .lastTextBaseline, spacing: 6) {
                Spacer()
                Text("\(Int(sliderValue.rounded()))")
                    .font(.system(size: 36, weight: .heavy))
                    .foregroundColor(Palette.deepInk)
                if !unit.isEmpty {
                    Text(unit)
                        .font(.system(size: 34, weight: .heavy))
                        .foregroundColor(Palette.deepInk)
                }
                Spacer()
            }

            Slider(
                value: $sliderValue,
                in: 0...Double(maxValue),
                step: 1
            )
            .tint(ring)

            Spacer().frame(height: 12)

            HStack(spacing: 8) {
                ForEach(chips, id: \.self) { chipValue in
                    let isSelected = Int(sliderValue.rounded()) == chipValue
                    let chipLabel = unit.isEmpty ? "\(chipValue)" : "\(chipValue) \(unit)"
                    Button(action: { sliderValue = Double(chipValue) }) {
                        Text(chipLabel)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(isSelected ? .white : Palette.deepInk)
                            .frame(maxWidth: .infinity)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 9)
                            .background(
                                Capsule().fill(isSelected ? ring : .white)
                            )
                            .overlay(
                                isSelected ? nil :
                                Capsule().strokeBorder(Palette.activityCardBorder, lineWidth: 1.5)
                            )
                    }
                    .buttonStyle(.plain)
                }
            }

            Spacer().frame(height: 24)

            Button(action: { onPick(activity.activityId, Int(sliderValue.rounded())) }) {
                Text("Сохранить")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(RoundedRectangle(cornerRadius: 14).fill(ring))
            }
            .buttonStyle(.plain)

            Spacer().frame(height: 8)

            Button(action: onDismiss) {
                Text("Отмена")
                    .font(.system(size: 16, weight: .bold))
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

struct AiTipCard: View {
    private let tip = "Ты на правильном пути! 3-й день подряд без пропусков. Ещё 4 дня — и новый рекорд твоего стрика. Не сдавайся"

    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            ZStack {
                Circle().fill(Palette.accentOrange)
                Image(systemName: "sparkles")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(.white)
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

private struct CoursesBlock: View {
    private let featured = FeaturedCourseModel(
        title: "Утреннее пробуждение",
        tag: "популярное",
        category: "Йога",
        duration: "25 мин",
        author: "Мария Л.",
        rating: "4.9",
        gradientStart: Color(red: 1.0, green: 0.529, blue: 0.455),
        gradientEnd: Color(red: 1.0, green: 0.353, blue: 0.235)
    )

    private let smallCourses: [SmallCourseModel] = [
        SmallCourseModel(
            title: "Кардио Хит",
            author: "Елена В.",
            duration: "20 мин",
            rating: "5.0",
            badge: "Интенсив",
            gradientStart: Color(red: 1.0, green: 0.667, blue: 0.541),
            gradientEnd: Color(red: 0.914, green: 0.290, blue: 0.173)
        ),
        SmallCourseModel(
            title: "Сила и мощь",
            author: "Борис Л.",
            duration: "20 мин",
            rating: "5.0",
            badge: "Интенсив",
            gradientStart: Color(red: 0.239, green: 0.239, blue: 0.271),
            gradientEnd: Color(red: 0.039, green: 0.039, blue: 0.039)
        ),
    ]

    var body: some View {
        ZStack {
            VStack(alignment: .leading, spacing: 12) {
                FeaturedCourseCard(course: featured)

                HStack(alignment: .top, spacing: 12) {
                    ForEach(smallCourses) { course in
                        SmallCourseCard(course: course)
                            .frame(maxWidth: .infinity)
                    }
                }

                SeeAllCoursesButton(action: { })
            }

            RoundedRectangle(cornerRadius: 20)
                .fill(Color.white.opacity(0.75))
                .overlay(
                    Text("В разработке")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(Color(red: 0.161, green: 0.141, blue: 0.125))
                )
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

private let courseCardBorder = Color(red: 0.929, green: 0.929, blue: 0.937)
private let starYellow = Color(red: 0.831, green: 0.627, blue: 0.090)
private let ratingPillBg = Color(red: 1.0, green: 0.965, blue: 0.839)
private let smallCourseBadgeBg = Color(red: 0.882, green: 0.945, blue: 1.0)
private let smallCourseBadgeFg = Color(red: 0.039, green: 0.518, blue: 1.0)

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

private let mealCardBorder = Color(red: 0.929, green: 0.929, blue: 0.937)

struct MealPlanIdHolder: Identifiable {
    let value: String
    var id: String { value }
}

struct MealPlanSection: View {
    let todayPlans: [TodayMealPlanCard]
    let isLoading: Bool
    let selectedDate: Date
    let onDateSelected: (Date) -> Void
    let onPlanTap: (String) -> Void
    let onCreatePlan: (String?, String?) -> Void
    var onPastDateBlocked: () -> Void = {}
    let isPremium: Bool
    let onOpenPaywall: () -> Void

    private static let standardSlots: [String] = ["BREAKFAST", "LUNCH", "DINNER"]

    private var isPastDate: Bool {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        let target = calendar.startOfDay(for: selectedDate)
        return target < today
    }

    private var totalKcal: Int {
        todayPlans.reduce(0) { acc, card in
            acc + Int(card.estimatedCalories?.intValue ?? 0)
        }
    }

    private var subtitle: String {
        if !isPremium { return "Доступен в Premium" }
        if isLoading { return "Загрузка..." }
        if todayPlans.isEmpty { return "Нет планов на эту дату" }
        return "\(todayPlans.count) \(Self.pluralizeMeals(todayPlans.count)) · \(totalKcal) ккал"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ActivitiesSectionHeader(
                title: "План питания",
                subtitle: subtitle,
                actionTitle: nil,
                destination: nil
            )

            if !isPremium {
                MealPlanLockedOverlay(onUnlock: onOpenPaywall)
            } else {
                MealDatePicker(
                    selectedDate: selectedDate,
                    onDateSelected: onDateSelected
                )

                if isLoading {
                    MealPlansLoadingState()
                } else if todayPlans.isEmpty {
                    EmptyMealPlanCard(onCreate: {
                        if isPastDate { onPastDateBlocked() } else { onCreatePlan(nil, nil) }
                    })
                } else {
                    let dateIso = Self.isoDateString(from: selectedDate)
                    VStack(spacing: 10) {
                        ForEach(Self.standardSlots, id: \.self) { slot in
                            if let card = todayPlans.first(where: { $0.mealType.uppercased() == slot }) {
                                TodayMealPlanRow(
                                    card: card,
                                    onTap: { onPlanTap(card.planId) }
                                )
                            } else {
                                EmptyMealSlotCard(
                                    mealType: slot,
                                    onTap: {
                                        if isPastDate { onPastDateBlocked() } else { onCreatePlan(slot, dateIso) }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private static func isoDateString(from date: Date) -> String {
        let fmt = DateFormatter()
        fmt.dateFormat = "yyyy-MM-dd"
        return fmt.string(from: date)
    }

    private static func pluralizeMeals(_ count: Int) -> String {
        let mod10 = count % 10
        let mod100 = count % 100
        if mod10 == 1 && mod100 != 11 { return "приём пищи" }
        if (2...4).contains(mod10) && !(12...14).contains(mod100) { return "приёма пищи" }
        return "приёмов пищи"
    }
}

private struct MealPlansLoadingState: View {
    private static let slots = ["BREAKFAST", "LUNCH", "DINNER"]

    var body: some View {
        VStack(spacing: 8) {
            ForEach(Self.slots, id: \.self) { slot in
                SkeletonMealCard(mealType: slot)
            }
        }
    }
}

private struct SkeletonMealCard: View {
    let mealType: String

    var body: some View {
        EmptyMealSlotCard(mealType: mealType, onTap: {})
            .redacted(reason: .placeholder)
            .allowsHitTesting(false)
    }
}

private struct MealDatePicker: View {
    let selectedDate: Date
    let onDateSelected: (Date) -> Void

    @State private var showDatePicker: Bool = false

    private var label: String {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        let target = calendar.startOfDay(for: selectedDate)

        if calendar.isDate(target, inSameDayAs: today) { return "Сегодня" }
        if let yesterday = calendar.date(byAdding: .day, value: -1, to: today),
           calendar.isDate(target, inSameDayAs: yesterday) {
            return "Вчера"
        }
        if let tomorrow = calendar.date(byAdding: .day, value: 1, to: today),
           calendar.isDate(target, inSameDayAs: tomorrow) {
            return "Завтра"
        }

        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ru_RU")
        formatter.dateFormat = "d MMMM"
        return formatter.string(from: target)
    }

    var body: some View {
        HStack(spacing: 12) {
            arrowButton(symbol: "chevron.left") {
                shift(by: -1)
            }

            Text(label)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(Palette.deepInk)
                .frame(maxWidth: .infinity)
                .multilineTextAlignment(.center)
                .lineLimit(1)
                .padding(.vertical, 8)
                .contentShape(Rectangle())
                .onTapGesture { showDatePicker = true }

            arrowButton(symbol: "chevron.right") {
                shift(by: 1)
            }
        }
        .sheet(isPresented: $showDatePicker) {
            datePickerSheet
        }
    }

    private var datePickerSheet: some View {
        VStack(spacing: 16) {
            DatePicker(
                "",
                selection: Binding(
                    get: { selectedDate },
                    set: { newValue in
                        onDateSelected(newValue)
                    }
                ),
                displayedComponents: .date
            )
            .datePickerStyle(.graphical)
            .labelsHidden()
            .environment(\.locale, Locale(identifier: "ru_RU"))

            Button(action: { showDatePicker = false }) {
                Text("Готово")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(RoundedRectangle(cornerRadius: 14).fill(Palette.coral))
            }
            .buttonStyle(.plain)
        }
        .padding(20)
        .presentationDetents([.medium, .large])
    }

    private func shift(by days: Int) {
        let calendar = Calendar.current
        guard let next = calendar.date(byAdding: .day, value: days, to: selectedDate) else { return }
        onDateSelected(next)
    }

    private func arrowButton(symbol: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            ZStack {
                Circle().fill(Color(red: 0.957, green: 0.957, blue: 0.965))
                Image(systemName: symbol)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(Color(red: 0.039, green: 0.039, blue: 0.039))
            }
            .frame(width: 40, height: 40)
        }
        .buttonStyle(.plain)
    }
}

private struct EmptyMealSlotCard: View {
    let mealType: String
    let onTap: () -> Void

    private var palette: MealPalette { MealPalette.forType(mealType) }
    private var displayName: String { MealPalette.displayName(mealType) }

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12).fill(palette.iconBg)
                    Image(palette.imageName)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 24, height: 24)
                }
                .frame(width: 42, height: 42)

                VStack(alignment: .leading, spacing: 2) {
                    Text(displayName)
                        .font(.system(size: 16.5, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                    Text("Ещё не составлено — добавь сейчас")
                        .font(.system(size: 14))
                        .foregroundColor(Palette.mutedText)
                        .lineLimit(1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Spacer().frame(width: 16)

                ZStack {
                    Circle().fill(Palette.deepInk)
                    Text("+")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.white)
                }
                .frame(width: 24, height: 24)
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

private struct EmptyMealPlanCard: View {
    let onCreate: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Image("ic_empty_food_plan")
                .resizable()
                .scaledToFit()
                .frame(height: 100)

            Spacer().frame(height: 16)

            Text("Нет плана питания на эту дату")
                .font(.system(size: 18, weight: .heavy))
                .foregroundColor(Palette.deepInk)

            Spacer().frame(height: 4)

            Text("Создай рацион — расписание само подскажет, что есть в этот день")
                .font(.system(size: 16))
                .foregroundColor(Palette.mutedText)
                .multilineTextAlignment(.center)

            Spacer().frame(height: 14)

            Button(action: onCreate) {
                Text("Создать план")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(RoundedRectangle(cornerRadius: 14).fill(Palette.coral))
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 24)
        .padding(.horizontal, 20)
        .background(RoundedRectangle(cornerRadius: 18).fill(.white))
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .strokeBorder(mealCardBorder, lineWidth: 1.5)
        )
    }
}

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
                    Image(palette.imageName)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 24, height: 24)
                }
                .frame(width: 42, height: 42)

                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 8) {
                        Text(displayName)
                            .font(.system(size: 16.5, weight: .bold))
                            .foregroundColor(Palette.deepInk)
                        Text(card.planName)
                            .font(.system(size: 13.5))
                            .foregroundColor(Palette.deepInk)
                            .lineLimit(1)
                    }
                    Text(dishSummary)
                        .font(.system(size: 14))
                        .foregroundColor(Palette.mutedText)
                        .lineLimit(1)
                }

                Spacer(minLength: 8)

                if let kcal = card.estimatedCalories {
                    VStack(alignment: .trailing, spacing: 0) {
                        Text("\(kcal.intValue)")
                            .font(.system(size: 16, weight: .heavy))
                            .foregroundColor(Palette.deepInk)
                        Text("ККАЛ")
                            .font(.system(size: 12, weight: .semibold))
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

private struct MealPalette {
    let iconBg: Color
    let iconFg: Color
    let imageName: String

    static func forType(_ wireValue: String) -> MealPalette {
        switch wireValue.uppercased() {
        case "BREAKFAST":
            return MealPalette(
                iconBg: Color(red: 1.0, green: 0.965, blue: 0.839),
                iconFg: Color(red: 0.831, green: 0.627, blue: 0.090),
                imageName: "ic_breakfast"
            )
        case "LUNCH":
            return MealPalette(
                iconBg: Color(red: 1.0, green: 0.933, blue: 0.867),
                iconFg: Color(red: 0.878, green: 0.482, blue: 0.0),
                imageName: "ic_lunch"
            )
        case "DINNER":
            return MealPalette(
                iconBg: Color(red: 0.902, green: 0.914, blue: 1.0),
                iconFg: Color(red: 0.231, green: 0.357, blue: 0.859),
                imageName: "ic_dinner"
            )
        case "SNACK":
            return MealPalette(
                iconBg: Color(red: 0.910, green: 0.969, blue: 0.910),
                iconFg: Color(red: 0.184, green: 0.620, blue: 0.267),
                imageName: "ic_dinner"
            )
        default:
            return MealPalette(
                iconBg: Color(red: 0.953, green: 0.949, blue: 0.937),
                iconFg: Color(red: 0.431, green: 0.431, blue: 0.463),
                imageName: "ic_lunch"
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
