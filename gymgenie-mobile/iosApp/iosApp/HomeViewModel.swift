import SwiftUI
import Shared

@MainActor
final class HomeViewModel: ObservableObject {
    @Published var isLoading: Bool = false
    @Published var userProfile: UserProfileResponse? = nil
    @Published var activeWorkoutPlans: [WorkoutPlanShortResponse] = []
    @Published var errorMessage: String? = nil

    private let userApi: UserApi
    private let workoutApi: WorkoutApi

    init(baseUrl: String = "http://localhost:8081/api/v1") {
        self.userApi = UserApi(baseUrl: baseUrl)
        self.workoutApi = WorkoutApi(baseUrl: baseUrl)
    }

    func loadData() {
        guard !isLoading else { return }
        isLoading = true
        errorMessage = nil

        Task {
            await loadProfile()
            await loadActivePlans()
            isLoading = false
        }
    }

    func retry() {
        loadData()
    }

    // MARK: - Private

    private func loadProfile() async {
        guard let token = accessToken else {
            errorMessage = "Не авторизован"
            return
        }

        do {
            let result = try await userApi.getProfile(accessToken: token)
            if let profile = unwrapResult(result, as: UserProfileResponse.self) {
                userProfile = profile
            }
        } catch {
            errorMessage = "Ошибка загрузки профиля: \(error.localizedDescription)"
        }
    }

    private func loadActivePlans() async {
        guard let token = accessToken else { return }

        do {
            let result = try await workoutApi.getActivePlans(accessToken: token)
            if let plans = unwrapListResult(result) {
                activeWorkoutPlans = plans
            }
        } catch {
            if errorMessage == nil {
                errorMessage = "Ошибка загрузки планов: \(error.localizedDescription)"
            }
        }
    }

    private var accessToken: String? {
        UserDefaults.standard.string(forKey: "access_token")
    }

    // MARK: - Kotlin Result unwrapping

    private func unwrapResult<T: AnyObject>(_ result: Any?, as type: T.Type) -> T? {
        if let value = result as? T {
            return value
        }
        if let kotlinResult = result as? Shared.KotlinResult<T> {
            return kotlinResult.getOrNull()
        }
        return nil
    }

    private func unwrapListResult(_ result: Any?) -> [WorkoutPlanShortResponse]? {
        if let list = result as? [WorkoutPlanShortResponse] {
            return list
        }
        return nil
    }
}
