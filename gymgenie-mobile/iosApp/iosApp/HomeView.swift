import SwiftUI

struct HomeView: View {
    var body: some View {
        VStack {
            Spacer()
            Text("after login")
                .font(.system(size: 18))
                .foregroundColor(.secondary)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(red: 0.961, green: 0.969, blue: 0.980))
    }
}
