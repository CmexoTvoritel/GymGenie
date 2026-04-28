import SwiftUI

/// End-of-workout summary.
///
/// Receives all data as plain immutable parameters so the view stays passive — the
/// total volume is computed by the wrapper and passed in, keeping this view free of
/// any presentation/business coupling.
struct WorkoutSummaryView: View {
    let planName: String
    let durationSeconds: Int
    let exerciseCount: Int
    let totalVolumeKg: Int
    /// Backend submit lifecycle — driven by the shared VM. Left at their default
    /// values so legacy call sites that don't care about submission state keep
    /// working without modification.
    var isSubmitting: Bool = false
    var isSubmitted: Bool = false
    var submitError: String? = nil
    var onRetry: () -> Void = {}
    let onDismiss: () -> Void

    private var durationMinutes: Int { durationSeconds / 60 }
    private var estimatedCalories: Int { exerciseCount * 45 + durationMinutes * 4 }

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 0) {
                // Top bar
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
                    Text("Сводка тренировки")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(Palette.deepInk)
                    Spacer()
                    Button(action: {}) {
                        Image(systemName: "trash")
                            .font(.system(size: 17))
                            .foregroundColor(.gray)
                            .frame(width: 40, height: 40)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .padding(.bottom, 24)

                // Trophy
                Circle()
                    .fill(Palette.coralLight)
                    .frame(width: 80, height: 80)
                    .overlay(
                        Text("🏆")
                            .font(.system(size: 36))
                    )
                    .padding(.bottom, 16)

                Text("Отличная работа!")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(Palette.deepInk)
                    .padding(.bottom, 6)

                Text("Тренировка \"\(planName)\" завершена")
                    .font(.system(size: 14))
                    .foregroundColor(.gray)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 16)

                Text(formattedDate)
                    .font(.system(size: 14))
                    .foregroundColor(.gray)
                    .padding(.bottom, 28)

                // Stats grid
                VStack(alignment: .leading, spacing: 12) {
                    Text("РЕЗУЛЬТАТЫ")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(.gray)
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

                Spacer(minLength: 20)
            }
        }
        .background(Palette.warmOffWhite.ignoresSafeArea())
        .safeAreaInset(edge: .bottom) {
            Button(action: onDismiss) {
                HStack(spacing: 8) {
                    Text("На главную")
                        .font(.system(size: 17, weight: .semibold))
                    Image(systemName: "arrow.right")
                        .font(.system(size: 16))
                }
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

    /// Backend-submission feedback. Renders nothing in the idle state so it
    /// doesn't add visual noise when no upload is in flight.
    @ViewBuilder
    private var submissionStatusView: some View {
        if isSubmitting {
            HStack(spacing: 8) {
                ProgressView()
                    .scaleEffect(0.8)
                Text("Сохраняем тренировку...")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 8)
        } else if isSubmitted {
            Label("Тренировка сохранена", systemImage: "checkmark.circle.fill")
                .font(.subheadline)
                .foregroundColor(.green)
                .frame(maxWidth: .infinity)
                .padding(.top, 8)
        } else if let error = submitError {
            VStack(spacing: 6) {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
                    .multilineTextAlignment(.center)
                Button(action: onRetry) {
                    Text("Повторить")
                        .font(.subheadline)
                        .foregroundColor(.accentColor)
                }
                .buttonStyle(.plain)
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 8)
        } else {
            EmptyView()
        }
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
                .font(.system(size: 12))
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
