import SwiftUI
import Shared

private struct WorkoutsScrollOffsetKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

private struct WorkoutsHeaderHeightKey: PreferenceKey {
    static var defaultValue: CGFloat = 110
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

struct WorkoutsView: View {
    @EnvironmentObject private var appState: AppState
    @EnvironmentObject private var tabBarState: TabBarState
    @StateObject private var viewModel = WorkoutsViewModelWrapper()

    @State private var localSearchQuery: String = ""
    @State private var selectedExerciseId: String? = nil
    @State private var selectedPlanId: String? = nil
    @State private var showCreateWorkout: Bool = false
    @State private var activeWorkoutSession: ActiveWorkoutSession? = nil
    @State private var showWorkoutSession: Bool = false
    @State private var exercisesScrollOffset: CGFloat = 0
    @State private var lastScrollOffset: CGFloat = 0
    @State private var headerVisible: Bool = true
    @State private var headerHeight: CGFloat = 110
    @State private var displayedTab: Shared.WorkoutsTab = .workouts
    @State private var showFilterSheet: Bool = false
    private let orange = Color(red: 0.941, green: 0.439, blue: 0.188)
    private let warmOffWhite = Color(red: 0.980, green: 0.976, blue: 0.969)

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12),
    ]

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            mainContent

            if displayedTab == .workouts {
                fabButton
            }

            if viewModel.isLoadingSession {
                Color.black.opacity(0.25).ignoresSafeArea()
                ProgressView()
                    .scaleEffect(1.4)
                    .tint(.white)
            }
        }
        .background(warmOffWhite.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
        .navigationDestination(isPresented: Binding(
            get: { selectedExerciseId != nil },
            set: { if !$0 { dismissExerciseDetail() } }
        )) {
            if let id = selectedExerciseId {
                ExerciseDetailView(
                    exerciseId: id,
                    onBack: { dismissExerciseDetail() }
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
                        startSession(planId: planId, planName: planName)
                    }
                )
                .toolbar(.hidden, for: .navigationBar)
            }
        }
        .fullScreenCover(isPresented: $showCreateWorkout) {
            CreateWorkoutFlowView(
                onDismiss: {
                    showCreateWorkout = false
                    viewModel.loadWorkoutPlans()
                }
            )
        }
        .fullScreenCover(isPresented: $showWorkoutSession) {
            if let session = activeWorkoutSession {
                WorkoutSessionView(session: session)
                    .onDisappear { activeWorkoutSession = nil }
            }
        }
        .onChange(of: viewModel.isLoggedOut) { loggedOut in
            if loggedOut {
                appState.navigate(to: .login)
            }
        }
        .onChange(of: selectedExerciseId) { id in
            tabBarState.isVisible = (id == nil && selectedPlanId == nil)
        }
        .onChange(of: selectedPlanId) { id in
            tabBarState.isVisible = (id == nil && selectedExerciseId == nil)
        }
        .onChange(of: viewModel.pendingSession?.planId) { _ in
            if let session = viewModel.pendingSession {
                activeWorkoutSession = session
                showWorkoutSession = true
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
        .sheet(isPresented: $showFilterSheet) {
            ExerciseFilterSheet(
                isPresented: $showFilterSheet,
                currentDifficulties: viewModel.selectedDifficulties,
                currentRequiresEquipment: viewModel.requiresEquipment,
                currentSortByDifficulty: viewModel.sortByDifficulty,
                currentSortByCalories: viewModel.sortByCalories,
                onApply: { difficulties, requiresEquipment, sortByDifficulty, sortByCalories in
                    viewModel.applyFilters(
                        difficulties: difficulties,
                        requiresEquipment: requiresEquipment,
                        sortByDifficulty: sortByDifficulty,
                        sortByCalories: sortByCalories
                    )
                }
            )
        }
    }

    private func dismissExerciseDetail() {
        selectedExerciseId = nil
    }

    private func dismissPlanDetail() {
        selectedPlanId = nil
        viewModel.loadWorkoutPlans()
    }

    private func startSession(planId: String, planName: String) {
        viewModel.startWorkout(planId: planId, planName: planName)
    }

    private var mainContent: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: "Тренировки",
                actions: []
            )

            WorkoutTabSelector(
                selectedTab: displayedTab,
                onTabSelected: { tab in
                    displayedTab = tab
                    viewModel.selectTab(tab)
                }
            )

            TabView(selection: tabSelectionBinding) {
                workoutsPage
                    .tag(Shared.WorkoutsTab.workouts)

                exercisesPage
                    .tag(Shared.WorkoutsTab.exercises)
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
        }
        .onAppear {
            // Only trigger a load on first appearance. The view is kept alive
            // inside MainView's ZStack and .onAppear fires on every tab switch;
            // re-triggering a load would flash the loading state and reset
            // scroll positions even though the shared VM guards against
            // concurrent loads.
            if !viewModel.workoutPlansLoaded {
                viewModel.loadWorkoutPlans()
            }
        }
        .onChange(of: viewModel.searchQuery) { newValue in
            if localSearchQuery != newValue {
                localSearchQuery = newValue
            }
        }
    }

    private var tabSelectionBinding: Binding<Shared.WorkoutsTab> {
        Binding(
            get: { displayedTab },
            set: { newValue in
                if newValue != displayedTab {
                    displayedTab = newValue
                    viewModel.selectTab(newValue)
                }
            }
        )
    }

    private var workoutsPage: some View {
        Group {
            if viewModel.isLoading && viewModel.workoutPlans.isEmpty {
                VStack {
                    Spacer()
                    ProgressView().scaleEffect(1.2).tint(orange)
                    Spacer()
                }
            } else if let error = viewModel.errorMessage,
                      viewModel.workoutPlans.isEmpty,
                      !(viewModel.workoutPlansLoaded && viewModel.workoutPlans.isEmpty) {
                VStack {
                    Spacer()
                    WorkoutsErrorView(message: error, onRetry: viewModel.retry)
                    Spacer()
                }
            } else {
                workoutsTab
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var exercisesPage: some View {
        Group {
            if viewModel.isLoading && viewModel.exercises.isEmpty {
                VStack {
                    Spacer()
                    ProgressView().scaleEffect(1.2).tint(orange)
                    Spacer()
                }
            } else if let error = viewModel.errorMessage,
                      viewModel.exercises.isEmpty,
                      !(viewModel.exercisesLoaded && viewModel.exercises.isEmpty && viewModel.errorMessage == nil) {
                VStack {
                    Spacer()
                    WorkoutsErrorView(message: error, onRetry: viewModel.retry)
                    Spacer()
                }
            } else {
                exercisesTab
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var workoutsTab: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 16) {
                if viewModel.workoutPlans.isEmpty {
                    WorkoutsEmptyState(onCreate: { })
                } else {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.workoutPlans, id: \.id) { plan in
                            WorkoutPlanCard(
                                plan: plan,
                                onView: { selectedPlanId = plan.id },
                                onStart: {
                                    startSession(planId: plan.id, planName: plan.name)
                                }
                            )
                        }
                    }
                }

                Color.clear.frame(height: 96)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
        }
        .refreshable { await refreshAndWait() }
        .tint(Palette.coral)
    }

    private var exercisesTab: some View {
        return ZStack(alignment: .top) {
            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    Color.clear.frame(height: headerHeight)

                    if viewModel.exercises.isEmpty && !viewModel.isLoading && !viewModel.isLoadingMore {
                        ExercisesEmptyState(
                            hasFilter: !localSearchQuery.isEmpty || viewModel.selectedMuscleGroup != nil ||
                                !viewModel.selectedDifficulties.isEmpty || viewModel.requiresEquipment != nil ||
                                viewModel.sortByDifficulty != nil || viewModel.sortByCalories != nil
                        )
                        .padding(.top, 40)
                    } else {
                        LazyVGrid(columns: columns, spacing: 12) {
                            ForEach(viewModel.exercises, id: \.id) { exercise in
                                ExerciseCard(
                                    exercise: exercise,
                                    onTap: { selectedExerciseId = exercise.id }
                                )
                                .onAppear {
                                    if exercise.id == viewModel.exercises.last?.id {
                                        viewModel.loadMoreExercises()
                                    }
                                }
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.vertical, 12)

                        if viewModel.isLoadingMore {
                            ProgressView().padding()
                        }

                        Color.clear.frame(height: 96)
                    }
                }
                .background(
                    GeometryReader { geo in
                        Color.clear.preference(
                            key: WorkoutsScrollOffsetKey.self,
                            value: -geo.frame(in: .named("exercisesScroll")).minY
                        )
                    }
                )
            }
            .coordinateSpace(name: "exercisesScroll")
            .onPreferenceChange(WorkoutsScrollOffsetKey.self) { offset in
                let delta = offset - lastScrollOffset
                if offset <= 0 {
                    if !headerVisible { withAnimation(.easeInOut(duration: 0.25)) { headerVisible = true } }
                } else if delta > 10 {
                    if headerVisible { withAnimation(.easeInOut(duration: 0.25)) { headerVisible = false } }
                } else if delta < -5 {
                    if !headerVisible { withAnimation(.easeInOut(duration: 0.25)) { headerVisible = true } }
                }
                lastScrollOffset = offset
                exercisesScrollOffset = offset
            }
            .onPreferenceChange(WorkoutsHeaderHeightKey.self) { height in
                if height > 0 { headerHeight = height }
            }
            .refreshable { await refreshAndWait() }
            .tint(Palette.coral)

            collapsibleHeader
                .background(
                    GeometryReader { geo in
                        Color.clear.preference(key: WorkoutsHeaderHeightKey.self, value: geo.size.height)
                    }
                )
                .offset(y: headerVisible ? 0 : -headerHeight)
                .allowsHitTesting(headerVisible)
                .animation(.easeInOut(duration: 0.25), value: headerVisible)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
    }

    private var collapsibleHeader: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                ExerciseSearchBar(
                    searchQuery: $localSearchQuery,
                    onSubmit: {
                        viewModel.updateSearchQuery(localSearchQuery)
                        viewModel.searchExercises()
                    },
                    onClear: {
                        localSearchQuery = ""
                        viewModel.updateSearchQuery("")
                        viewModel.loadExercises(reset: true)
                    }
                )
                .onChange(of: localSearchQuery) { newValue in
                    viewModel.updateSearchQuery(newValue)
                    if newValue.isEmpty {
                        viewModel.loadExercises(reset: true)
                    }
                }

                filterButton
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 8)

            if localSearchQuery.isEmpty {
                MuscleGroupFilterChips(
                    selected: viewModel.selectedMuscleGroup,
                    onSelected: { group in
                        viewModel.filterByMuscleGroup(group)
                    }
                )
            }
        }
        .background(warmOffWhite)
    }

    private var activeFiltersCount: Int {
        var count = 0
        if !viewModel.selectedDifficulties.isEmpty { count += 1 }
        if viewModel.requiresEquipment != nil { count += 1 }
        if viewModel.sortByDifficulty != nil { count += 1 }
        if viewModel.sortByCalories != nil { count += 1 }
        return count
    }

    private var filterButton: some View {
        // Solid orange fill is the active indicator. No corner badge: it keeps
        // the affordance clean inside the 44pt touch target and avoids
        // clipping artefacts on the circle's edge.
        let isActive = activeFiltersCount > 0
        let background: Color = isActive ? orange : .white
        let iconColor: Color = isActive ? .white : Color(red: 0.298, green: 0.298, blue: 0.325)
        let borderColor: Color = isActive ? orange : Color(red: 0.929, green: 0.929, blue: 0.937)

        return Button(action: { showFilterSheet = true }) {
            Image(systemName: "slider.horizontal.3")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(iconColor)
                .frame(width: 44, height: 44)
                .background(Circle().fill(background))
                .overlay(Circle().stroke(borderColor, lineWidth: 1.5))
                .shadow(color: .black.opacity(0.06), radius: 4, y: 2)
        }
        .buttonStyle(.plain)
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

    private var fabButton: some View {
        Button(action: { showCreateWorkout = true }) {
            Image(systemName: "plus")
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(.white)
                .frame(width: 56, height: 56)
                .background(Circle().fill(orange))
                .shadow(color: orange.opacity(0.4), radius: 8, y: 4)
        }
        .buttonStyle(.plain)
        .padding(.trailing, 20)
        .padding(.bottom, 20)
    }
}
