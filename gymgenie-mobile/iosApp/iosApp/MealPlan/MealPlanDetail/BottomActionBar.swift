import SwiftUI

private let deleteBg = Color(red: 1.0, green: 0.941, blue: 0.925)
private let deleteIcon = Color(red: 0.914, green: 0.290, blue: 0.173)
private let borderColor = Color(red: 0.929, green: 0.929, blue: 0.937)

struct MealPlanBottomActionBar: View {
    let onDelete: () -> Void
    let onEdit: () -> Void
    var showEdit: Bool = true

    var body: some View {
        VStack(spacing: 0) {
            Rectangle()
                .fill(borderColor)
                .frame(height: 1)

            HStack(spacing: 12) {
                Button(action: onDelete) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 14).fill(deleteBg)
                        Image(systemName: "trash")
                            .font(.system(size: 17, weight: .medium))
                            .foregroundColor(deleteIcon)
                    }
                    .frame(maxWidth: showEdit ? nil : .infinity)
                    .frame(width: showEdit ? 52 : nil, height: 52)
                }
                .buttonStyle(.plain)

                if showEdit {
                    Button(action: onEdit) {
                        HStack(spacing: 8) {
                            Image(systemName: "pencil")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(.white)
                            Text("Редактировать")
                                .font(.system(size: 17, weight: .bold))
                                .foregroundColor(.white)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(RoundedRectangle(cornerRadius: 14).fill(Palette.coral))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)
            .padding(.bottom, 20)
            .background(Color.white)
        }
    }
}
