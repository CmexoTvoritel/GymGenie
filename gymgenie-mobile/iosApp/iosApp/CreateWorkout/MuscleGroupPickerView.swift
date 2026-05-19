import SwiftUI
import Shared

/// Step 1 of the create-workout flow — choose which muscle group to browse.
///
/// Pure UI: it relies on the already-populated `CreateWorkoutViewModelWrapper`
/// to hold the muscle-group list (so tapping Back and re-entering does not
/// re-hit the backend).
struct MuscleGroupPickerView: View {
    @ObservedObject var vm: CreateWorkoutViewModelWrapper
    let onBack: () -> Void
    let onGroupSelected: (String, String) -> Void

    private let orange = Color(red: 0.941, green: 0.439, blue: 0.188)
    private let deepInk = Color(red: 0.161, green: 0.141, blue: 0.125)
    private let warmOffWhite = Color(red: 0.980, green: 0.976, blue: 0.969)
    private let softCard = Color(red: 0.953, green: 0.949, blue: 0.937)
    private let mutedText = Color(red: 0.463, green: 0.447, blue: 0.416)

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12),
    ]

    var body: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: "Выбери группу мышц",
                showBackNavigation: true,
                onBackTap: onBack
            )
            WorkoutFlowStepHeader(currentStep: 1, totalSteps: 3)
            content
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(warmOffWhite.ignoresSafeArea())
    }

    // MARK: - Content

    @ViewBuilder
    private var content: some View {
        if vm.isMuscleGroupsLoading && vm.muscleGroups.isEmpty {
            Spacer()
            ProgressView().scaleEffect(1.2).tint(orange)
            Spacer()
        } else if let error = vm.errorMessage, vm.muscleGroups.isEmpty {
            Spacer()
            errorView(message: error)
            Spacer()
        } else if vm.muscleGroups.isEmpty {
            Spacer()
            emptyView
            Spacer()
        } else {
            grid
        }
    }

    private var grid: some View {
        // SwiftUI's `ScrollView` extends under the home indicator; padding the
        // last spacer to the safe-area bottom ensures the final row clears the
        // gesture handle on every device class.
        GeometryReader { proxy in
            ScrollView(showsIndicators: false) {
                LazyVGrid(columns: columns, spacing: 12) {
                    ForEach(vm.muscleGroups, id: \.key) { group in
                        cell(for: group)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)

                Color.clear.frame(height: proxy.safeAreaInsets.bottom + 16)
            }
        }
    }

    private func cell(for group: Shared.MuscleGroupInfo) -> some View {
        Button {
            onGroupSelected(group.key, group.nameRu)
        } label: {
            ZStack {
                RoundedRectangle(cornerRadius: 16)
                    .fill(softCard)

                Image(muscleGroupImageName(group.key))
                    .resizable()
                    .scaledToFit()
                    .padding(12)
            }
            .aspectRatio(1, contentMode: .fit)
        }
        .buttonStyle(.plain)
    }

    private var emptyView: some View {
        VStack(spacing: 10) {
            Text("🗂").font(.system(size: 44))
            Text("Нет доступных групп")
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(deepInk)
            Text("Попробуйте повторить попытку позже")
                .font(.system(size: 13))
                .foregroundColor(mutedText)

            Button(action: { vm.loadMuscleGroups(forceReload: true) }) {
                Text("Повторить")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 10)
                    .background(Capsule().fill(orange))
            }
            .buttonStyle(.plain)
            .padding(.top, 4)
        }
    }

    private func errorView(message: String) -> some View {
        VStack(spacing: 12) {
            Text("⚠️").font(.system(size: 40))
            Text(message)
                .font(.system(size: 14))
                .foregroundColor(mutedText)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            Button(action: { vm.loadMuscleGroups(forceReload: true) }) {
                Text("Повторить")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Capsule().fill(orange))
            }
            .buttonStyle(.plain)
        }
    }
}
