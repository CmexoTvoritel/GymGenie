import SwiftUI

struct MuscleGroupFilterChips: View {
    let selected: String?
    let onSelected: (String?) -> Void

    private let orange = Color(red: 0.941, green: 0.439, blue: 0.188)
    private let deepInk = Color(red: 0.161, green: 0.141, blue: 0.125)
    private let softCard = Color(red: 0.953, green: 0.949, blue: 0.937)

    private let groups: [(String?, String)] = [
        (nil, "Все"),
        ("CHEST", "Грудь"),
        ("BACK", "Спина"),
        ("SHOULDERS", "Плечи"),
        ("QUADRICEPS", "Ноги"),
        ("ABS", "Пресс"),
        ("BICEPS", "Руки"),
        ("CARDIO", "Кардио"),
        ("FULL_BODY", "Всё тело"),
    ]

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(Array(groups.enumerated()), id: \.offset) { _, item in
                    chip(value: item.0, label: item.1)
                }
            }
            .padding(.horizontal, 20)
        }
        .padding(.vertical, 4)
    }

    @ViewBuilder
    private func chip(value: String?, label: String) -> some View {
        let isSelected = value == selected
        Button(action: { onSelected(value) }) {
            Text(label)
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(isSelected ? .white : deepInk)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(Capsule().fill(isSelected ? orange : softCard))
        }
        .buttonStyle(.plain)
    }
}
