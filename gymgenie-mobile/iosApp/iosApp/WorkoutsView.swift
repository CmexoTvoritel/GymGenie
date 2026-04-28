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
                switch viewModel.selectedTab {
                case .workouts:
                    workoutsTab
                case .exercises:
                    exercisesTab
                @unknown default:
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
        switch viewModel.selectedTab {
        case .workouts:
            return viewModel.workoutPlans.isEmpty
        case .exercises:
            return viewModel.exercises.isEmpty
        @unknown default:
            return true
        }
    }

    /// Distinguishes "successful empty result" from "error while empty".
    private var isExpectedEmptyState: Bool {
        switch viewModel.selectedTab {
        case .workouts:
            return viewModel.workoutPlansLoaded && viewModel.workoutPlans.isEmpty
        case .exercises:
            return viewModel.exercisesLoaded && viewModel.exercises.isEmpty && viewModel.errorMessage == nil
        @unknown default:
            return false
        }
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
                    if let featured = viewModel.workoutPlans.first {
                        FeaturedWorkoutCard(plan: featured) {
                            startSession(planId: featured.id, planName: featured.name)
                        }
                    }

                    let otherPlans = Array(viewModel.workoutPlans.dropFirst())
                    if !otherPlans.isEmpty {
                        LazyVGrid(columns: columns, spacing: 12) {
                            ForEach(otherPlans, id: \.id) { plan in
                                WorkoutCardSmall(plan: plan) {
                                    startSession(planId: plan.id, planName: plan.name)
                                }
                            }
                        }
                    }
                }

                Color.clear.frame(height: 96)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
        }
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
