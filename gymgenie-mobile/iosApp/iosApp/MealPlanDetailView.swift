import SwiftUI
import Shared

struct MealPlanDetailView: View {
    let planId: String
    let onClose: () -> Void
    var onDeleted: (() -> Void)? = nil
    var onEdit: (() -> Void)? = nil

    @StateObject private var viewModel: MealPlanDetailViewModelWrapper
    @State private var showDeleteDialog = false

    init(planId: String, onClose: @escaping () -> Void, onDeleted: (() -> Void)? = nil, onEdit: (() -> Void)? = nil) {
        self.planId = planId
        self.onClose = onClose
        self.onDeleted = onDeleted
        self.onEdit = onEdit
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
                    detailContent(plan: plan)
                } else {
                    Spacer()
                }
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .onAppear { viewModel.load() }
        .onChange(of: viewModel.isDeleted) { deleted in
            if deleted {
                if let onDeleted {
                    onDeleted()
                } else {
                    onClose()
                }
            }
        }
        .alert("Удалить план питания?", isPresented: $showDeleteDialog) {
            Button("Отмена", role: .cancel) {}
            Button("Удалить", role: .destructive) {
                viewModel.delete()
            }
            .disabled(viewModel.isDeleting)
        } message: {
            Text("План питания и все добавленные продукты будут удалены. Действие нельзя отменить.")
        }
    }

    private var header: some View {
        HStack(spacing: 4) {
            Button(action: onClose) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
                    .frame(width: 44, height: 44)
            }

            Text(titleText)
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

    private var titleText: String {
        guard let plan = viewModel.plan else { return "" }
        let firstMealType = plan.meals.first?.mealType ?? ""
        return AiMealType.companion.fromWireValue(value: firstMealType)?.displayName ?? plan.name
    }

    private func errorView(message: String) -> some View {
        VStack(spacing: 12) {
            Text("⚠️").font(.system(size: 40))
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
                    .background(Capsule().fill(Palette.accentOrange))
            }
            .buttonStyle(.plain)
        }
    }

    private func detailContent(plan: MealPlanDetail) -> some View {
        let allDishes = plan.meals.flatMap { ($0.dishes as? [MealPlanDetailDish]) ?? [] }
        let totalKcal = allDishes.reduce(0) { $0 + ($1.calories?.intValue ?? 0) }
        let totalProtein = allDishes.reduce(0) { $0 + ($1.proteinG?.intValue ?? 0) }
        let totalFat = allDishes.reduce(0) { $0 + ($1.fatG?.intValue ?? 0) }
        let totalCarbs = allDishes.reduce(0) { $0 + ($1.carbsG?.intValue ?? 0) }

        return VStack(spacing: 0) {
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 12) {
                    ScheduleChipView(plan: plan)

                    HeroMacrosCard(
                        totalKcal: totalKcal,
                        proteinG: totalProtein,
                        fatG: totalFat,
                        carbsG: totalCarbs
                    )

                    Text("ПРОДУКТЫ: \(allDishes.count) \(productDeclension(allDishes.count))")
                        .font(.system(size: 14.5, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .tracking(0.5)
                        .padding(.top, 8)

                    ForEach(allDishes, id: \.id) { dish in
                        ProductCardView(dish: dish)
                    }

                    Color.clear.frame(height: 12)
                }
                .padding(.horizontal, 20)
                .padding(.top, 12)
            }

            MealPlanBottomActionBar(
                onDelete: { showDeleteDialog = true },
                onEdit: { onEdit?() }
            )
        }
    }

    private func productDeclension(_ count: Int) -> String {
        let mod100 = count % 100
        let mod10 = count % 10
        if mod100 >= 11 && mod100 <= 19 {
            return "продуктов"
        }
        if mod10 == 1 {
            return "продукт"
        }
        if mod10 >= 2 && mod10 <= 4 {
            return "продукта"
        }
        return "продуктов"
    }
}
