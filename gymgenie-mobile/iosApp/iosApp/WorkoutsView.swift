import SwiftUI
import Shared

struct WorkoutsView: View {
    @StateObject private var viewModel = WorkoutsViewModel()

    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890)
    private let backgroundColor = Color(red: 0.961, green: 0.969, blue: 0.980)
    private let darkColor = Color(red: 0.102, green: 0.102, blue: 0.180)
    private let greenColor = Color(red: 0.180, green: 0.800, blue: 0.443)

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12),
    ]

    var body: some View {
        VStack(spacing: 0) {
            // Header
            headerSection

            // Segmented control
            segmentedControl

            // Content
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
                }
            }
        }
        .background(backgroundColor)
        .onAppear {
            viewModel.loadWorkoutPlans()
        }
    }

    private var contentIsEmpty: Bool {
        switch viewModel.selectedTab {
        case .workouts:
            return viewModel.workoutPlans.isEmpty
        case .exercises:
            return viewModel.exercises.isEmpty
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

    // MARK: - Segmented Control

    private var segmentedControl: some View {
        HStack(spacing: 0) {
            segmentButton(title: "Тренировки", tab: .workouts)
            segmentButton(title: "Упражнения", tab: .exercises)
        }
        .padding(4)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color.gray.opacity(0.1))
        )
        .padding(.horizontal, 20)
        .padding(.vertical, 8)
    }

    private func segmentButton(title: String, tab: WorkoutsTab) -> some View {
        Button(action: {
            withAnimation(.easeInOut(duration: 0.2)) {
                viewModel.selectTab(tab)
            }
        }) {
            Text(title)
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(viewModel.selectedTab == tab ? .white : .gray)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .background(
                    RoundedRectangle(cornerRadius: 10)
                        .fill(viewModel.selectedTab == tab ? accentColor : Color.clear)
                )
        }
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
                // Featured plan
                if let featured = viewModel.workoutPlans.first {
                    featuredPlanCard(plan: featured)
                }

                // Other plans grid
                let otherPlans = Array(viewModel.workoutPlans.dropFirst())
                if !otherPlans.isEmpty {
                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(otherPlans, id: \.id) { plan in
                            smallPlanCard(plan: plan)
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

    private func featuredPlanCard(plan: WorkoutPlanShortResponse) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Today plan")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Capsule().fill(Color.white.opacity(0.3)))

                Spacer()

                Text("09:00")
                    .font(.system(size: 13))
                    .foregroundColor(.white.opacity(0.8))
            }

            Text(plan.name)
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(.white)

            if let description = plan.description_ {
                Text(description)
                    .font(.system(size: 13))
                    .foregroundColor(.white.opacity(0.8))
                    .lineLimit(2)
            }

            HStack {
                Text("\(plan.daysCount) дн.")
                    .font(.system(size: 12))
                    .foregroundColor(.white.opacity(0.7))

                Spacer()
            }

            HStack(spacing: 12) {
                Button(action: {}) {
                    Text("Детали")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 8)
                        .background(
                            Capsule().stroke(.white, lineWidth: 1.5)
                        )
                }

                Button(action: {}) {
                    Text("Начать")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(accentColor)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 8)
                        .background(
                            Capsule().fill(.white)
                        )
                }
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 20)
                .fill(
                    LinearGradient(
                        gradient: Gradient(colors: [accentColor, accentColor.opacity(0.7)]),
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
        )
    }

    private func smallPlanCard(plan: WorkoutPlanShortResponse) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("План")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(accentColor)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Capsule().fill(accentColor.opacity(0.1)))

                Spacer()
            }

            Text(plan.name)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(darkColor)
                .lineLimit(2)

            Text("\(plan.daysCount) дн.")
                .font(.system(size: 12))
                .foregroundColor(.gray)

            Spacer(minLength: 4)

            HStack(spacing: 8) {
                Button(action: {}) {
                    Text("Детали")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(accentColor)
                }

                Spacer()

                Button(action: {}) {
                    Text("Начать")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 5)
                        .background(Capsule().fill(accentColor))
                }
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, minHeight: 140, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 14)
                .fill(.white)
        )
        .shadow(color: Color.black.opacity(0.04), radius: 4, y: 2)
    }

    // MARK: - Exercises Tab

    private var exercisesTab: some View {
        VStack(spacing: 0) {
            // Search bar
            searchBar

            ScrollView(showsIndicators: false) {
                if viewModel.exercises.isEmpty && !viewModel.isLoading {
                    emptyStateView(message: "Упражнения не найдены")
                        .padding(.top, 40)
                } else {
                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(viewModel.exercises, id: \.id) { exercise in
                            exerciseCard(exercise: exercise)
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

    private var searchBar: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.gray)

            TextField("Поиск упражнений...", text: $viewModel.searchQuery)
                .font(.system(size: 15))
                .autocapitalization(.none)
                .disableAutocorrection(true)
                .onSubmit {
                    viewModel.searchExercises()
                }
                .onChange(of: viewModel.searchQuery) { newValue in
                    if newValue.isEmpty {
                        viewModel.loadExercises(reset: true)
                    }
                }

            if !viewModel.searchQuery.isEmpty {
                Button(action: {
                    viewModel.searchQuery = ""
                    viewModel.loadExercises(reset: true)
                }) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.gray)
                }
            }
        }
        .padding(.horizontal, 14)
        .frame(height: 44)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(.white)
        )
        .padding(.horizontal, 20)
        .padding(.vertical, 8)
    }

    private func exerciseCard(exercise: ExerciseShortResponse) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            // Image placeholder
            ZStack(alignment: .topLeading) {
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.gray.opacity(0.15))
                    .frame(height: 100)
                    .overlay(
                        Image(systemName: "figure.strengthtraining.traditional")
                            .font(.system(size: 28))
                            .foregroundColor(.gray.opacity(0.4))
                    )

                if !exercise.muscleGroup.isEmpty {
                    Text(exercise.muscleGroup)
                        .font(.system(size: 9, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Capsule().fill(accentColor))
                        .padding(8)
                }
            }

            Text(exercise.nameRu)
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(darkColor)
                .lineLimit(2)

            if let duration = exercise.durationMinutes {
                HStack(spacing: 4) {
                    Image(systemName: "clock")
                        .font(.system(size: 10))
                        .foregroundColor(.gray)
                    Text("\(duration) мин")
                        .font(.system(size: 11))
                        .foregroundColor(.gray)
                }
            }

            if !exercise.difficultyLevel.isEmpty {
                Text(exercise.difficultyLevel)
                    .font(.system(size: 10))
                    .foregroundColor(.gray)
            }
        }
        .padding(10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 14)
                .fill(.white)
        )
        .shadow(color: Color.black.opacity(0.04), radius: 4, y: 2)
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
