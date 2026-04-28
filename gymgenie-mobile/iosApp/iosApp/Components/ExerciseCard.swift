import SwiftUI
import Shared

struct ExerciseCard: View {
    let exercise: ExerciseShortResponse
    var onTap: () -> Void = {}

    private let orange = Color(red: 0.941, green: 0.439, blue: 0.188)
    private let deepInk = Color(red: 0.161, green: 0.141, blue: 0.125)
    private let softCard = Color(red: 0.953, green: 0.949, blue: 0.937)

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 10) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(softCard)
                        .frame(height: 110)
                    Text(muscleGroupEmoji(exercise.muscleGroup))
                        .font(.system(size: 36))
                }

                Text(exercise.nameRu)
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(deepInk)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)

                if !exercise.difficultyLevel.isEmpty {
                    difficultyBadge(exercise.difficultyLevel)
                }
            }
            .padding(10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(RoundedRectangle(cornerRadius: 16).fill(.white))
            .shadow(color: Color.black.opacity(0.04), radius: 4, y: 2)
        }
        .buttonStyle(.plain)
    }

    private func difficultyBadge(_ level: String) -> some View {
        let (label, color): (String, Color) = {
            switch level.uppercased() {
            case "BEGINNER": return ("Легко", Color(red: 0.298, green: 0.686, blue: 0.314))
            case "INTERMEDIATE": return ("Средн.", orange)
            case "ADVANCED": return ("Сложн.", Color(red: 0.898, green: 0.224, blue: 0.208))
            default: return (level, .gray)
            }
        }()
        return Text(label)
            .font(.system(size: 10, weight: .bold))
            .foregroundColor(.white)
            .padding(.horizontal, 9)
            .padding(.vertical, 4)
            .background(Capsule().fill(color))
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
