import SwiftUI

private let srBlack = Color(red: 0.039, green: 0.039, blue: 0.039)
private let srMuted = Color(red: 0.545, green: 0.545, blue: 0.573)
private let srSoft = Color(red: 0.957, green: 0.957, blue: 0.965)
private let srDangerRed = Color(red: 0.898, green: 0.282, blue: 0.302)
private let srDangerSoft = Color(red: 0.996, green: 0.906, blue: 0.910)
private let srChevron = Color(red: 0.784, green: 0.784, blue: 0.808)

struct SettingsRowView: View {
    let label: String
    let icon: String
    var value: String? = nil
    var labelColor: Color? = nil
    var iconColor: Color? = nil
    var action: (() -> Void)? = nil

    var body: some View {
        let tint = iconColor ?? srBlack
        let isDestructive = iconColor == srDangerRed
        let iconBackground = isDestructive ? srDangerSoft : srSoft

        Button(action: { action?() }) {
            HStack(spacing: 14) {
                RoundedRectangle(cornerRadius: 12)
                    .fill(iconBackground)
                    .frame(width: 40, height: 40)
                    .overlay(
                        Image(systemName: icon)
                            .font(.system(size: 20))
                            .foregroundColor(tint)
                    )
                Text(label)
                    .font(.system(size: 18, weight: .regular))
                    .foregroundColor(labelColor ?? srBlack)
                Spacer()
                HStack(spacing: 12) {
                    if let value {
                        Text(value)
                            .font(.system(size: 16, weight: .regular))
                            .foregroundColor(srMuted)
                    }
                    Image(systemName: "chevron.right")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(srChevron)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
