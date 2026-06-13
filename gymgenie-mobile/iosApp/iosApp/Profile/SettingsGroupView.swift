import SwiftUI

private let sgBorder = Color(red: 0.929, green: 0.929, blue: 0.937)

struct SettingsGroupView<Content: View>: View {
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(spacing: 0) {
            content()
        }
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 18)
                .fill(Color.white)
                .overlay(RoundedRectangle(cornerRadius: 18).stroke(sgBorder, lineWidth: 1))
        )
    }
}
