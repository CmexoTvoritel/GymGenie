import SwiftUI
import Shared

// MARK: - Home screen (premium redesign)

struct HomeView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel = HomeViewModelWrapper()

    @State private var showingSession = false
    @State private var activeSession: ActiveWorkoutSession? = nil

    var body: some View {
        NavigationStack {
            ZStack {
                Palette.warmOffWhite.ignoresSafeArea()

                if viewModel.isLoading && viewModel.userProfile == nil {
                    loadingView
                } else if let error = viewModel.errorMessage, viewModel.userProfile == nil {
                    errorView(message: error)
                } else {
                    contentView
                }
            }
            .fullScreenCover(isPresented: $showingSession) {
                if let session = activeSession {
                    WorkoutSessionView(session: session)
                }
            }
            .onAppear {
                if viewModel.userProfile == nil {
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
        ScrollView(showsIndicators: false) {
            LazyVStack(alignment: .leading, spacing: 20) {
                HomeHeaderSection(
                    username: viewModel.username.isEmpty ? "друг" : viewModel.username,
                    streakDays: Int(viewModel.streakDays)
                )

                workoutOfTheDaySection

                ActivitiesSectionHeader(
                    title: "Активности",
                    subtitle: "Отметить вручную — без умных часов",
                    actionTitle: "Ещё",
                    destination: AnyView(ActivitiesView())
                )

                ActivityRingsCard()

                ActivityRowsCard()

                AddActivityButton(destination: AnyView(ActivityCatalogView()))

                AiTipCard()

                coursesSection

                Spacer().frame(height: 24)
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)
        }
    }

    // MARK: - Section: workout of the day

    private var workoutOfTheDaySection: some View {
        Group {
            if let plan = viewModel.activeWorkoutPlans.first {
                WorkoutOfTheDayCard(plan: plan) {
                    activeSession = ActiveWorkoutSession(
                        planId: plan.id,
                        planName: plan.name,
                        exercises: [],
                        restSeconds: 60
                    )
                    showingSession = true
                }
            } else {
                EmptyWorkoutCard()
            }
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

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    // TODO: replace with backend-provided courses data.
                    CourseCard(
                        badge: "НОВИЧОК",
                        title: "Силовая база",
                        trainer: "Елена В.",
                        ctaTitle: "Бесплатно",
                        isPro: false
                    )
                    CourseCard(
                        badge: "ИНТЕНСИВ",
                        title: "HIIT каждый день",
                        trainer: "Борис Л.",
                        ctaTitle: "PRO",
                        isPro: true
                    )
                }
                .padding(.horizontal, 2)
            }
        }
    }
}

// MARK: - Header

private struct HomeHeaderSection: View {
    let username: String
    let streakDays: Int

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            // Avatar
            ZStack {
                Circle().fill(Palette.softCard)
                Text(String(username.prefix(1)).uppercased())
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(Palette.deepInk)
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

// MARK: - Workout of the day

private struct WorkoutOfTheDayCard: View {
    let plan: WorkoutPlanShortResponse
    let onStart: () -> Void

    // TODO: replace duration/calories with backend-provided values when exposed.
    private let duration = 42
    private let calories = 320

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("ТРЕНИРОВКА ДНЯ · ДЕНЬ \(max(plan.daysCount, 1))")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(Palette.accentOrange)
                        .tracking(0.5)

                    Text(plan.name)
                        .font(.system(size: 26, weight: .heavy))
                        .foregroundColor(Palette.deepInk)
                        .lineLimit(2)

                    Text(plan.description ?? "Упражнения для всего тела")
                        .font(.system(size: 14))
                        .foregroundColor(Palette.mutedText)
                        .lineLimit(2)
                }

                Spacer()

                ZStack {
                    Circle().fill(Palette.accentOrange.opacity(0.15))
                        .frame(width: 72, height: 72)
                    Text("🏋️")
                        .font(.system(size: 32))
                }
            }

            HStack(spacing: 16) {
                StatPill(value: "\(plan.daysCount)", label: "УПРАЖН.")
                StatPill(value: "\(duration)", label: "МИН")
                StatPill(value: "\(calories)", label: "ККАЛ СЖЕЧЬ")
            }

            Button(action: onStart) {
                HStack(spacing: 8) {
                    Image(systemName: "play.fill").font(.system(size: 14, weight: .bold))
                    Text("Начать тренировку")
                        .font(.system(size: 16, weight: .bold))
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(RoundedRectangle(cornerRadius: 12).fill(Palette.accentOrange))
            }
        }
        .padding(24)
        .background(RoundedRectangle(cornerRadius: 24).fill(Palette.softCard))
    }
}

private struct StatPill: View {
    let value: String
    let label: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value)
                .font(.system(size: 18, weight: .heavy))
                .foregroundColor(Palette.deepInk)
            Text(label)
                .font(.system(size: 10, weight: .semibold))
                .foregroundColor(Palette.mutedText)
                .tracking(0.3)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct EmptyWorkoutCard: View {
    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: "figure.strengthtraining.traditional")
                .font(.system(size: 28))
                .foregroundColor(Palette.accentOrange.opacity(0.6))
            Text("Нет активных тренировок")
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(Palette.deepInk)
            Text("Создай или выбери план, чтобы начать")
                .font(.system(size: 13))
                .foregroundColor(Palette.mutedText)
        }
        .frame(maxWidth: .infinity)
        .padding(28)
        .background(RoundedRectangle(cornerRadius: 24).fill(Palette.softCard))
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

            if let actionTitle = actionTitle, let destination = destination {
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
            }
        }
    }
}

// MARK: - Activity rings card

struct ActivityRingsCard: View {
    // TODO: replace with backend data when Activity feed is exposed.
    private let movementProgress: CGFloat = 420.0 / 600.0
    private let activityProgress: CGFloat = 28.0 / 45.0
    private let warmupProgress: CGFloat = 6.0 / 12.0

    var body: some View {
        HStack(alignment: .center, spacing: 20) {
            RingsView(
                outer: (Palette.ringMovement, movementProgress),
                middle: (Palette.ringActivity, activityProgress),
                inner: (Palette.ringWarmups, warmupProgress)
            )
            .frame(width: 132, height: 132)

            VStack(alignment: .leading, spacing: 12) {
                RingLegendRow(color: Palette.ringMovement, title: "ДВИЖЕНИЕ", value: "420/600 ккал")
                RingLegendRow(color: Palette.ringActivity, title: "АКТИВНОСТЬ", value: "28/45 мин")
                RingLegendRow(color: Palette.ringWarmups, title: "РАЗМИНКИ", value: "6/12 раз")
            }

            Spacer(minLength: 0)
        }
        .padding(24)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 24).fill(Palette.softCard))
    }
}

private struct RingsView: View {
    let outer: (Color, CGFloat)
    let middle: (Color, CGFloat)
    let inner: (Color, CGFloat)

    private let strokeWidth: CGFloat = 12

    var body: some View {
        ZStack {
            ring(color: outer.0, progress: outer.1, inset: 0)
            ring(color: middle.0, progress: middle.1, inset: strokeWidth + 4)
            ring(color: inner.0, progress: inner.1, inset: 2 * (strokeWidth + 4))
        }
    }

    private func ring(color: Color, progress: CGFloat, inset: CGFloat) -> some View {
        ZStack {
            Circle()
                .stroke(color.opacity(0.18), lineWidth: strokeWidth)
            Circle()
                .trim(from: 0, to: min(max(progress, 0), 1))
                .stroke(
                    color,
                    style: StrokeStyle(lineWidth: strokeWidth, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))
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
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(Palette.mutedText)
                    .tracking(0.4)
                Text(value)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
            }
        }
    }
}

// MARK: - Activity rows card

struct ActivityRowsCard: View {
    // TODO: replace with backend data when Activity feed is exposed.
    private let rows: [ActivityRowData] = [
        ActivityRowData(emoji: "💧", title: "Вода", progressLabel: "1.2 / 2.5 л", progress: 0.48, color: Palette.activityWater),
        ActivityRowData(emoji: "🚶", title: "Ходьба", progressLabel: "20 / 45 мин", progress: 0.44, color: Palette.activityWalking),
        ActivityRowData(emoji: "🤸", title: "Растяжка", progressLabel: "0 / 1 раз", progress: 0.0, color: Palette.activityStretching),
        ActivityRowData(emoji: "🧘", title: "Медитация", progressLabel: "5 / 10 мин", progress: 0.5, color: Palette.activityMeditation),
    ]

    var body: some View {
        VStack(spacing: 14) {
            ForEach(rows) { row in
                ActivityRow(data: row)
            }
        }
        .padding(20)
        .background(RoundedRectangle(cornerRadius: 24).fill(Palette.softCard))
    }
}

struct ActivityRowData: Identifiable {
    let id = UUID()
    let emoji: String
    let title: String
    let progressLabel: String
    let progress: CGFloat
    let color: Color
}

private struct ActivityRow: View {
    let data: ActivityRowData

    var body: some View {
        HStack(spacing: 14) {
            ZStack {
                Circle().fill(data.color.opacity(0.18))
                Text(data.emoji).font(.system(size: 18))
            }
            .frame(width: 36, height: 36)

            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(data.title)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                    Spacer()
                    Text(data.progressLabel)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(Palette.mutedText)
                }

                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Capsule().fill(data.color.opacity(0.15))
                        Capsule()
                            .fill(data.color)
                            .frame(width: geo.size.width * min(max(data.progress, 0), 1))
                    }
                }
                .frame(height: 6)
            }

            Button(action: {}) {
                Image(systemName: "plus")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 30, height: 30)
                    .background(Circle().fill(Palette.deepInk))
            }
        }
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

// MARK: - Course card

private struct CourseCard: View {
    let badge: String
    let title: String
    let trainer: String
    let ctaTitle: String
    let isPro: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(badge)
                .font(.system(size: 10, weight: .heavy))
                .foregroundColor(isPro ? .white : Palette.deepInk)
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(
                    Capsule().fill(
                        isPro
                            ? Color.white.opacity(0.18)
                            : Palette.accentOrange.opacity(0.18)
                    )
                )

            Spacer(minLength: 20)

            Text(title)
                .font(.system(size: 20, weight: .heavy))
                .foregroundColor(isPro ? .white : Palette.deepInk)
                .lineLimit(2)
                .fixedSize(horizontal: false, vertical: true)

            Text(trainer)
                .font(.system(size: 13))
                .foregroundColor(isPro ? .white.opacity(0.7) : Palette.mutedText)

            HStack {
                Spacer()
                HStack(spacing: 6) {
                    Text(ctaTitle)
                        .font(.system(size: 13, weight: .bold))
                    Text("›")
                }
                .foregroundColor(isPro ? .white : .white)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(
                    Capsule().fill(isPro ? Palette.accentOrange : Palette.deepInk)
                )
            }
        }
        .padding(20)
        .frame(width: 240, height: 200, alignment: .topLeading)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(isPro ? Palette.deepInk : Palette.warmOffWhite)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .strokeBorder(Palette.deepInk.opacity(isPro ? 0 : 0.06), lineWidth: 1)
        )
    }
}
