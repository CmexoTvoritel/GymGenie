import SwiftUI

struct SplashView: View {
    private let backgroundColor = Color(red: 0.98039, green: 0.97647, blue: 0.96863)
    private let onBackgroundColor = Color(red: 0.102, green: 0.102, blue: 0.180)
    private let coralColor = Color(red: 1.0, green: 0.353, blue: 0.235)

    var body: some View {
        ZStack {
            backgroundColor
                .ignoresSafeArea()

            Image("SplashLogo")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 200, height: 200)

            VStack {
                Spacer()
                HStack(spacing: 12) {
                    Text("Загружаем данные")
                        .font(.system(size: 24, weight: .semibold))
                        .foregroundColor(onBackgroundColor)

                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: coralColor))
                        .scaleEffect(1.3)
                }
                .padding(.bottom, 24)
            }
        }
    }
}

#Preview {
    SplashView()
}
