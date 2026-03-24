import SwiftUI
import Shared

struct PaywallView: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var viewModel = PaywallViewModelWrapper()

    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890)
    private let backgroundColor = Color(red: 0.961, green: 0.969, blue: 0.980)
    private let darkColor = Color(red: 0.102, green: 0.102, blue: 0.180)

    var body: some View {
        ZStack(alignment: .top) {
            backgroundColor.edgesIgnoringSafeArea(.all)

            VStack(spacing: 0) {
                // Top bar
                HStack {
                    Button(action: {
                        appState.completeOnboardingFlow()
                    }) {
                        Image(systemName: "xmark")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(darkColor)
                            .frame(width: 32, height: 32)
                            .background(Circle().fill(Color.gray.opacity(0.15)))
                    }

                    Spacer()

                    Button(action: {
                        // TODO:GymGenie - Replace with real restore logic (StoreKit)
                    }) {
                        Text("Восстановить")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(.gray)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 24) {
                        // Title
                        Text("Получи доступ ко всем функциям")
                            .font(.system(size: 26, weight: .bold))
                            .foregroundColor(darkColor)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 24)
                            .padding(.top, 20)

                        // Features list
                        featuresSection

                        // Plans
                        plansSection

                        // Purchase button
                        GymGenieButton(title: "Получить полный доступ") {
                            viewModel.purchase()
                        }
                        .padding(.horizontal, 24)

                        // Terms note
                        Text("Автопродление. Отмена в любое время.")
                            .font(.system(size: 12))
                            .foregroundColor(.gray)
                            .padding(.bottom, 24)
                    }
                }
            }
        }
        .onChange(of: viewModel.purchaseSuccess) { success in
            if success {
                viewModel.consumePurchaseSuccess()
                appState.completePurchase()
            }
        }
    }

    // MARK: - Features

    private var featuresSection: some View {
        let features = [
            "Функция ИИ",
            "Функция \u{00AB}План\u{00BB}",
            "Функция \u{00AB}Следующий\u{00BB}",
            "Функция \u{00AB}Ещё\u{00BB}",
        ]

        return VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(features.enumerated()), id: \.offset) { index, feature in
                FeatureListItem(
                    text: feature,
                    isFirst: index == 0,
                    isLast: index == features.count - 1
                )
            }
        }
        .padding(.horizontal, 40)
    }

    // MARK: - Plans

    private var plansSection: some View {
        VStack(spacing: 12) {
            PlanCard(
                isSelected: viewModel.selectedPlan == .monthly,
                title: "1 Месяц",
                price: "$7.99 / МО",
                onTap: { viewModel.selectPlan(.monthly) }
            )

            PlanCard(
                isSelected: viewModel.selectedPlan == .yearly,
                badge: "ПОПУЛЯРНО",
                title: "1 Год",
                price: "$59.99",
                originalPrice: "$95.88",
                subtitle: "$4.99 / МО",
                onTap: { viewModel.selectPlan(.yearly) }
            )
        }
        .padding(.horizontal, 24)
    }
}
