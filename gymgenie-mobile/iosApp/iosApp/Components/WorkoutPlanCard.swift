import SwiftUI
import Shared

/// Shared card representing a single workout plan.
///
/// Used in two contexts:
///  - The workouts catalog list, where the eye/start action pair is shown via
///    a non-nil `onView` handler.
///  - The home tab pager, where `onView` is `nil` and the card collapses to a
///    single full-width "Начать тренировку" CTA on a deep-ink background.
///
/// Active plans get a coral border + a 3pt top stripe so they read at a glance
/// without forcing the layout to grow taller. The body is wrapped in a
/// coral → white vertical gradient that signals the action color without
/// overpowering the content.
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
                // Row 1: image + title + badge
                HStack(alignment: .center, spacing: 8) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 14)
                            .fill(visual.bg)
                        Text("🏋️").font(.system(size: 22))
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

                // Row 2: description (always reserves 2-line height)
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
        .background(
            LinearGradient(
                gradient: Gradient(colors: [Palette.coral.opacity(0.1), .white]),
                startPoint: .top,
                endPoint: .bottom
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .strokeBorder(
                    plan.isActive ? Palette.coral : Color(red: 0.929, green: 0.929, blue: 0.937),
                    lineWidth: plan.isActive ? 2 : 1.5
                )
        )
    }

    // MARK: - Source badge

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

    // MARK: - Chips

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

    // MARK: - Footer

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

// MARK: - InfoChip

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

// MARK: - Muscle group visual style

private struct MuscleGroupVisualStyle {
    let bg: Color
    let emoji: String
}

private func muscleGroupStyle(_ group: String?) -> MuscleGroupVisualStyle {
    switch group?.uppercased() {
    case "CHEST":
        return .init(bg: Color(red: 1.0, green: 0.910, blue: 0.886), emoji: "🫁")
    case "BACK":
        return .init(bg: Color(red: 0.902, green: 0.933, blue: 1.0), emoji: "🦾")
    case "SHOULDERS", "SHOULDER":
        return .init(bg: Color(red: 0.902, green: 0.933, blue: 1.0), emoji: "💪")
    case "BICEPS", "TRICEPS", "FOREARMS", "ARMS":
        return .init(bg: Color(red: 1.0, green: 0.957, blue: 0.839), emoji: "💪")
    case "ABS", "CORE":
        return .init(bg: Color(red: 0.918, green: 0.902, blue: 1.0), emoji: "🔥")
    case "QUADRICEPS", "HAMSTRINGS", "GLUTES", "CALVES", "LEGS":
        return .init(bg: Color(red: 0.910, green: 0.969, blue: 0.910), emoji: "🦵")
    case "CARDIO":
        return .init(bg: Color(red: 0.988, green: 0.910, blue: 0.949), emoji: "❤️")
    case "FULL_BODY":
        return .init(bg: Color(red: 1.0, green: 0.957, blue: 0.839), emoji: "⭐")
    default:
        return .init(bg: Color(red: 1.0, green: 0.910, blue: 0.886), emoji: "🏋")
    }
}

// MARK: - Schedule day formatting

private let dayAbbreviations: [(String, String)] = [
    ("MONDAY", "Пн"),
    ("TUESDAY", "Вт"),
    ("WEDNESDAY", "Ср"),
    ("THURSDAY", "Чт"),
    ("FRIDAY", "Пт"),
    ("SATURDAY", "Сб"),
    ("SUNDAY", "Вс"),
]

private func formatScheduleDays(_ days: [String]) -> String {
    if days.isEmpty { return "Постоянная" }
    let upper = Set(days.map { $0.uppercased() })
    let ordered = dayAbbreviations.filter { upper.contains($0.0) }.map { $0.1 }
    return ordered.isEmpty ? "Постоянная" : ordered.joined(separator: " · ")
}
