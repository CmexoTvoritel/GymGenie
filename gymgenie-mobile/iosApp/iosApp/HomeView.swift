import SwiftUI
import Shared

struct HomeView: View {
    @StateObject private var viewModel = HomeViewModel()

    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890)
    private let greenColor = Color(red: 0.180, green: 0.800, blue: 0.443)
    private let backgroundColor = Color(red: 0.961, green: 0.969, blue: 0.980)
    private let darkColor = Color(red: 0.102, green: 0.102, blue: 0.180)

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
                // Header
                headerSection

                // Daily challenge
                dailyChallengeCard

                // Workout plan section
                workoutPlanSection

                // Activities section
                activitiesSection

                // Meal plan section
                mealPlanSection

                Spacer().frame(height: 20)
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
        }
    }

    // MARK: - Header

    private var headerSection: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("Привет, \(viewModel.userProfile?.username ?? "")!")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(darkColor)

                HStack(spacing: 4) {
                    Text("\u{1F525}")
                    Text("5 дней подряд")
                        .font(.system(size: 13))
                        .foregroundColor(.gray)
                }
            }

            Spacer()

            Button(action: {}) {
                Image(systemName: "bell")
                    .font(.system(size: 20))
                    .foregroundColor(darkColor)
                    .frame(width: 40, height: 40)
                    .background(
                        Circle().fill(.white)
                    )
                    .shadow(color: Color.black.opacity(0.06), radius: 4, y: 2)
            }
        }
    }

    // MARK: - Daily Challenge

    private var dailyChallengeCard: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 20)
                .fill(
                    LinearGradient(
                        gradient: Gradient(colors: [
                            accentColor,
                            accentColor.opacity(0.7),
                        ]),
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )

            HStack {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Ежедневный челлендж")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.white)

                    Text("Выполните задание дня и получите бонус")
                        .font(.system(size: 13))
                        .foregroundColor(.white.opacity(0.85))

                    Button(action: {}) {
                        HStack(spacing: 4) {
                            Text("Начать")
                                .font(.system(size: 14, weight: .semibold))
                            Image(systemName: "arrow.right")
                                .font(.system(size: 12, weight: .semibold))
                        }
                        .foregroundColor(accentColor)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(
                            Capsule().fill(.white)
                        )
                    }
                    .padding(.top, 4)
                }

                Spacer()

                Image(systemName: "flame.fill")
                    .font(.system(size: 48))
                    .foregroundColor(.white.opacity(0.3))
            }
            .padding(20)
        }
        .frame(height: 160)
    }

    // MARK: - Workout Plan

    private var workoutPlanSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Тренировочный план")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(darkColor)
                Spacer()
            }

            if viewModel.activeWorkoutPlans.isEmpty {
                emptyPlanCard
            } else {
                ForEach(viewModel.activeWorkoutPlans, id: \.id) { plan in
                    workoutPlanCard(plan: plan)
                }
            }

            Button(action: {}) {
                HStack {
                    Text("Перейти к тренировкам")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(accentColor)
                    Image(systemName: "arrow.right")
                        .font(.system(size: 12))
                        .foregroundColor(accentColor)
                }
            }
        }
    }

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

    private func workoutPlanCard(plan: WorkoutPlanShortResponse) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Today plan")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Capsule().fill(accentColor))

                Spacer()

                Text("09:00")
                    .font(.system(size: 13))
                    .foregroundColor(.gray)
            }

            Text(plan.name)
                .font(.system(size: 17, weight: .bold))
                .foregroundColor(darkColor)

            if let description = plan.description_ {
                Text(description)
                    .font(.system(size: 13))
                    .foregroundColor(.gray)
                    .lineLimit(2)
            }

            HStack(spacing: 12) {
                Button(action: {}) {
                    Text("Детали")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(accentColor)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 8)
                        .background(
                            Capsule().stroke(accentColor, lineWidth: 1.5)
                        )
                }

                Button(action: {}) {
                    Text("Начать")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 8)
                        .background(
                            Capsule().fill(accentColor)
                        )
                }
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(.white)
        )
        .shadow(color: Color.black.opacity(0.04), radius: 4, y: 2)
    }

    // MARK: - Activities

    private var activitiesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Активности")
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(darkColor)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    activityPill(title: "Бег", icon: "figure.run", color: .orange)
                    activityPill(title: "Йога", icon: "figure.yoga", color: .purple)
                    activityPill(title: "Силовая", icon: "dumbbell", color: accentColor)
                    activityPill(title: "Кардио", icon: "heart.circle", color: .red)
                    activityPill(title: "Растяжка", icon: "figure.flexibility", color: greenColor)
                }
            }
        }
    }

    private func activityPill(title: String, icon: String, color: Color) -> some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 16))
                .foregroundColor(color)

            Text(title)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(darkColor)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(
            Capsule()
                .fill(color.opacity(0.1))
        )
    }

    // MARK: - Meal Plan

    private var mealPlanSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("План питания")
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(darkColor)

            let meals = [
                ("Завтрак", "sunrise", Color.orange),
                ("Обед", "sun.max", accentColor),
                ("Ужин", "moon", Color.purple),
                ("Перекус", "leaf", greenColor),
            ]

            ForEach(meals, id: \.0) { meal in
                mealCard(title: meal.0, icon: meal.1, color: meal.2)
            }
        }
    }

    private func mealCard(title: String, icon: String, color: Color) -> some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(color.opacity(0.1))
                    .frame(width: 44, height: 44)

                Image(systemName: icon)
                    .font(.system(size: 18))
                    .foregroundColor(color)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(darkColor)

                Text("Нажмите для деталей")
                    .font(.system(size: 12))
                    .foregroundColor(.gray)
            }

            Spacer()

            Button(action: {}) {
                Text("Детали")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(accentColor)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 6)
                    .background(
                        Capsule().fill(accentColor.opacity(0.1))
                    )
            }
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 14)
                .fill(.white)
        )
        .shadow(color: Color.black.opacity(0.03), radius: 3, y: 1)
    }
}
