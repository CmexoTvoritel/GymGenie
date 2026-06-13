import Foundation

func muscleGroupExerciseImageName(_ key: String) -> String {
    switch key.uppercased() {
    case "CHEST": return "ic_exercise_chest"
    case "BACK": return "ic_exercise_back"
    case "SHOULDERS": return "ic_exercise_shoulders"
    case "BICEPS", "TRICEPS", "FOREARMS": return "ic_exercise_arms"
    case "ABS": return "ic_exercise_abs"
    case "QUADRICEPS", "HAMSTRINGS", "GLUTES", "CALVES": return "ic_exercise_legs"
    case "FULL_BODY": return "ic_exercise_fullbody"
    case "CARDIO": return "ic_exercise_cardio"
    default: return "ic_exercise_fullbody"
    }
}

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

