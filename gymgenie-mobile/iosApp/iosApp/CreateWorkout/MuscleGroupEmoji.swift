import Foundation

func muscleGroupImageName(_ key: String) -> String {
    switch key.uppercased() {
    case "CHEST":       return "ic_chest"
    case "BACK":        return "ic_back"
    case "SHOULDERS":   return "ic_shoulders"
    case "BICEPS":      return "ic_biceps"
    case "TRICEPS":     return "ic_triceps"
    case "FOREARMS":    return "ic_forearms"
    case "ABS":         return "ic_abs"
    case "QUADRICEPS":  return "ic_quadriceps"
    case "HAMSTRINGS":  return "ic_hamstrings"
    case "GLUTES":      return "ic_glutes"
    case "CALVES":      return "ic_calves"
    case "FULL_BODY":   return "ic_fullbody"
    case "CARDIO":      return "ic_cardio"
    default:            return "ic_fullbody"
    }
}

func muscleGroupEmoji(_ key: String) -> String {
    switch key.uppercased() {
    case "CHEST":       return "🫁"
    case "BACK":        return "🦾"
    case "SHOULDERS":   return "💪"
    case "BICEPS":      return "💪"
    case "TRICEPS":     return "🤸"
    case "FOREARMS":    return "✊"
    case "ABS":         return "🔥"
    case "QUADRICEPS":  return "🦵"
    case "HAMSTRINGS":  return "🦵"
    case "GLUTES":      return "🍑"
    case "CALVES":      return "🦶"
    case "FULL_BODY":   return "⭐"
    case "CARDIO":      return "❤️"
    default:            return "💪"
    }
}
