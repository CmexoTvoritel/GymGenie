import SwiftUI

struct ExercisesEmptyState: View {
    var hasFilter: Bool

    private let deepInk = Color(red: 0.161, green: 0.141, blue: 0.125)
    private let mutedText = Color(red: 0.463, green: 0.447, blue: 0.416)

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: "shippingbox")
                .font(.system(size: 42))
                .foregroundColor(mutedText)
            Text(hasFilter ? "Ничего не найдено" : "Каталог пуст")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(deepInk)
            Text(hasFilter ? "Попробуйте изменить запрос или фильтр" : "Упражнения скоро появятся")
                .font(.system(size: 13))
                .foregroundColor(mutedText)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 40)
    }
}
