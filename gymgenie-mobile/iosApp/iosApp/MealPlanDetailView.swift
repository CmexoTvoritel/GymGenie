import SwiftUI
import Shared

/// Read-only detail view for a saved AI meal plan.
///
/// Layout (top → bottom):
///  - Header with close button and the plan name
///  - Optional plan description + summary stats (goal / total kcal / meal count)
///  - One section per meal slot (breakfast / lunch / dinner)
///  - Each section lists its dishes with portion + macros
///
/// State machine:
///  - first appear → `viewModel.load()` → loading spinner
///  - success → render `plan` (always non-nil in the success branch)
///  - failure → inline error card with retry CTA
struct MealPlanDetailView: View {
    let planId: String
    let onClose: () -> Void

    @StateObject private var viewModel: MealPlanDetailViewModelWrapper

    init(planId: String, onClose: @escaping () -> Void) {
        self.planId = planId
        self.onClose = onClose
        _viewModel = StateObject(wrappedValue: MealPlanDetailViewModelWrapper(planId: planId))
    }

    var body: some View {
        ZStack {
            Palette.warmOffWhite.ignoresSafeArea()

            VStack(spacing: 0) {
                header

                if viewModel.isLoading && viewModel.plan == nil {
                    Spacer()
                    ProgressView().scaleEffect(1.2).tint(Palette.coral)
                    Spacer()
                } else if let error = viewModel.errorMessage, viewModel.plan == nil {
                    Spacer()
                    errorView(message: error)
                    Spacer()
                } else if let plan = viewModel.plan {
                    contentView(plan: plan)
                } else {
                    Spacer()
                }
            }
        }
        .onAppear { viewModel.load() }
    }

    // MARK: - Header

    private var header: some View {
        HStack(spacing: 4) {
            Button(action: onClose) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
                    .frame(width: 44, height: 44)
            }
            Text(viewModel.plan?.name ?? "Рацион")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(Palette.deepInk)
                .lineLimit(1)
            Spacer()
        }
        .padding(.leading, 4)
        .padding(.trailing, 16)
        .padding(.top, 12)
        .padding(.bottom, 4)
    }

    // MARK: - Error

    private func errorView(message: String) -> some View {
        VStack(spacing: 12) {
            Text("⚠️").font(.system(size: 36))
            Text(message)
                .font(.system(size: 14))
                .foregroundColor(Palette.mutedText)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            Button(action: { viewModel.retry() }) {
                Text("Повторить")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 10)
                    .background(Capsule().fill(Palette.coral))
            }
            .buttonStyle(.plain)
        }
    }

    // MARK: - Content

    private func contentView(plan: MealPlanDetail) -> some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 20) {
                summaryCard(plan: plan)

                ForEach(plan.meals, id: \.id) { meal in
                    mealSection(meal: meal)
                }

                Color.clear.frame(height: 24)
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)
            .padding(.bottom, 16)
        }
    }

    private func summaryCard(plan: MealPlanDetail) -> some View {
        let goalLabel = MealGoal.companion.fromWireValue(value: plan.goal)?.displayName

        return VStack(alignment: .leading, spacing: 12) {
            Text(plan.name)
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(Palette.deepInk)

            if let desc = plan.description_, !desc.isEmpty {
                Text(desc)
                    .font(.system(size: 14))
                    .foregroundColor(Palette.mutedText)
                    .lineSpacing(2)
            }

            HStack(spacing: 8) {
                if let goal = goalLabel {
                    SummaryChip(icon: "target", text: goal)
                }
                if let kcal = plan.totalCalories?.intValue, kcal > 0 {
                    SummaryChip(icon: "flame.fill", text: "\(kcal) ккал")
                }
                SummaryChip(icon: "list.bullet", text: "\(plan.meals.count) приёма")
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .cornerRadius(20)
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .strokeBorder(Color(red: 0.929, green: 0.929, blue: 0.937), lineWidth: 1.5)
        )
    }

    private func mealSection(meal: MealPlanDetailMeal) -> some View {
        let mealType = AiMealType.companion.fromWireValue(value: meal.mealType)

        return VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .center, spacing: 10) {
                Text(mealType?.emoji ?? "🍽️").font(.system(size: 22))
                VStack(alignment: .leading, spacing: 2) {
                    Text(mealType?.displayName ?? meal.name)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                    if mealType?.displayName != meal.name && !meal.name.isEmpty {
                        Text(meal.name)
                            .font(.system(size: 12))
                            .foregroundColor(Palette.mutedText)
                    }
                }
                Spacer()
                if let kcal = meal.estimatedCalories?.intValue, kcal > 0 {
                    Text("\(kcal) ккал")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(Palette.coral)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Capsule().fill(Palette.coral.opacity(0.12)))
                }
            }

            if meal.dishes.isEmpty {
                Text("Список блюд пуст")
                    .font(.system(size: 13))
                    .foregroundColor(Palette.mutedText)
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.white)
                    .cornerRadius(12)
            } else {
                VStack(spacing: 8) {
                    ForEach(meal.dishes, id: \.id) { dish in
                        DishRow(dish: dish)
                    }
                }
            }
        }
    }
}

// MARK: - Sub-components

private struct DishRow: View {
    let dish: MealPlanDetailDish

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(dish.name)
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(Palette.deepInk)

            if let desc = dish.description_, !desc.isEmpty {
                Text(desc)
                    .font(.system(size: 12))
                    .foregroundColor(Palette.mutedText)
                    .lineSpacing(2)
            }

            if let portion = dish.portionDescription, !portion.isEmpty {
                Text(portion)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(Color(red: 0.361, green: 0.361, blue: 0.388))
            }

            macrosRow
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .cornerRadius(14)
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .strokeBorder(Color(red: 0.929, green: 0.929, blue: 0.937), lineWidth: 1)
        )
    }

    private var macrosRow: some View {
        // Build macro chips lazily so absent fields don't render as "—".
        let chips: [(icon: String, label: String, color: Color)] = {
            var out: [(String, String, Color)] = []
            if let kcal = dish.calories?.intValue, kcal > 0 {
                out.append(("flame.fill", "\(kcal) ккал", Palette.coral))
            }
            if let p = dish.proteinG?.intValue, p > 0 {
                out.append(("p.circle.fill", "\(p) Б", Color(red: 0.298, green: 0.831, blue: 0.482)))
            }
            if let f = dish.fatG?.intValue, f > 0 {
                out.append(("f.circle.fill", "\(f) Ж", Color(red: 0.984, green: 0.502, blue: 0.580)))
            }
            if let c = dish.carbsG?.intValue, c > 0 {
                out.append(("c.circle.fill", "\(c) У", Color(red: 0.231, green: 0.671, blue: 0.969)))
            }
            return out
        }()

        return Group {
            if chips.isEmpty {
                EmptyView()
            } else {
                HStack(spacing: 6) {
                    ForEach(Array(chips.enumerated()), id: \.offset) { _, chip in
                        HStack(spacing: 4) {
                            Image(systemName: chip.icon).font(.system(size: 10, weight: .semibold))
                            Text(chip.label).font(.system(size: 11, weight: .semibold))
                        }
                        .foregroundColor(chip.color)
                        .padding(.horizontal, 8).padding(.vertical, 4)
                        .background(Capsule().fill(chip.color.opacity(0.12)))
                    }
                }
                .padding(.top, 2)
            }
        }
    }
}

private struct SummaryChip: View {
    let icon: String
    let text: String

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: icon).font(.system(size: 11, weight: .semibold))
            Text(text).font(.system(size: 12, weight: .semibold))
        }
        .foregroundColor(Color(red: 0.227, green: 0.227, blue: 0.251))
        .padding(.horizontal, 10).padding(.vertical, 5)
        .background(RoundedRectangle(cornerRadius: 10).fill(Color(red: 0.957, green: 0.957, blue: 0.965)))
    }
}
