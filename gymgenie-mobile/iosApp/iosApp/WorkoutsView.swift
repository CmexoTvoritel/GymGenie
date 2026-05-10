import SwiftUI
import Shared

struct WorkoutsView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel = WorkoutsViewModelWrapper()

    @State private var localSearchQuery: String = ""
    @State private var selectedExerciseId: String? = nil
    @State private var showCreateWorkout: Bool = false
    @State private var activeWorkoutSession: ActiveWorkoutSession? = nil
    @State private var showWorkoutSession: Bool = false

    private let orange = Color(red: 0.941, green: 0.439, blue: 0.188)
    private let deepInk = Color(red: 0.161, green: 0.141, blue: 0.125)
    private let warmOffWhite = Color(red: 0.980, green: 0.976, blue: 0.969)
    private let softCard = Color(red: 0.953, green: 0.949, blue: 0.937)
    private let mutedText = Color(red: 0.463, green: 0.447, blue: 0.416)

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12),
    ]

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            warmOffWhite.edgesIgnoringSafeArea(.all)

            mainContent

            fabButton
        }
        .fullScreenCover(
            isPresented: Binding(
                get: { selectedExerciseId != nil },
                set: { if !$0 { selectedExerciseId = nil } }
            )
        ) {
            if let id = selectedExerciseId {
                ExerciseDetailView(
                    exerciseId: id,
                    onBack: { selectedExerciseId = nil }
                )
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
    }

    private func startSession(planId: String, planName: String) {
        activeWorkoutSession = ActiveWorkoutSession(
            planId: planId,
            planName: planName,
            exercises: [],
            restSeconds: 60
        )
        showWorkoutSession = true
    }

    private var mainContent: some View {
        VStack(spacing: 0) {
            headerSection

            WorkoutTabSelector(
                selectedTab: viewModel.selectedTab,
                onTabSelected: { tab in
                    viewModel.selectTab(tab)
                }
            )

            if viewModel.isLoading && contentIsEmpty {
                Spacer()
                ProgressView().scaleEffect(1.2).tint(orange)
                Spacer()
            } else if let error = viewModel.errorMessage, contentIsEmpty, !isExpectedEmptyState {
                Spacer()
                errorView(message: error)
                Spacer()
            } else {
                if viewModel.selectedTab == .workouts {
                    workoutsTab
                } else if viewModel.selectedTab == .exercises {
                    exercisesTab
                } else {
                    workoutsTab
                }
            }
        }
        .onAppear { viewModel.loadWorkoutPlans() }
        .onChange(of: viewModel.searchQuery) { newValue in
            if localSearchQuery != newValue {
                localSearchQuery = newValue
            }
        }
    }

    private var contentIsEmpty: Bool {
        if viewModel.selectedTab == .workouts { return viewModel.workoutPlans.isEmpty }
        if viewModel.selectedTab == .exercises { return viewModel.exercises.isEmpty }
        return true
    }

    /// Distinguishes "successful empty result" from "error while empty".
    private var isExpectedEmptyState: Bool {
        if viewModel.selectedTab == .workouts { return viewModel.workoutPlansLoaded && viewModel.workoutPlans.isEmpty }
        if viewModel.selectedTab == .exercises { return viewModel.exercisesLoaded && viewModel.exercises.isEmpty && viewModel.errorMessage == nil }
        return false
    }

    // MARK: - Header

    private var headerSection: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("Тренировки")
                .font(.system(size: 24, weight: .bold))
                .foregroundColor(deepInk)
            Text("Твои планы и каталог упражнений")
                .font(.system(size: 13))
                .foregroundColor(mutedText)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 20)
        .padding(.top, 16)
        .padding(.bottom, 8)
    }

    // MARK: - Error

    private func errorView(message: String) -> some View {
        VStack(spacing: 12) {
            Text("⚠️").font(.system(size: 36))
            Text(message)
                .font(.system(size: 14))
                .foregroundColor(mutedText)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            Button(action: viewModel.retry) {
                Text("Повторить")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 10)
                    .background(Capsule().fill(orange))
            }
            .buttonStyle(.plain)
        }
    }

    // MARK: - Workouts Tab

    private var workoutsTab: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 16) {
                if viewModel.workoutPlans.isEmpty {
                    workoutsEmptyState
                } else {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.workoutPlans, id: \.id) { plan in
                            WorkoutPlanCard(
                                plan: plan,
                                onView: {
                                    // TODO: navigate to plan detail once the
                                    // route is wired up on iOS.
                                },
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

    private var workoutsEmptyState: some View {
        VStack(spacing: 10) {
            Text("🏋").font(.system(size: 48))
            Text("Нет тренировочных планов")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(deepInk)
            Text("Создайте свой первый план тренировок")
                .font(.system(size: 13))
                .foregroundColor(mutedText)

            Button(action: {}) {
                Text("Создать первый план")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Capsule().fill(orange))
            }
            .buttonStyle(.plain)
            .padding(.top, 8)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 40)
    }

    // MARK: - Exercises Tab

    private var exercisesTab: some View {
        VStack(spacing: 0) {
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

            if localSearchQuery.isEmpty {
                MuscleGroupFilterChips(
                    selected: viewModel.selectedMuscleGroup,
                    onSelected: { group in
                        viewModel.filterByMuscleGroup(group)
                    }
                )
            }

            ScrollView(showsIndicators: false) {
                if viewModel.exercises.isEmpty && !viewModel.isLoading && !viewModel.isLoadingMore {
                    exercisesEmptyState
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
            .refreshable { await refreshAndWait() }
            .tint(Palette.coral)
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

    private var exercisesEmptyState: some View {
        let hasFilter = !localSearchQuery.isEmpty || viewModel.selectedMuscleGroup != nil
        return VStack(spacing: 8) {
            Text("📦").font(.system(size: 48))
            Text(hasFilter ? "Ничего не найдено" : "Каталог пуст")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(deepInk)
            Text(hasFilter ? "Попробуйте изменить запрос или фильтр" : "Упражнения скоро появятся")
                .font(.system(size: 13))
                .foregroundColor(mutedText)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 40)
    }

    // MARK: - FAB

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
