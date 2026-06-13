import SwiftUI

struct ExerciseFilterSheet: View {
    @Binding var isPresented: Bool
    let currentDifficulties: [String]
    let currentRequiresEquipment: Bool?
    let currentSortByDifficulty: String?
    let currentSortByCalories: String?
    let onApply: ([String], Bool?, String?, String?) -> Void

    @State private var selectedDifficulties: Set<String>
    @State private var requiresEquipment: Bool?
    @State private var sortByDifficulty: String?
    @State private var sortByCalories: String?

    private let orange = Palette.accentOrange
    private let titleColor = Color(red: 0.039, green: 0.039, blue: 0.039)
    private let secondaryText = Color(red: 0.298, green: 0.298, blue: 0.325)

    private let neutralBackground = Color(red: 0.961, green: 0.961, blue: 0.961)
    private let neutralBorder = Color(red: 0.878, green: 0.878, blue: 0.878)
    private let neutralText = Color(red: 0.4, green: 0.4, blue: 0.4)

    private static let sortDesc = "DESC"
    private static let sortAsc = "ASC"

    private struct DifficultyOption {
        let value: String
        let label: String
        let color: Color
    }

    private let difficultyOptions: [DifficultyOption] = [
        DifficultyOption(
            value: "BEGINNER",
            label: "Легко",
            color: Color(red: 0.133, green: 0.627, blue: 0.420)
        ),
        DifficultyOption(
            value: "INTERMEDIATE",
            label: "Средне",
            color: Color(red: 0.910, green: 0.608, blue: 0.071)
        ),
        DifficultyOption(
            value: "ADVANCED",
            label: "Сложно",
            color: Color(red: 0.820, green: 0.263, blue: 0.263)
        ),
    ]

    init(
        isPresented: Binding<Bool>,
        currentDifficulties: [String],
        currentRequiresEquipment: Bool?,
        currentSortByDifficulty: String?,
        currentSortByCalories: String?,
        onApply: @escaping ([String], Bool?, String?, String?) -> Void
    ) {
        self._isPresented = isPresented
        self.currentDifficulties = currentDifficulties
        self.currentRequiresEquipment = currentRequiresEquipment
        self.currentSortByDifficulty = currentSortByDifficulty
        self.currentSortByCalories = currentSortByCalories
        self.onApply = onApply
        self._selectedDifficulties = State(initialValue: Set(currentDifficulties))
        self._requiresEquipment = State(initialValue: currentRequiresEquipment)
        self._sortByDifficulty = State(initialValue: currentSortByDifficulty)
        self._sortByCalories = State(initialValue: currentSortByCalories)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Фильтры")
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(titleColor)
                .padding(.top, 20)

            sectionTitle("Уровень сложности")
                .padding(.top, 20)

            difficultyChipsRow
                .padding(.top, 12)

            sectionTitle("Сортировка по сложности")
                .padding(.top, 24)

            sortChipsRow(
                selected: sortByDifficulty,
                onSelected: { sortByDifficulty = $0 }
            )
            .padding(.top, 12)

            sectionTitle("Сортировка по калориям")
                .padding(.top, 24)

            sortChipsRow(
                selected: sortByCalories,
                onSelected: { sortByCalories = $0 }
            )
            .padding(.top, 12)

            sectionTitle("Оборудование")
                .padding(.top, 24)

            equipmentChipsRow(
                selected: requiresEquipment,
                onSelected: { requiresEquipment = $0 }
            )
            .padding(.top, 12)

            Spacer(minLength: 28)

            Button(action: applyAndDismiss) {
                Text("Применить")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(orange)
                    )
            }
            .buttonStyle(.plain)

            Button(action: resetAndApply) {
                Text("Сбросить")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(secondaryText)
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
            }
            .buttonStyle(.plain)
            .padding(.top, 4)
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .onAppear {

            selectedDifficulties = Set(currentDifficulties)
            requiresEquipment = currentRequiresEquipment
            sortByDifficulty = currentSortByDifficulty
            sortByCalories = currentSortByCalories
        }
    }

    private func sectionTitle(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 18, weight: .semibold))
            .foregroundColor(titleColor)
    }

    private var difficultyChipsRow: some View {
        HStack(spacing: 8) {
            ForEach(difficultyOptions, id: \.value) { option in
                difficultyChip(option)
            }
        }
    }

    private func difficultyChip(_ option: DifficultyOption) -> some View {
        let isSelected = selectedDifficulties.contains(option.value)
        let background = isSelected ? option.color.opacity(0.15) : neutralBackground
        let borderColor = isSelected ? option.color : neutralBorder
        let textColor = isSelected ? option.color : neutralText

        return Button(action: {
            if isSelected {
                selectedDifficulties.remove(option.value)
            } else {
                selectedDifficulties.insert(option.value)
            }
        }) {
            Text(option.label)
                .font(.system(size: 16, weight: isSelected ? .semibold : .medium))
                .foregroundColor(textColor)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .background(
                    Capsule().fill(background)
                )
                .overlay(
                    Capsule().stroke(borderColor, lineWidth: 1.5)
                )
        }
        .buttonStyle(.plain)
    }

    private func sortChipsRow(
        selected: String?,
        onSelected: @escaping (String?) -> Void
    ) -> some View {
        HStack(spacing: 8) {
            sortChip(
                label: "Убывание",
                isSelected: selected == Self.sortDesc,
                onTap: { onSelected(selected == Self.sortDesc ? nil : Self.sortDesc) }
            )
            sortChip(
                label: "Возрастание",
                isSelected: selected == Self.sortAsc,
                onTap: { onSelected(selected == Self.sortAsc ? nil : Self.sortAsc) }
            )
        }
    }

    private func sortChip(
        label: String,
        isSelected: Bool,
        onTap: @escaping () -> Void
    ) -> some View {
        let background = isSelected ? Color.black.opacity(0.08) : neutralBackground
        let borderColor: Color = isSelected ? .black : neutralBorder
        let textColor: Color = isSelected ? .black : neutralText

        return Button(action: onTap) {
            Text(label)
                .font(.system(size: 16, weight: isSelected ? .semibold : .medium))
                .foregroundColor(textColor)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .background(
                    Capsule().fill(background)
                )
                .overlay(
                    Capsule().stroke(borderColor, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }

    private func equipmentChipsRow(
        selected: Bool?,
        onSelected: @escaping (Bool?) -> Void
    ) -> some View {
        HStack(spacing: 8) {
            sortChip(
                label: "Да",
                isSelected: selected == true,
                onTap: { onSelected(selected == true ? nil : true) }
            )
            sortChip(
                label: "Нет",
                isSelected: selected == false,
                onTap: { onSelected(selected == false ? nil : false) }
            )
        }
    }

    private func applyAndDismiss() {
        onApply(Array(selectedDifficulties).sorted(), requiresEquipment, sortByDifficulty, sortByCalories)
        isPresented = false
    }

    private func resetAndApply() {
        selectedDifficulties = []
        requiresEquipment = nil
        sortByDifficulty = nil
        sortByCalories = nil
        onApply([], nil, nil, nil)
        isPresented = false
    }
}
