import SwiftUI

struct WorkoutExerciseItem: Identifiable {
    let id = UUID()
    var name: String
    var sets: Int = 3
    var reps: Int = 12
}

struct CreateWorkoutView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var workoutName = ""
    @State private var useTimer = true
    @State private var restSeconds = 45
    @State private var exercises: [WorkoutExerciseItem] = [
        WorkoutExerciseItem(name: "Жим лёжа"),
        WorkoutExerciseItem(name: "Приседания"),
    ]

    private let greenColor = Color(red: 0.133, green: 0.773, blue: 0.369)
    private let backgroundColor = Color(red: 0.961, green: 0.969, blue: 0.980)
    private let darkColor = Color(red: 0.102, green: 0.102, blue: 0.180)

    private var estimatedMinutes: Int {
        exercises.count * (exercises.first.map { $0.sets * 2 } ?? 6) + exercises.count * (restSeconds / 60 + 1)
    }

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottom) {
                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 24) {

                        fieldSection(label: "НАЗВАНИЕ") {
                            TextField("Напр. Силовая тренировка", text: $workoutName)
                                .font(.system(size: 15))
                                .padding(14)
                                .background(RoundedRectangle(cornerRadius: 12).fill(.white))
                                .shadow(color: .black.opacity(0.04), radius: 4, y: 2)
                        }

                        aiBanner

                        fieldSection(label: "НАСТРОЙКИ ВРЕМЕНИ") {
                            VStack(spacing: 0) {

                                HStack(spacing: 12) {
                                    Circle()
                                        .fill(Color.blue.opacity(0.1))
                                        .frame(width: 40, height: 40)
                                        .overlay(
                                            Image(systemName: "timer")
                                                .font(.system(size: 18))
                                                .foregroundColor(.blue)
                                        )
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text("Общее время")
                                            .font(.system(size: 15, weight: .medium))
                                            .foregroundColor(darkColor)
                                        Text("Длительность тренировки")
                                            .font(.system(size: 12))
                                            .foregroundColor(.gray)
                                    }
                                    Spacer()
                                    Toggle("", isOn: $useTimer)
                                        .tint(greenColor)
                                }
                                .padding(16)

                                if useTimer {
                                    Divider().padding(.horizontal, 16)
                                    HStack {
                                        Text("Расчётное время:")
                                            .font(.system(size: 14))
                                            .foregroundColor(.gray)
                                        Spacer()
                                        Text("\(estimatedMinutes) мин")
                                            .font(.system(size: 16, weight: .bold))
                                            .foregroundColor(darkColor)
                                    }
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 12)
                                }

                                Divider().padding(.horizontal, 16)

                                HStack(spacing: 12) {
                                    Circle()
                                        .fill(greenColor.opacity(0.1))
                                        .frame(width: 40, height: 40)
                                        .overlay(
                                            Image(systemName: "clock.arrow.circlepath")
                                                .font(.system(size: 18))
                                                .foregroundColor(greenColor)
                                        )
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text("Отдых")
                                            .font(.system(size: 15, weight: .medium))
                                            .foregroundColor(darkColor)
                                        Text("Между подходами")
                                            .font(.system(size: 12))
                                            .foregroundColor(.gray)
                                    }
                                    Spacer()
                                    Text("\(restSeconds) сек")
                                        .font(.system(size: 14, weight: .semibold))
                                        .foregroundColor(.white)
                                        .padding(.horizontal, 14)
                                        .padding(.vertical, 7)
                                        .background(Capsule().fill(greenColor))
                                }
                                .padding(16)
                            }
                            .background(RoundedRectangle(cornerRadius: 16).fill(.white))
                            .shadow(color: .black.opacity(0.04), radius: 6, y: 2)
                        }

                        exercisesSection
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                    .padding(.bottom, 100)
                }
                .background(backgroundColor)

                Button(action: { dismiss() }) {
                    HStack(spacing: 8) {
                        Text("Создать план")
                            .font(.system(size: 17, weight: .semibold))
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 18))
                    }
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 54)
                    .background(RoundedRectangle(cornerRadius: 14).fill(greenColor))
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 24)
                .background(.white.shadow(.drop(radius: 8, y: -2)))
            }
            .navigationTitle("Новый план")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "chevron.left")
                            .foregroundColor(darkColor)
                    }
                }
            }
        }
    }

    private var aiBanner: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(Color.blue.opacity(0.1))
                .frame(width: 44, height: 44)
                .overlay(
                    Image(systemName: "sparkles")
                        .font(.system(size: 20))
                        .foregroundColor(.blue)
                )
            VStack(alignment: .leading, spacing: 2) {
                Text("Попробовать ИИ")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.180))
                Text("Составить тренировку с ИИ")
                    .font(.system(size: 13))
                    .foregroundColor(.gray)
            }
            Spacer()
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 14)
                .fill(Color.blue.opacity(0.06))
                .overlay(
                    RoundedRectangle(cornerRadius: 14)
                        .stroke(Color.blue.opacity(0.15), lineWidth: 1)
                )
        )
    }

    private var exercisesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("УПРАЖНЕНИЯ (\(exercises.count))")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(.gray)
                Spacer()
                Button(action: {
                    exercises.append(WorkoutExerciseItem(name: "Новое упражнение"))
                }) {
                    HStack(spacing: 4) {
                        Image(systemName: "plus.circle.fill")
                            .foregroundColor(greenColor)
                        Text("Добавить")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(greenColor)
                    }
                }
            }

            VStack(spacing: 8) {
                ForEach(exercises) { exercise in
                    exerciseRow(exercise)
                }

                Button(action: {}) {
                    HStack(spacing: 8) {
                        Image(systemName: "square.grid.2x2")
                            .font(.system(size: 16))
                        Text("Выбрать из каталога")
                            .font(.system(size: 14, weight: .medium))
                    }
                    .foregroundColor(.gray)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color.gray.opacity(0.3), style: StrokeStyle(lineWidth: 1.5, dash: [6]))
                    )
                }
            }
        }
    }

    private func exerciseRow(_ exercise: WorkoutExerciseItem) -> some View {
        HStack(spacing: 0) {
            Circle()
                .fill(greenColor.opacity(0.1))
                .frame(width: 40, height: 40)
                .overlay(
                    Image(systemName: "dumbbell")
                        .font(.system(size: 16))
                        .foregroundColor(greenColor)
                )

            VStack(alignment: .leading, spacing: 2) {
                Text(exercise.name)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.180))
                Text("\(exercise.sets) подхода • \(exercise.reps) повторов")
                    .font(.system(size: 12))
                    .foregroundColor(.gray)
            }
            .padding(.leading, 12)
            .frame(maxWidth: .infinity, alignment: .leading)

            Button(action: {}) {
                Image(systemName: "pencil")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.gray)
                    .frame(width: 36, height: 36)
                    .background(Circle().fill(Color(red: 0.953, green: 0.949, blue: 0.937)))
            }
            .buttonStyle(.plain)

            Spacer().frame(width: 8)

            Button(action: {
                exercises.removeAll { $0.id == exercise.id }
            }) {
                Image(systemName: "trash")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Color(red: 0.898, green: 0.224, blue: 0.208))
                    .frame(width: 36, height: 36)
                    .background(Circle().fill(Color(red: 0.898, green: 0.224, blue: 0.208).opacity(0.12)))
            }
            .buttonStyle(.plain)
        }
        .padding(12)
        .background(RoundedRectangle(cornerRadius: 12).fill(.white))
        .shadow(color: .black.opacity(0.04), radius: 4, y: 2)
    }

    private func fieldSection<Content: View>(label: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(.gray)
            content()
        }
    }
}
