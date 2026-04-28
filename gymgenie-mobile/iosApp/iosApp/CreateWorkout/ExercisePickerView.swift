import SwiftUI
import Shared

/// Step 2 — browse and pick an exercise scoped to one muscle group.
///
/// Tapping the card body selects the exercise and advances the flow, while
/// the small info badge opens the read-only detail sheet so the user can
/// inspect technique before committing.
struct ExercisePickerView: View {
    let muscleGroupKey: String
    let muscleGroupNameRu: String
    let onBack: () -> Void
    let onExerciseSelected: (Shared.ExerciseShortResponse) -> Void

    @StateObject private var picker = ExercisePickerViewModelWrapper()
    @State private var detailExerciseId: String? = nil

    private let orange = Color(red: 0.941, green: 0.439, blue: 0.188)
    private let deepInk = Color(red: 0.161, green: 0.141, blue: 0.125)
    private let warmOffWhite = Color(red: 0.980, green: 0.976, blue: 0.969)
    private let softCard = Color(red: 0.953, green: 0.949, blue: 0.937)
    private let mutedText = Color(red: 0.463, green: 0.447, blue: 0.416)

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12),
    ]

    var body: some View {
        ZStack {
            warmOffWhite.edgesIgnoringSafeArea(.all)

            VStack(spacing: 0) {
                header
                content
            }
        }
        .onAppear {
            picker.load(muscleGroup: muscleGroupKey)
        }
        .sheet(
            isPresented: Binding(
                get: { detailExerciseId != nil },
                set: { if !$0 { detailExerciseId = nil } }
            )
        ) {
            if let id = detailExerciseId {
                ExerciseDetailView(
                    exerciseId: id,
                    onBack: { detailExerciseId = nil },
                    onAddToWorkout: { _ in detailExerciseId = nil }
                )
            }
        }
    }

    // MARK: - Header

    private var header: some View {
        HStack(spacing: 12) {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(deepInk)
                    .frame(width: 40, height: 40)
                    .background(Circle().fill(.white))
                    .shadow(color: Color.black.opacity(0.06), radius: 2, y: 1)
            }
            .buttonStyle(.plain)

            VStack(alignment: .leading, spacing: 2) {
                Text(muscleGroupNameRu)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(deepInk)
                Text("Выбери упражнение")
                    .font(.system(size: 12))
                    .foregroundColor(mutedText)
            }

            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.top, 12)
        .padding(.bottom, 12)
    }

    // MARK: - Content

    @ViewBuilder
    private var content: some View {
        if picker.isLoading && picker.exercises.isEmpty {
            Spacer()
            ProgressView().scaleEffect(1.2).tint(orange)
            Spacer()
        } else if let error = picker.errorMessage, picker.exercises.isEmpty {
            Spacer()
            errorView(message: error)
            Spacer()
        } else if picker.exercises.isEmpty {
            Spacer()
            emptyView
            Spacer()
        } else {
            grid
        }
    }

    private var grid: some View {
        ScrollView(showsIndicators: false) {
            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(picker.exercises, id: \.id) { exercise in
                    exerciseCell(exercise)
                        .onAppear {
                            if exercise.id == picker.exercises.last?.id && picker.hasMore {
                                picker.loadMore()
                            }
                        }
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 8)

            if picker.isLoadingMore {
                ProgressView().tint(orange).padding(.vertical, 12)
            }

            Color.clear.frame(height: 32)
        }
    }

    private func exerciseCell(_ exercise: Shared.ExerciseShortResponse) -> some View {
        ZStack(alignment: .topTrailing) {
            ExerciseCard(
                exercise: exercise,
                onTap: { onExerciseSelected(exercise) }
            )

            Button {
                detailExerciseId = exercise.id
            } label: {
                ZStack {
                    Circle()
                        .fill(orange)
                        .frame(width: 28, height: 28)
                        .shadow(color: Color.black.opacity(0.15), radius: 2, y: 1)
                    Text("i")
                        .font(.system(size: 14, weight: .bold, design: .serif))
                        .italic()
                        .foregroundColor(.white)
                }
            }
            .buttonStyle(.plain)
            .padding(6)
            .accessibilityLabel("Подробнее об упражнении")
        }
    }

    private var emptyView: some View {
        VStack(spacing: 8) {
            Text("📦").font(.system(size: 44))
            Text("Упражнений не найдено")
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(deepInk)
            Text("Попробуйте выбрать другую группу")
                .font(.system(size: 13))
                .foregroundColor(mutedText)
                .multilineTextAlignment(.center)
        }
    }

    private func errorView(message: String) -> some View {
        VStack(spacing: 12) {
            Text("⚠️").font(.system(size: 40))
            Text(message)
                .font(.system(size: 14))
                .foregroundColor(mutedText)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            Button(action: { picker.retry() }) {
                Text("Повторить")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Capsule().fill(orange))
            }
            .buttonStyle(.plain)
        }
    }
}
