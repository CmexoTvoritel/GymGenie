import SwiftUI

struct WorkoutSummaryView: View {
    struct ExerciseSummaryData: Identifiable {
        let id = UUID()
        let name: String
        let sets: Int
        let reps: Int
        let maxWeight: Double
    }

    let planName: String
    let durationSeconds: Int
    let exerciseCount: Int
    let totalVolumeKg: Int
    var exerciseSummaries: [ExerciseSummaryData] = []
    var isSubmitting: Bool = false
    var isSubmitted: Bool = false
    var submitError: String? = nil
    var onRetry: () -> Void = {}
    let onDismiss: () -> Void

    private var durationMinutes: Int { durationSeconds / 60 }
    private var estimatedCalories: Int { exerciseCount * 45 + durationMinutes * 4 }

    var body: some View {
        VStack(spacing: 0) {
            toolbarView

            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    Circle()
                        .fill(Palette.coralLight)
                        .frame(width: 80, height: 80)
                        .overlay(
                            Text("🏆")
                                .font(.system(size: 36))
                        )
                        .padding(.top, 24)
                        .padding(.bottom, 16)

                    Text("Отличная работа!")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .padding(.bottom, 6)

                    Text("Тренировка \"\(planName)\" завершена")
                        .font(.system(size: 17))
                        .foregroundColor(Palette.deepInk)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 16)

                    Text(formattedDate)
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
                            statCard(
                                icon: "clock",
                                value: "\(durationMinutes) мин",
                                label: "Общее время"
                            )
                            statCard(
                                icon: "flame.fill",
                                value: "\(estimatedCalories) ккал",
                                label: "Сожжено"
                            )
                            statCard(
                                icon: "scalemass",
                                value: "\(totalVolumeKg) кг",
                                label: "Общий объём"
                            )
                            statCard(
                                icon: "dumbbell",
                                value: "\(exerciseCount)",
                                label: "Упражнений"
                            )
                        }
                        .padding(.horizontal, 16)
                    }
                    .padding(.bottom, 24)

                    submissionStatusView
                        .padding(.horizontal, 16)

                    if !exerciseSummaries.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Упражнения")
                                .font(.system(size: 19, weight: .medium))
                                .foregroundColor(Palette.deepInk)
                                .padding(.horizontal, 16)

                            ForEach(exerciseSummaries) { item in
                                exerciseRow(item)
                                    .padding(.horizontal, 16)
                            }
                        }
                        .padding(.bottom, 24)
                    }

                    Spacer(minLength: 20)
                }
            }
        }
        .background(Palette.warmOffWhite.ignoresSafeArea())
        .safeAreaInset(edge: .bottom) {
            Button(action: onDismiss) {
                Text("На главную")
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 54)
                    .background(RoundedRectangle(cornerRadius: 14).fill(Palette.coral))
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 16)
            .background(Palette.warmOffWhite)
        }
    }

    private var toolbarView: some View {
        HStack {
            Button(action: onDismiss) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
                    .frame(width: 40, height: 40)
                    .background(Circle().fill(Color.white))
                    .shadow(color: .black.opacity(0.08), radius: 4, y: 2)
            }
            Spacer()
            Text("Результат тренировки")
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(Palette.deepInk)
            Spacer()
            Color.clear
                .frame(width: 40, height: 40)
        }
        .padding(.horizontal, 16)
        .padding(.top, 12)
        .padding(.bottom, 24)
    }

    @ViewBuilder
    private var submissionStatusView: some View {
        if isSubmitting {
            HStack(spacing: 10) {
                ProgressView()
                    .scaleEffect(0.8)
                Text("Сохраняем тренировку...")
                    .font(.system(size: 13))
                    .foregroundColor(.gray)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, 8)
        } else if isSubmitted {
            EmptyView()
        } else if let error = submitError {
            VStack(alignment: .leading, spacing: 8) {
                Text(error)
                    .font(.system(size: 13))
                    .foregroundColor(Color(red: 0.898, green: 0.224, blue: 0.208))
                    .multilineTextAlignment(.leading)
                Button(action: onRetry) {
                    Text("Повторить")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(Palette.coral)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(Palette.coral.opacity(0.5), lineWidth: 1)
                        )
                }
                .buttonStyle(.plain)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, 8)
        } else {
            EmptyView()
        }
    }

    private func exerciseRow(_ item: ExerciseSummaryData) -> some View {
        HStack(spacing: 12) {
            Circle()
                .fill(Palette.coralLight)
                .frame(width: 36, height: 36)
                .overlay(
                    Image(systemName: "dumbbell")
                        .font(.system(size: 14))
                        .foregroundColor(Palette.coral)
                )

            VStack(alignment: .leading, spacing: 2) {
                Text(item.name)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(Palette.deepInk)

                let weightStr = item.maxWeight > 0 ? " · \(Int(item.maxWeight)) кг" : ""
                Text("\(item.sets) подходов · \(item.reps) повторений\(weightStr)")
                    .font(.system(size: 14))
                    .foregroundColor(.gray)
            }

            Spacer()
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 12).fill(.white))
        .shadow(color: .black.opacity(0.03), radius: 4, y: 2)
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

    private var formattedDate: String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ru_RU")
        formatter.dateFormat = "d MMMM, HH:mm"
        return formatter.string(from: Date())
    }
}
