import SwiftUI
import Shared

private class ScrollTrackingCoordinator {
    var observation: NSKeyValueObservation?
    var onScroll: ((CGFloat, Bool) -> Void)?
}

private struct ScrollOffsetTracker: UIViewRepresentable {
    var onScroll: (CGFloat, Bool) -> Void

    func makeCoordinator() -> ScrollTrackingCoordinator { ScrollTrackingCoordinator() }

    func makeUIView(context: Context) -> UIView {
        let v = UIView(frame: .zero)
        v.isUserInteractionEnabled = false
        return v
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        let coord = context.coordinator
        coord.onScroll = onScroll
        guard coord.observation == nil else { return }
        DispatchQueue.main.async {
            guard let sv = uiView.enclosingScrollView() else { return }
            coord.observation = sv.observe(\.contentOffset, options: .new) { scrollView, change in
                guard let pt = change.newValue else { return }
                let dragging = scrollView.isTracking || scrollView.isDragging
                let adjusted = pt.y + scrollView.adjustedContentInset.top
                DispatchQueue.main.async { coord.onScroll?(adjusted, dragging) }
            }
        }
    }
}

private extension UIView {
    func enclosingScrollView() -> UIScrollView? {
        var v: UIView? = superview
        while let s = v {
            if let sv = s as? UIScrollView { return sv }
            v = s.superview
        }
        return nil
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
    @State private var headerOffset: CGFloat = 0
    @State private var headerHeight: CGFloat = 0
    @State private var lastScrollY: CGFloat = 0
    @State private var scrollInitialized: Bool = false
    @State private var snapWork: DispatchWorkItem? = nil
    @State private var displayedTab: Shared.WorkoutsTab = .workouts
    @State private var showFilterSheet: Bool = false
    @State private var catalogExerciseForWorkout: Shared.ExerciseDetailResponse? = nil
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
                    onBack: { dismissExerciseDetail() },
                    onAddToWorkout: { exercise in
                        selectedExerciseId = nil
                        catalogExerciseForWorkout = exercise
                    }
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
        .fullScreenCover(
            isPresented: Binding(
                get: { catalogExerciseForWorkout != nil },
                set: { if !$0 { catalogExerciseForWorkout = nil } }
            )
        ) {
            if let detail = catalogExerciseForWorkout {
                let exerciseShort = Shared.ExerciseShortResponse(
                    id: detail.id,
                    nameRu: detail.nameRu,
                    nameEn: detail.nameEn,
                    muscleGroup: detail.muscleGroup,
                    category: detail.category,
                    difficultyLevel: detail.difficultyLevel,
                    secondsPer10Reps: nil,
                    caloriesBurned: nil,
                    rating: nil,
                    imageUrl: nil,
                    requiresWeight: detail.requiresWeight
                )
                CreateWorkoutFlowView(
                    onDismiss: {
                        catalogExerciseForWorkout = nil
                        viewModel.loadWorkoutPlans()
                    },
                    initialExercise: exerciseShort
                )
            }
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
        .onChange(of: catalogExerciseForWorkout) { exercise in
            if exercise == nil {
                tabBarState.isVisible = (selectedExerciseId == nil && selectedPlanId == nil)
            } else {
                tabBarState.isVisible = false
            }
        }
        .onChange(of: showCreateWorkout) { showing in
            if !showing {
                tabBarState.isVisible = (selectedExerciseId == nil && selectedPlanId == nil)
            } else {
                tabBarState.isVisible = false
            }
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
                    headerOffset = 0
                    scrollInitialized = false
                }
            )

            ZStack {
                workoutsPage
                    .opacity(displayedTab == .workouts ? 1 : 0)
                    .allowsHitTesting(displayedTab == .workouts)

                exercisesPage
                    .opacity(displayedTab == .exercises ? 1 : 0)
                    .allowsHitTesting(displayedTab == .exercises)
            }
        }
        .onAppear {
            viewModel.loadWorkoutPlans()
        }
        .onChange(of: viewModel.searchQuery) { newValue in
            if localSearchQuery != newValue {
                localSearchQuery = newValue
            }
        }
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
                    WorkoutsEmptyState(onCreate: { showCreateWorkout = true })
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
        Group {
            if viewModel.exercises.isEmpty && !viewModel.isLoading && !viewModel.isLoadingMore {
                VStack(spacing: 0) {
                    collapsibleHeader
                        .overlay(GeometryReader { geo in
                            Color.clear.onAppear {
                                if headerHeight == 0 { headerHeight = geo.size.height }
                            }
                        })
                    ExercisesEmptyState(
                        hasFilter: !localSearchQuery.isEmpty || viewModel.selectedMuscleGroup != nil ||
                            !viewModel.selectedDifficulties.isEmpty || viewModel.requiresEquipment != nil ||
                            viewModel.sortByDifficulty != nil || viewModel.sortByCalories != nil
                    )
                    .padding(.top, 40)
                    Spacer()
                }
            } else {
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 0) {
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
                    .background(ScrollOffsetTracker { offsetY, isUserDragging in
                        guard headerHeight > 0 else { return }

                        guard offsetY > 0 else {
                            if headerOffset != 0 { headerOffset = 0 }
                            lastScrollY = 0
                            return
                        }

                        if !scrollInitialized {
                            lastScrollY = offsetY
                            scrollInitialized = true
                            return
                        }

                        let delta = offsetY - lastScrollY
                        lastScrollY = offsetY

                        let newOffset = headerOffset + delta
                        let clamped = min(max(newOffset, 0), headerHeight)
                        if clamped != headerOffset {
                            headerOffset = clamped
                        }

                        snapWork?.cancel()
                        if !isUserDragging {
                            let work = DispatchWorkItem {
                                withAnimation(.easeOut(duration: 0.2)) {
                                    headerOffset = headerOffset > headerHeight * 0.5 ? headerHeight : 0
                                }
                            }
                            snapWork = work
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15, execute: work)
                        }
                    })
                }
                .safeAreaInset(edge: .top, spacing: 0) {
                    collapsibleHeader
                        .overlay(GeometryReader { geo in
                            Color.clear.onAppear {
                                if headerHeight == 0 { headerHeight = geo.size.height }
                            }
                        })
                        .offset(y: -headerOffset)
                        .frame(height: headerHeight > 0 ? headerHeight : nil, alignment: .top)
                        .clipped()
                        .allowsHitTesting(headerOffset < headerHeight * 0.9)
                }
                .refreshable { await refreshAndWait() }
                .tint(Palette.coral)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
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
