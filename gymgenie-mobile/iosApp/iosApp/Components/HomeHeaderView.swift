import SwiftUI

struct HomeHeaderView: View {
    let username: String
    let streakDays: Int32

    private let darkColor = Color(red: 0.102, green: 0.102, blue: 0.180)

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("Привет, \(username)!")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(darkColor)

                HStack(spacing: 4) {
                    Text("\u{1F525}")
                    Text("\(streakDays) дней подряд")
                        .font(.system(size: 13))
                        .foregroundColor(.gray)
                }
            }

            Spacer()

            Button(action: {}) {
                Image(systemName: "bell")
                    .font(.system(size: 20))
                    .foregroundColor(darkColor)
                    .frame(width: 40, height: 40)
                    .background(
                        Circle().fill(.white)
                    )
                    .shadow(color: Color.black.opacity(0.06), radius: 4, y: 2)
            }
        }
    }
}
