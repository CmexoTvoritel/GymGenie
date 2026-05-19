import SwiftUI

struct HeroMacrosCard: View {
    let totalKcal: Int
    let proteinG: Int
    let fatG: Int
    let carbsG: Int

    private var macroKcalTotal: Int { proteinG * 4 + fatG * 9 + carbsG * 4 }
    private var proteinPct: Int { macroKcalTotal > 0 ? (proteinG * 4 * 100) / macroKcalTotal : 0 }
    private var fatPct: Int { macroKcalTotal > 0 ? (fatG * 9 * 100) / macroKcalTotal : 0 }
    private var carbsPct: Int { macroKcalTotal > 0 ? 100 - proteinPct - fatPct : 0 }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 20) {
                MacroDonutView(
                    proteinG: proteinG,
                    fatG: fatG,
                    carbsG: carbsG,
                    totalKcal: totalKcal
                )

                VStack(alignment: .leading, spacing: 2) {
                    Text("ВСЕГО ЗА ПРИЁМ")
                        .font(.system(size: 14.5, weight: .bold))
                        .foregroundColor(Palette.mutedText)
                        .tracking(1)

                    HStack(alignment: .bottom, spacing: 4) {
                        Text("\(totalKcal)")
                            .font(.system(size: 32, weight: .heavy))
                            .foregroundColor(Palette.deepInk)
                        Text("ккал")
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(Palette.mutedText)
                            .padding(.bottom, 5)
                    }
                }
            }

            StackedMacroBar(proteinG: proteinG, fatG: fatG, carbsG: carbsG)

            HStack(spacing: 8) {
                MacroMiniCard(label: "Белки", grams: proteinG, pct: proteinPct, dotColor: proteinColor)
                MacroMiniCard(label: "Жиры", grams: fatG, pct: fatPct, dotColor: fatColor)
                MacroMiniCard(label: "Углеводы", grams: carbsG, pct: carbsPct, dotColor: carbsColor)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(Color.white)
                .shadow(color: Palette.coral.opacity(0.1), radius: 8, x: 0, y: 2)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .strokeBorder(Palette.deepInk.opacity(0.15), lineWidth: 1)
        )
        .overlay(
            RadialGradient(
                colors: [Palette.coral.opacity(0.07), Color.clear],
                center: .topTrailing,
                startRadius: 0,
                endRadius: 200
            )
            .clipShape(RoundedRectangle(cornerRadius: 24))
            .allowsHitTesting(false)
        )
    }
}

private struct StackedMacroBar: View {
    let proteinG: Int
    let fatG: Int
    let carbsG: Int

    private var total: CGFloat {
        CGFloat(proteinG * 4 + fatG * 9 + carbsG * 4)
    }

    var body: some View {
        GeometryReader { geo in
            HStack(spacing: 0) {
                if total > 0 {
                    let pW = geo.size.width * CGFloat(proteinG * 4) / total
                    let fW = geo.size.width * CGFloat(fatG * 9) / total
                    let cW = geo.size.width * CGFloat(carbsG * 4) / total

                    Rectangle().fill(proteinColor).frame(width: pW)
                    Rectangle().fill(fatColor).frame(width: fW)
                    Rectangle().fill(carbsColor).frame(width: cW)
                } else {
                    Rectangle().fill(Palette.deepInk.opacity(0.06))
                }
            }
        }
        .frame(height: 8)
        .clipShape(Capsule())
    }
}

private struct MacroMiniCard: View {
    let label: String
    let grams: Int
    let pct: Int
    let dotColor: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 6) {
                Circle().fill(dotColor).frame(width: 6, height: 6)
                Text(label)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(Palette.mutedText)
            }
            HStack(alignment: .bottom, spacing: 4) {
                Text("\(grams)г")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Palette.deepInk)
                Text("\(pct)%")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(Palette.mutedText)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 10)
        .padding(.vertical, 12)
        .background(RoundedRectangle(cornerRadius: 12).fill(Palette.deepInk.opacity(0.05)))
    }
}
