import SwiftUI

private let slMuted = Color(red: 0.545, green: 0.545, blue: 0.573)

struct ProfileSectionLabel: View {
    let text: String

    var body: some View {
        Text(text.uppercased())
            .font(.system(size: 18, weight: .medium))
            .foregroundColor(slMuted)
            .tracking(0.8)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.leading, 4)
            .padding(.top, 24)
            .padding(.bottom, 10)
    }
}
