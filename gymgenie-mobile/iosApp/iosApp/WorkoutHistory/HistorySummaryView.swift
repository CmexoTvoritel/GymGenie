import SwiftUI
import Shared

struct HistorySummaryView: View {
    let session: WorkoutSessionHistoryItem
    let onBack: () -> Void

    private var isCompleted: Bool { session.status == "COMPLETED" }
    private var durationMinutes: Int { Int(session.durationMinutes?.int32Value ?? 0) }
    private var estimatedCalories: Int { Int(session.totalExercises) * 45 + durationMinutes * 4 }

    private var dateStr: String {
        let date = Date(timeIntervalSince1970: session.startedAt / 1000.0)
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ru_RU")
        formatter.dateFormat = "d MMMM, HH:mm"
        return formatter.string(from: date)
    }

    var body: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: "Результат тренировки",
                showBackNavigation: true,
                onBackTap: onBack
            )

            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    Image("ic_train_finish")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 175, height: 175)
                    .padding(.top, 24)
                    .padding(.bottom, 12)

                    Text(isCompleted ? "Отличная работа!" : "Тренировка не завершена")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .padding(.bottom, 6)

                    Text(isCompleted
                         ? "Тренировка \"\(session.name)\" завершена"
                         : "Тренировка \"\(session.name)\"")
                        .font(.system(size: 17))
                        .foregroundColor(Palette.deepInk)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 16)

                    Text(dateStr)
                        .font(.system(size: 17))
                        .foregroundColor(Palette.deepInk)
                        .padding(.bottom, 28)

                    VStack(alignment: .leading, spacing: 12) {
                        Text("Результаты")
                            .font(.system(size: 19, weight: .medium))
                            .foregroundColor(Palette.deepInk)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 16)

                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                            statCard(iconName: "ic_timer", value: "\(durationMinutes) мин", label: "Общее время", bgColor: Palette.statTimerBg)
                            statCard(iconName: "ic_calories", value: "\(estimatedCalories) ккал", label: "Сожжено", bgColor: Palette.statCaloriesBg)
                            statCard(iconName: "ic_amount", value: isCompleted ? "\(session.totalExercises)" : "\(session.completedExercises) / \(session.totalExercises)", label: "Упражнений", bgColor: Palette.statAmountBg)
                            statCard(iconName: "ic_repeat", value: isCompleted ? "\(session.completedSets)" : "\(session.completedSets) / \(session.totalSets)", label: "Подходов", bgColor: Palette.statRepeatBg)
                        }
                        .padding(.horizontal, 16)
                    }
                    .padding(.bottom, 32)
                }
            }
        }
        .background(Palette.warmOffWhite.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
    }

    private func statCard(iconName: String, value: String, label: String, bgColor: Color = Palette.coralLight) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Circle()
                .fill(bgColor)
                .frame(width: 36, height: 36)
                .overlay(
                    Image(iconName)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 24, height: 24)
                )

            Text(value)
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(Palette.deepInk)

            Text(label)
                .font(.system(size: 14))
                .foregroundColor(.gray)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(RoundedRectangle(cornerRadius: 16).fill(.white))
        .shadow(color: .black.opacity(0.05), radius: 6, y: 2)
    }
}
