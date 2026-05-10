import SwiftUI
import Shared

struct PaywallView: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var viewModel = PaywallViewModelWrapper()

    private let darkColor = Color(red: 0.102, green: 0.102, blue: 0.180)

    var body: some View {
        ZStack {
            // White base background
            Color.white
                .ignoresSafeArea()

            // Decorative background image layered on top of white
            Image("ic_paywall_background")
                .resizable()
                .scaledToFill()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .clipped()
                .ignoresSafeArea()

            // Foreground content
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
                            .font(.system(size: 18, weight: .medium))
                            .foregroundColor(.gray)
                    }
                }
                .padding(.top, 8)

                Spacer().frame(height: 24)

                // Title
                Text("Получи доступ ко всем функциям")
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(darkColor)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 8)

                Spacer()

                // Features list (centered horizontally)
                featuresSection

                Spacer()

                // Plans
                plansSection

                Spacer().frame(height: 24)

                // Purchase button
                GymGenieButton(title: "Получить полный доступ", accentColor: Palette.coral) {
                    viewModel.purchase()
                }

                Spacer().frame(height: 12)

                // Terms note
                Text("Автопродление. Отмена в любое время.")
                    .font(.system(size: 12))
                    .foregroundColor(.gray)

                Spacer().frame(height: 24)
            }
            .padding(.horizontal, 24)
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
            "Личный ИИ-тренер",
            "Сбалансированный план питания",
            "Удобство, скорость и легкость",
            "Анализ прогресса и рекомендации",
        ]

        return VStack(alignment: .center, spacing: 0) {
            VStack(alignment: .leading, spacing: 0) {
                ForEach(Array(features.enumerated()), id: \.offset) { index, feature in
                    FeatureListItem(
                        text: feature,
                        isFirst: index == 0,
                        isLast: index == features.count - 1
                    )
                }
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 40)
    }

    // MARK: - Plans

    private var plansSection: some View {
        VStack(spacing: 12) {
            PlanCard(
                isSelected: viewModel.selectedPlan == .monthly,
                title: "1 Месяц",
                price: "799 ₽ / МО",
                onTap: { viewModel.selectPlan(.monthly) }
            )

            PlanCard(
                isSelected: viewModel.selectedPlan == .yearly,
                badge: "ПОПУЛЯРНО",
                title: "1 Год",
                price: "4 990 ₽",
                originalPrice: "7 590 ₽",
                subtitle: "416 ₽ / МО",
                onTap: { viewModel.selectPlan(.yearly) }
            )
        }
    }
}
