import SwiftUI

// MARK: - Muscle group → Russian display name

/// Returns the Russian display name for a backend muscle group constant.
func muscleGroupNameRu(_ group: String) -> String {
    switch group.uppercased() {
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
    default: return group
    }
}

// MARK: - Muscle group → accent color

/// Returns a brand color for the given muscle group constant.
func muscleGroupColor(_ group: String) -> Color {
    switch group.uppercased() {
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

// MARK: - Muscle group → background / foreground pair

struct MuscleGroupColorPair {
    let bg: Color
    let fg: Color
}

/// Returns background and foreground colors for a muscle group, used in history cards and plan cards.
func muscleGroupColorPair(_ group: String?) -> MuscleGroupColorPair {
    switch group?.uppercased() {
    case "CHEST":
        return MuscleGroupColorPair(bg: Color(red: 1.0, green: 0.910, blue: 0.886), fg: Color(red: 0.914, green: 0.290, blue: 0.173))
    case "BACK":
        return MuscleGroupColorPair(bg: Color(red: 0.902, green: 0.933, blue: 1.0), fg: Color(red: 0.231, green: 0.357, blue: 0.859))
    case "QUADRICEPS", "HAMSTRINGS", "GLUTES", "CALVES", "LEGS":
        return MuscleGroupColorPair(bg: Color(red: 0.910, green: 0.969, blue: 0.910), fg: Color(red: 0.184, green: 0.620, blue: 0.267))
    case "BICEPS", "TRICEPS", "FOREARMS", "ARMS":
        return MuscleGroupColorPair(bg: Color(red: 1.0, green: 0.957, blue: 0.839), fg: Color(red: 0.722, green: 0.525, blue: 0.043))
    case "CARDIO":
        return MuscleGroupColorPair(bg: Color(red: 0.988, green: 0.910, blue: 0.949), fg: Color(red: 0.761, green: 0.149, blue: 0.361))
    case "ABS", "CORE":
        return MuscleGroupColorPair(bg: Color(red: 0.918, green: 0.902, blue: 1.0), fg: Color(red: 0.404, green: 0.255, blue: 0.851))
    case "SHOULDERS", "SHOULDER":
        return MuscleGroupColorPair(bg: Color(red: 0.902, green: 0.933, blue: 1.0), fg: Color(red: 0.231, green: 0.357, blue: 0.859))
    case "FULL_BODY":
        return MuscleGroupColorPair(bg: Color(red: 1.0, green: 0.957, blue: 0.839), fg: Color(red: 1.0, green: 0.353, blue: 0.235))
    default:
        return MuscleGroupColorPair(bg: Color(red: 1.0, green: 0.910, blue: 0.886), fg: Color(red: 0.914, green: 0.290, blue: 0.173))
    }
}
