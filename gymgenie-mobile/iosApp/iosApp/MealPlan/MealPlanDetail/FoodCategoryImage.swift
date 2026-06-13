import Foundation
import Shared

func foodCategoryImageName(_ category: FoodCategory) -> String {
    if category == FoodCategory.meat { return "ic_food_meat" }
    if category == FoodCategory.fish { return "ic_food_fish" }
    if category == FoodCategory.dairy { return "ic_food_milk" }
    if category == FoodCategory.eggs { return "ic_food_egg" }
    if category == FoodCategory.grains { return "ic_food_cereals" }
    if category == FoodCategory.legumes { return "ic_food_legumes" }
    if category == FoodCategory.vegetables { return "ic_food_vegetables" }
    if category == FoodCategory.fruits { return "ic_food_fruits" }
    if category == FoodCategory.nutsSeeds { return "ic_food_nuts" }
    if category == FoodCategory.oils { return "ic_food_oil" }
    return "ic_food_other"
}

func deriveFoodImageName(_ dishName: String) -> String {
    let lower = dishName.lowercased()
    if containsAny(lower, "курица", "куриц", "грудка", "мяс", "говядин", "свинин", "стейк", "индейк") { return "ic_food_meat" }
    if containsAny(lower, "рыб", "лосось", "тунец", "форель", "сёмга", "семга") { return "ic_food_fish" }
    if containsAny(lower, "яйц", "омлет") { return "ic_food_egg" }
    if containsAny(lower, "молок", "творог", "йогурт", "кефир", "сыр", "сметан") { return "ic_food_milk" }
    if containsAny(lower, "каш", "овсян", "рис", "гречк", "хлеб", "тост", "паст", "макарон", "спагетт") { return "ic_food_cereals" }
    if containsAny(lower, "салат", "овощ", "капуст", "помидор", "огурц", "морков", "свекл", "брокколи") { return "ic_food_vegetables" }
    if containsAny(lower, "фрукт", "яблок", "банан", "апельсин", "ягод") { return "ic_food_fruits" }
    if containsAny(lower, "орех", "миндал", "фундук", "кешью", "семечк") { return "ic_food_nuts" }
    if containsAny(lower, "масл", "оливк") { return "ic_food_oil" }
    if containsAny(lower, "фасол", "горох", "чечевиц", "нут", "бобов") { return "ic_food_legumes" }
    return "ic_food_other"
}

func foodCategoryFromWireValue(_ value: String?) -> FoodCategory? {
    guard let value = value else { return nil }
    switch value {
    case "MEAT": return FoodCategory.meat
    case "FISH": return FoodCategory.fish
    case "DAIRY": return FoodCategory.dairy
    case "EGGS": return FoodCategory.eggs
    case "GRAINS": return FoodCategory.grains
    case "LEGUMES": return FoodCategory.legumes
    case "VEGETABLES": return FoodCategory.vegetables
    case "FRUITS": return FoodCategory.fruits
    case "NUTS_SEEDS": return FoodCategory.nutsSeeds
    case "OILS": return FoodCategory.oils
    case "OTHER": return FoodCategory.other
    default: return nil
    }
}

private func containsAny(_ haystack: String, _ needles: String...) -> Bool {
    needles.contains { haystack.contains($0) }
}
