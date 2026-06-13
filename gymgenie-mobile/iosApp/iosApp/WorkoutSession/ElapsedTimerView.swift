import SwiftUI

struct ElapsedTimerView: View {
    let exerciseIndex: Int32
    let setIndex: Int32

    @State private var elapsed: Int = 0
    @State private var paused: Bool = false
    @State private var timerTask: Task<Void, Never>? = nil

    var body: some View {
        VStack(spacing: 12) {
            HStack(spacing: 8) {
                timerBox(String(format: "%02d", elapsed / 60))
                Text(":")
                    .font(.system(size: 38, weight: .bold))
                    .foregroundColor(Palette.coral)
                timerBox(String(format: "%02d", elapsed % 60))
            }
            .padding(.horizontal, 16)

            Button(action: { paused.toggle() }) {
                Image(systemName: paused ? "play.fill" : "pause.fill")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(Palette.coral)
                    .frame(width: 56, height: 56)
                    .background(Circle().fill(Color.white))
                    .overlay(Circle().stroke(Palette.coral, lineWidth: 1.5))
                    .shadow(color: Palette.coral.opacity(0.3), radius: 6, y: 3)
            }
            .buttonStyle(.plain)
        }
        .onAppear { startTimer() }
        .onDisappear { stopTimer() }
        .onChange(of: exerciseIndex) { _ in startTimer() }
        .onChange(of: setIndex) { _ in startTimer() }
    }

    private func startTimer() {
        stopTimer()
        elapsed = 0
        paused = false
        timerTask = Task { @MainActor in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                if Task.isCancelled { break }
                if !paused { elapsed += 1 }
            }
        }
    }

    private func stopTimer() {
        timerTask?.cancel()
        timerTask = nil
    }

    private func timerBox(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 48, weight: .bold, design: .monospaced))
            .foregroundColor(Palette.deepInk)
            .frame(maxWidth: .infinity)
            .frame(height: 76)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(Palette.coralLight)
            )
    }
}
