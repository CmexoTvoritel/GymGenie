import SwiftUI
import Shared

/// Equal-height exercise tile used in the workouts list and the create-workout
/// picker.
///
/// - `onTap`: primary action (select / navigate / filter).
/// - `onLongPress`: optional long-press shortcut, typically used by the
///   create-workout picker to surface the detail sheet without committing the
///   selection.
/// - `onInfoTap`: optional callback for the small "i" info badge floating in
///   the top-right corner of the image area. When `nil`, the badge is hidden.
struct ExerciseCard: View {
    let exercise: ExerciseShortResponse
    var onTap: () -> Void = {}
    var onLongPress: (() -> Void)? = nil
    var onInfoTap: (() -> Void)? = nil

    private let cardBorder = Color(red: 0.929, green: 0.929, blue: 0.937)
    private let imageBackground = Color(red: 0.973, green: 0.973, blue: 0.980)
    private let primaryText = Color(red: 0.039, green: 0.039, blue: 0.039)
    private let metaText = Color(red: 0.298, green: 0.298, blue: 0.325)
    private let infoBadgeColor = Color(red: 0.941, green: 0.439, blue: 0.188) // accent orange

    var body: some View {
        // The long-press gesture must be installed on a fully built view, so we
        // capture the button once and attach the optional gesture afterwards.
        // `_LongPressOptional` keeps the opaque return type stable regardless
        // of whether the caller provided a handler.
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 0) {
                imageSection

                Text(exercise.nameRu)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(primaryText)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                    .lineSpacing(2)
                    .frame(maxWidth: .infinity, minHeight: 35, alignment: .topLeading)
                    .padding(.top, 10)

                if !exercise.difficultyLevel.isEmpty {
                    difficultyChip(exercise.difficultyLevel)
                        .padding(.top, 6)
                }

                metaRow
                    .padding(.top, 8)
            }
            .padding(10)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(.white)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(cardBorder, lineWidth: 1.5)
            )
            .shadow(color: Color.black.opacity(0.03), radius: 3, y: 1)
        }
        .buttonStyle(.plain)
        .modifier(LongPressOptionalModifier(onLongPress: onLongPress))
    }

    /// View modifier that conditionally attaches `onLongPressGesture` without
    /// branching on the opaque type, which would break `some View` inference.
    private struct LongPressOptionalModifier: ViewModifier {
        let onLongPress: (() -> Void)?
        func body(content: Content) -> some View {
            if let onLongPress = onLongPress {
                content.onLongPressGesture(minimumDuration: 0.4, perform: onLongPress)
            } else {
                content
            }
        }
    }

    private var imageSection: some View {
        // ZStack with topTrailing alignment so the optional info badge sits
        // exactly in the corner of the image area without disturbing the
        // emoji's centering or the card's natural aspect ratio.
        ZStack(alignment: .topTrailing) {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(imageBackground)

            Text(Self.muscleGroupEmoji(exercise.muscleGroup))
                .font(.system(size: 54))
                .frame(maxWidth: .infinity, maxHeight: .infinity)

            if let onInfoTap = onInfoTap {
                Button(action: onInfoTap) {
                    ZStack {
                        Circle()
                            .fill(infoBadgeColor)
                            .frame(width: 28, height: 28)
                            .shadow(color: Color.black.opacity(0.15), radius: 2, y: 1)
                        Text("i")
                            .font(.system(size: 14, weight: .bold, design: .serif))
                            .italic()
                            .foregroundColor(.white)
                    }
                }
                .buttonStyle(.plain)
                .padding(.top, 8)
                .padding(.trailing, 8)
                .accessibilityLabel("Подробнее об упражнении")
            }
        }
        .aspectRatio(1, contentMode: .fit)
        .frame(maxWidth: .infinity)
    }

    private var metaRow: some View {
        HStack(spacing: 5) {
            Circle()
                .fill(Self.muscleGroupColor(exercise.muscleGroup))
                .frame(width: 7, height: 7)
            Text(Self.muscleGroupLabel(exercise.muscleGroup))
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(metaText)
                .lineLimit(1)
                .truncationMode(.tail)
        }
    }

    private func difficultyChip(_ level: String) -> some View {
        let (label, color): (String, Color) = {
            switch level.uppercased() {
            case "BEGINNER":
                return ("Легко", Color(red: 0.133, green: 0.627, blue: 0.420))
            case "INTERMEDIATE":
                return ("Средне", Color(red: 0.910, green: 0.608, blue: 0.071))
            case "ADVANCED":
                return ("Сложно", Color(red: 0.820, green: 0.263, blue: 0.263))
            default:
                return (level, metaText)
            }
        }()
        return Text(label)
            .font(.system(size: 12, weight: .semibold))
            .foregroundColor(color)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(color.opacity(0.12))
            .clipShape(Capsule())
    }

    private static func muscleGroupLabel(_ mg: String) -> String {
        switch mg.uppercased() {
        case "CHEST": return "Грудь"
        case "BACK": return "Спина"
        case "SHOULDERS": return "Плечи"
        case "BICEPS": return "Бицепс"
        case "TRICEPS": return "Трицепс"
        case "FOREARMS": return "Предплечья"
        case "ABS": return "Пресс"
        case "QUADRICEPS": return "Квадрицепс"
        case "HAMSTRINGS": return "Бицепс бедра"
        case "GLUTES": return "Ягодицы"
        case "CALVES": return "Икры"
        case "FULL_BODY": return "Всё тело"
        case "CARDIO": return "Кардио"
        default: return mg
        }
    }

    private static func muscleGroupColor(_ mg: String) -> Color {
        switch mg.uppercased() {
        case "CHEST": return Color(red: 0.914, green: 0.290, blue: 0.173)
        case "BACK": return Color(red: 0.231, green: 0.357, blue: 0.859)
        case "SHOULDERS": return Color(red: 0.910, green: 0.608, blue: 0.071)
        case "BICEPS", "TRICEPS", "FOREARMS":
            return Color(red: 0.722, green: 0.525, blue: 0.043)
        case "ABS": return Color(red: 0.404, green: 0.255, blue: 0.851)
        case "QUADRICEPS", "HAMSTRINGS", "GLUTES", "CALVES":
            return Color(red: 0.184, green: 0.620, blue: 0.267)
        case "FULL_BODY": return Color(red: 1.0, green: 0.353, blue: 0.235)
        case "CARDIO": return Color(red: 0.761, green: 0.145, blue: 0.361)
        default: return Color(red: 0.463, green: 0.447, blue: 0.416)
        }
    }

    private static func muscleGroupEmoji(_ mg: String) -> String {
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
