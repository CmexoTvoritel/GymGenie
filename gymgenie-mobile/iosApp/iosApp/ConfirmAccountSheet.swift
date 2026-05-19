import SwiftUI

private let csDangerSoft = Color(red: 0.996, green: 0.906, blue: 0.910)
private let csCoralSoft = Color(red: 1.0, green: 0.910, blue: 0.882)
private let csDangerRed = Color(red: 0.898, green: 0.282, blue: 0.302)
private let csCoral = Color(red: 1.0, green: 0.353, blue: 0.235)
private let csBlack = Color(red: 0.039, green: 0.039, blue: 0.039)
private let csMuted = Color(red: 0.545, green: 0.545, blue: 0.573)

struct ConfirmAccountSheet: View {
    let isDelete: Bool
    let onConfirm: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        ZStack(alignment: .top) {
            Color.white.ignoresSafeArea()

            VStack(spacing: 0) {
            ZStack {
                Circle()
                    .fill(isDelete ? csDangerSoft : csCoralSoft)
                    .frame(width: 56, height: 56)
                Image(systemName: isDelete ? "trash.fill" : "rectangle.portrait.and.arrow.right")
                    .font(.system(size: 26))
                    .foregroundColor(isDelete ? csDangerRed : csCoral)
            }
            .padding(.top, 8)

            Text(isDelete ? "Удалить аккаунт?" : "Выйти из аккаунта?")
                .font(.system(size: 21, weight: .bold))
                .foregroundColor(csBlack)
                .padding(.top, 14)

            Text(isDelete
                 ? "Все ваши тренировки и прогресс будут безвозвратно удалены."
                 : "Чтобы продолжить, нужно будет войти заново.")
                .font(.system(size: 16))
                .foregroundColor(csMuted)
                .multilineTextAlignment(.center)
                .lineLimit(nil)
                .padding(.horizontal, 8)
                .padding(.top, 8)

            Button(action: onConfirm) {
                Text(isDelete ? "Удалить навсегда" : "Выйти")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Capsule().fill(isDelete ? csDangerRed : csCoral))
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 22)
            .padding(.top, 22)

            Button(action: onDismiss) {
                Text("Отмена")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(csMuted)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 22)
            .padding(.bottom, 8)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 8)
        .padding(.bottom, 4)
        }
    }
}
