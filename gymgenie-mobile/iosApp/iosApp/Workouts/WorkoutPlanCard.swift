import SwiftUI
import Shared

struct WorkoutPlanCard: View {
    let plan: WorkoutPlanShortResponse
    var onView: (() -> Void)? = nil
    var onStart: () -> Void = {}

    private var visual: MuscleGroupVisualStyle { muscleGroupStyle(plan.primaryMuscleGroup) }
    private var isAi: Bool { plan.createdBy.uppercased() == "AI" }
    private var isRecurring: Bool { plan.scheduleType.uppercased() == "RECURRING" }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if plan.isActive {
                Rectangle()
                    .fill(Palette.coral)
                    .frame(height: 3)
            }

            VStack(alignment: .leading, spacing: 0) {

                HStack(alignment: .center, spacing: 8) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .fill(visual.bg)
                        Image(muscleGroupExerciseImageName(plan.primaryMuscleGroup ?? ""))
                            .resizable()
                            .aspectRatio(1, contentMode: .fit)
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                    .frame(width: 48, height: 48)

                    Text(plan.name)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .lineLimit(2)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    sourceBadge
                }

                Spacer().frame(height: 8)

                let descText: String = {
                    guard let d = plan.description_, !d.trimmingCharacters(in: .whitespaces).isEmpty else { return "\n" }
                    return d
                }()
                Text(descText)
                    .font(.system(size: 14))
                    .foregroundColor(descText == "\n" ? .clear : Color(red: 0.361, green: 0.361, blue: 0.388))
                    .lineLimit(2)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Spacer().frame(height: 12)

                chipsRow

                Spacer().frame(height: 12)

                footer
            }
            .padding(16)
        }
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .strokeBorder(
                    plan.isActive ? Palette.coral : Color(red: 0.929, green: 0.929, blue: 0.937),
                    lineWidth: plan.isActive ? 2 : 1.5
                )
        )
    }

    private var sourceBadge: some View {
        Group {
            if isAi {
                Text("✦ AI")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Capsule().fill(Palette.coral))
            } else {
                Text("Ручная")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(Color(red: 0.361, green: 0.361, blue: 0.388))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Capsule().fill(Color(red: 0.957, green: 0.957, blue: 0.965)))
            }
        }
    }

    private var chipsRow: some View {
        HStack(spacing: 6) {
            InfoChip(icon: "clock", text: "~\(plan.estimatedMinutes) мин")
            InfoChip(icon: "list.bullet", text: "\(plan.exercisesCount) упр.")
            scheduleChip
        }
    }

    private var scheduleChip: some View {
        Group {
            if isRecurring {
                let days = formatScheduleDays(Array(plan.scheduleDays))
                HStack(spacing: 4) {
                    Image(systemName: "repeat").font(.system(size: 13, weight: .semibold))
                    Text(days).font(.system(size: 14, weight: .semibold))
                }
                .foregroundColor(Color(red: 0.914, green: 0.290, blue: 0.173))
                .padding(.horizontal, 10).padding(.vertical, 5)
                .background(RoundedRectangle(cornerRadius: 10).fill(Color(red: 1.0, green: 0.957, blue: 0.941)))
            } else {
                HStack(spacing: 4) {
                    Image(systemName: "clock").font(.system(size: 13, weight: .semibold))
                    Text("Разовая").font(.system(size: 14, weight: .semibold))
                }
                .foregroundColor(Color(red: 0.227, green: 0.227, blue: 0.251))
                .padding(.horizontal, 10).padding(.vertical, 5)
                .background(RoundedRectangle(cornerRadius: 10).fill(Color(red: 0.957, green: 0.957, blue: 0.965)))
            }
        }
    }

    @ViewBuilder
    private var footer: some View {
        if let onView = onView {
            HStack(spacing: 10) {
                Button(action: onView) {
                    Image(systemName: "eye")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(Palette.deepInk)
                        .frame(width: 48, height: 48)
                        .background(
                            RoundedRectangle(cornerRadius: 14)
                                .fill(Color.white)
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .strokeBorder(Color(red: 0.929, green: 0.929, blue: 0.937), lineWidth: 1.5)
                        )
                }
                .buttonStyle(.plain)

                Button(action: onStart) {
                    HStack(spacing: 6) {
                        Image(systemName: "play.fill").font(.system(size: 14, weight: .bold))
                        Text("Начать тренировку").font(.system(size: 18, weight: .bold))
                    }
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .background(RoundedRectangle(cornerRadius: 14).fill(Palette.coral))
                }
                .buttonStyle(.plain)
            }
        } else {
            Button(action: onStart) {
                HStack(spacing: 8) {
                    Image(systemName: "play.fill").font(.system(size: 14, weight: .bold))
                    Text("Начать тренировку").font(.system(size: 18, weight: .bold))
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 48)
                .background(RoundedRectangle(cornerRadius: 14).fill(Palette.deepInk))
            }
            .buttonStyle(.plain)
        }
    }
}

private struct InfoChip: View {
    let icon: String
    let text: String

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: icon).font(.system(size: 13, weight: .semibold))
            Text(text).font(.system(size: 14, weight: .semibold))
        }
        .foregroundColor(Color(red: 0.227, green: 0.227, blue: 0.251))
        .padding(.horizontal, 10).padding(.vertical, 5)
        .background(RoundedRectangle(cornerRadius: 10).fill(Color(red: 0.957, green: 0.957, blue: 0.965)))
    }
}

private struct MuscleGroupVisualStyle {
    let bg: Color
}

private func muscleGroupStyle(_ group: String?) -> MuscleGroupVisualStyle {
    let pair = muscleGroupColorPair(group)
    return MuscleGroupVisualStyle(bg: pair.bg)
}

private func formatScheduleDays(_ days: [String]) -> String {
    if days.isEmpty { return "Постоянная" }
    let upper = Set(days.map { $0.uppercased() })
    let ordered = weekdayOrder.filter { upper.contains($0) }.compactMap { backendDayToShort[$0] }
    return ordered.isEmpty ? "Постоянная" : ordered.joined(separator: " · ")
}
