import SwiftUI
import Shared

struct ExerciseInfoSheet: View {
    let exerciseId: String
    @StateObject private var viewModel = ExerciseDetailViewModelWrapper()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationView {
            ZStack {
                if viewModel.isLoading && viewModel.exercise == nil {
                    ProgressView()
                        .scaleEffect(1.2)
                        .tint(Palette.coral)
                } else if let error = viewModel.errorMessage, viewModel.exercise == nil {
                    errorContent(message: error)
                } else if let exercise = viewModel.exercise {
                    exerciseContent(exercise)
                }
            }
            .background(Palette.warmOffWhite.ignoresSafeArea())
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(Palette.deepInk)
                    }
                }
                ToolbarItem(placement: .principal) {
                    Text("Информация")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(Palette.deepInk)
                }
            }
        }
        .onAppear { viewModel.load(id: exerciseId) }
    }

    private func errorContent(message: String) -> some View {
        VStack(spacing: 12) {
            Text(message)
                .font(.system(size: 14))
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            Button(action: { viewModel.retry(id: exerciseId) }) {
                Text("Повторить")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Capsule().fill(Palette.coral))
            }
            .buttonStyle(.plain)
        }
    }

    private func exerciseContent(_ exercise: ExerciseDetailResponse) -> some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(exercise.nameRu)
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                    if !exercise.nameEn.isEmpty {
                        Text(exercise.nameEn)
                            .font(.system(size: 14))
                            .foregroundColor(.gray)
                    }
                }

                if !exercise.muscleGroup.isEmpty {
                    HStack(spacing: 6) {
                        Text(muscleGroupRu(exercise.muscleGroup))
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Capsule().fill(Palette.coral))
                    }
                }

                if let description = exercise.description_, !description.isEmpty {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Описание")
                            .font(.system(size: 17, weight: .bold))
                            .foregroundColor(Palette.deepInk)
                        Text(description)
                            .font(.system(size: 14))
                            .foregroundColor(Palette.deepInk)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }

                if !exercise.instructions.isEmpty {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Техника выполнения")
                            .font(.system(size: 17, weight: .bold))
                            .foregroundColor(Palette.deepInk)
                        ForEach(Array(exercise.instructions.enumerated()), id: \.offset) { idx, step in
                            HStack(alignment: .top, spacing: 10) {
                                Text("\(idx + 1)")
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundColor(.white)
                                    .frame(width: 24, height: 24)
                                    .background(Circle().fill(Palette.coral))
                                Text(step)
                                    .font(.system(size: 14))
                                    .foregroundColor(Palette.deepInk)
                                    .fixedSize(horizontal: false, vertical: true)
                                Spacer(minLength: 0)
                            }
                        }
                    }
                }

                if let tip = exercise.techniqueTip, !tip.isEmpty {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Совет по технике")
                            .font(.system(size: 17, weight: .bold))
                            .foregroundColor(Palette.deepInk)
                        Text(tip)
                            .font(.system(size: 14))
                            .foregroundColor(Palette.deepInk)
                            .padding(14)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(Palette.coral.opacity(0.08))
                            )
                    }
                }

                if !exercise.equipment.isEmpty {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Оборудование")
                            .font(.system(size: 17, weight: .bold))
                            .foregroundColor(Palette.deepInk)
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(exercise.equipment, id: \.self) { item in
                                    Text(item)
                                        .font(.system(size: 12))
                                        .foregroundColor(Palette.deepInk)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .background(
                                            Capsule()
                                                .stroke(Palette.deepInk.opacity(0.2), lineWidth: 1)
                                        )
                                }
                            }
                        }
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
    }

    private func muscleGroupRu(_ g: String) -> String {
        switch g.uppercased() {
        case "CHEST": return "Грудь"
        case "BACK": return "Спина"
        case "SHOULDERS": return "Плечи"
        case "BICEPS": return "Бицепс"
        case "TRICEPS": return "Трицепс"
        case "FOREARMS": return "Предплечья"
        case "ABS": return "Пресс"
        case "QUADRICEPS": return "Квадрицепс"
        case "HAMSTRINGS": return "Бицепс бедра"
        case "CALVES": return "Икры"
        case "GLUTES": return "Ягодицы"
        case "CARDIO": return "Кардио"
        case "FULL_BODY": return "Всё тело"
        default: return g
        }
    }
}
