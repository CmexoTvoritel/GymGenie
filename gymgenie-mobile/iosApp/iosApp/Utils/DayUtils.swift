import Foundation

// MARK: - Backend day name → full Russian name

/// Converts a backend weekday constant (e.g. "MONDAY") to its full Russian name.
func dayNameRu(_ day: String) -> String {
    switch day.uppercased() {
    case "MONDAY": return "понедельник"
    case "TUESDAY": return "вторник"
    case "WEDNESDAY": return "среда"
    case "THURSDAY": return "четверг"
    case "FRIDAY": return "пятница"
    case "SATURDAY": return "суббота"
    case "SUNDAY": return "воскресенье"
    default: return day.lowercased()
    }
}

// MARK: - Backend day name → short Russian abbreviation

/// Converts a backend weekday constant (e.g. "MONDAY") to a short Russian label ("Пн").
func dayShortRu(_ day: String) -> String {
    switch day.uppercased() {
    case "MONDAY": return "Пн"
    case "TUESDAY": return "Вт"
    case "WEDNESDAY": return "Ср"
    case "THURSDAY": return "Чт"
    case "FRIDAY": return "Пт"
    case "SATURDAY": return "Сб"
    case "SUNDAY": return "Вс"
    default: return day
    }
}

// MARK: - Calendar weekday index → short Russian abbreviation

/// Converts a `Calendar.component(.weekday)` value (1 = Sunday … 7 = Saturday) to "Пн", "Вт", etc.
func dayShortRuFromCalendarIndex(_ index: Int) -> String {
    switch index {
    case 1: return "Вс"
    case 2: return "Пн"
    case 3: return "Вт"
    case 4: return "Ср"
    case 5: return "Чт"
    case 6: return "Пт"
    case 7: return "Сб"
    default: return ""
    }
}

// MARK: - Calendar weekday index → backend day constant

/// Converts `Calendar.component(.weekday)` (1 = Sunday) to the backend enum string "MONDAY", etc.
func calendarWeekdayToBackend(_ weekday: Int) -> String {
    switch weekday {
    case 1: return "SUNDAY"
    case 2: return "MONDAY"
    case 3: return "TUESDAY"
    case 4: return "WEDNESDAY"
    case 5: return "THURSDAY"
    case 6: return "FRIDAY"
    case 7: return "SATURDAY"
    default: return "MONDAY"
    }
}

// MARK: - Lookup tables

/// Maps backend day constants to short Russian labels.
let backendDayToShort: [String: String] = [
    "MONDAY": "Пн", "TUESDAY": "Вт", "WEDNESDAY": "Ср",
    "THURSDAY": "Чт", "FRIDAY": "Пт", "SATURDAY": "Сб", "SUNDAY": "Вс"
]

/// Maps short Russian labels to backend day constants.
let dayLabelToBackend: [String: String] = [
    "Пн": "MONDAY", "Вт": "TUESDAY", "Ср": "WEDNESDAY",
    "Чт": "THURSDAY", "Пт": "FRIDAY", "Сб": "SATURDAY", "Вс": "SUNDAY"
]

/// Canonical Monday-first backend day ordering.
let weekdayOrder: [String] = [
    "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
]

/// Short Russian day labels in Monday-first order, matching `weekdayOrder`.
let daysOfWeekLabels: [String] = ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"]
