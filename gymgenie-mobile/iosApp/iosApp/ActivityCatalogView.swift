import SwiftUI
import Shared

// MARK: - Activity catalog screen
//
// Shows the full backend-driven catalog of activities the user can add to
// their daily plan. Items are grouped by ring (Movement / Mind / Life) and
// the per-row toggle reflects current plan membership.

struct ActivityCatalogView: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel = ActivityCatalogViewModelWrapper()

    @State private var searchQuery: String = ""

    var body: some View {
        ZStack {
            Palette.warmOffWhite.ignoresSafeArea()

            VStack(spacing: 0) {
                navigationBar
                searchBar

                if viewModel.isLoading {
                    spinner
                } else if let error = viewModel.error, viewModel.catalog.isEmpty {
                    ErrorBanner(message: error, onRetry: { viewModel.load() })
                } else if filteredItems.isEmpty {
                    EmptyHint(
                        icon: "magnifyingglass",
                        message: "Ничего не найдено",
                        hint: "Попробуй изменить запрос"
                    )
                } else {
                    ScrollView(showsIndicators: false) {
                        VStack(alignment: .leading, spacing: 8) {
                            ForEach(ringOrder, id: \.self) { ring in
                                let items = grouped[ring] ?? []
                                if !items.isEmpty {
                                    Text(ringLabel(ring))
                                        .font(.system(size: 13, weight: .bold))
                                        .foregroundColor(Palette.mutedText)
                                        .padding(.top, 8)
                                        .padding(.bottom, 4)
                                    ForEach(items, id: \.id) { activity in
                                        CatalogActivityCard(
                                            activity: activity,
                                            isInPlan: viewModel.planIds.contains(activity.id),
                                            onToggle: { viewModel.togglePlan(activityId: activity.id) }
                                        )
                                    }
                                }
                            }
                            Spacer().frame(height: 24)
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 16)
                    }
                }
            }
        }
        .navigationBarHidden(true)
        .onAppear { viewModel.load() }
    }

    // MARK: - Derived data

    private var filteredItems: [ActivityCatalogResponse] {
        if searchQuery.isEmpty { return viewModel.catalog }
        let query = searchQuery.lowercased()
        return viewModel.catalog.filter { $0.name.lowercased().contains(query) }
    }

    private var grouped: [String: [ActivityCatalogResponse]] {
        Dictionary(grouping: filteredItems, by: { $0.ring })
    }

    private let ringOrder: [String] = ["MOVE", "MIND", "LIFE"]

    private func ringLabel(_ ring: String) -> String {
        switch ring {
        case "MOVE": return "Движение"
        case "MIND": return "Разум"
        case "LIFE": return "Режим"
        default: return ring
        }
    }

    // MARK: - Navigation bar

    private var navigationBar: some View {
        HStack {
            Button(action: { dismiss() }) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
                    .frame(width: 40, height: 40)
                    .background(Circle().fill(Palette.softCard))
            }

            Spacer()

            Text("Каталог активностей")
                .font(.system(size: 17, weight: .heavy))
                .foregroundColor(Palette.deepInk)

            Spacer()

            Spacer().frame(width: 40, height: 40)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 8)
    }

    // MARK: - Search

    private var searchBar: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .foregroundColor(Palette.mutedText)
            TextField("Найти активность...", text: $searchQuery)
                .font(.system(size: 15))
                .foregroundColor(Palette.deepInk)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(RoundedRectangle(cornerRadius: 16).fill(Palette.softCard))
        .padding(.horizontal, 20)
        .padding(.top, 8)
    }

    private var spinner: some View {
        ProgressView()
            .progressViewStyle(.circular)
            .tint(Palette.accentOrange)
            .frame(maxWidth: .infinity)
            .frame(maxHeight: .infinity)
    }
}

// MARK: - Card

private struct CatalogActivityCard: View {
    let activity: ActivityCatalogResponse
    let isInPlan: Bool
    let onToggle: () -> Void

    private var ring: Color { ringColor(for: activity.ring) }

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle().fill(ring.opacity(0.15))
                Text(String(activity.name.prefix(1)).uppercased())
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(ring)
            }
            .frame(width: 36, height: 36)

            VStack(alignment: .leading, spacing: 2) {
                Text(activity.name)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
                Text(kindLabel(activity.kind))
                    .font(.system(size: 12))
                    .foregroundColor(Palette.mutedText)
            }

            Spacer()

            Button(action: onToggle) {
                ZStack {
                    Circle().fill(isInPlan ? ring : Palette.softCard)
                    Image(systemName: isInPlan ? "checkmark" : "plus")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(isInPlan ? .white : ring)
                }
                .frame(width: 32, height: 32)
            }
            .buttonStyle(.plain)
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 14).fill(Color.white))
    }

    private func ringColor(for ring: String) -> Color {
        switch ring {
        case "MOVE": return Palette.ringMove
        case "MIND": return Palette.ringMind
        case "LIFE": return Palette.ringLife
        default: return Palette.accentOrange
        }
    }

    private func kindLabel(_ kind: String) -> String {
        switch kind {
        case "BINARY": return "Да/Нет"
        case "COUNTER": return "Счётчик"
        case "PRESET": return "Пресеты"
        default: return kind
        }
    }
}

// MARK: - State views

private struct EmptyHint: View {
    let icon: String
    let message: String
    let hint: String

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 32))
                .foregroundColor(Palette.accentOrange.opacity(0.7))
            Text(message)
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(Palette.deepInk)
            Text(hint)
                .font(.system(size: 13))
                .foregroundColor(Palette.mutedText)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(.horizontal, 32)
    }
}

private struct ErrorBanner: View {
    let message: String
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 32))
                .foregroundColor(Palette.accentOrange.opacity(0.8))
            Text("Не удалось загрузить")
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(Palette.deepInk)
            Text(message)
                .font(.system(size: 13))
                .foregroundColor(Palette.mutedText)
                .multilineTextAlignment(.center)
            Button(action: onRetry) {
                Text("Повторить")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 10)
                    .background(RoundedRectangle(cornerRadius: 12).fill(Palette.accentOrange))
            }
            .buttonStyle(.plain)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(.horizontal, 32)
    }
}
