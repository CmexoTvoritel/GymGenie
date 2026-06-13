import SwiftUI

let proteinColor = Color(red: 0.039, green: 0.518, blue: 1.0)
let fatColor = Color(red: 1.0, green: 0.690, blue: 0.125)
let carbsColor = Color(red: 0.204, green: 0.780, blue: 0.349)

struct MacroDonutView: View {
    let proteinG: Int
    let fatG: Int
    let carbsG: Int
    let totalKcal: Int

    private let strokeWidth: CGFloat = 13
    private let outerSize: CGFloat = 108

    private var proteinKcal: CGFloat { CGFloat(proteinG) * 4 }
    private var fatKcal: CGFloat { CGFloat(fatG) * 9 }
    private var carbsKcal: CGFloat { CGFloat(carbsG) * 4 }
    private var macroTotal: CGFloat { proteinKcal + fatKcal + carbsKcal }

    private var proteinFrac: CGFloat { macroTotal > 0 ? proteinKcal / macroTotal : 0 }
    private var fatFrac: CGFloat { macroTotal > 0 ? fatKcal / macroTotal : 0 }
    private var carbsFrac: CGFloat { macroTotal > 0 ? carbsKcal / macroTotal : 0 }

    var body: some View {
        ZStack {
            if macroTotal <= 0 {
                Circle()
                    .stroke(Palette.deepInk.opacity(0.1), lineWidth: strokeWidth)
            } else {
                let gapFrac: CGFloat = 0.008
                let totalGap = gapFrac * 3
                let available = 1.0 - totalGap

                let pEnd = proteinFrac * available
                let fStart = pEnd + gapFrac
                let fEnd = fStart + fatFrac * available
                let cStart = fEnd + gapFrac
                let cEnd = cStart + carbsFrac * available

                Circle()
                    .trim(from: 0, to: pEnd)
                    .stroke(proteinColor, style: StrokeStyle(lineWidth: strokeWidth, lineCap: .round))
                    .rotationEffect(.degrees(-90))

                Circle()
                    .trim(from: fStart, to: fEnd)
                    .stroke(fatColor, style: StrokeStyle(lineWidth: strokeWidth, lineCap: .round))
                    .rotationEffect(.degrees(-90))

                Circle()
                    .trim(from: cStart, to: cEnd)
                    .stroke(carbsColor, style: StrokeStyle(lineWidth: strokeWidth, lineCap: .round))
                    .rotationEffect(.degrees(-90))
            }

            VStack(spacing: 0) {
                Text("\(totalKcal)")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(Palette.deepInk)
                Text("ККАЛ")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(Palette.deepInk.opacity(0.55))
                    .tracking(0.5)
            }
        }
        .frame(width: outerSize, height: outerSize)
    }
}
