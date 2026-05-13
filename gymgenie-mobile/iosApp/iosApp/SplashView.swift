import SwiftUI

/// Branded launch screen displayed while `AppState` resolves the initial
/// destination (onboarding flag, persisted token, profile fetch). Routing
/// is driven entirely by `AppState`; this view is intentionally stateless
/// and renders visuals only.
///
/// Layout: centered logo + title + subtitle block, with a spinner pinned
/// near the bottom so it does not visually compete with the brand mark.
///
/// The logo is currently a styled "G" placeholder; swap `SplashLogo` for
/// an `Image("...")` once a dedicated brand asset is available.
struct SplashView: View {
    // Matches Android `Background` (#F5F7FA) for cross-platform parity.
    private let backgroundColor = Color(red: 0.961, green: 0.969, blue: 0.980)
    private let onBackgroundColor = Color(red: 0.102, green: 0.102, blue: 0.180) // #1A1A2E

    var body: some View {
        ZStack {
            backgroundColor
                .ignoresSafeArea()

            VStack(spacing: 0) {
                SplashLogo()

                Spacer().frame(height: 24)

                Text("GymGenie")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(onBackgroundColor)
                    .multilineTextAlignment(.center)

                Spacer().frame(height: 8)

                Text("Загружаем ваши данные...")
                    .font(.system(size: 14))
                    .foregroundColor(Palette.mutedText)
                    .multilineTextAlignment(.center)
            }
            .padding(.horizontal, 32)

            VStack {
                Spacer()
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: Palette.accentOrange))
                    .scaleEffect(1.2)
                    .padding(.bottom, 64)
            }
        }
    }
}

/// Placeholder logo badge — renders the brand initial inside a tinted
/// circle. Mirrors the Android `SplashLogo` so the two platforms stay
/// visually aligned until a final brand asset is provided.
private struct SplashLogo: View {
    var body: some View {
        ZStack {
            Circle()
                .fill(Palette.accentOrange)
                .frame(width: 112, height: 112)

            Text("G")
                .font(.system(size: 56, weight: .bold))
                .foregroundColor(.white)
        }
    }
}

#Preview {
    SplashView()
}
