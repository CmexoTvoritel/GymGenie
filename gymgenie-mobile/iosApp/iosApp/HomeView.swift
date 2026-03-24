import SwiftUI
import Shared

struct HomeView: View {
    @StateObject private var viewModel = HomeViewModelWrapper()

    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890)
    private let backgroundColor = Color(red: 0.961, green: 0.969, blue: 0.980)

    var body: some View {
        ZStack {
            backgroundColor.edgesIgnoringSafeArea(.all)

            if viewModel.isLoading && viewModel.userProfile == nil {
                loadingView
            } else if let error = viewModel.errorMessage, viewModel.userProfile == nil {
                errorView(message: error)
            } else {
                contentView
            }
        }
        .onAppear {
            if viewModel.userProfile == nil {
                viewModel.loadData()
            }
        }
    }

    // MARK: - Loading

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.2)
            Text("Загрузка...")
                .font(.system(size: 15))
                .foregroundColor(.gray)
        }
    }

    // MARK: - Error

    private func errorView(message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 40))
                .foregroundColor(.orange)

            Text(message)
                .font(.system(size: 15))
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            Button(action: viewModel.retry) {
                Text("Повторить")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 32)
                    .padding(.vertical, 12)
                    .background(
                        Capsule().fill(accentColor)
                    )
            }
        }
    }

    // MARK: - Content

    private var contentView: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 20) {
                HomeHeaderView(
                    username: viewModel.username,
                    streakDays: viewModel.streakDays
                )

                DailyChallengeCard()

                SectionHeader(title: "Тренировочный план")

                if viewModel.activeWorkoutPlans.isEmpty {
                    emptyPlanCard
                } else {
                    ForEach(viewModel.activeWorkoutPlans, id: \.id) { plan in
                        WorkoutPlanCard(plan: plan)
                    }
                }

                SectionHeader(
                    title: "Перейти к тренировкам",
                    actionTitle: nil
                )

                SectionHeader(title: "Активности")
                ActivityTimelineView()

                SectionHeader(title: "План питания")

                let meals: [(String, String, Color)] = [
                    ("Завтрак", "sunrise", Color.orange),
                    ("Обед", "sun.max", accentColor),
                    ("Ужин", "moon", Color.purple),
                    ("Перекус", "leaf", Color(red: 0.180, green: 0.800, blue: 0.443)),
                ]

                ForEach(meals, id: \.0) { meal in
                    MealPlanCard(title: meal.0, icon: meal.1, color: meal.2)
                }

                Spacer().frame(height: 20)
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
        }
    }

    // MARK: - Empty Plan

    private var emptyPlanCard: some View {
        VStack(spacing: 12) {
            Image(systemName: "calendar.badge.plus")
                .font(.system(size: 28))
                .foregroundColor(accentColor.opacity(0.5))

            Text("Нет активных планов")
                .font(.system(size: 14))
                .foregroundColor(.gray)
        }
        .frame(maxWidth: .infinity)
        .padding(24)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(.white)
        )
        .shadow(color: Color.black.opacity(0.04), radius: 4, y: 2)
    }
}
