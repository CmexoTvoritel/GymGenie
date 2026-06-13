# Описание программной реализации финальных модулей приложения GymGenie

## 1. Модуль AI-ассистента (генерация тренировок)

**Назначение:** Интеллектуальный помощник, генерирующий персонализированные планы тренировок на основе диалога с пользователем. Интеграция с нейросетевой моделью GigaChat (Сбер).

### Архитектура бэкенда

Модуль реализует паттерн «конверсационный AI» — сервер поддерживает сессию диалога для каждого пользователя в in-memory хранилище (`ConversationSessionStore`), ограничивая историю 21 сообщением.

Контроллер `AiChatController` предоставляет REST API:

```kotlin
// gymgenieBackend/.../ai/controller/AiChatController.kt
@RestController
@RequestMapping("/api/v1/ai/chat")
class AiChatController(private val workoutAiService: WorkoutAiService) {

    @PostMapping
    fun chat(@RequestBody request: AiChatRequest,
             @AuthenticationPrincipal user: UserEntity): AiChatResponse =
        workoutAiService.chat(user, request)

    @PostMapping("/save")
    fun saveWorkout(@RequestBody request: SaveWorkoutRequest,
                    @AuthenticationPrincipal user: UserEntity): SaveWorkoutResponse =
        workoutAiService.saveGeneratedWorkout(user, request)

    @DeleteMapping("/session")
    fun clearSession(@AuthenticationPrincipal user: UserEntity) =
        workoutAiService.clearSession(user.id!!)
}
```

Сервис `WorkoutAiService` содержит основную бизнес-логику:

- формирование контекста пользователя (возраст, рост, вес, опыт, ограничения по здоровью);
- инъекция каталога упражнений в системный промпт;
- валидация ответа ИИ — удаление «галлюцинированных» упражнений, которых нет в базе;
- расчёт рекомендуемых весов на основе массы тела и уровня подготовки;
- парсинг и ремонт JSON из ответа нейросети.

Ответ AI имеет два типа: `CLARIFICATION` (уточняющий вопрос) или `WORKOUT` (готовый план):

```kotlin
// gymgenieBackend/.../ai/dto/AiChatDtos.kt
data class AiChatResponse(
    val type: AiResponseType,     // CLARIFICATION | WORKOUT
    val message: String,
    val workout: AiWorkoutDto?
)

data class AiWorkoutDto(
    val name: String,
    val description: String,
    val estimatedDurationMinutes: Int,
    val restSeconds: Int,
    val exercises: List<AiWorkoutExerciseParsedDto>
)
```

### Архитектура мобильного клиента

Shared-модуль (KMM) содержит `AiViewModel`, который управляет многоэтапным пользовательским потоком:

```
CHOOSE → PROFILE → EXPERIENCE → HEALTH → CHAT
```

На этапах PROFILE–HEALTH собираются метрики пользователя, затем начинается диалог с AI:

```kotlin
// shared/.../ai/AiViewModel.kt
class AiViewModel(private val aiApi: AiApi, ...) {
    private val _state = MutableStateFlow(AiUiState())
    val state: StateFlow<AiUiState> = _state.asStateFlow()

    fun sendMessage(text: String) {
        scope.launch {
            _state.update { it.copy(isTyping = true) }
            val request = AiChatRequest(
                message = text,
                ageYears = _state.value.age,
                heightCm = _state.value.height,
                weightKg = _state.value.weight,
                experience = _state.value.experience,
                frequency = _state.value.frequency,
                healthIssues = _state.value.healthIssues
            )
            val response = aiApi.chat(request)
            // обновление UI: добавление сообщения, парсинг тренировки
        }
    }
}
```

На Android диалог отображается на экране `AiFlowScreen.kt` (Jetpack Compose), на iOS — `AiCoachView.swift` (SwiftUI).

---

## 2. Модуль AI-ассистента (планирование питания)

**Назначение:** Генерация персонализированных планов питания через диалог с ИИ с учётом цели пользователя (похудение, поддержание, набор массы), аллергий и диетических ограничений.

### Архитектура бэкенда

Отдельный контроллер `AiMealController` и сервис `MealAiService` с собственным хранилищем сессий (`MealConversationSessionStore`):

```kotlin
// gymgenieBackend/.../ai/nutrition/AiMealController.kt
@RestController
@RequestMapping("/api/v1/ai/meal")
class AiMealController(private val mealAiService: MealAiService) {

    @PostMapping("/chat")
    fun chat(@RequestBody request: AiMealChatRequest,
             @AuthenticationPrincipal user: UserEntity): AiMealChatResponse =
        mealAiService.chat(user, request)

    @PostMapping("/save")
    fun saveMealPlan(@RequestBody request: SaveMealPlanRequest,
                     @AuthenticationPrincipal user: UserEntity) =
        mealAiService.saveMealPlan(user, request)

    @GetMapping("/booked-days")
    fun getBookedDays(@AuthenticationPrincipal user: UserEntity) =
        mealAiService.getBookedDays(user.id!!)

    @GetMapping("/check-conflicts")
    fun checkConflicts(@RequestParam mealType: String,
                       @RequestParam scheduleDays: List<String>,
                       @AuthenticationPrincipal user: UserEntity) =
        mealAiService.checkConflicts(user.id!!, mealType, scheduleDays)
}
```

Ключевые возможности сервиса:

- кэширование каталога продуктов с TTL 5 минут для контекста нейросети;
- детекция конфликтов расписания (нельзя назначить два завтрака на один день);
- поддержка целей питания: `LOSE_WEIGHT`, `MAINTAIN`, `GAIN_MUSCLE`.

### Мобильный клиент

`AiMealViewModel` реализует аналогичный многоэтапный поток с указанием цели, аллергий и типа приёма пищи. На Android — `AiMealFlowScreen.kt`, на iOS — `AiMealCoachView.swift`.

---

## 3. Модуль питания (CRUD, каталог продуктов)

**Назначение:** Управление планами питания, ручное создание рационов, просмотр нутриентного состава, каталог продуктов.

### Модель данных бэкенда (иерархическая)

```
MealPlan → Meal → Dish → FoodProduct
```

```kotlin
// gymgenieBackend/.../nutrition/entity/MealPlanEntity.kt
@Entity @Table(name = "meal_plans")
class MealPlanEntity(
    @Id @GeneratedValue val id: UUID? = null,
    @ManyToOne val user: UserEntity,
    val name: String,
    val description: String?,
    @Enumerated val goal: MealGoal,              // LOSE_WEIGHT | MAINTAIN | GAIN_MUSCLE
    val totalCalories: Int?,
    @Enumerated val createdBy: NutritionCreatedBy, // AI | USER
    @Enumerated val scheduleType: ScheduleType,    // ONE_TIME | RECURRING
    val scheduleDays: List<String>?,
    val oneOffDate: LocalDate?,
    @Enumerated val primaryMealType: MealType,     // BREAKFAST | LUNCH | DINNER
    val isActive: Boolean = true,
    @OneToMany val meals: MutableList<MealEntity> = mutableListOf()
)
```

```kotlin
// gymgenieBackend/.../nutrition/entity/DishEntity.kt
@Entity @Table(name = "dishes")
class DishEntity(
    @Id @GeneratedValue val id: UUID? = null,
    @ManyToOne val meal: MealEntity,
    val name: String,
    val calories: Int,
    val protein: Double, val carbs: Double, val fat: Double,
    val foodProductId: UUID?,
    val grams: Int?,
    @Enumerated val foodCategory: FoodCategory?
)
```

Каталог продуктов (`FoodProductEntity`) содержит нутриентный состав на 100 г: калории, белки, жиры, углеводы, клетчатка, сахар, а также категорию и emoji для визуализации.

### API эндпоинты

| Метод | URL | Описание |
|-------|-----|----------|
| GET | `/api/v1/meal-plans` | Список планов (пагинация) |
| GET | `/api/v1/meal-plans/{id}` | Детали плана с блюдами |
| POST | `/api/v1/meal-plans/manual` | Ручное создание плана |
| DELETE | `/api/v1/meal-plans/{id}` | Удаление плана |
| GET | `/api/v1/products` | Поиск продуктов |
| GET | `/api/v1/products/{id}` | Детали продукта |

### Мобильный клиент

Экраны реализованы на обеих платформах:

- `CreateMealPlanFlowScreen.kt` / `CreateMealPlanFlowView.swift` — мастер создания плана питания;
- `MealPlanDetailScreen.kt` / `MealPlanDetailView.swift` — детальный просмотр с круговой диаграммой макронутриентов (`MacroDonut`);
- Компоненты: `ProductCard`, `ScheduleChip`, `HeroMacrosCard`.

ViewModels: `CreateMealPlanViewModel`, `MealPlanDetailViewModel`, `MealPlansListViewModel` — все в shared-модуле KMM.

---

## 4. Модуль активностей (трекинг здоровья и привычек)

**Назначение:** Отслеживание ежедневных активностей по трём «кольцам»: движение (MOVE), осознанность (MIND), образ жизни (LIFE). Вдохновлено концепцией Activity Rings от Apple.

### Модель данных

```kotlin
// gymgenieBackend/.../activity/entity/ActivityDefinitionEntity.kt
@Entity @Table(name = "activity_definitions")
class ActivityDefinitionEntity(
    @Id @GeneratedValue val id: UUID? = null,
    val slug: String,              // уникальный идентификатор: "steps", "water", "meditation"
    val name: String,
    @Enumerated val ring: ActivityRing,   // MOVE | MIND | LIFE
    @Enumerated val kind: ActivityKind,   // BINARY | COUNTER | PRESET
    val presets: List<Int>?,       // [15, 30, 45, 60] для PRESET
    val unit: String?,             // "шаги", "мл", "мин"
    val defaultGoal: Int,
    val inverse: Boolean = false,  // true = цель "не более N"
    val sortOrder: Int = 0
)
```

Три типа активностей:

- **BINARY** — выполнено/не выполнено (например, «медитация»);
- **COUNTER** — числовая цель (например, 10 000 шагов);
- **PRESET** — быстрый выбор из предустановок (15/30/45/60 минут).

Журнал активностей (`ActivityLogEntity`) хранит значение по дням. Пользовательские настройки (`UserActivityEntity`) — цель и расписание.

### API

```kotlin
// gymgenieBackend/.../activity/controller/ActivityController.kt
@RestController
@RequestMapping("/api/v1/activities")
class ActivityController(private val activityService: ActivityService) {

    @GetMapping("/catalog")
    fun getCatalog() = activityService.getCatalog()

    @GetMapping("/today")
    fun getToday(@AuthenticationPrincipal user: UserEntity) =
        activityService.getTodayActivities(user.id!!)

    @PostMapping("/{activityId}/checkin")
    fun checkin(@PathVariable activityId: UUID,
                @RequestBody request: CheckinRequest,
                @AuthenticationPrincipal user: UserEntity) =
        activityService.checkin(user.id!!, activityId, request)

    @GetMapping("/history")
    fun getHistory(@RequestParam from: LocalDate,
                   @RequestParam to: LocalDate,
                   @AuthenticationPrincipal user: UserEntity) =
        activityService.getHistory(user.id!!, from, to)
}
```

### Мобильный клиент

Shared: `ActivitiesViewModel` агрегирует прогресс по кольцам, вычисляя `fraction` (0–1) для каждой активности:

```kotlin
// shared/.../activity/ActivityModels.kt
data class ActivityProgress(
    val activity: ActivityTodayResponse,
    val fraction: Float,   // logValue / goal
    val isDone: Boolean
)
```

Экраны:

- `ActivitiesScreen.kt` / `ActivitiesView.swift` — ежедневный трекер с прогресс-кольцами;
- `ActivityCatalogScreen.kt` / `ActivityCatalogView.swift` — каталог доступных активностей;
- `ActivityGoalSettingsScreen.kt` — настройка целей;
- `ActivityScheduleSettingsScreen.kt` — настройка расписания.

На главном экране прогресс отображается через `ActivityRingsCard` (круговые индикаторы) и `ActivityRowsCard` (линейные).

---

## 5. Модуль тренировок (финальная реализация)

**Назначение:** Полный цикл управления тренировками — от создания плана до live-сессии с трекингом подходов.

### Создание плана тренировки (ручное)

Многоэтапный поток: выбор группы мышц → выбор упражнений → настройка подходов/повторений/весов → сохранение.

```kotlin
// shared/.../workout/CreateWorkoutViewModel.kt
class CreateWorkoutViewModel(...) {
    data class UiState(
        val step: Step = Step.MUSCLE_GROUP,
        val selectedMuscleGroups: Set<String> = emptySet(),
        val selectedExercises: List<ExerciseShortResponse> = emptyList(),
        val exerciseConfigs: Map<String, ExerciseConfig> = emptyMap(),
        val planName: String = "",
        // ...
    )
    // Пошаговая навигация, валидация, сохранение через API
}
```

### Live-сессия тренировки

`WorkoutSessionViewModel` управляет выполнением в реальном времени:

- отслеживание текущего упражнения и подхода;
- таймер отдыха между подходами (`RestTimerScreen`);
- запись фактических повторений и весов по каждому подходу;
- локальное сохранение через SQLDelight при потере сети.

```sql
-- shared/.../db/WorkoutSession.sq
CREATE TABLE PendingWorkoutSession (
    id TEXT NOT NULL PRIMARY KEY,
    plan_id TEXT, plan_day_id TEXT, name TEXT NOT NULL,
    started_at TEXT NOT NULL, finished_at TEXT, status TEXT NOT NULL
);

CREATE TABLE PendingWorkoutSet (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id TEXT NOT NULL,
    exercise_id TEXT NOT NULL,
    set_number INTEGER NOT NULL,
    reps INTEGER, weight_kg REAL,
    completed INTEGER NOT NULL DEFAULT 0,
    duration_seconds INTEGER,
    FOREIGN KEY (session_id) REFERENCES PendingWorkoutSession(id)
);
```

`PendingSessionUploader` при запуске приложения проверяет наличие незагруженных сессий и отправляет их на сервер.

### Бэкенд — сессии

```kotlin
// gymgenieBackend/.../workout/controller/WorkoutSessionController.kt
@PostMapping("/submit")
fun submitSession(@RequestBody request: SubmitSessionRequest,
                  @AuthenticationPrincipal user: UserEntity): WorkoutSessionResponse =
    workoutSessionService.submitCompleted(user, request)
```

Статусы сессии: `IN_PROGRESS` → `COMPLETED` / `CANCELLED`.

### История и аналитика

Экраны `WorkoutHistoryScreen` / `HistorySummaryScreen` отображают пройденные тренировки по датам с детализацией по упражнениям и подходам.

---

## 6. Модуль профиля пользователя

**Назначение:** Управление персональными данными, физическими метриками, уровнем подготовки и подпиской.

### Модель на бэкенде

```kotlin
// gymgenieBackend/.../user/entity/UserEntity.kt
@Entity @Table(name = "users")
class UserEntity(
    @Id @GeneratedValue val id: UUID? = null,
    val email: String,
    val passwordHash: String,
    val firstName: String,
    val lastName: String?,
    @Enumerated val gender: Gender?,           // MALE | FEMALE | OTHER
    val birthDate: LocalDate?,
    val weightKg: Double?,
    val heightCm: Double?,
    val ageYears: Int?,
    val experience: String?,                    // BEGINNER | INTERMEDIATE | ADVANCED
    val frequency: String?,                     // частота тренировок
    val healthIssues: String?,                  // ограничения по здоровью
    @Enumerated val subscriptionType: SubscriptionType = SubscriptionType.FREE,
    val subscriptionExpiresAt: LocalDateTime?
)
```

### Мобильный клиент

`UserProfileStore` (KMM shared) хранит профиль в `StateFlow` и используется всеми модулями (AI, Home, Profile).

Экраны редактирования: `EditProfileScreen`, `EditMetricsScreen`, `EditHealthScreen`, `EditExperienceScreen` — на обеих платформах.

---

## 7. Модуль аутентификации

**Назначение:** Регистрация, авторизация, управление токенами (JWT access + refresh).

### Бэкенд

Безопасность реализована через Spring Security с stateless-сессиями и JWT-фильтром:

```kotlin
// gymgenieBackend/.../common/security/JwtAuthenticationFilter.kt
class JwtAuthenticationFilter(private val jwtProvider: JwtProvider) : OncePerRequestFilter() {
    override fun doFilterInternal(request, response, filterChain) {
        val token = extractToken(request)
        if (token != null && jwtProvider.validateToken(token)) {
            val userId = jwtProvider.getUserIdFromToken(token)
            // установка SecurityContext
        }
        filterChain.doFilter(request, response)
    }
}
```

Эндпоинты: регистрация, логин, обновление токена, выход из одной/всех сессий.

### Мобильный клиент

`AuthViewModel` управляет формами ввода, валидацией, сохранением токенов в платформенное хранилище (`TokenStorage` — SharedPreferences на Android, Keychain на iOS). HTTP-клиент автоматически подставляет `Authorization: Bearer` заголовок и обрабатывает 401 через рефреш токена.

---

## 8. Модуль подписки (Paywall)

**Назначение:** Разграничение функционала между бесплатными и премиум-пользователями.

Типы подписки: `FREE` и `PREMIUM`. Премиум-функции (например, модуль питания) блокируются оверлеем `PremiumLockedOverlay` для бесплатных пользователей.

Экраны: `PaywallScreen` / `PaywallView` — презентация преимуществ и тарифов. `PurchaseSuccessScreen` — подтверждение покупки.

---

## 9. Модуль главного экрана (Dashboard)

**Назначение:** Агрегация данных из всех модулей в единый дашборд.

```kotlin
// shared/.../presentation/HomeViewModel.kt
data class HomeUiState(
    val screenState: ScreenState,
    val name: String,
    val subscriptionType: String,
    val activeWorkoutPlans: List<WorkoutPlanShortResponse>,
    val todayActivities: List<ActivityTodayResponse>,
    val todayMealPlans: List<TodayMealPlanCard>,
    val pendingSession: ActiveWorkoutSession?
)
```

Компоненты: `HomeHeaderSection` (приветствие), `WorkoutTodayPager` (карусель тренировок), `ActivityRingsCard` (кольца прогресса), `MealPlanSection` (сегодняшний рацион).

---

## 10. Навигация (Decompose)

На Android реализована через библиотеку Decompose с корневым стеком:

```
Splash → Onboarding → Privacy → Auth → [Paywall] → Main
```

Внутри `Main` — 4 вкладки: Home, Workouts, AI, Profile.

Каждая вкладка имеет собственный подстек навигации (`ChildStack`). Сессия тренировки и Paywall реализованы как модальные слоты (`ChildSlot`).

На iOS — ручная маршрутизация через `AppState` enum + SwiftUI `NavigationStack`.

---

## Сводная таблица модулей

| Модуль | Бэкенд (контроллеры/сервисы) | Shared KMM (API/ViewModel) | Android UI | iOS UI |
|--------|-------|------|------|------|
| Аутентификация | AuthController, AuthService | AuthApi, AuthViewModel | AuthScreen | AuthView |
| Профиль | UserController, UserService | UserApi, ProfileViewModel | ProfileScreen + Edit* | ProfileView + Edit* |
| AI тренировки | AiChatController, WorkoutAiService | AiApi, AiViewModel | AiFlowScreen | AiCoachView |
| AI питание | AiMealController, MealAiService | AiMealApi, AiMealViewModel | AiMealFlowScreen | AiMealCoachView |
| Тренировки | WorkoutPlanController, WorkoutSessionController | WorkoutApi, Create/Session/HistoryVM | 8 экранов | 7 экранов |
| Каталог упражнений | ExerciseController, ExerciseService | ExerciseApi | ExerciseDetail, Picker | ExerciseDetailView |
| Питание | MealPlanController, FoodProductController | MealPlansApi, FoodProductApi | CreateMealPlan, Detail | CreateMealPlan, Detail |
| Активности | ActivityController, ActivityService | ActivityApi, ActivitiesVM | 4 экрана + компоненты | 4 экрана |
| Главный экран | — (агрегация) | HomeViewModel | HomeScreen | HomeView |
| Подписка | UserController (подписка) | PaywallViewModel | PaywallScreen | PaywallView |

---

## Технологический стек

| Уровень | Технологии |
|---------|------------|
| Бэкенд | Kotlin, Spring Boot 3, Spring Security, JWT, Hibernate JPA, PostgreSQL, GigaChat API |
| Shared (KMM) | Kotlin Multiplatform, Ktor Client, Koin, SQLDelight, Kotlinx Serialization, Coroutines |
| Android | Jetpack Compose, Decompose, Material 3 |
| iOS | SwiftUI, NavigationStack |

---

## Количественные показатели реализации

**Бэкенд:** 7 контроллеров, 12 сервисов, 14 сущностей, 10 репозиториев, 50+ DTO, 12 перечислений.

**Мобильный клиент:** 15+ ViewModel (shared), ~20 экранов на Android, ~15 экранов на iOS, SQLDelight для офлайн-персистенции.
