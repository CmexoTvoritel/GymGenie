import SwiftUI

/// Step indicator shown under the toolbar on every numbered step of the
/// "add exercise" sub-flow.
///
/// Renders the canonical "Шаг X из Y" label plus a coral progress bar whose
/// fill mirrors `currentStep / totalSteps`. The component is intentionally
/// non-scrollable: callers place it between the toolbar and the scrollable
/// content so the indicator stays pinned regardless of list size.
struct WorkoutFlowStepHeader: View {
    let currentStep: Int
    var totalSteps: Int = 3

    /// Background track for the progress bar. Light gray per spec.
    private let trackColor = Color(red: 0.878, green: 0.878, blue: 0.878) // #E0E0E0

    var body: some View {
        let safeTotal = max(totalSteps, 1)
        let safeCurrent = min(max(currentStep, 1), safeTotal)
        let fraction = CGFloat(safeCurrent) / CGFloat(safeTotal)

        VStack(alignment: .leading, spacing: 8) {
            Text("Шаг \(safeCurrent) из \(safeTotal)")
                .font(.system(size: 16, weight: .regular))
                .foregroundColor(.black)

            GeometryReader { proxy in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 2, style: .continuous)
                        .fill(trackColor)
                    RoundedRectangle(cornerRadius: 2, style: .continuous)
                        .fill(Palette.coral)
                        .frame(width: proxy.size.width * fraction)
                }
            }
            .frame(height: 4)
        }
        .padding(.horizontal, 16)
        .padding(.top, 12)
        .padding(.bottom, 8)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
