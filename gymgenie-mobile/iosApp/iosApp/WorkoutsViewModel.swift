import SwiftUI
import Shared

enum WorkoutsTab {
    case workouts
    case exercises
}

@MainActor
final class WorkoutsViewModel: ObservableObject {
    @Published var selectedTab: WorkoutsTab = .workouts
    @Published var workoutPlans: [WorkoutPlanShortResponse] = []
    @Published var exercises: [ExerciseShortResponse] = []
    @Published var searchQuery: String = ""
    @Published var isLoading: Bool = false
    @Published var isLoadingMore: Bool = false
    @Published var hasMoreExercises: Bool = true
    @Published var exercisesLoaded: Bool = false
    @Published var errorMessage: String? = nil

    private let workoutApi: WorkoutApi
    private let exerciseApi: ExerciseApi
    private var currentExercisePage: Int = 0

    init(baseUrl: String = "http://localhost:8081/api/v1") {
        self.workoutApi = WorkoutApi(baseUrl: baseUrl)
        self.exerciseApi = ExerciseApi(baseUrl: baseUrl)
    }

    func selectTab(_ tab: WorkoutsTab) {
        selectedTab = tab
        if tab == .exercises && !exercisesLoaded {
            exercisesLoaded = true
            loadExercises(reset: true)
        }
    }

    func loadWorkoutPlans() {
        guard !isLoading else { return }
        isLoading = true
        errorMessage = nil

        Task {
            guard let token = accessToken else {
                errorMessage = "Не авторизован"
                isLoading = false
                return
            }

            do {
                let pagedResult = try await workoutApi.getPlans(accessToken: token, page: 0, size: 20)
                workoutPlans = pagedResult.content as? [WorkoutPlanShortResponse] ?? []
            } catch {
                errorMessage = "Ошибка загрузки: \(error.localizedDescription)"
            }

            isLoading = false
        }
    }

    func loadExercises(reset: Bool = false) {
        if reset {
            currentExercisePage = 0
            exercises = []
            hasMoreExercises = true
        }

        guard !isLoading, !isLoadingMore, hasMoreExercises else { return }

        if currentExercisePage == 0 {
            isLoading = true
        } else {
            isLoadingMore = true
        }
        errorMessage = nil

        Task {
            do {
                let pagedResult: Any
                if searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    pagedResult = try await exerciseApi.getExercises(
                        muscleGroup: nil,
                        category: nil,
                        page: Int32(currentExercisePage),
                        size: 20
                    )
                } else {
                    pagedResult = try await exerciseApi.searchExercises(
                        query: searchQuery,
                        page: Int32(currentExercisePage),
                        size: 20
                    )
                }

                let items = unwrapPagedExerciseResult(pagedResult)
                if currentExercisePage == 0 {
                    exercises = items
                } else {
                    exercises.append(contentsOf: items)
                }
                hasMoreExercises = !items.isEmpty && items.count >= 20
                currentExercisePage += 1
            } catch {
                if errorMessage == nil {
                    errorMessage = "Ошибка загрузки: \(error.localizedDescription)"
                }
            }

            isLoading = false
            isLoadingMore = false
        }
    }

    func loadMoreExercises() {
        loadExercises()
    }

    func searchExercises() {
        loadExercises(reset: true)
    }

    func retry() {
        switch selectedTab {
        case .workouts:
            loadWorkoutPlans()
        case .exercises:
            loadExercises(reset: true)
        }
    }

    // MARK: - Private

    private var accessToken: String? {
        UserDefaults.standard.string(forKey: "access_token")
    }

    private func unwrapPagedExerciseResult(_ result: Any) -> [ExerciseShortResponse] {
        if let pagedResponse = result as? PagedResponse<ExerciseShortResponse> {
            return pagedResponse.content as? [ExerciseShortResponse] ?? []
        }
        // Fallback: SKIE may bridge PagedResponse with erased generics
        if let pagedResponse = result as? PagedResponse<AnyObject> {
            return pagedResponse.content as? [ExerciseShortResponse] ?? []
        }
        return []
    }
}
