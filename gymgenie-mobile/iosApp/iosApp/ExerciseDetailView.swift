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
        let tokenStorage = TokenStorageKt.createTokenStorage()
        let authApi = AuthApi()
        let client = AuthenticatedHttpClientKt.createAuthenticatedClient(
            tokenStorage: tokenStorage,
            authApi: authApi
        )
        self.vm = Shared.ExerciseDetailViewModel(
            exerciseApi: ExerciseApi(client: client)
        )
        startObserving()
    }

    private func startObserving() {
        observationTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self = self else { break }
                guard let state = self.vm.state.value as? ExerciseDetailUiState else { continue }
                self.exercise = state.exercise
                self.isLoading = state.isLoading
                self.errorMessage = state.errorMessage
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

    @StateObject private var viewModel = ExerciseDetailViewModelWrapper()

    private let orange = Color(red: 0.941, green: 0.439, blue: 0.188)
    private let deepInk = Color(red: 0.161, green: 0.141, blue: 0.125)
    private let warmOffWhite = Color(red: 0.980, green: 0.976, blue: 0.969)
    private let softCard = Color(red: 0.953, green: 0.949, blue: 0.937)
    private let mutedText = Color(red: 0.463, green: 0.447, blue: 0.416)

    var body: some View {
        ZStack {
            warmOffWhite.edgesIgnoringSafeArea(.all)

            if viewModel.isLoading && viewModel.exercise == nil {
                ProgressView().scaleEffect(1.2).tint(orange)
            } else if let error = viewModel.errorMessage, viewModel.exercise == nil {
                errorView(message: error)
            } else if let exercise = viewModel.exercise {
                content(for: exercise)
            }
        }
        .onAppear { viewModel.load(id: exerciseId) }
    }

    // MARK: - Error view

    private func errorView(message: String) -> some View {
        VStack(spacing: 12) {
            Text("⚠️").font(.system(size: 40))
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
                    .background(Capsule().fill(orange))
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

    // MARK: - Content

    private func content(for exercise: ExerciseDetailResponse) -> some View {
        ZStack(alignment: .bottom) {
            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    heroSection(exercise: exercise)
                    contentSheet(exercise: exercise)
                        .offset(y: -20)
                }
            }
            .safeAreaInset(edge: .bottom) {
                Color.clear.frame(height: 80)
            }

            bottomAddButton(for: exercise)
        }
    }

    private func heroSection(exercise: ExerciseDetailResponse) -> some View {
        ZStack {
            Rectangle()
                .fill(softCard)
                .frame(height: 220)

            Text(muscleGroupEmoji(exercise.muscleGroup))
                .font(.system(size: 72))

            VStack {
                HStack {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundColor(deepInk)
                            .frame(width: 40, height: 40)
                            .background(Circle().fill(.white))
                            .shadow(color: Color.black.opacity(0.1), radius: 2, y: 1)
                    }
                    .buttonStyle(.plain)
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.top, 16)
                Spacer()
            }
        }
        .frame(height: 220)
    }

    private func contentSheet(exercise: ExerciseDetailResponse) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            headerRow(exercise: exercise)
            tagsRow(exercise: exercise)
            statsRow(exercise: exercise)

            if let description = exercise.description_, !description.isEmpty {
                section(title: "Описание") {
                    Text(description)
                        .font(.system(size: 14))
                        .foregroundColor(deepInk)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }

            if !exercise.instructions.isEmpty {
                section(title: "Техника выполнения") {
                    VStack(alignment: .leading, spacing: 10) {
                        ForEach(Array(exercise.instructions.enumerated()), id: \.offset) { idx, step in
                            instructionRow(index: idx + 1, text: step)
                        }
                    }
                }
            }

            if let tip = exercise.techniqueTip, !tip.isEmpty {
                section(title: "Совет по технике") {
                    Text(tip)
                        .font(.system(size: 14))
                        .foregroundColor(deepInk)
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(orange.opacity(0.08))
                        )
                }
            }

            if !exercise.equipment.isEmpty {
                section(title: "Оборудование") {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(exercise.equipment, id: \.self) { item in
                                Text(item)
                                    .font(.system(size: 12))
                                    .foregroundColor(deepInk)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(
                                        Capsule()
                                            .stroke(deepInk.opacity(0.2), lineWidth: 1)
                                    )
                            }
                        }
                    }
                }
            }

            if !exercise.secondaryMuscleGroups.isEmpty {
                section(title: "Вспомогательные мышцы") {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(exercise.secondaryMuscleGroups, id: \.self) { group in
                                Text(muscleGroupRu(group))
                                    .font(.system(size: 12))
                                    .foregroundColor(deepInk)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(Capsule().fill(softCard))
                            }
                        }
                    }
                }
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(warmOffWhite)
        )
    }

    // MARK: - Subviews

    private func headerRow(exercise: ExerciseDetailResponse) -> some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 2) {
                Text(exercise.nameRu)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(deepInk)
                if !exercise.nameEn.isEmpty {
                    Text(exercise.nameEn)
                        .font(.system(size: 14))
                        .foregroundColor(mutedText)
                }
                let subtitle: String = {
                    var parts: [String] = []
                    if !exercise.muscleGroup.isEmpty {
                        parts.append(muscleGroupRu(exercise.muscleGroup))
                    }
                    if !exercise.category.isEmpty {
                        parts.append(categoryRu(exercise.category))
                    }
                    return parts.joined(separator: " • ")
                }()
                if !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.system(size: 13))
                        .foregroundColor(mutedText)
                        .padding(.top, 2)
                }
            }
            Spacer()
            if let rating = exercise.rating {
                HStack(spacing: 4) {
                    Text("⭐")
                    Text(String(format: "%.1f", rating.doubleValue))
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(deepInk)
                }
            }
        }
    }

    private func tagsRow(exercise: ExerciseDetailResponse) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                if !exercise.muscleGroup.isEmpty {
                    pill(
                        text: muscleGroupRu(exercise.muscleGroup),
                        textColor: .white,
                        background: Capsule().fill(orange)
                    )
                }
                if let equipment = exercise.equipment.first {
                    pill(
                        text: equipment,
                        textColor: deepInk,
                        background: Capsule().stroke(deepInk.opacity(0.25), lineWidth: 1)
                    )
                }
                if !exercise.difficultyLevel.isEmpty {
                    let (label, color) = difficultyStyle(exercise.difficultyLevel)
                    pill(
                        text: label,
                        textColor: .white,
                        background: Capsule().fill(color)
                    )
                }
            }
        }
    }

    private func statsRow(exercise: ExerciseDetailResponse) -> some View {
        let items: [(String, Color)] = {
            var result: [(String, Color)] = []
            if let duration = exercise.durationMinutes {
                result.append(("⏱ \(duration.intValue) мин", orange))
            }
            if !exercise.difficultyLevel.isEmpty {
                result.append(("📊 \(difficultyLabel(exercise.difficultyLevel))",
                               Color(red: 0.298, green: 0.686, blue: 0.314)))
            }
            if let calories = exercise.caloriesBurned {
                result.append(("🔥 \(calories.intValue) ккал",
                               Color(red: 0.898, green: 0.224, blue: 0.208)))
            }
            return result
        }()

        if items.isEmpty { return AnyView(EmptyView()) }

        return AnyView(
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                        Text(item.0)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(item.1)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(item.1.opacity(0.1))
                            )
                    }
                }
            }
        )
    }

    private func section<Content: View>(
        title: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.system(size: 17, weight: .bold))
                .foregroundColor(deepInk)
                .padding(.top, 8)
            content()
        }
    }

    private func instructionRow(index: Int, text: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Text("\(index)")
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(.white)
                .frame(width: 24, height: 24)
                .background(Circle().fill(orange))
            Text(text)
                .font(.system(size: 14))
                .foregroundColor(deepInk)
                .fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 0)
        }
    }

    private func pill<B: View>(text: String, textColor: Color, background: B) -> some View {
        Text(text)
            .font(.system(size: 11, weight: .semibold))
            .foregroundColor(textColor)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(background)
    }

    private func bottomAddButton(for exercise: ExerciseDetailResponse) -> some View {
        VStack(spacing: 0) {
            LinearGradient(
                colors: [warmOffWhite.opacity(0), warmOffWhite],
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(height: 16)

            Button(action: { onAddToWorkout(exercise) }) {
                Text("🏋  Добавить в тренировку")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(RoundedRectangle(cornerRadius: 16).fill(orange))
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 20)
            .padding(.bottom, 16)
            .background(warmOffWhite)
        }
    }

    // MARK: - Helpers

    private func difficultyStyle(_ level: String) -> (String, Color) {
        switch level.uppercased() {
        case "BEGINNER": return ("Легко", Color(red: 0.298, green: 0.686, blue: 0.314))
        case "INTERMEDIATE": return ("Средне", orange)
        case "ADVANCED": return ("Сложно", Color(red: 0.898, green: 0.224, blue: 0.208))
        default: return (level, mutedText)
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

    private func categoryRu(_ c: String) -> String {
        switch c.uppercased() {
        case "STRENGTH": return "Сила"
        case "CARDIO": return "Кардио"
        case "FLEXIBILITY": return "Гибкость"
        case "BALANCE": return "Баланс"
        case "PLYOMETRIC": return "Плиометрика"
        case "FUNCTIONAL": return "Функционал"
        default: return c
        }
    }

    private func muscleGroupEmoji(_ mg: String) -> String {
        switch mg.uppercased() {
        case "CHEST": return "🤸"
        case "BACK": return "🏋"
        case "SHOULDERS": return "💪"
        case "BICEPS", "TRICEPS", "FOREARMS": return "💪"
        case "ABS": return "⚡"
        case "QUADRICEPS", "HAMSTRINGS", "CALVES": return "🏃"
        case "GLUTES": return "🔥"
        case "CARDIO": return "❤️"
        case "FULL_BODY": return "⭐"
        default: return "🏋"
        }
    }
}
