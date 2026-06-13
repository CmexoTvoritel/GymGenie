import SwiftUI

private let eCoralSoft = Color(red: 1.0, green: 0.910, blue: 0.882)
private let eCoral = Color(red: 1.0, green: 0.353, blue: 0.235)
private let eMuted = Color(red: 0.545, green: 0.545, blue: 0.573)
private let eBlack = Color(red: 0.039, green: 0.039, blue: 0.039)
private let eBorder = Color(red: 0.929, green: 0.929, blue: 0.937)

struct ExperienceStrip: View {
    let experience: String
    let frequency: String

    var body: some View {
        HStack(spacing: 14) {
            RoundedRectangle(cornerRadius: 12)
                .fill(eCoralSoft)
                .frame(width: 40, height: 40)
                .overlay(
                    Image(systemName: "dumbbell.fill")
                        .font(.system(size: 16))
                        .foregroundColor(eCoral)
                )
            VStack(alignment: .leading, spacing: 2) {
                Text("ОПЫТ")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(eMuted)
                    .tracking(0.6)
                let text = frequency.isEmpty
                    ? experience
                    : "\(experience) · \(frequency.lowercased())"
                Text(text)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(eBlack)
            }
            Spacer()
        }
        .padding(16)
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 18)
                .fill(Color.white)
                .overlay(RoundedRectangle(cornerRadius: 18).stroke(eBorder, lineWidth: 1))
        )
    }
}
