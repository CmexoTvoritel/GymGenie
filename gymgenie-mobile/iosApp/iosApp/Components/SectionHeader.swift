import SwiftUI

struct SectionHeader: View {
    let title: String
    var actionTitle: String? = nil
    var onAction: (() -> Void)? = nil

    private let darkColor = Color(red: 0.102, green: 0.102, blue: 0.180)
    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890)

    var body: some View {
        HStack {
            Text(title)
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(darkColor)

            Spacer()

            if let actionTitle = actionTitle, let onAction = onAction {
                Button(action: onAction) {
                    HStack(spacing: 4) {
                        Text(actionTitle)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(accentColor)
                        Image(systemName: "arrow.right")
                            .font(.system(size: 12))
                            .foregroundColor(accentColor)
                    }
                }
            }
        }
    }
}
