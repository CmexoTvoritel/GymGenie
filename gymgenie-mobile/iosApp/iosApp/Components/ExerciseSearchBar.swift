import SwiftUI

struct ExerciseSearchBar: View {
    @Binding var searchQuery: String
    var onSubmit: () -> Void
    var onClear: () -> Void

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.gray)

            TextField("Поиск упражнений...", text: $searchQuery)
                .font(.system(size: 15))
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .onSubmit {
                    onSubmit()
                }

            if !searchQuery.isEmpty {
                Button(action: onClear) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.gray)
                }
            }
        }
        .padding(.horizontal, 14)
        .frame(height: 44)
        .background(Color.white)
        .clipShape(Capsule())
        .shadow(color: .black.opacity(0.06), radius: 6, y: 2)
    }
}
