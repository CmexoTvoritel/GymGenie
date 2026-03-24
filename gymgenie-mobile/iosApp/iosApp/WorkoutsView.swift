import SwiftUI
import Shared

struct WorkoutsView: View {
    @StateObject private var viewModel = WorkoutsViewModelWrapper()

    @State private var localSearchQuery: String = ""

    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890)
    private let backgroundColor = Color(red: 0.961, green: 0.969, blue: 0.980)
    private let darkColor = Color(red: 0.102, green: 0.102, blue: 0.180)

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12),
    ]

    var body: some View {
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
                ProgressView()
                    .scaleEffect(1.2)
                Spacer()
            } else if let error = viewModel.errorMessage, contentIsEmpty {
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
        .background(backgroundColor)
        .onAppear {
            viewModel.loadWorkoutPlans()
        }
        .onChange(of: viewModel.searchQuery) { newValue in
            localSearchQuery = newValue
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

    // MARK: - Header

    private var headerSection: some View {
        HStack {
            Text("Мои тренировки")
                .font(.system(size: 24, weight: .bold))
                .foregroundColor(darkColor)

            Spacer()

            Button(action: {}) {
                HStack(spacing: 4) {
                    Image(systemName: "plus")
                        .font(.system(size: 13, weight: .semibold))
                    Text("Добавить")
                        .font(.system(size: 13, weight: .semibold))
                }
                .foregroundColor(accentColor)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(
                    Capsule().fill(accentColor.opacity(0.1))
                )
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 16)
        .padding(.bottom, 8)
    }

    // MARK: - Error

    private func errorView(message: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 36))
                .foregroundColor(.orange)

            Text(message)
                .font(.system(size: 14))
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            Button(action: viewModel.retry) {
                Text("Повторить")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 10)
                    .background(Capsule().fill(accentColor))
            }
        }
    }

    // MARK: - Workouts Tab

    private var workoutsTab: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 16) {
                if let featured = viewModel.workoutPlans.first {
                    FeaturedWorkoutCard(plan: featured)
                }

                let otherPlans = Array(viewModel.workoutPlans.dropFirst())
                if !otherPlans.isEmpty {
                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(otherPlans, id: \.id) { plan in
                            WorkoutCardSmall(plan: plan)
                        }
                    }
                }

                if viewModel.workoutPlans.isEmpty {
                    emptyStateView(message: "Нет тренировочных планов")
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
        }
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

            ScrollView(showsIndicators: false) {
                if viewModel.exercises.isEmpty && !viewModel.isLoading {
                    emptyStateView(message: "Упражнения не найдены")
                        .padding(.top, 40)
                } else {
                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(viewModel.exercises, id: \.id) { exercise in
                            ExerciseCard(exercise: exercise)
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
                        ProgressView()
                            .padding()
                    }
                }
            }
        }
    }

    // MARK: - Empty State

    private func emptyStateView(message: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: "tray")
                .font(.system(size: 36))
                .foregroundColor(.gray.opacity(0.5))

            Text(message)
                .font(.system(size: 15))
                .foregroundColor(.gray)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 40)
    }
}
