import SwiftUI

struct BottomNavBar: View {

    struct Item: Identifiable {
        let id: Int
        let title: String
        let icon: String
        let selectedIcon: String
    }

    fileprivate static let barHeight: CGFloat = 60
    fileprivate static let itemWidth: CGFloat = 91
    fileprivate static let itemHeight: CGFloat = 54
    fileprivate static let edgePadding: CGFloat = 6
    fileprivate static let iconSize: CGFloat = 24
    fileprivate static let iconLabelGap: CGFloat = 2
    fileprivate static let labelFontSize: CGFloat = 13
    fileprivate static let cornerRadius: CGFloat = 100

    let items: [Item]
    let selectedIndex: Int
    let onItemSelected: (Int) -> Void

    var body: some View {
        let barWidth = Self.edgePadding * 2 + Self.itemWidth * CGFloat(items.count)
        let indicatorX = Self.edgePadding + Self.itemWidth * CGFloat(selectedIndex)

        ZStack {

            glassBackdrop

            Capsule()
                .fill(Palette.coral)
                .frame(width: Self.itemWidth, height: Self.itemHeight)
                .position(
                    x: indicatorX + Self.itemWidth / 2,
                    y: Self.barHeight / 2
                )
                .animation(.easeInOut(duration: 0.25), value: selectedIndex)
                .allowsHitTesting(false)

            HStack(spacing: 0) {
                ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                    BottomNavItemView(
                        item: item,
                        itemWidth: Self.itemWidth,
                        itemHeight: Self.barHeight,
                        isSelected: index == selectedIndex,
                        onTap: { onItemSelected(index) }
                    )
                }
            }
            .padding(.horizontal, Self.edgePadding)
        }
        .frame(width: barWidth, height: Self.barHeight)
        .clipShape(Capsule())
        .shadow(color: Color.black.opacity(0.10), radius: 12, x: 0, y: 4)
    }

    private var glassBackdrop: some View {
        ZStack {
            Capsule().fill(Palette.neutrals400.opacity(0.35))
            Capsule().strokeBorder(Color.white.opacity(0.45), lineWidth: 0.8)
        }
    }

}

private struct BottomNavItemView: View {
    let item: BottomNavBar.Item
    let itemWidth: CGFloat
    let itemHeight: CGFloat
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: BottomNavBar.iconLabelGap) {
                Image(systemName: isSelected ? item.selectedIcon : item.icon)
                    .font(.system(size: 18, weight: .medium))
                    .frame(width: BottomNavBar.iconSize, height: BottomNavBar.iconSize)
                    .foregroundColor(isSelected ? .white : Palette.neutrals700)
                Text(item.title)
                    .font(.system(size: BottomNavBar.labelFontSize, weight: .medium))
                    .foregroundColor(isSelected ? .white : Palette.neutrals700)
                    .lineLimit(1)
            }
            .frame(width: itemWidth, height: itemHeight)
            .contentShape(Rectangle())
        }
        .buttonStyle(NoFeedbackButtonStyle())
    }
}

private struct NoFeedbackButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
    }
}
