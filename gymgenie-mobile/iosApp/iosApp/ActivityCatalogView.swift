import SwiftUI

// MARK: - Activity catalog screen

struct ActivityCatalogView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var searchQuery: String = ""
    @State private var selectedGoal: GoalConfig? = nil

    private let categories: [ActivityCategoryItem] = [
        ActivityCategoryItem(emoji: "🏃", title: "Физическая активность", subtitle: "Шаги, кардио, бег", background: Palette.pastelCoral, unit: "шагов", defaultValue: 10000, step: 500),
        ActivityCategoryItem(emoji: "💧", title: "Питьевой режим", subtitle: "Вода, напитки", background: Palette.pastelBlue, unit: "стаканов", defaultValue: 8, step: 1),
        ActivityCategoryItem(emoji: "😴", title: "Сон", subtitle: "Отслеживание сна", background: Palette.pastelLavender, unit: "часов", defaultValue: 8, step: 1),
        ActivityCategoryItem(emoji: "🧘", title: "Медитация", subtitle: "Осознанность, дыхание", background: Palette.pastelGreen, unit: "минут", defaultValue: 15, step: 5),
        ActivityCategoryItem(emoji: "⭐", title: "Кастомная цель", subtitle: "Создай свою активность", background: Palette.pastelYellow, unit: "раз", defaultValue: 1, step: 1),
    ]

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12),
    ]

    var body: some View {
        ZStack {
            Palette.warmOffWhite.ignoresSafeArea()

            VStack(spacing: 0) {
                navigationBar
                searchBar

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 12) {
                        gridSection
                        fullWidthSection
                        Spacer().frame(height: 24)
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                }
            }
        }
        .navigationBarHidden(true)
        .sheet(item: $selectedGoal) { goal in
            ActivityGoalSettingsView(
                emoji: goal.emoji,
                title: goal.title,
                unit: goal.unit,
                defaultValue: goal.defaultValue,
                step: goal.step,
                onDismiss: { selectedGoal = nil }
            )
        }
    }

    private var filteredCategories: [ActivityCategoryItem] {
        if searchQuery.isEmpty { return categories }
        let query = searchQuery.lowercased()
        return categories.filter {
            $0.title.lowercased().contains(query) || $0.subtitle.lowercased().contains(query)
        }
    }

    // MARK: - Navigation bar

    private var navigationBar: some View {
        HStack {
            Button(action: { dismiss() }) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
                    .frame(width: 40, height: 40)
                    .background(Circle().fill(Palette.softCard))
            }

            Spacer()

            Text("Каталог активностей")
                .font(.system(size: 17, weight: .heavy))
                .foregroundColor(Palette.deepInk)

            Spacer()

            Spacer().frame(width: 40, height: 40)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 8)
    }

    // MARK: - Search

    private var searchBar: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .foregroundColor(Palette.mutedText)
            TextField("Найти активность...", text: $searchQuery)
                .font(.system(size: 15))
                .foregroundColor(Palette.deepInk)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(RoundedRectangle(cornerRadius: 16).fill(Palette.softCard))
        .padding(.horizontal, 20)
        .padding(.top, 8)
    }

    // MARK: - Grid

    private var gridSection: some View {
        let gridItems = filteredCategories.count % 2 == 0
            ? filteredCategories
            : Array(filteredCategories.dropLast())

        return LazyVGrid(columns: columns, spacing: 12) {
            ForEach(gridItems) { category in
                CategoryCard(category: category, fullWidth: false) {
                    selectedGoal = GoalConfig(from: category)
                }
            }
        }
    }

    @ViewBuilder
    private var fullWidthSection: some View {
        if filteredCategories.count % 2 == 1, let last = filteredCategories.last {
            CategoryCard(category: last, fullWidth: true) {
                selectedGoal = GoalConfig(from: last)
            }
        }
    }
}

// MARK: - Models

struct ActivityCategoryItem: Identifiable {
    let id = UUID()
    let emoji: String
    let title: String
    let subtitle: String
    let background: Color
    let unit: String
    let defaultValue: Int
    let step: Int
}

struct GoalConfig: Identifiable {
    let id = UUID()
    let emoji: String
    let title: String
    let unit: String
    let defaultValue: Int
    let step: Int

    init(from category: ActivityCategoryItem) {
        self.emoji = category.emoji
        self.title = category.title
        self.unit = category.unit
        self.defaultValue = category.defaultValue
        self.step = category.step
    }
}

// MARK: - Card

private struct CategoryCard: View {
    let category: ActivityCategoryItem
    let fullWidth: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 10) {
                Text(category.emoji)
                    .font(.system(size: fullWidth ? 44 : 40))
                Text(category.title)
                    .font(.system(size: 16, weight: .heavy))
                    .foregroundColor(Palette.deepInk)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
                Text(category.subtitle)
                    .font(.system(size: 12))
                    .foregroundColor(Palette.mutedText)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(20)
            .frame(maxWidth: .infinity, minHeight: fullWidth ? 120 : 150, alignment: .topLeading)
            .background(RoundedRectangle(cornerRadius: 24).fill(category.background))
        }
        .buttonStyle(.plain)
    }
}
