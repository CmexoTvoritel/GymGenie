import SwiftUI
import Combine

struct AuthView: View {
    @EnvironmentObject var appState: AppState
    @ObservedObject var viewModel: AuthViewModelWrapper

    @State private var isLoginMode: Bool
    @State private var usernameField: String = ""
    @State private var emailField: String = ""
    @State private var passwordField: String = ""
    @State private var keyboardHeight: CGFloat = 0
    @FocusState private var focusedField: AuthField?

    private enum AuthField: Hashable {
        case loginEmail, loginPassword
        case registerName, registerEmail, registerPassword
    }

    private let coralColor = Color(.sRGB, red: 1.0, green: 0.353, blue: 0.235)
    private let textPrimary = Color(red: 0.1, green: 0.15, blue: 0.3)

    private var isKeyboardVisible: Bool { keyboardHeight > 0 }

    init(viewModel: AuthViewModelWrapper, initialIsLogin: Bool = true) {
        self.viewModel = viewModel
        self._isLoginMode = State(initialValue: initialIsLogin)
    }

    var body: some View {
        mainLayout
            .contentShape(Rectangle())
            .onTapGesture { hideKeyboard() }
            .onChange(of: emailField) { newValue in viewModel.onEmailChanged(newValue) }
            .onChange(of: passwordField) { newValue in viewModel.onPasswordChanged(newValue) }
            .onChange(of: usernameField) { newValue in viewModel.onUsernameChanged(newValue) }
            .onChange(of: viewModel.loginSuccess) { success in
                guard success else { return }
                viewModel.consumeLoginSuccess()
                appState.completeLogin(isPremium: viewModel.subscriptionType == "PREMIUM")
            }
            .onChange(of: viewModel.registerSuccess) { success in
                guard success else { return }
                viewModel.consumeRegisterSuccess()
                appState.completeLogin(isPremium: viewModel.subscriptionType == "PREMIUM")
            }
            .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillShowNotification)) { notification in
                if let frame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect {
                    keyboardHeight = frame.height
                }
            }
            .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillHideNotification)) { _ in
                keyboardHeight = 0
            }
    }

    // MARK: - Main Layout

    private var mainLayout: some View {
        GeometryReader { geo in
            screenContent(
                screenWidth: geo.size.width,
                safeTop: geo.safeAreaInsets.top,
                safeBottom: geo.safeAreaInsets.bottom
            )
        }
        .edgesIgnoringSafeArea(.all)
    }

    @ViewBuilder
    private func screenContent(screenWidth: CGFloat, safeTop: CGFloat, safeBottom: CGFloat) -> some View {
        let metrics = ImageMetrics(screenWidth: screenWidth, safeTop: safeTop)
        let keyboardOffset = max(keyboardHeight - safeBottom, 0)

        ZStack(alignment: .top) {
            backgroundLayer

            illustrationArea(metrics: metrics)

            VStack(spacing: 0) {
                Spacer()
                    .frame(height: metrics.cardTopOffset)
                    .fixedSize(horizontal: false, vertical: true)
                cardContent(safeBottom: safeBottom)
                    .frame(maxHeight: .infinity)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .offset(y: -keyboardOffset)
        .animation(.easeOut(duration: 0.25), value: keyboardOffset)
    }

    private var backgroundLayer: some View {
        VStack(spacing: 0) {
            coralColor.opacity(0.20)
            Color.white
        }
        .edgesIgnoringSafeArea(.all)
    }

    // MARK: - Illustration

    @ViewBuilder
    private func illustrationArea(metrics: ImageMetrics) -> some View {
        ZStack(alignment: .bottom) {
            Image("ic_login")
                .resizable()
                .scaledToFit()
                .padding(.horizontal, 20)
                .frame(maxWidth: .infinity)
                .opacity(isLoginMode ? 1 : 0)
                .animation(.easeInOut(duration: 0.3), value: isLoginMode)
            Image("ic_registration")
                .resizable()
                .scaledToFit()
                .padding(.horizontal, 20)
                .frame(maxWidth: .infinity)
                .opacity(isLoginMode ? 0 : 1)
                .animation(.easeInOut(duration: 0.3), value: isLoginMode)
        }
        .frame(width: metrics.screenWidth, height: metrics.imageAreaHeight)
    }

    // MARK: - Card

    @ViewBuilder
    private func cardContent(safeBottom: CGFloat) -> some View {
        VStack(spacing: 0) {
            Spacer(minLength: 16)
            cardFormContent
            if isKeyboardVisible {
                Spacer().frame(height: safeBottom + 12)
            } else {
                Spacer(minLength: 16)
            }
        }
        .padding(.horizontal, 24)
        .padding(.bottom, isKeyboardVisible ? 0 : safeBottom)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.white)
        .clipShape(RoundedCorner(radius: 24, corners: [.topLeft, .topRight]))
    }

    private var cardFormContent: some View {
        let compact = isKeyboardVisible
        let mainSpacing: CGFloat = compact ? 12 : 24

        return VStack(spacing: 0) {
            titleSection
            Spacer().frame(height: mainSpacing)
            fieldsSection
            errorSection
            Spacer().frame(height: mainSpacing)
            actionButtonSection
            Spacer().frame(height: 24)
            socialSection
            Spacer().frame(height: 24)
            modeSwitchSection
        }
        .animation(.easeOut(duration: 0.25), value: compact)
    }

    // MARK: - Card Sections

    private var titleSection: some View {
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
        .frame(maxWidth: .infinity, alignment: .center)
    }

    private var fieldsSection: some View {
        ZStack {
            registerFields
            loginFields
        }
        .animation(.easeInOut(duration: 0.3), value: isLoginMode)
    }

    private var registerFields: some View {
        VStack(spacing: 16) {
            GymGenieTextField(
                placeholder: "Имя",
                text: $usernameField,
                icon: "person",
                accentColor: coralColor
            )
            .focused($focusedField, equals: .registerName)
            .submitLabel(.next)
            .onSubmit { focusedField = .registerEmail }

            GymGenieTextField(
                placeholder: "Email",
                text: $emailField,
                icon: "envelope",
                accentColor: coralColor
            )
            .textContentType(.emailAddress)
            .keyboardType(.emailAddress)
            .focused($focusedField, equals: .registerEmail)
            .submitLabel(.next)
            .onSubmit { focusedField = .registerPassword }

            GymGenieTextField(
                placeholder: "Пароль",
                text: $passwordField,
                icon: "lock",
                isSecure: true,
                accentColor: coralColor
            )
            .focused($focusedField, equals: .registerPassword)
            .submitLabel(.done)
            .onSubmit { focusedField = nil }
        }
        .opacity(isLoginMode ? 0 : 1)
        .allowsHitTesting(!isLoginMode)
    }

    private var loginFields: some View {
        VStack(spacing: 16) {
            GymGenieTextField(
                placeholder: "Email",
                text: $emailField,
                icon: "envelope",
                accentColor: coralColor
            )
            .textContentType(.emailAddress)
            .keyboardType(.emailAddress)
            .focused($focusedField, equals: .loginEmail)
            .submitLabel(.next)
            .onSubmit { focusedField = .loginPassword }

            GymGenieTextField(
                placeholder: "Пароль",
                text: $passwordField,
                icon: "lock",
                isSecure: true,
                accentColor: coralColor
            )
            .focused($focusedField, equals: .loginPassword)
            .submitLabel(.done)
            .onSubmit { focusedField = nil }
        }
        .opacity(isLoginMode ? 1 : 0)
        .allowsHitTesting(isLoginMode)
    }

    @ViewBuilder
    private var errorSection: some View {
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
    }

    private var actionButtonSection: some View {
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
    }

    private var socialSection: some View {
        VStack(spacing: 16) {
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
            HStack(spacing: 20) {
                SocialIconButton(label: "G")
                SocialIconButton(systemImage: "apple.logo")
                SocialIconButton(label: "VK")
                SocialIconButton(systemImage: "paperplane.fill")
            }
        }
    }

    private var modeSwitchSection: some View {
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

    // MARK: - Actions

    private func switchMode(toLogin: Bool) {
        guard isLoginMode != toLogin else { return }
        focusedField = nil
        hideKeyboard()
        viewModel.clearFields()
        usernameField = ""
        emailField = ""
        passwordField = ""
        withAnimation(.easeInOut(duration: 0.3)) {
            isLoginMode = toLogin
        }
    }

    private func hideKeyboard() {
        UIApplication.shared.sendAction(
            #selector(UIResponder.resignFirstResponder),
            to: nil,
            from: nil,
            for: nil
        )
    }
}

// MARK: - Image Metrics

private struct ImageMetrics {
    let screenWidth: CGFloat
    let imageAreaHeight: CGFloat
    let cardTopOffset: CGFloat

    init(screenWidth: CGFloat, safeTop: CGFloat) {
        self.screenWidth = max(screenWidth, 1)
        let imageWidth = max(screenWidth - 40, 1)
        let loginSize = UIImage(named: "ic_login")?.size ?? CGSize(width: 1, height: 1)
        let registerSize = UIImage(named: "ic_registration")?.size ?? CGSize(width: 1, height: 1)
        let loginAR = loginSize.width / max(loginSize.height, 1)
        let registerAR = registerSize.width / max(registerSize.height, 1)
        let loginImageHeight = loginAR > 0 ? imageWidth / loginAR : 0
        let registerImageHeight = registerAR > 0 ? imageWidth / registerAR : 0
        let maxImageHeight = max(loginImageHeight, registerImageHeight)
        imageAreaHeight = max(safeTop + 48 + maxImageHeight, 1)
        cardTopOffset = imageAreaHeight - 32
    }
}

// MARK: - Social Icon Button

struct SocialIconButton: View {
    var label: String? = nil
    var systemImage: String? = nil

    private let coralColor = Color(.sRGB, red: 1.0, green: 0.353, blue: 0.235)

    var body: some View {
        ZStack {
            if let systemImage = systemImage {
                Image(systemName: systemImage)
                    .font(.system(size: 18))
                    .foregroundColor(coralColor)
            } else if let label = label {
                Text(label)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(coralColor)
            }
        }
        .frame(width: 48, height: 48)
        .background(
            Circle()
                .fill(coralColor.opacity(0.1))
        )
    }
}

// MARK: - Rounded Corner Shape

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
