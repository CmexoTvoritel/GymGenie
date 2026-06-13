import SwiftUI
import Shared

@MainActor
final class ExerciseDetailViewModelWrapper: ObservableObject {
    private let vm: Shared.ExerciseDetailViewModel

    @Published private(set) var exercise: ExerciseDetailResponse? = nil
    @Published private(set) var isLoading: Bool = false
    @Published private(set) var errorMessage: String? = nil

    private var observationTask: Task<Void, Never>?

    init() {
        self.vm = Shared.ExerciseDetailViewModel(
            exerciseApi: KoinHelper.shared.getExerciseApi()
        )
        startObserving()
    }

    private func startObserving() {
        observationTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self = self else { break }
                guard let state = self.vm.state.value as? ExerciseDetailUiState else {
                    try? await Task.sleep(nanoseconds: 50_000_000)
                    continue
                }
                if self.isLoading != state.isLoading { self.isLoading = state.isLoading }
                if self.errorMessage != state.errorMessage { self.errorMessage = state.errorMessage }
                if self.exercise?.id != state.exercise?.id { self.exercise = state.exercise }
                try? await Task.sleep(nanoseconds: 50_000_000)
            }
        }
    }

    func load(id: String) {
        vm.load(id: id)
    }

    func retry(id: String) {
        vm.retry(id: id)
    }

    deinit {
        observationTask?.cancel()
        vm.onCleared()
    }
}

struct ExerciseDetailView: View {
    let exerciseId: String
    var onBack: () -> Void = {}
    var onAddToWorkout: (ExerciseDetailResponse) -> Void = { _ in }
    var showAddButton: Bool = true

    @StateObject private var viewModel = ExerciseDetailViewModelWrapper()

    private let accentOrange = Color(red: 0.941, green: 0.439, blue: 0.188)
    private let deepInk = Color(red: 0.161, green: 0.141, blue: 0.125)
    private let warmOffWhite = Color(red: 0.980, green: 0.976, blue: 0.969)
    private let softCard = Color(red: 0.953, green: 0.949, blue: 0.937)
    private let mutedText = Color(red: 0.463, green: 0.447, blue: 0.416)

    var body: some View {
        ZStack {
            if viewModel.isLoading && viewModel.exercise == nil {
                ProgressView().scaleEffect(1.2).tint(accentOrange)
            } else if let error = viewModel.errorMessage, viewModel.exercise == nil {
                errorView(message: error)
            } else if let exercise = viewModel.exercise {
                exerciseContent(exercise)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(warmOffWhite.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
        .onAppear { viewModel.load(id: exerciseId) }
    }

    private func errorView(message: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 36))
                .foregroundColor(accentOrange)
            Text(message)
                .font(.system(size: 14))
                .foregroundColor(mutedText)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
            Button(action: { viewModel.retry(id: exerciseId) }) {
                Text("Повторить")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Capsule().fill(accentOrange))
            }
            .buttonStyle(.plain)
            Button(action: onBack) {
                Text("Назад")
                    .font(.system(size: 14))
                    .foregroundColor(mutedText)
                    .padding(8)
            }
            .buttonStyle(.plain)
        }
    }

    private func exerciseContent(_ exercise: ExerciseDetailResponse) -> some View {
        ZStack(alignment: .bottom) {
            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    heroSection(exercise)
                    contentSheet(exercise)
                        .offset(y: -20)
                }
            }
            .safeAreaInset(edge: .bottom) {
                if showAddButton {
                    Color.clear.frame(height: 80)
                }
            }

            if showAddButton {
                bottomAddButton(exercise)
            }
        }
        .overlay(alignment: .topLeading) {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(deepInk)
                    .frame(width: 40, height: 40)
                    .background(Circle().fill(.white))
                    .shadow(color: .black.opacity(0.1), radius: 2, y: 1)
            }
            .buttonStyle(.plain)
            .padding(.leading, 16)
            .padding(.top, 16)
        }
    }

    private func heroSection(_ exercise: ExerciseDetailResponse) -> some View {
        ZStack {
            Rectangle().fill(softCard)
            Image(muscleGroupExerciseImageName(exercise.muscleGroup))
                .resizable()
                .aspectRatio(1, contentMode: .fit)
        }
        .aspectRatio(1, contentMode: .fit)
    }

    private func contentSheet(_ exercise: ExerciseDetailResponse) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(exercise.nameRu)
                .font(.system(size: 26, weight: .bold))
                .foregroundColor(deepInk)
                .lineSpacing(6)
                .frame(maxWidth: .infinity, alignment: .leading)

            let stats = buildStats(exercise)
            if !stats.isEmpty {
                WrappingHStack(spacing: 8) {
                    ForEach(Array(stats.enumerated()), id: \.offset) { _, stat in
                        statChip(icon: stat.icon, text: stat.text, color: stat.color)
                    }
                }
                .padding(.top, 12)
            }

            if let description = exercise.description_, !description.isEmpty {
                sectionTitle("Описание").padding(.top, 20)
                Text(description)
                    .font(.system(size: 17))
                    .foregroundColor(deepInk)
                    .lineSpacing(5)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.top, 8)
            }

            if !exercise.instructions.isEmpty {
                sectionTitle("Техника выполнения").padding(.top, 20)
                VStack(alignment: .leading, spacing: 10) {
                    ForEach(Array(exercise.instructions.enumerated()), id: \.offset) { idx, step in
                        instructionRow(index: idx + 1, text: step)
                    }
                }
                .padding(.top, 10)
            }

            if let tip = exercise.techniqueTip, !tip.isEmpty {
                sectionTitle("Совет по технике").padding(.top, 20)
                Text(tip)
                    .font(.system(size: 17))
                    .foregroundColor(deepInk)
                    .lineSpacing(5)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(RoundedRectangle(cornerRadius: 12).fill(accentOrange.opacity(0.08)))
                    .padding(.top, 8)
            }

            if !exercise.equipment.isEmpty {
                sectionTitle("Оборудование").padding(.top, 20)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(exercise.equipment, id: \.self) { item in
                            Text(item)
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(deepInk)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 4)
                                .background(Capsule().stroke(deepInk.opacity(0.25), lineWidth: 1))
                        }
                    }
                }
                .padding(.top, 10)
            }

            if !exercise.secondaryMuscleGroups.isEmpty {
                sectionTitle("Вспомогательные мышцы").padding(.top, 20)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(exercise.secondaryMuscleGroups, id: \.self) { group in
                            Text(muscleGroupNameRu(group))
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(deepInk)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 4)
                                .background(Capsule().stroke(deepInk.opacity(0.25), lineWidth: 1))
                        }
                    }
                }
                .padding(.top, 10)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 24).fill(warmOffWhite))
    }

    private func sectionTitle(_ title: String) -> some View {
        Text(title)
            .font(.system(size: 21, weight: .bold))
            .foregroundColor(deepInk)
    }

    private struct StatData {
        let icon: String
        let text: String
        let color: Color
    }

    private func buildStats(_ exercise: ExerciseDetailResponse) -> [StatData] {
        var items: [StatData] = []
        if !exercise.muscleGroup.isEmpty {
            items.append(StatData(
                icon: "dumbbell.fill",
                text: muscleGroupNameRu(exercise.muscleGroup),
                color: muscleGroupColor(exercise.muscleGroup)
            ))
        }
        if let secPer10 = exercise.secondsPer10Reps {
            items.append(StatData(
                icon: "clock",
                text: "\(secPer10.intValue) сек/10 повт.",
                color: accentOrange
            ))
        }
        if !exercise.difficultyLevel.isEmpty {
            let color: Color = {
                switch exercise.difficultyLevel.uppercased() {
                case "BEGINNER": return Color(red: 0.298, green: 0.686, blue: 0.314)
                case "INTERMEDIATE": return accentOrange
                case "ADVANCED": return Color(red: 0.898, green: 0.224, blue: 0.208)
                default: return mutedText
                }
            }()
            items.append(StatData(
                icon: "speedometer",
                text: difficultyLabel(exercise.difficultyLevel),
                color: color
            ))
        }
        if let calories = exercise.caloriesBurned {
            items.append(StatData(
                icon: "flame.fill",
                text: "\(calories.intValue) ккал",
                color: Color(red: 0.898, green: 0.224, blue: 0.208)
            ))
        }
        return items
    }

    private func statChip(icon: String, text: String, color: Color) -> some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
                .font(.system(size: 14))
                .foregroundColor(color)
            Text(text)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(color)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 4)
        .background(Capsule().fill(color.opacity(0.12)))
    }

    private func instructionRow(index: Int, text: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Text("\(index)")
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(.white)
                .frame(width: 24, height: 24)
                .background(Circle().fill(accentOrange))
            Text(text)
                .font(.system(size: 17))
                .foregroundColor(deepInk)
                .lineSpacing(5)
                .fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 0)
        }
    }

    private func bottomAddButton(_ exercise: ExerciseDetailResponse) -> some View {
        VStack(spacing: 0) {
            LinearGradient(
                colors: [warmOffWhite.opacity(0), warmOffWhite],
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(height: 16)

            Button(action: { onAddToWorkout(exercise) }) {
                Text("Добавить в тренировку")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(RoundedRectangle(cornerRadius: 16).fill(accentOrange))
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 20)
            .padding(.bottom, 16)
            .background(warmOffWhite)
        }
    }

    private func difficultyLabel(_ level: String) -> String {
        switch level.uppercased() {
        case "BEGINNER": return "Легко"
        case "INTERMEDIATE": return "Средне"
        case "ADVANCED": return "Сложно"
        default: return level
        }
    }

}

private struct WrappingHStack: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? .infinity
        var x: CGFloat = 0
        var y: CGFloat = 0
        var lineHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > width && x > 0 {
                y += lineHeight + spacing
                x = 0
                lineHeight = 0
            }
            x += size.width + spacing
            lineHeight = max(lineHeight, size.height)
        }
        return CGSize(width: width, height: y + lineHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var lineHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > bounds.maxX && x > bounds.minX {
                y += lineHeight + spacing
                x = bounds.minX
                lineHeight = 0
            }
            subview.place(at: CGPoint(x: x, y: y), anchor: .topLeading, proposal: .unspecified)
            x += size.width + spacing
            lineHeight = max(lineHeight, size.height)
        }
    }
}
