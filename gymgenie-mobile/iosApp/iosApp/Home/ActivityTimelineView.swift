import SwiftUI

struct ActivityTimelineView: View {
    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890)
    private let greenColor = Color(red: 0.180, green: 0.800, blue: 0.443)
    private let darkColor = Color(red: 0.102, green: 0.102, blue: 0.180)

    private let activities: [(String, String, Color)] = [
        ("Бег", "figure.run", .orange),
        ("Йога", "figure.yoga", .purple),
        ("Силовая", "dumbbell", Color(red: 0.173, green: 0.757, blue: 0.890)),
        ("Кардио", "heart.circle", .red),
        ("Растяжка", "figure.flexibility", Color(red: 0.180, green: 0.800, blue: 0.443)),
    ]

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 10) {
                ForEach(activities, id: \.0) { title, icon, color in
                    ActivityPill(title: title, icon: icon, color: color)
                }
            }
        }
    }
}

struct ActivityPill: View {
    let title: String
    let icon: String
    let color: Color

    private let darkColor = Color(red: 0.102, green: 0.102, blue: 0.180)

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 16))
                .foregroundColor(color)

            Text(title)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(darkColor)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(
            Capsule()
                .fill(color.opacity(0.1))
        )
    }
}
