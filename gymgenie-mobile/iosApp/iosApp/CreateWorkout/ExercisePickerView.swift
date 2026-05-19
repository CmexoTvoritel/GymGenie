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
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: muscleGroupNameRu,
                showBackNavigation: true,
                onBackTap: onBack
            )
            WorkoutFlowStepHeader(currentStep: 2, totalSteps: 3)
            content
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(warmOffWhite.ignoresSafeArea())
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
        GeometryReader { proxy in
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
                .padding(.top, 8)

                if picker.isLoadingMore {
                    ProgressView().tint(orange).padding(.vertical, 12)
                }

                // Safe-area + 16pt floor so the last row clears the home indicator.
                Color.clear.frame(height: proxy.safeAreaInsets.bottom + 16)
            }
        }
    }

    /// Renders a single exercise card with three gestures:
    /// - tap on the card body → select & advance the flow.
    /// - long-press on the card → open the read-only detail sheet.
    /// - tap on the floating "i" badge inside the image area → also opens the
    ///   detail sheet.
    ///
    /// The info badge and the long-press handler now live inside
    /// `ExerciseCard` itself so the picker stays free of layout glue.
    private func exerciseCell(_ exercise: Shared.ExerciseShortResponse) -> some View {
        ExerciseCard(
            exercise: exercise,
            onTap: { onExerciseSelected(exercise) },
            onLongPress: { detailExerciseId = exercise.id },
            onInfoTap: { detailExerciseId = exercise.id }
        )
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
