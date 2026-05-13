import SwiftUI

struct ToolbarAction {
    let content: AnyView
    let action: () -> Void
}

struct GymGenieToolbar: View {
    let title: String
    var showBackNavigation: Bool = false
    var onBackTap: () -> Void = {}
    var actions: [ToolbarAction] = []

    private let titleColor = Color(red: 0.039, green: 0.039, blue: 0.039)
    private let actionBackground = Color(red: 0.957, green: 0.957, blue: 0.965)

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            HStack(alignment: .center, spacing: 12) {
                if showBackNavigation {
                    Button(action: onBackTap) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(titleColor)
                            .frame(width: 40, height: 40)
                            .background(Circle().fill(actionBackground))
                    }
                    .buttonStyle(.plain)
                }
                Text(title)
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundColor(titleColor)
            }

            Spacer()

            if !actions.isEmpty {
                HStack(spacing: 12) {
                    ForEach(actions.indices, id: \.self) { index in
                        Button(action: actions[index].action) {
                            HStack(spacing: 6) {
                                actions[index].content
                            }
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(
                                RoundedRectangle(cornerRadius: 16, style: .continuous)
                                    .fill(actionBackground)
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(red: 0.980, green: 0.976, blue: 0.969))
    }
}
