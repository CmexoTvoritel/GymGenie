import SwiftUI

struct PaywallView: View {
    @EnvironmentObject var appState: AppState
    @State private var selectedPlan: PaywallPlan = .yearly

    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890)
    private let greenColor = Color(red: 0.180, green: 0.800, blue: 0.443)
    private let backgroundColor = Color(red: 0.961, green: 0.969, blue: 0.980)
    private let darkColor = Color(red: 0.102, green: 0.102, blue: 0.180)

    enum PaywallPlan {
        case monthly
        case yearly
    }

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
                            // TODO:GymGenie - Replace with real purchase logic (StoreKit/BillingClient)
                            appState.completePurchase()
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
    }

    // MARK: - Features

    private var featuresSection: some View {
        VStack(alignment: .leading, spacing: 0) {
            let features = [
                "Функция ИИ",
                "Функция «План»",
                "Функция «Следующий»",
                "Функция «Ещё»",
            ]

            ForEach(Array(features.enumerated()), id: \.offset) { index, feature in
                HStack(spacing: 14) {
                    VStack(spacing: 0) {
                        if index > 0 {
                            Rectangle()
                                .fill(greenColor.opacity(0.3))
                                .frame(width: 2, height: 12)
                        } else {
                            Spacer().frame(width: 2, height: 12)
                        }

                        Circle()
                            .fill(greenColor)
                            .frame(width: 12, height: 12)

                        if index < features.count - 1 {
                            Rectangle()
                                .fill(greenColor.opacity(0.3))
                                .frame(width: 2, height: 12)
                        } else {
                            Spacer().frame(width: 2, height: 12)
                        }
                    }

                    Text(feature)
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(darkColor)
                }
            }
        }
        .padding(.horizontal, 40)
    }

    // MARK: - Plans

    private var plansSection: some View {
        VStack(spacing: 12) {
            // Monthly plan
            planCard(
                isSelected: selectedPlan == .monthly,
                badge: nil,
                title: "1 Месяц",
                price: "$7.99 / МО",
                originalPrice: nil
            ) {
                selectedPlan = .monthly
            }

            // Yearly plan
            planCard(
                isSelected: selectedPlan == .yearly,
                badge: "ПОПУЛЯРНО",
                title: "1 Год",
                price: "$59.99",
                originalPrice: "$95.88",
                subtitle: "$4.99 / МО"
            ) {
                selectedPlan = .yearly
            }
        }
        .padding(.horizontal, 24)
    }

    private func planCard(
        isSelected: Bool,
        badge: String?,
        title: String,
        price: String,
        originalPrice: String?,
        subtitle: String? = nil,
        onTap: @escaping () -> Void
    ) -> some View {
        Button(action: onTap) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    if let badge = badge {
                        Text(badge)
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(
                                Capsule().fill(greenColor)
                            )
                    }

                    Text(title)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(darkColor)
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 2) {
                    HStack(spacing: 6) {
                        if let originalPrice = originalPrice {
                            Text(originalPrice)
                                .font(.system(size: 14))
                                .foregroundColor(.gray)
                                .strikethrough()
                        }
                        Text(price)
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(darkColor)
                    }
                    if let subtitle = subtitle {
                        Text(subtitle)
                            .font(.system(size: 13))
                            .foregroundColor(.gray)
                    }
                }
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(.white)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(isSelected ? accentColor : Color.clear, lineWidth: 2)
                    )
            )
            .shadow(color: Color.black.opacity(0.05), radius: 4, y: 2)
        }
    }
}
