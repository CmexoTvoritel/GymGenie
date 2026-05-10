import SwiftUI
import Shared

/// Saved AI-generated meal plans tab.
///
/// Mirrors `WorkoutsView` in spirit: a list/grid scroller with header,
/// pull-to-refresh, error fallback, and a primary "create new" entry point
/// (here: the AI meal coach). Cards expand on tap into [MealPlanDetailView].
struct NutritionView: View {
    var onClose: (() -> Void)? = nil

    @StateObject private var viewModel = MealPlansListViewModelWrapper()

    @State private var selectedPlanId: String? = nil
    @State private var showAiCoach: Bool = false
    @State private var planToDeleteId: String? = nil

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            Palette.warmOffWhite.edgesIgnoringSafeArea(.all)

            mainContent

            fabButton
        }
        .fullScreenCover(
            isPresented: Binding(
                get: { selectedPlanId != nil },
                set: { if !$0 { selectedPlanId = nil } }
            )
        ) {
            if let id = selectedPlanId {
                MealPlanDetailView(
                    planId: id,
                    onClose: { selectedPlanId = nil }
                )
            }
        }
        .fullScreenCover(isPresented: $showAiCoach) {
            AiMealCoachView(onClose: {
                showAiCoach = false
                viewModel.refresh()
            })
        }
        .alert(
            "Удалить рацион?",
            isPresented: Binding(
                get: { planToDeleteId != nil },
                set: { if !$0 { planToDeleteId = nil } }
            ),
            presenting: planToDeleteId
        ) { id in
            Button("Удалить", role: .destructive) {
                viewModel.delete(planId: id)
                planToDeleteId = nil
            }
            Button("Отмена", role: .cancel) { planToDeleteId = nil }
        } message: { _ in
            Text("Это действие нельзя отменить.")
        }
        .onAppear {
            if !viewModel.plansLoaded {
                viewModel.load()
            }
        }
    }

    private var mainContent: some View {
        VStack(spacing: 0) {
            headerSection

            if viewModel.isLoading && viewModel.plans.isEmpty {
                Spacer()
                ProgressView().scaleEffect(1.2).tint(Palette.coral)
                Spacer()
            } else if let error = viewModel.errorMessage,
                      viewModel.plans.isEmpty,
                      !isExpectedEmptyState {
                Spacer()
                errorView(message: error)
                Spacer()
            } else {
                listContent
            }
        }
    }

    /// Distinguishes "successful empty result" from "error while empty".
    private var isExpectedEmptyState: Bool {
        viewModel.plansLoaded && viewModel.plans.isEmpty && viewModel.errorMessage == nil
    }

    // MARK: - Header

    private var headerSection: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 2) {
                Text("Рацион")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(Palette.deepInk)
                Text("Сохранённые планы питания от ИИ")
                    .font(.system(size: 13))
                    .foregroundColor(Palette.mutedText)
            }
            Spacer()
            if let close = onClose {
                Button(action: close) {
                    Image(systemName: "xmark")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(Palette.deepInk)
                        .padding(10)
                        .background(Circle().fill(Color(red: 0.92, green: 0.92, blue: 0.94)))
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 20)
        .padding(.top, 16)
        .padding(.bottom, 8)
    }

    // MARK: - List / empty / error

    private var listContent: some View {
        ScrollView(showsIndicators: false) {
            LazyVStack(spacing: 12) {
                if viewModel.plans.isEmpty {
                    emptyState
                } else {
                    ForEach(viewModel.plans, id: \.id) { plan in
                        MealPlanListCard(
                            plan: plan,
                            isDeleting: viewModel.deletingPlanId == plan.id,
                            onTap: { selectedPlanId = plan.id },
                            onDelete: { planToDeleteId = plan.id }
                        )
                        .onAppear {
                            if plan.id == viewModel.plans.last?.id {
                                viewModel.loadMore()
                            }
                        }
                    }

                    if viewModel.isLoadingMore {
                        ProgressView()
                            .padding(.vertical, 12)
                            .tint(Palette.coral)
                    }
                }

                Color.clear.frame(height: 96)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
        }
        .refreshable { await refreshAndWait() }
        .tint(Palette.coral)
    }

    private var emptyState: some View {
        VStack(spacing: 10) {
            Text("🥗").font(.system(size: 48))
            Text("Пока нет сохранённых рационов")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(Palette.deepInk)
            Text("Создайте свой первый рацион с ИИ-нутрициологом")
                .font(.system(size: 13))
                .foregroundColor(Palette.mutedText)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)

            Button(action: { showAiCoach = true }) {
                HStack(spacing: 6) {
                    Image(systemName: "sparkles").font(.system(size: 14, weight: .semibold))
                    Text("Создать рацион").font(.system(size: 14, weight: .semibold))
                }
                .foregroundColor(.white)
                .padding(.horizontal, 24)
                .padding(.vertical, 12)
                .background(Capsule().fill(Palette.coral))
            }
            .buttonStyle(.plain)
            .padding(.top, 8)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 40)
    }

    private func errorView(message: String) -> some View {
        VStack(spacing: 12) {
            Text("⚠️").font(.system(size: 36))
            Text(message)
                .font(.system(size: 14))
                .foregroundColor(Palette.mutedText)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            Button(action: { viewModel.retry() }) {
                Text("Повторить")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 10)
                    .background(Capsule().fill(Palette.coral))
            }
            .buttonStyle(.plain)
        }
    }

    /// SwiftUI's `.refreshable` keeps the system spinner visible until the
    /// async closure returns. Polls the wrapper's `isRefreshing` flag so the
    /// indicator stays in sync with the underlying KMM state machine — same
    /// pattern as `WorkoutsView`.
    private func refreshAndWait() async {
        viewModel.refresh()
        let pollInterval: UInt64 = 50_000_000 // 50ms
        let startWaitMaxNanos: UInt64 = 500_000_000
        var startWaited: UInt64 = 0
        while !viewModel.isRefreshing && startWaited < startWaitMaxNanos {
            try? await Task.sleep(nanoseconds: pollInterval)
            startWaited += pollInterval
        }
        let maxWaitNanos: UInt64 = 15_000_000_000
        var elapsed: UInt64 = 0
        while viewModel.isRefreshing && elapsed < maxWaitNanos {
            try? await Task.sleep(nanoseconds: pollInterval)
            elapsed += pollInterval
        }
    }

    // MARK: - FAB

    private var fabButton: some View {
        Button(action: { showAiCoach = true }) {
            Image(systemName: "sparkles")
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(.white)
                .frame(width: 56, height: 56)
                .background(Circle().fill(Palette.coral))
                .shadow(color: Palette.coral.opacity(0.4), radius: 8, y: 4)
        }
        .buttonStyle(.plain)
        .padding(.trailing, 20)
        .padding(.bottom, 20)
    }
}

// MARK: - Card

private struct MealPlanListCard: View {
    let plan: MealPlanShortInfo
    let isDeleting: Bool
    let onTap: () -> Void
    let onDelete: () -> Void

    /// Displays the picked goal as a localized chip. Falls back to a neutral
    /// label if the wire value is unknown so unfamiliar (or future) goals do
    /// not crash the row.
    private var goalDisplay: String? {
        guard let raw = plan.goal else { return nil }
        return MealGoal.companion.fromWireValue(value: raw)?.displayName
    }

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top, spacing: 12) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 14)
                            .fill(Palette.coral.opacity(0.12))
                        Text("🥗").font(.system(size: 22))
                    }
                    .frame(width: 48, height: 48)

                    VStack(alignment: .leading, spacing: 4) {
                        Text(plan.name)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(Palette.deepInk)
                            .lineLimit(2)
                            .multilineTextAlignment(.leading)
                        if let desc = plan.description_, !desc.isEmpty {
                            Text(desc)
                                .font(.system(size: 12))
                                .foregroundColor(Palette.mutedText)
                                .lineLimit(2)
                                .multilineTextAlignment(.leading)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)

                    Menu {
                        Button(role: .destructive, action: onDelete) {
                            Label("Удалить", systemImage: "trash")
                        }
                    } label: {
                        Image(systemName: "ellipsis")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(Palette.mutedText)
                            .frame(width: 32, height: 32)
                    }
                    .disabled(isDeleting)
                }

                HStack(spacing: 6) {
                    if let goal = goalDisplay {
                        MealPlanChip(icon: "target", text: goal)
                    }
                    if let kcal = plan.totalCalories?.intValue, kcal > 0 {
                        MealPlanChip(icon: "flame.fill", text: "\(kcal) ккал")
                    }
                    MealPlanChip(icon: "list.bullet", text: "\(plan.mealsCount) приёма")
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 20))
            .overlay(
                RoundedRectangle(cornerRadius: 20)
                    .strokeBorder(Color(red: 0.929, green: 0.929, blue: 0.937), lineWidth: 1.5)
            )
            .opacity(isDeleting ? 0.5 : 1.0)
        }
        .buttonStyle(.plain)
        .disabled(isDeleting)
    }
}

private struct MealPlanChip: View {
    let icon: String
    let text: String

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: icon).font(.system(size: 11, weight: .semibold))
            Text(text).font(.system(size: 12, weight: .semibold))
        }
        .foregroundColor(Color(red: 0.227, green: 0.227, blue: 0.251))
        .padding(.horizontal, 10).padding(.vertical, 5)
        .background(RoundedRectangle(cornerRadius: 10).fill(Color(red: 0.957, green: 0.957, blue: 0.965)))
    }
}
