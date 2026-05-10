import SwiftUI

struct AuthView: View {
    @EnvironmentObject var appState: AppState
    @ObservedObject var viewModel: AuthViewModelWrapper

    @State private var isLoginMode: Bool
    @State private var usernameField: String = ""
    @State private var emailField: String = ""
    @State private var passwordField: String = ""

    private let coralColor = Color(.sRGB, red: 1.0, green: 0.353, blue: 0.235)
    private let backgroundColor = Color(red: 0.961, green: 0.969, blue: 0.980)
    private let textPrimary = Color(red: 0.1, green: 0.15, blue: 0.3)

    init(viewModel: AuthViewModelWrapper, initialIsLogin: Bool = true) {
        self.viewModel = viewModel
        self._isLoginMode = State(initialValue: initialIsLogin)
    }

    var body: some View {
        GeometryReader { geo in
            let imageAreaHeight = geo.size.height * 0.38
            let cardTopOffset = imageAreaHeight - 32

            ZStack(alignment: .top) {
                // Full-screen coral tint — visible behind the card's rounded top
                // corners, so the curved edge sits on coral instead of gray.
                coralColor.opacity(0.20)
                    .edgesIgnoringSafeArea(.all)

                // Top illustration area — image bottom-aligned and pushed up 32pt
                // so its bottom edge meets the card's visual top. Both
                // illustrations are stacked and opacity-cross-faded in lockstep
                // with the rest of the mode-dependent UI, so the image swap
                // stays in sync with the card content.
                ZStack(alignment: .bottom) {
                    Image("ic_login")
                        .resizable()
                        .scaledToFit()
                        .padding(.horizontal, 20)
                        .frame(maxWidth: .infinity)
                        .padding(.bottom, 32)
                        .opacity(isLoginMode ? 1 : 0)
                        .animation(.easeInOut(duration: 0.3), value: isLoginMode)
                    Image("ic_registration")
                        .resizable()
                        .scaledToFit()
                        .padding(.horizontal, 20)
                        .frame(maxWidth: .infinity)
                        .padding(.bottom, 32)
                        .opacity(isLoginMode ? 0 : 1)
                        .animation(.easeInOut(duration: 0.3), value: isLoginMode)
                }
                .frame(width: geo.size.width, height: imageAreaHeight)

                // Bottom white card — overlaps the coral region by 32pt so its
                // rounded top corners sit on the coral tint, not the gray app bg.
                VStack(spacing: 0) {
                    Spacer().frame(height: cardTopOffset)

                    ScrollView {
                        VStack(spacing: 0) {
                        // Title — both variants stacked, opacity-cross-faded.
                        ZStack {
                            Text("С возвращением")
                                .font(.system(size: 28, weight: .bold))
                                .foregroundColor(textPrimary)
                                .opacity(isLoginMode ? 1 : 0)
                                .allowsHitTesting(isLoginMode)
                            Text("Добро пожаловать")
                                .font(.system(size: 28, weight: .bold))
                                .foregroundColor(textPrimary)
                                .opacity(isLoginMode ? 0 : 1)
                                .allowsHitTesting(!isLoginMode)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)

                        Spacer().frame(height: 24)

                        // Fields area — register VStack defines ZStack height,
                        // login VStack is centered (since it has fewer fields).
                        ZStack {
                            // Register fields — define ZStack height (3 fields).
                            VStack(spacing: 16) {
                                GymGenieTextField(
                                    placeholder: "Имя",
                                    text: $usernameField,
                                    icon: "person",
                                    accentColor: coralColor
                                )
                                GymGenieTextField(
                                    placeholder: "Email",
                                    text: $emailField,
                                    icon: "envelope",
                                    accentColor: coralColor
                                )
                                .textContentType(.emailAddress)
                                .keyboardType(.emailAddress)
                                GymGenieTextField(
                                    placeholder: "Пароль",
                                    text: $passwordField,
                                    icon: "lock",
                                    isSecure: true,
                                    accentColor: coralColor
                                )
                            }
                            .opacity(isLoginMode ? 0 : 1)
                            .allowsHitTesting(!isLoginMode)

                            // Login fields — ZStack centers them (2 fields).
                            VStack(spacing: 16) {
                                GymGenieTextField(
                                    placeholder: "Email",
                                    text: $emailField,
                                    icon: "envelope",
                                    accentColor: coralColor
                                )
                                .textContentType(.emailAddress)
                                .keyboardType(.emailAddress)
                                GymGenieTextField(
                                    placeholder: "Пароль",
                                    text: $passwordField,
                                    icon: "lock",
                                    isSecure: true,
                                    accentColor: coralColor
                                )
                            }
                            .opacity(isLoginMode ? 1 : 0)
                            .allowsHitTesting(isLoginMode)
                        }
                        .animation(.easeInOut(duration: 0.3), value: isLoginMode)

                        if let error = viewModel.errorMessage {
                            Spacer().frame(height: 8)
                            HStack {
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .foregroundColor(.red)
                                Text(error)
                                    .font(.system(size: 13))
                                    .foregroundColor(.red)
                                Spacer()
                            }
                            .padding(12)
                            .background(
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(Color.red.opacity(0.1))
                            )
                        }

                        Spacer().frame(height: 24)

                        // Primary action — both variants stacked, only visible one
                        // is interactive. Coral accent per design.
                        ZStack {
                            GymGenieButton(
                                title: "Войти",
                                isLoading: viewModel.isLoading,
                                isEnabled: isLoginMode,
                                accentColor: coralColor,
                                fontSize: 18
                            ) {
                                viewModel.login()
                            }
                            .opacity(isLoginMode ? 1 : 0)
                            .allowsHitTesting(isLoginMode)

                            GymGenieButton(
                                title: "Зарегистрироваться",
                                isLoading: viewModel.isLoading,
                                isEnabled: !isLoginMode,
                                accentColor: coralColor,
                                fontSize: 18
                            ) {
                                viewModel.register()
                            }
                            .opacity(isLoginMode ? 0 : 1)
                            .allowsHitTesting(!isLoginMode)
                        }
                        .animation(.easeInOut(duration: 0.3), value: isLoginMode)

                        Spacer().frame(height: 24)

                        // Static section — divider + social row never transitions.
                        HStack {
                            Rectangle()
                                .fill(Color.gray.opacity(0.3))
                                .frame(height: 1)
                            Text("Войдите через")
                                .font(.system(size: 15))
                                .foregroundColor(.gray)
                                .layoutPriority(1)
                            Rectangle()
                                .fill(Color.gray.opacity(0.3))
                                .frame(height: 1)
                        }

                        Spacer().frame(height: 16)

                        HStack(spacing: 20) {
                            SocialIconButton(label: "G")
                            SocialIconButton(systemImage: "apple.logo")
                            SocialIconButton(label: "VK")
                            SocialIconButton(systemImage: "paperplane.fill")
                        }

                        Spacer().frame(height: 24)

                        // Bottom mode-switch link — both labels stacked, only the
                        // visible one captures taps so the hidden one can't fire.
                        ZStack {
                            HStack(spacing: 4) {
                                Text("Еще нет аккаунта?")
                                    .font(.system(size: 17))
                                    .foregroundColor(.gray)
                                Button(action: { switchMode(toLogin: false) }) {
                                    Text("Зарегистрируйтесь")
                                        .font(.system(size: 17, weight: .semibold))
                                        .foregroundColor(coralColor)
                                }
                                .buttonStyle(.plain)
                            }
                            .opacity(isLoginMode ? 1 : 0)
                            .allowsHitTesting(isLoginMode)

                            HStack(spacing: 4) {
                                Text("Уже есть аккаунт?")
                                    .font(.system(size: 17))
                                    .foregroundColor(.gray)
                                Button(action: { switchMode(toLogin: true) }) {
                                    Text("Войти")
                                        .font(.system(size: 17, weight: .semibold))
                                        .foregroundColor(coralColor)
                                }
                                .buttonStyle(.plain)
                            }
                            .opacity(isLoginMode ? 0 : 1)
                            .allowsHitTesting(!isLoginMode)
                        }
                        .animation(.easeInOut(duration: 0.3), value: isLoginMode)
                        }
                        .padding(.horizontal, 24)
                        .padding(.top, 32)
                        .padding(.bottom, geo.safeAreaInsets.bottom + 24)
                        .frame(maxWidth: .infinity)
                    }
                    .clipShape(RoundedCorner(radius: 24, corners: [.topLeft, .topRight]))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color.white.ignoresSafeArea(edges: .bottom))
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background(coralColor.opacity(0.20))
        .edgesIgnoringSafeArea(.all)
        // onChange registered once at body level — avoids double-firing from
        // the duplicated text fields inside the cross-fade ZStack.
        .onChange(of: emailField) { newValue in viewModel.onEmailChanged(newValue) }
        .onChange(of: passwordField) { newValue in viewModel.onPasswordChanged(newValue) }
        .onChange(of: usernameField) { newValue in viewModel.onUsernameChanged(newValue) }
        .onChange(of: viewModel.loginSuccess) { success in
            if success {
                viewModel.consumeLoginSuccess()
                appState.completeLogin(isPremium: viewModel.subscriptionType == "PREMIUM")
            }
        }
        .onChange(of: viewModel.registerSuccess) { success in
            if success {
                viewModel.consumeRegisterSuccess()
                appState.completeLogin(isPremium: viewModel.subscriptionType == "PREMIUM")
            }
        }
    }

    private func switchMode(toLogin: Bool) {
        guard isLoginMode != toLogin else { return }
        viewModel.clearFields()
        usernameField = ""
        emailField = ""
        passwordField = ""
        withAnimation(.easeInOut(duration: 0.3)) {
            isLoginMode = toLogin
        }
    }
}

// Clips only the requested corners — used to round the card's top edges
// while keeping the bottom flush with the screen.
private struct RoundedCorner: Shape {
    let radius: CGFloat
    let corners: UIRectCorner

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}
