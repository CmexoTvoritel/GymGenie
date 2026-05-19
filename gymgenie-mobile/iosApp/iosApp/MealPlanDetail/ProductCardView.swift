import SwiftUI
import Shared

private let borderColor = Color(red: 0.929, green: 0.929, blue: 0.937)
private let softBg = Color(red: 0.957, green: 0.957, blue: 0.965)
private let proteinBg = Color(red: 0.882, green: 0.945, blue: 1.0)
private let fatBg = Color(red: 1.0, green: 0.957, blue: 0.863)
private let carbsBg = Color(red: 0.910, green: 0.969, blue: 0.910)

struct ProductCardView: View {
    let dish: MealPlanDetailDish
    @State private var expanded = false

    private var emoji: String { deriveEmoji(dish.name) }
    private var gramsLabel: String { dish.portionDescription ?? "Порция" }
    private var kcal: Int { dish.calories?.intValue ?? 0 }
    private var protein: Int { dish.proteinG?.intValue ?? 0 }
    private var fat: Int { dish.fatG?.intValue ?? 0 }
    private var carbs: Int { dish.carbsG?.intValue ?? 0 }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button(action: { withAnimation(.easeInOut(duration: 0.25)) { expanded.toggle() } }) {
                VStack(alignment: .leading, spacing: 10) {
                    HStack(spacing: 12) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 14).fill(softBg)
                            Text(emoji).font(.system(size: 22))
                        }
                        .frame(width: 48, height: 48)

                        VStack(alignment: .leading, spacing: 2) {
                            Text(dish.name)
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(Palette.deepInk)
                                .lineLimit(2)
                                .multilineTextAlignment(.leading)
                            Text(gramsLabel)
                                .font(.system(size: 16))
                                .foregroundColor(Palette.deepInk)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)

                        VStack(alignment: .trailing, spacing: 0) {
                            Text("\(kcal)")
                                .font(.system(size: 18, weight: .heavy))
                                .foregroundColor(Palette.deepInk)
                            Text("ККАЛ")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(Palette.mutedText)
                        }
                    }

                    HStack(spacing: 6) {
                        MacroChipView(label: "Б", value: "\(protein)г", bg: proteinBg)
                        MacroChipView(label: "Ж", value: "\(fat)г", bg: fatBg)
                        MacroChipView(label: "У", value: "\(carbs)г", bg: carbsBg)
                    }
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            if expanded {
                per100Section
                    .padding(.top, 12)
                    .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 18).fill(.white))
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .strokeBorder(borderColor, lineWidth: 1.5)
        )
    }

    private var per100Section: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("На 100 г — справочно")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Palette.mutedText)
                .tracking(0.3)

            let gramsNumber = parseGramsNumber(dish.portionDescription)
            let factor = gramsNumber > 0 ? 100.0 / gramsNumber : 0.0
            let kcalPer100 = Int(Double(kcal) * factor)
            let proteinPer100 = Int(Double(protein) * factor)
            let fatPer100 = Int(Double(fat) * factor)
            let carbsPer100 = Int(Double(carbs) * factor)

            HStack(spacing: 6) {
                Per100Cell(label: "Ккал", value: "\(kcalPer100)")
                Per100Cell(label: "Б", value: "\(proteinPer100)г")
                Per100Cell(label: "Ж", value: "\(fatPer100)г")
                Per100Cell(label: "У", value: "\(carbsPer100)г")
            }
        }
    }

    private func parseGramsNumber(_ text: String?) -> Double {
        guard let text, !text.isEmpty else { return 0 }
        let pattern = #"(\d+(?:\.\d+)?)"#
        guard let regex = try? NSRegularExpression(pattern: pattern),
              let match = regex.firstMatch(in: text, range: NSRange(text.startIndex..., in: text)),
              let range = Range(match.range(at: 1), in: text) else {
            return 0
        }
        return Double(text[range]) ?? 0
    }

    private func deriveEmoji(_ dishName: String) -> String {
        let lower = dishName.lowercased()
        if lower.contains("курица") || lower.contains("куриц") || lower.contains("грудка") { return "🍗" }
        if lower.contains("рыб") || lower.contains("лосось") || lower.contains("тунец") || lower.contains("форель") { return "🐟" }
        if lower.contains("яйц") || lower.contains("омлет") { return "🥚" }
        if lower.contains("салат") || lower.contains("овощ") { return "🥗" }
        if lower.contains("каш") || lower.contains("овсян") || lower.contains("рис") || lower.contains("гречк") { return "🌾" }
        if lower.contains("хлеб") || lower.contains("тост") { return "🍞" }
        if lower.contains("молок") || lower.contains("творог") || lower.contains("йогурт") || lower.contains("кефир") { return "🥛" }
        if lower.contains("фрукт") || lower.contains("яблок") || lower.contains("банан") { return "🍎" }
        if lower.contains("орех") { return "🥜" }
        if lower.contains("суп") || lower.contains("бульон") { return "🍲" }
        if lower.contains("мяс") || lower.contains("говядин") || lower.contains("свинин") || lower.contains("стейк") { return "🥩" }
        if lower.contains("паст") || lower.contains("макарон") || lower.contains("спагетт") { return "🍝" }
        if lower.contains("чай") || lower.contains("кофе") { return "☕" }
        if lower.contains("сок") || lower.contains("смузи") { return "🥤" }
        return "🍽️"
    }
}

private struct MacroChipView: View {
    let label: String
    let value: String
    let bg: Color

    var body: some View {
        HStack(spacing: 4) {
            Text(label)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Palette.deepInk)
            Text(value)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(Palette.deepInk)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background(RoundedRectangle(cornerRadius: 10).fill(bg))
    }
}

private struct Per100Cell: View {
    let label: String
    let value: String

    var body: some View {
        VStack(spacing: 2) {
            Text(label)
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(Palette.mutedText)
            Text(value)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Palette.deepInk)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .padding(.horizontal, 6)
        .background(RoundedRectangle(cornerRadius: 8).fill(Color(red: 0.957, green: 0.957, blue: 0.965)))
    }
}
