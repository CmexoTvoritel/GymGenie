import SwiftUI
import Shared

private let borderColor = Color(red: 0.929, green: 0.929, blue: 0.937)

struct ScheduleChipView: View {
    let plan: MealPlanDetail

    private var firstMealType: String {
        plan.meals.first?.mealType ?? "BREAKFAST"
    }

    private var mealType: AiMealType? {
        AiMealType.companion.fromWireValue(value: firstMealType)
    }

    private var palette: (bg: Color, emoji: String) {
        switch firstMealType.uppercased() {
        case "BREAKFAST": return (Color(red: 1.0, green: 0.965, blue: 0.839), "☀️")
        case "LUNCH": return (Color(red: 1.0, green: 0.933, blue: 0.867), "🥗")
        case "DINNER": return (Color(red: 0.902, green: 0.914, blue: 1.0), "🌙")
        default: return (Color(red: 0.957, green: 0.957, blue: 0.965), "🍽️")
        }
    }

    private var mealLabel: String {
        mealType?.displayName ?? "Приём пищи"
    }

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 12).fill(palette.bg)
                Text(palette.emoji).font(.system(size: 18))
            }
            .frame(width: 40, height: 40)

            VStack(alignment: .leading, spacing: 2) {
                if let dayLabel = buildDayLabel() {
                    Text(dayLabel.uppercased())
                        .font(.system(size: 16, weight: .regular))
                        .foregroundColor(Palette.mutedText)
                        .tracking(0.5)
                }
                Text("\(mealLabel), \(buildDateLabel())")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundColor(Palette.deepInk)
            }

            Spacer()
        }
        .padding(12)
        .background(RoundedRectangle(cornerRadius: 14).fill(.white))
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .strokeBorder(borderColor, lineWidth: 1.5)
        )
    }

    private func buildDateLabel() -> String {
        if let dateStr = plan.oneOffDate {
            let fmt = DateFormatter()
            fmt.locale = Locale(identifier: "ru_RU")
            fmt.dateFormat = "yyyy-MM-dd"
            if let date = fmt.date(from: dateStr) {
                let outFmt = DateFormatter()
                outFmt.locale = Locale(identifier: "ru_RU")
                outFmt.dateFormat = "d MMMM, EEEE"
                return outFmt.string(from: date)
            }
            return dateStr
        }

        if !plan.scheduleDays.isEmpty {
            return plan.scheduleDays.map { weekdayShort($0) }.joined(separator: ", ")
        }

        return String(plan.createdAt.prefix(10))
    }

    private func buildDayLabel() -> String? {
        guard let dateStr = plan.oneOffDate else { return nil }
        let fmt = DateFormatter()
        fmt.dateFormat = "yyyy-MM-dd"
        guard let date = fmt.date(from: dateStr) else { return nil }
        let calendar = Calendar.current
        if calendar.isDateInToday(date) { return "Сегодня" }
        let dayFmt = DateFormatter()
        dayFmt.locale = Locale(identifier: "ru_RU")
        dayFmt.dateFormat = "EEEE"
        return dayFmt.string(from: date)
    }

    private func weekdayShort(_ wire: String) -> String {
        switch wire.uppercased() {
        case "MONDAY": return "Пн"
        case "TUESDAY": return "Вт"
        case "WEDNESDAY": return "Ср"
        case "THURSDAY": return "Чт"
        case "FRIDAY": return "Пт"
        case "SATURDAY": return "Сб"
        case "SUNDAY": return "Вс"
        default: return wire
        }
    }
}
