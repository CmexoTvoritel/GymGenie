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
                    Circle()
                        .fill(Palette.coralLight)
                        .frame(width: 80, height: 80)
                        .overlay(
                            Text(isCompleted ? "🏆" : "⚠️")
                                .font(.system(size: 36))
                        )
                        .padding(.top, 24)
                        .padding(.bottom, 16)

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
                            statCard(icon: "clock", value: "\(durationMinutes) мин", label: "Общее время")
                            statCard(icon: "flame.fill", value: "\(estimatedCalories) ккал", label: "Сожжено")
                            statCard(icon: "dumbbell", value: "\(session.totalExercises)", label: "Упражнений")
                            statCard(icon: "checkmark.circle", value: "\(session.completedSets)", label: "Подходов")
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

    private func statCard(icon: String, value: String, label: String) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Circle()
                .fill(Palette.coralLight)
                .frame(width: 36, height: 36)
                .overlay(
                    Image(systemName: icon)
                        .font(.system(size: 16))
                        .foregroundColor(Palette.coral)
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
