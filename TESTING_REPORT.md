# Отчёт по тестированию приложения GymGenie

## Содержание
1. [Unit-тестирование](#1-unit-тестирование)
2. [Функциональное тестирование интерфейса](#2-функциональное-тестирование-интерфейса)
3. [Тестирование производительности](#3-тестирование-производительности)
4. [Тестирование удобства использования (Usability)](#4-тестирование-удобства-использования-usability)
5. [Тестирование безопасности](#5-тестирование-безопасности)

---

## Описание проекта

**GymGenie** — AI-powered спортивное мобильное приложение для планирования тренировок, отслеживания питания и физической активности.

**Технологический стек:**
- Backend: Kotlin + Spring Boot 4.0.3, PostgreSQL, JWT-аутентификация
- Мобильная логика: Kotlin Multiplatform (KMM) shared-модуль
- Android UI: Jetpack Compose
- iOS UI: SwiftUI
- Навигация: Decompose
- AI-интеграция: GigaChat API (чат-бот для тренировок и питания)
- DI: Koin 4.0.0

**Ключевые модули приложения:**
- Аутентификация (регистрация, логин, JWT-токены, refresh-токены)
- Тренировки (создание планов, живое отслеживание сессий, история)
- Каталог упражнений (фильтрация по мышечным группам, сложности, оборудованию)
- Питание (AI-генерация и ручное создание планов питания, макронутриенты)
- Активности (шаги, вода, зарядка и т.д., кольца активности)
- Профиль пользователя (метрики тела, подписка, настройки)
- AI-ассистент (чат для генерации тренировок и планов питания)

---

## 1. Unit-тестирование

### 1.1. Инструменты и подход

#### Backend (Spring Boot)

| Параметр | Значение |
|---|---|
| Фреймворк тестирования | JUnit 5 (JUnit Platform) |
| Мокирование | MockK 1.13.16 |
| Язык | Kotlin |
| Ассерты | kotlin.test (assertEquals, assertFailsWith) |
| Запуск | Gradle `./gradlew test` |

**Подход:** Все сервисы тестируются изолированно — зависимости (репозитории, провайдеры, энкодеры) замокированы через MockK. Каждый тест-класс содержит helper-функции для создания тестовых сущностей (createUser, createExercise и т.д.). Тесты покрывают как позитивные сценарии (happy path), так и негативные (ошибки валидации, не найденные сущности, конфликты).

#### Mobile (KMM shared-модуль)

| Параметр | Значение |
|---|---|
| Фреймворк тестирования | kotlin.test (KMP) |
| Корутины | kotlinx-coroutines-test 1.10.1 (UnconfinedTestDispatcher) |
| HTTP моки | Ktor MockEngine 3.1.3 |
| Язык | Kotlin Multiplatform (commonTest) |
| Ассерты | kotlin.test (assertEquals, assertNull, assertTrue и т.д.) |
| Запуск | Gradle `./gradlew :shared:testDebugUnitTest` |

**Подход:** ViewModel'ы тестируются с подменой `Dispatchers.Main` через `UnconfinedTestDispatcher`. API-зависимости создаются с Ktor MockEngine (HTTP-вызовы не выполняются в синхронных тестах). Для интерфейсов (TokenStorage) используются fake-реализации. Тесты покрывают валидацию ввода, нормализацию данных, граничные значения, state-переходы и чистые функции расчёта макронутриентов.

### 1.2. Покрытие

#### Backend (38 тестов)

| Тест-класс | Тестируемый сервис | Кол-во тестов | Покрытые сценарии |
|---|---|---|---|
| `AuthServiceTest` | AuthService | 10 | Регистрация, логин, refresh, logout |
| `ExerciseServiceTest` | ExerciseService | 6 | CRUD, получение мышечных групп |
| `WorkoutSessionServiceTest` | WorkoutSessionService | 9 | Старт, submit, add set, finish, delete |
| `UserServiceTest` | UserService | 4 | Профиль, обновление, подписка |
| `ActivityServiceTest` | ActivityService | 9 | Каталог, checkin, план, удаление |
| **Итого backend** | **5 сервисов** | **38 тестов** | |

#### Mobile shared (79 тестов)

| Тест-класс | Тестируемый компонент | Кол-во тестов | Покрытые сценарии |
|---|---|---|---|
| `CreateWorkoutViewModelTest` | CreateWorkoutViewModel | 30 | Rest seconds, расписание, нормализация упражнений, весов, валидация сохранения |
| `AuthViewModelTest` | AuthViewModel | 19 | Валидация логина/регистрации, обновление полей, очистка ошибок, сброс состояния |
| `FoodPickerViewModelTest` | FoodPickerViewModel | 19 | Санитизация ввода, парсинг граммов, фильтрация продуктов, выбор категорий |
| `FoodProductMacrosTest` | FoodProduct + FoodCategory | 11 | Расчёт макросов за граммы, парсинг категорий |
| **Итого mobile** | **4 компонента** | **79 тестов** | |

#### Общий итог: **117 тестов** (38 backend + 79 mobile), все проходят

### 1.3. Детализация тестов

#### AuthServiceTest (10 тестов)

| # | Тест | Описание | Ожидание |
|---|---|---|---|
| 1 | `register_success` | Регистрация нового пользователя с валидными данными | Пользователь создан, возвращены access + refresh токены, subscriptionType=FREE |
| 2 | `register_emailAlreadyExists_throwsConflict` | Попытка регистрации с уже занятым email | ConflictException("Email already registered") |
| 3 | `login_success` | Логин с правильными email и паролем | Возвращены токены, данные пользователя |
| 4 | `login_wrongEmail_throwsUnauthorized` | Логин с несуществующим email | UnauthorizedException("Invalid email or password") |
| 5 | `login_wrongPassword_throwsUnauthorized` | Логин с неправильным паролем | UnauthorizedException("Invalid email or password") |
| 6 | `refresh_success` | Обновление токена с валидным refresh-токеном | Старый токен удалён, новая пара токенов выдана |
| 7 | `refresh_invalidToken_throwsUnauthorized` | Невалидный refresh-токен | UnauthorizedException("Invalid refresh token") |
| 8 | `refresh_expiredToken_throwsUnauthorized` | Истёкший refresh-токен | Токен удалён + UnauthorizedException("Refresh token expired") |
| 9 | `logout_deletesToken` | Выход из одной сессии | Вызван deleteByToken |
| 10 | `logoutAll_deletesAllTokens` | Выход из всех сессий | Вызван deleteAllByUserId |

#### ExerciseServiceTest (6 тестов)

| # | Тест | Описание | Ожидание |
|---|---|---|---|
| 1 | `getById_success` | Получение упражнения по ID | Все поля маппятся корректно (nameRu, nameEn, muscleGroup, difficulty и т.д.) |
| 2 | `getById_notFound_throwsNotFoundException` | Несуществующий ID | NotFoundException("Exercise not found") |
| 3 | `getMuscleGroups_returns13Groups` | Список мышечных групп | 13 групп, каждая с русским и английским названием |
| 4 | `create_success` | Создание упражнения | Сохранено и возвращено с корректными полями |
| 5 | `delete_success` | Удаление существующего упражнения | existsById + deleteById вызваны |
| 6 | `delete_notFound_throwsNotFoundException` | Удаление несуществующего | NotFoundException("Exercise not found") |

#### WorkoutSessionServiceTest (9 тестов)

| # | Тест | Описание | Ожидание |
|---|---|---|---|
| 1 | `start_success` | Старт новой тренировки | Сессия создана со статусом IN_PROGRESS |
| 2 | `submit_success` | Отправка завершённой тренировки с подходами | Сессия + подходы сохранены, маппинг корректен |
| 3 | `submit_finishedAtBeforeStartedAt_throwsBadRequest` | finishedAt раньше startedAt | BadRequestException("finishedAt must not be before startedAt") |
| 4 | `submit_inProgressStatus_throwsBadRequest` | Отправка со статусом IN_PROGRESS | BadRequestException("Submitted session must be in a finished state...") |
| 5 | `addSet_success` | Добавление подхода к активной сессии | Подход сохранён, verify вызова save |
| 6 | `addSet_finishedSession_throwsBadRequest` | Добавление подхода к завершённой сессии | BadRequestException("Cannot add sets to a finished session") |
| 7 | `finish_success` | Завершение тренировки | Статус = COMPLETED, finishedAt установлен |
| 8 | `finish_alreadyFinished_throwsBadRequest` | Завершение уже завершённой | BadRequestException("Session is already finished") |
| 9 | `delete_success` | Удаление сессии | repository.delete вызван |

#### UserServiceTest (4 теста)

| # | Тест | Описание | Ожидание |
|---|---|---|---|
| 1 | `getProfile_success` | Получение профиля | Все поля маппятся (email, имя, метрики, подписка) |
| 2 | `getProfile_notFound_throwsNotFoundException` | Несуществующий пользователь | NotFoundException("User not found") |
| 3 | `updateProfile_success` | Частичное обновление профиля | Обновлённые поля изменились, остальные сохранились |
| 4 | `activateSubscription_success` | Активация премиум-подписки | subscriptionType стал PREMIUM |

#### ActivityServiceTest (9 тестов)

| # | Тест | Описание | Ожидание |
|---|---|---|---|
| 1 | `getCatalog_success` | Получение каталога активностей | Отсортировано по sortOrder, presets распарсены |
| 2 | `checkin_binaryActivity_setsValueTo1` | Чекин бинарной активности (зарядка) | value=1 независимо от переданного значения |
| 3 | `checkin_counterActivity_usesRequestValue` | Чекин активности-счётчика (вода) | value = переданное значение (5) |
| 4 | `checkin_updatesExistingLog` | Повторный чекин на ту же дату | Обновляет существующий лог, а не создаёт новый |
| 5 | `addToPlan_success` | Добавление активности в план | Сохранено с расписанием и целью |
| 6 | `addToPlan_alreadyExists_throwsConflict` | Повторное добавление | ConflictException("Activity is already in the user's plan") |
| 7 | `addToPlan_recurringWithoutDays_throwsBadRequest` | RECURRING без дней | BadRequestException("scheduleDays is required for RECURRING activities") |
| 8 | `removeFromPlan_success` | Удаление активности из плана | deleteByUserIdAndActivityId вызван |
| 9 | `removeFromPlan_notInPlan_throwsNotFoundException` | Удаление отсутствующей | NotFoundException("Activity is not in the user's plan") |

### 1.4. Детализация мобильных тестов (shared-модуль)

#### CreateWorkoutViewModelTest (30 тестов)

| # | Тест | Описание | Ожидание |
|---|---|---|---|
| 1 | `setRestSeconds_clampsToMinimum` | Установка rest=5 (ниже MIN_REST=10) | rest = 10 |
| 2 | `setRestSeconds_clampsToMaximum` | Установка rest=700 (выше MAX_REST=600) | rest = 600 |
| 3 | `setRestSeconds_acceptsValueInRange` | Установка rest=120 | rest = 120 |
| 4 | `incrementRestSeconds_addsStep` | 60 + шаг(5) | rest = 65 |
| 5 | `decrementRestSeconds_subtractsStep` | 60 - шаг(5) | rest = 55 |
| 6 | `decrementRestSeconds_doesNotGoBelowMin` | Декремент от минимума | rest = 10 (не ниже) |
| 7 | `setScheduleType_clearsDaysOnChange` | Смена типа расписания | Дни очищены |
| 8 | `setScheduleType_sameType_doesNotClearDays` | Повтор того же типа | Дни сохранены |
| 9 | `toggleScheduleDay_addsAndRemoves` | Двойное нажатие на день | Добавлен → Удалён |
| 10 | `addExercise_clampsSetsAndReps` | sets=15, reps=30 | sets=10, reps=25 |
| 11 | `addExercise_clampsLowSetsAndReps` | sets=0, reps=1 | sets=1, reps=2 |
| 12 | `addExercise_normalizesWeights_noWeight` | requiresWeight=false | setWeightsKg=null |
| 13 | `addExercise_normalizesWeights_noExistingWeights` | requiresWeight=true, нет весов | List(3) { 20.0 } |
| 14 | `addExercise_normalizesWeights_padsList` | 2 веса но 4 подхода | Дополнено последним значением |
| 15 | `addExercise_normalizesWeights_truncatesList` | 5 весов но 3 подхода | Обрезано до 3 |
| 16 | `addExercise_normalizesWeights_exactMatch` | 3 веса и 3 подхода | Без изменений |
| 17 | `addExercise_clampsWeightValues` | 600.0 и -5.0 | 500.0 и 0.0 |
| 18 | `addExercise_preservesNullWeightInList` | [null, 50.0] | null сохранён |
| 19 | `updateExerciseAt_validIndex_normalizesAndReplaces` | Обновление по индексу | Нормализация применена |
| 20 | `updateExerciseAt_invalidIndex_noChange` | Индекс вне диапазона | Без изменений |
| 21 | `removeExerciseAt_validIndex_removes` | Удаление первого упражнения | Осталось одно |
| 22 | `removeExerciseAt_invalidIndex_noChange` | Индекс 5 при 1 элементе | Без изменений |
| 23 | `removeExerciseAt_negativeIndex_noChange` | Индекс -1 | Без изменений |
| 24 | `saveWorkout_emptyName_setsError` | Пустое название | "Введите название тренировки" |
| 25 | `saveWorkout_noExercises_setsError` | Нет упражнений | "Добавьте хотя бы одно упражнение" |
| 26 | `saveWorkout_recurringWithoutDays_setsError` | RECURRING без дней | "Выберите хотя бы один день недели" |
| 27 | `setWorkoutName_updatesState` | Установка названия | Название обновлено |
| 28 | `setDescription_updatesState` | Установка описания | Описание обновлено |
| 29 | `clearError_clearsErrorMessage` | Очистка ошибки | errorMessage = null |
| 30 | `reset_clearsState` | Полный сброс | Все поля по умолчанию |

#### AuthViewModelTest (19 тестов)

| # | Тест | Описание | Ожидание |
|---|---|---|---|
| 1 | `login_emptyEmail_setsError` | Логин без email | "Заполните все поля" |
| 2 | `login_emptyPassword_setsError` | Логин без пароля | "Заполните все поля" |
| 3 | `login_bothEmpty_setsError` | Оба поля пустые | "Заполните все поля" |
| 4 | `login_blankEmail_setsError` | Email = "   " | "Заполните все поля" |
| 5 | `register_emptyName_setsError` | Регистрация без имени | "Заполните все поля" |
| 6 | `register_emptyEmail_setsError` | Регистрация без email | "Заполните все поля" |
| 7 | `register_emptyPassword_setsError` | Регистрация без пароля | "Заполните все поля" |
| 8 | `register_shortPassword_setsError` | Пароль 5 символов | "Пароль должен быть не менее 6 символов" |
| 9 | `register_exactSixCharPassword_passesValidation` | Пароль 6 символов | Валидация пройдена |
| 10 | `onEmailChanged_updatesState` | Ввод email | Поле обновлено |
| 11 | `onEmailChanged_clearsError` | Ввод email после ошибки | Ошибка сброшена |
| 12 | `onPasswordChanged_updatesState` | Ввод пароля | Поле обновлено |
| 13 | `onPasswordChanged_clearsError` | Ввод пароля после ошибки | Ошибка сброшена |
| 14 | `onNameChanged_updatesState` | Ввод имени | Поле обновлено |
| 15 | `onNameChanged_clearsError` | Ввод имени после ошибки | Ошибка сброшена |
| 16 | `clearError_clearsErrorMessage` | Очистка ошибки | errorMessage = null |
| 17 | `resetState_resetsToDefaults` | Полный сброс | Все поля по умолчанию, subscriptionType="FREE" |
| 18 | `consumeLoginSuccess_resetsFlag` | Потребление флага | loginSuccess = false |
| 19 | `consumeRegisterSuccess_resetsFlag` | Потребление флага | registerSuccess = false |

#### FoodPickerViewModelTest (19 тестов)

| # | Тест | Описание | Ожидание |
|---|---|---|---|
| 1 | `onAmountChange_filtersNonDigitsAndDots` | "abc123" | "123" |
| 2 | `onAmountChange_replacesCommaWithDot` | "10,5" | "10.5" |
| 3 | `onAmountChange_removesDuplicateDots` | "10.5.3" | "10.53" |
| 4 | `onAmountChange_truncatesAtMaxLength` | "1234567890" | "123456" (макс 6 символов) |
| 5 | `onAmountChange_allowsEmptyString` | "" | "" |
| 6 | `onAmountChange_commaConvertedBeforeDuplicateDotRemoval` | "1,2,3" | "1.23" |
| 7 | `parsedAmountGrams_validInput` | "150" | 150.0 |
| 8 | `parsedAmountGrams_decimalInput` | "10.5" | 10.5 |
| 9 | `parsedAmountGrams_zeroReturnsNull` | "0" | null |
| 10 | `parsedAmountGrams_emptyReturnsNull` | "" | null |
| 11 | `canConfirmAmount_noProduct_false` | Нет продукта | false |
| 12 | `canConfirmAmount_withProductAndValidAmount_true` | Продукт + 100г | true |
| 13 | `canConfirmAmount_withProductAndZeroAmount_false` | Продукт + 0г | false |
| 14 | `selectProduct_setsDefaultAmount` | Выбор продукта | amountGrams = "100" |
| 15 | `dismissProductDetail_clearsSelection` | Закрытие деталей | selectedProduct = null |
| 16 | `onSearchQueryChange_filtersProducts` | Поиск "курица" | searchQuery обновлён |
| 17 | `onCategorySelected_updatesState` | Выбор категории MEAT | selectedCategory = MEAT |
| 18 | `onCategorySelected_null_clearsFilter` | Сброс категории | selectedCategory = null |
| 19 | `onAmountPresetSelected_setsAmount` | Пресет 200г | amountGrams = "200" |

#### FoodProductMacrosTest (11 тестов)

| # | Тест | Описание | Ожидание |
|---|---|---|---|
| 1 | `macrosForGrams_100g_returnsSameValues` | 100г = per100g | Идентичные значения |
| 2 | `macrosForGrams_200g_doublesValues` | 200г | Удвоенные значения |
| 3 | `macrosForGrams_50g_halvesValues` | 50г | Половина значений |
| 4 | `macrosForGrams_0g_returnsZeros` | 0г | Все нули |
| 5 | `macrosForGrams_nullFiberAndSugar_returnsNulls` | fiber=null, sugar=null | fiberG=null, sugarG=null |
| 6 | `macrosForGrams_decimalGrams` | 75г | 0.75x |
| 7 | `fromKeyOrOther_validKey_returnsCategory` | "MEAT" | FoodCategory.MEAT |
| 8 | `fromKeyOrOther_allValidKeys` | Все ключи | Все enum-значения корректны |
| 9 | `fromKeyOrOther_invalidKey_returnsOther` | "UNKNOWN" | FoodCategory.OTHER |
| 10 | `fromKeyOrOther_emptyKey_returnsOther` | "" | FoodCategory.OTHER |
| 11 | `fromKeyOrOther_lowercaseKey_returnsOther` | "meat" | FoodCategory.OTHER (case-sensitive) |

### 1.5. Результаты запуска

**Backend:**
```
./gradlew test (gymgenieBackend)
BUILD SUCCESSFUL — 38 tests, 0 failures, 0 errors
```

**Mobile shared:**
```
./gradlew :shared:testDebugUnitTest (gymgenie-mobile)
BUILD SUCCESSFUL — 79 tests, 0 failures, 0 errors
100% success rate
```

### 1.6. Расположение тестов

```
gymgenieBackend/src/test/kotlin/com/asc/gymgenie/
├── auth/service/AuthServiceTest.kt          (10 тестов)
├── exercise/service/ExerciseServiceTest.kt  (6 тестов)
├── workout/service/WorkoutSessionServiceTest.kt  (9 тестов)
├── user/service/UserServiceTest.kt          (4 теста)
└── activity/service/ActivityServiceTest.kt  (9 тестов)

gymgenie-mobile/shared/src/commonTest/kotlin/com/asc/gymgenie/
├── presentation/
│   ├── CreateWorkoutViewModelTest.kt    (30 тестов)
│   ├── AuthViewModelTest.kt            (19 тестов)
│   └── FoodPickerViewModelTest.kt       (19 тестов)
└── nutrition/
    └── FoodProductMacrosTest.kt         (11 тестов)
```

---

## 2. Функциональное тестирование интерфейса

### 2.1. Описание приложения и навигация

Приложение содержит 4 основных таба (нижняя навигация):

| Таб | Иконка | Содержание |
|---|---|---|
| Главная (Home) | Домик | Дашборд: активности сегодня, план питания, тренировки |
| Тренировки (Workouts) | Гантеля | Список тренировок, каталог упражнений, создание |
| AI-ассистент (AI) | Мозг/робот | Чат с AI для генерации тренировок |
| Профиль (Profile) | Человек | Профиль, настройки, метрики, подписка |

### 2.2. Пользовательские потоки (User Flows)

#### Поток 1: Регистрация и онбординг

**Сценарий:** Новый пользователь открывает приложение впервые.

| Шаг | Экран | Действие пользователя | Что происходит |
|---|---|---|---|
| 1 | SplashScreen | Ожидание | Показывается заставка приложения, проверяется наличие сохранённого токена |
| 2 | OnboardingScreen | Свайп по слайдам → «Далее» | Показываются 3-4 слайда с преимуществами приложения |
| 3 | PrivacyScreen | Чтение политики → «Принять» | Пользователь принимает политику конфиденциальности |
| 4 | AuthScreen | Ввод имени, email, пароля → «Зарегистрироваться» | POST /api/v1/auth/register → получение JWT-токенов |
| 5 | PaywallScreen | «Попробовать бесплатно» или «Купить подписку» | Показываются преимущества Premium-подписки |
| 6 | HomeScreen | Просмотр дашборда | Пользователь попадает на главный экран |

**Что проверять:**
- Кнопка «Зарегистрироваться» недоступна при пустых полях
- Валидация email-формата
- Валидация пароля (мин. 8 символов)
- Корректная обработка ошибки «Email already registered»
- Корректный переход через все шаги онбординга
- Токен сохраняется после регистрации (повторный запуск не требует логина)

#### Поток 2: Авторизация существующего пользователя

| Шаг | Экран | Действие | Что происходит |
|---|---|---|---|
| 1 | AuthScreen | Переключение на вкладку «Вход» | Форма меняется на логин (email + пароль) |
| 2 | AuthScreen | Ввод email и пароля → «Войти» | POST /api/v1/auth/login → получение токенов |
| 3 | HomeScreen | Дашборд загружается | Показываются данные пользователя |

**Что проверять:**
- Ошибка «Invalid email or password» при неправильных данных
- Переключение между вкладками «Вход» / «Регистрация»
- Индикатор загрузки при ожидании ответа от сервера

#### Поток 3: Создание тренировки

**Сценарий:** Пользователь создаёт план тренировки с нуля.

| Шаг | Экран | Действие | Что происходит |
|---|---|---|---|
| 1 | WorkoutsScreen | Таб «Тренировки» → «+» (создать) | Переход в мастер создания тренировки |
| 2 | CreateWorkoutFlowScreen | Ввод названия тренировки | Пользователь вводит «Тренировка груди» |
| 3 | MuscleGroupPickerScreen | Выбор мышечных групп (чипы) | Пользователь тапает на «Грудь», «Трицепс» |
| 4 | ExercisePickerScreen | Выбор упражнений из каталога | Отображается отфильтрованный список упражнений по выбранным мышцам. Пользователь выбирает «Жим лёжа», «Разведение гантелей» |
| 5 | ExerciseConfigScreen | Настройка подходов/повторений/весов | Для каждого упражнения: кол-во подходов (sets), повторений (reps), вес (kg) для каждого подхода |
| 6 | WorkoutBuilderScreen | Обзор плана → «Создать» | Показывается финальный список упражнений. POST /api/v1/workout-plans/simple |
| 7 | WorkoutsScreen | Тренировка появилась в списке | Список обновлён |

**Что проверять:**
- Выбор нескольких мышечных групп фильтрует упражнения корректно
- Настройка весов для каждого подхода сохраняется
- Валидация: нельзя создать тренировку без упражнений
- Для RECURRING тренировки требуется выбрать дни недели
- Пустое название блокирует кнопку создания

#### Поток 4: Проведение тренировки (Workout Session)

**Сценарий:** Пользователь запускает тренировку из своего плана.

| Шаг | Экран | Действие | Что происходит |
|---|---|---|---|
| 1 | HomeScreen / WorkoutDetailScreen | «Начать тренировку» | Модальное окно тренировки открывается поверх навигации. POST /api/v1/workout-sessions (start) |
| 2 | WorkoutSessionScreen | Просмотр текущего упражнения | ExerciseHero показывает название, картинку, мышечную группу. Таймер отсчитывает время |
| 3 | WorkoutSessionScreen | Отметка подхода → «Готово» | Пользователь вводит фактические reps/weight → POST /api/v1/workout-sessions/{id}/sets |
| 4 | RestTimerScreen | Ожидание отдыха | Автоматически запускается таймер отдыха (60 сек по умолчанию) |
| 5 | WorkoutSessionScreen | Переход к следующему упражнению | По свайпу или кнопке «Далее» |
| 6 | WorkoutSessionScreen | «Завершить тренировку» | POST /api/v1/workout-sessions/{id}/finish (status=COMPLETED) |
| 7 | WorkoutSummaryScreen | Просмотр результатов | Общее время, кол-во подходов/повторений, основная мышечная группа |

**Что проверять:**
- Таймер считает время корректно
- Таймер отдыха работает и издаёт уведомление по завершении
- Данные подхода (reps, weight) сохраняются на сервер
- Нельзя добавить подход к завершённой сессии
- При сворачивании приложения сессия не теряется
- Summary-экран показывает корректную статистику

#### Поток 5: Работа с AI-ассистентом (генерация тренировки)

| Шаг | Экран | Действие | Что происходит |
|---|---|---|---|
| 1 | MainView | Таб «AI» | Открывается экран чата |
| 2 | AiFlowScreen | Ввод текста «Составь тренировку на грудь и плечи для среднего уровня» | POST /api/v1/ai/chat → AI генерирует план тренировки |
| 3 | AiFlowScreen | Просмотр ответа AI | AI показывает структурированный план с упражнениями, подходами, повторениями |
| 4 | AiFlowScreen | «Сохранить тренировку» | POST /api/v1/ai/chat/save → план сохраняется в тренировки пользователя |
| 5 | WorkoutsScreen | Тренировка от AI видна в списке | createdBy = AI |

**Что проверять:**
- Индикатор загрузки при ожидании ответа AI
- Обработка ошибки при недоступности AI-сервиса
- Сохранённая тренировка корректно отображается в списке
- Контекст разговора сохраняется в рамках сессии
- Кнопка «Очистить сессию» работает

#### Поток 6: Отслеживание активностей

| Шаг | Экран | Действие | Что происходит |
|---|---|---|---|
| 1 | HomeScreen | Просмотр колец активности | Кольца показывают прогресс: движение (MOVE), ментальное (MIND), бытовое (LIFE) |
| 2 | HomeScreen | Тап на секцию «Активности» | Переход на экран ActivitiesScreen |
| 3 | ActivitiesScreen | Просмотр запланированных активностей | Показываются активности на сегодня с прогрессом |
| 4 | ActivitiesScreen | Тап «+» (добавить активность) | Переход на ActivityCatalogScreen |
| 5 | ActivityCatalogScreen | Выбор активности «Вода» | Переход к настройке цели и расписания |
| 6 | ActivityGoalSettingsScreen | Установка цели «8 стаканов» | Выбор из presets (1-8) или ввод вручную |
| 7 | ActivityScheduleSettingsScreen | Выбор расписания: «Каждый день» (RECURRING, все дни) | Настройка дней недели |
| 8 | ActivitiesScreen | Тап «+1 стакан» | POST /api/v1/activities/{id}/checkin → logValue увеличивается |
| 9 | HomeScreen | Кольцо обновилось | Прогресс кольца LIFE увеличился |

**Что проверять:**
- Кольца активности рассчитываются корректно (completionPct)
- Бинарные активности (зарядка) логируют value=1
- Счётчиковые активности корректно накапливают значение
- Расписание фильтрует активности по дню недели
- Повторный чекин обновляет, а не дублирует запись

#### Поток 7: Управление питанием

| Шаг | Экран | Действие | Что происходит |
|---|---|---|---|
| 1 | HomeScreen | Секция «План питания» → «Создать план» | Переход в CreateMealPlanFlowScreen |
| 2 | CreateMealPlanFlowScreen | Выбор типа приёма пищи (Завтрак/Обед/Ужин) | Фильтрация по MealType |
| 3 | CreateMealPlanFlowScreen | Добавление продуктов из каталога | Поиск по названию продукта, указание граммовки |
| 4 | CreateMealPlanFlowScreen | Просмотр макронутриентов | Автоматический расчёт: белки, жиры, углеводы, калории (формула: per100g × grams / 100) |
| 5 | CreateMealPlanFlowScreen | «Сохранить» | POST /api/v1/meal-plans/manual |
| 6 | MealPlanDetailScreen | Просмотр плана | Donut-диаграмма макросов, карточки продуктов с граммовкой |

**Что проверять:**
- Расчёт макросов корректен (не NaN, не отрицательные)
- Поиск продуктов работает по частичному совпадению
- Планы на одну и ту же дату/тип не конфликтуют
- Premium-функции заблокированы для FREE пользователей (MealPlanLockedOverlay)

### 2.3. Рекомендуемые инструменты для функционального тестирования

| Платформа | Инструмент | Описание |
|---|---|---|
| Android | Compose Testing (ui-test-junit4) | Тестирование Compose UI компонентов |
| Android | Espresso | E2E тестирование Android UI |
| iOS | XCUITest | Тестирование SwiftUI экранов |
| Backend API | REST Assured / MockMvc | Функциональные тесты API-эндпоинтов |

---

## 3. Тестирование производительности

### 3.1. Backend: нагрузочное тестирование API

#### Эндпоинты для нагрузочного тестирования

| # | Эндпоинт | Метод | Приоритет | Обоснование |
|---|---|---|---|---|
| 1 | `/api/v1/auth/login` | POST | Высокий | Критичный путь, вызывается при каждом открытии приложения |
| 2 | `/api/v1/auth/refresh` | POST | Высокий | Вызывается автоматически при истечении access-токена |
| 3 | `/api/v1/workout-plans` | GET | Высокий | Загружается на главном экране и табе тренировок |
| 4 | `/api/v1/workout-plans/active` | GET | Высокий | Загружается для показа текущих тренировок |
| 5 | `/api/v1/exercises` | GET | Средний | Каталог упражнений с фильтрацией |
| 6 | `/api/v1/exercises/search` | GET | Средний | Поиск — может генерировать множество запросов при вводе |
| 7 | `/api/v1/activities/today` | GET | Высокий | Загружается на главном экране каждый раз |
| 8 | `/api/v1/activities/{id}/checkin` | POST | Средний | Частые мутации в течение дня |
| 9 | `/api/v1/workout-sessions/submit` | POST | Средний | Сложный эндпоинт с каскадным сохранением |
| 10 | `/api/v1/ai/chat` | POST | Низкий | Зависит от внешнего AI API (GigaChat) |
| 11 | `/api/v1/meal-plans` | GET | Средний | Список планов питания с пагинацией |
| 12 | `/api/v1/products` | GET | Средний | Поиск продуктов питания |

#### Рекомендуемый план нагрузочного тестирования

**Инструмент:** Apache JMeter, Gatling или k6

**Профили нагрузки:**

| Профиль | Пользователи | Длительность | Цель |
|---|---|---|---|
| Baseline | 10 | 5 мин | Определение базовых метрик (среднее время ответа) |
| Нормальная нагрузка | 50 | 10 мин | Имитация типичного использования |
| Пиковая нагрузка | 200 | 10 мин | Проверка поведения при утреннем/вечернем пике |
| Стресс-тест | 500+ | 15 мин | Определение точки отказа |

**Метрики для сбора:**
- Response Time (p50, p95, p99)
- Throughput (requests/sec)
- Error Rate (%)
- CPU/Memory на сервере
- Database connection pool usage
- JVM Garbage Collection pauses

**Ожидаемые SLA:**

| Эндпоинт | p95 < | p99 < |
|---|---|---|
| GET запросы (списки) | 200ms | 500ms |
| POST auth (login/register) | 300ms | 700ms |
| POST mutations (submit session) | 500ms | 1000ms |
| AI chat | 5000ms | 10000ms (зависит от GigaChat) |

#### Пример сценария для k6

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '2m', target: 50 },   // ramp-up
    { duration: '5m', target: 50 },   // steady
    { duration: '2m', target: 200 },  // peak
    { duration: '5m', target: 200 },  // steady peak
    { duration: '2m', target: 0 },    // ramp-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  // 1. Login
  const loginRes = http.post('http://localhost:8080/api/v1/auth/login', 
    JSON.stringify({ email: 'test@example.com', password: 'password123' }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(loginRes, { 'login status 200': (r) => r.status === 200 });
  
  const token = JSON.parse(loginRes.body).accessToken;
  const authHeaders = { 
    headers: { 
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  };

  // 2. Get workouts
  const workoutsRes = http.get('http://localhost:8080/api/v1/workout-plans?page=0&size=10', authHeaders);
  check(workoutsRes, { 'workouts status 200': (r) => r.status === 200 });

  // 3. Get today activities
  const activitiesRes = http.get('http://localhost:8080/api/v1/activities/today', authHeaders);
  check(activitiesRes, { 'activities status 200': (r) => r.status === 200 });

  // 4. Get exercises
  const exercisesRes = http.get('http://localhost:8080/api/v1/exercises?page=0&size=20', authHeaders);
  check(exercisesRes, { 'exercises status 200': (r) => r.status === 200 });

  sleep(1);
}
```

### 3.2. Mobile: метрики производительности

#### Android

**Инструмент:** Android Profiler (Android Studio), Firebase Performance Monitoring

| Метрика | Как измерить | Ожидание |
|---|---|---|
| Время холодного старта | Android Profiler → Startup | < 2 секунды |
| Время тёплого старта | Android Profiler → Startup | < 1 секунда |
| Потребление RAM | Android Profiler → Memory | < 150 MB в покое |
| Утечки памяти | LeakCanary | 0 утечек |
| Jank frames | Android Profiler → Frame rendering | < 5% janky frames |
| Время отрисовки экрана | Compose @Preview metrics | < 16ms/frame (60fps) |
| Размер APK | `./gradlew assembleRelease` | < 50 MB |

**Что мониторить:**
- Переход между табами (анимация плавная, без рывков)
- Скролл длинных списков (тренировки, упражнения, продукты)
- Загрузка картинок упражнений
- AI-чат: отзывчивость UI во время ожидания ответа

#### iOS

**Инструмент:** Xcode Instruments (Time Profiler, Allocations, Leaks, Animation Hitches)

| Метрика | Инструмент Xcode | Ожидание |
|---|---|---|
| Время запуска | Time Profiler | < 2 секунды |
| Потребление RAM | Allocations | < 150 MB в покое |
| Утечки памяти | Leaks | 0 утечек |
| Hitch rate | Animation Hitches | < 5 ms/s |
| CPU usage в покое | Activity Monitor | < 3% |

---

## 4. Тестирование удобства использования (Usability)

### 4.1. Методология

**Метод:** Юзабилити-тестирование с реальными пользователями + экспертная эвристическая оценка по чек-листу Якоба Нильсена.

**Участники:** 3-5 человек целевой аудитории (люди, занимающиеся фитнесом, 18-40 лет, пользователи смартфонов).

### 4.2. Сценарии для тестирования с пользователями

#### Сценарий 1: Регистрация и первый запуск
**Задача:** «Зарегистрируйтесь в приложении и пройдите начальную настройку»
- **Засекаем:** время выполнения
- **Наблюдаем:** понял ли пользователь последовательность шагов, были ли затруднения на paywall-экране

#### Сценарий 2: Создание тренировки
**Задача:** «Создайте тренировку на грудь и трицепс из 3 упражнений, по 4 подхода каждое»
- **Засекаем:** время выполнения, количество ошибок
- **Наблюдаем:** нашёл ли кнопку создания, понял ли мастер создания (шаги), смог ли настроить вес для каждого подхода

#### Сценарий 3: Проведение тренировки
**Задача:** «Запустите созданную тренировку и отметьте выполнение 2 подходов»
- **Засекаем:** время от нажатия «Начать» до первого отмеченного подхода
- **Наблюдаем:** понял ли интерфейс сессии, таймер отдыха, кнопки управления

#### Сценарий 4: Отслеживание активности
**Задача:** «Добавьте активность "Вода" в свой план и отметьте 3 стакана»
- **Засекаем:** время выполнения
- **Наблюдаем:** нашёл ли раздел активностей, понял ли механику чекинов

#### Сценарий 5: Получение тренировки от AI
**Задача:** «Попросите AI-ассистента составить тренировку для ног и сохраните её»
- **Засекаем:** время до успешного сохранения
- **Наблюдаем:** понял ли как пользоваться чатом, нашёл ли кнопку сохранения

### 4.3. Шаблон таблицы результатов

| Сценарий | Участник | Успешность | Время (сек) | Ошибки | Комментарии |
|---|---|---|---|---|---|
| 1. Регистрация | П1 | Да/Нет | ... | ... | ... |
| 1. Регистрация | П2 | Да/Нет | ... | ... | ... |
| 2. Создание тренировки | П1 | Да/Нет | ... | ... | ... |
| ... | ... | ... | ... | ... | ... |

### 4.4. Эвристическая оценка по Нильсену

| # | Эвристика | Оценка (1-5) | Комментарий |
|---|---|---|---|
| 1 | **Видимость состояния системы** | ? | Есть ли индикаторы загрузки? Понятно ли пользователю, что данные сохраняются? Есть ли feedback после действий (чекин, сохранение тренировки)? |
| 2 | **Соответствие реальному миру** | ? | Используются ли понятные термины? «Подходы», «Повторения», «Макронутриенты» — понятны ли целевой аудитории? |
| 3 | **Свобода действий пользователя** | ? | Можно ли отменить действие? Есть ли кнопка «Назад»? Можно ли выйти из мастера создания тренировки? |
| 4 | **Единообразие и стандарты** | ? | Одинаково ли выглядят кнопки, карточки, поля ввода на всех экранах? |
| 5 | **Предотвращение ошибок** | ? | Заблокированы ли кнопки при невалидных данных? Есть ли подтверждение удаления? |
| 6 | **Распознавание вместо запоминания** | ? | Показываются ли подсказки, названия мышечных групп, описания упражнений? |
| 7 | **Гибкость и эффективность** | ? | Есть ли shortcuts для опытных пользователей? Быстрое создание тренировки? |
| 8 | **Эстетика и минимализм** | ? | Нет ли избыточной информации? Достаточно ли «воздуха» в интерфейсе? |
| 9 | **Помощь в распознавании и исправлении ошибок** | ? | Понятны ли сообщения об ошибках? «Invalid email or password» — достаточно ли информативно? |
| 10 | **Справка и документация** | ? | Есть ли onboarding-подсказки, туториалы, tooltips? |

### 4.5. Известные UX-риски

| Проблема | Экран | Описание |
|---|---|---|
| Сложность мастера создания тренировки | CreateWorkoutFlow | 4-5 шагов (название → мышцы → упражнения → настройка → обзор) — может быть перегружено для новичков |
| Paywall при онбординге | PaywallScreen | Показ платного контента до знакомства с приложением может оттолкнуть |
| Premium-overlay | MealPlanLockedOverlay | Заблокированные фичи видны, но недоступны — может вызывать фрустрацию |
| AI-зависимость | AiFlowScreen | Если GigaChat недоступен, функция полностью не работает |
| Обилие данных | HomeScreen | Дашборд содержит активности + тренировки + питание — информационная перегрузка |

---

## 5. Тестирование безопасности

### 5.1. Аутентификация и авторизация

#### Архитектура безопасности

| Компонент | Реализация |
|---|---|
| Аутентификация | JWT (JSON Web Token) через HMAC-SHA |
| Access Token | JWT, срок действия 15 минут |
| Refresh Token | UUID-строка, срок действия 30 дней, хранится в БД |
| Хеширование паролей | BCrypt (через Spring Security PasswordEncoder) |
| HTTP-фильтр | JwtAuthenticationFilter — проверка Bearer-токена |
| Защита эндпоинтов | Spring Security: публичные (/auth/register, /auth/login, /auth/refresh, /auth/logout) и защищённые (все остальные) |
| Сессии | Stateless (CSRF отключён, SessionCreationPolicy.STATELESS) |

#### Что проверять

| # | Проверка | Метод | Ожидание |
|---|---|---|---|
| 1 | Доступ без токена | GET /api/v1/workout-plans без Authorization header | 401 Unauthorized |
| 2 | Невалидный токен | Bearer invalid-token | 401 Unauthorized |
| 3 | Истёкший access token | JWT с прошедшим exp | 401 Unauthorized |
| 4 | Чужие данные | Запрос к /api/v1/workout-plans/{чужой_id} | 404 Not Found (не 403 — не раскрываем существование ресурса) |
| 5 | Refresh token reuse | Повторное использование refresh-токена после refresh | 401 (токен удалён) |
| 6 | Logout | После logout refresh-токен не работает | 401 |
| 7 | Logout All | После logoutAll все refresh-токены невалидны | 401 |
| 8 | Password brute-force | Множественные попытки логина | Нет rate limiting (потенциальная уязвимость — см. рекомендации) |
| 9 | SQL Injection в email | `' OR 1=1 --` в email при логине | Ошибка валидации, не SQL-ошибка |
| 10 | XSS в полях профиля | `<script>alert(1)</script>` в firstName | Значение сохраняется как есть, но не рендерится на бэке (проверить на мобилке) |

### 5.2. Безопасность API

#### Защита от OWASP Top 10

| # | Уязвимость (OWASP) | Статус | Комментарий |
|---|---|---|---|
| A01 | Broken Access Control | Частично защищено | Все эндпоинты привязаны к userId из JWT. Проверяется принадлежность ресурса пользователю (findByIdAndUserId). Но нет role-based access control |
| A02 | Cryptographic Failures | Защищено | BCrypt для паролей, HMAC-SHA для JWT |
| A03 | Injection | Защищено | Spring Data JPA с параметризованными запросами. Кастомные @Query используют параметры (:userId) |
| A04 | Insecure Design | Частично | Нет rate limiting, нет account lockout после N попыток |
| A05 | Security Misconfiguration | Проверить | CORS-настройки, /error endpoint доступен публично |
| A06 | Vulnerable Components | Проверить | Запустить `./gradlew dependencyCheckAnalyze` (OWASP Dependency Check) |
| A07 | Authentication Failures | Частично | JWT-secret хранится в application.yml (проверить — не захардкожен ли) |
| A08 | Software and Data Integrity | OK | Зависимости из Maven Central |
| A09 | Logging Failures | Частично | Логируется: login/register attempts. Не логируется: подозрительная активность |
| A10 | SSRF | Защищено | Нет эндпоинтов, принимающих URL для обращения на стороне сервера |

#### Рекомендуемый инструмент: OWASP ZAP

**Как запустить:**
1. Запустить бэкенд локально (`./gradlew bootRun`)
2. Запустить OWASP ZAP → Active Scan → Target: `http://localhost:8080`
3. Настроить аутентификацию в ZAP (Bearer-токен)
4. Запустить Spider + Active Scan
5. Проанализировать findings

### 5.3. Безопасность мобильного приложения

#### Android

| # | Проверка | Как проверить | Ожидание |
|---|---|---|---|
| 1 | Хранение токенов | Проверить, используется ли Android Keystore / EncryptedSharedPreferences | Токены зашифрованы, недоступны через adb |
| 2 | Network security | Проверить AndroidManifest.xml → networkSecurityConfig | Только HTTPS, certificate pinning |
| 3 | Экспортируемые компоненты | Проверить android:exported для Activity/Service | Только MainActivity exported=true |
| 4 | ProGuard/R8 обфускация | Проверить `minifyEnabled true` в release | Код обфусцирован в release-сборке |
| 5 | Root detection | Проверить, работает ли приложение на rooted устройстве | Предупреждение или блокировка (опционально) |
| 6 | Screenshot protection | Проверить FLAG_SECURE на экранах с чувствительными данными | Экраны auth/profile не скриншотятся |

#### iOS

| # | Проверка | Как проверить | Ожидание |
|---|---|---|---|
| 1 | Хранение токенов | Проверить использование Keychain Services | Токены хранятся в Keychain, не в UserDefaults |
| 2 | App Transport Security | Проверить Info.plist → NSAppTransportSecurity | Только HTTPS |
| 3 | Certificate Pinning | Проверить конфигурацию URLSession | SSL Pinning активен |
| 4 | Jailbreak detection | Проверить наличие jailbreak-проверки | Предупреждение (опционально) |
| 5 | Biometric auth | Проверить интеграцию с Face ID / Touch ID | Доступно для быстрого входа (если реализовано) |

### 5.4. Безопасность данных

| Данные | Где хранятся | Риск | Рекомендации |
|---|---|---|---|
| Пароли | PostgreSQL (bcrypt hash) | Низкий | BCrypt с cost factor ≥ 10 |
| JWT-secret | application.yml / env variables | Средний | Использовать переменные окружения, не коммитить в git |
| Refresh-токены | PostgreSQL | Средний | Есть expiry, есть deleteExpired |
| Профиль пользователя | PostgreSQL | Низкий | Доступен только владельцу через JWT |
| AI-конversации | In-memory (ConversationSessionStore) | Низкий | Очищаются при удалении сессии |
| Локальные данные (мобилка) | SQLDelight / SharedPreferences | Средний | Проверить шифрование |

### 5.5. Рекомендации по улучшению безопасности

| # | Рекомендация | Приоритет | Описание |
|---|---|---|---|
| 1 | Rate Limiting | Высокий | Добавить rate limit на /auth/login (например, 5 попыток/минуту) через Spring Cloud Gateway или interceptor |
| 2 | Account Lockout | Средний | Блокировка аккаунта после 10 неудачных попыток входа |
| 3 | Input Validation | Средний | Дополнительная санитизация HTML-тегов в текстовых полях (firstName, notes и т.д.) |
| 4 | CORS Configuration | Средний | Проверить и ограничить разрешённые origins |
| 5 | Dependency Audit | Средний | Интегрировать OWASP Dependency Check в CI/CD |
| 6 | Audit Logging | Низкий | Логировать все мутации с userId, IP, timestamp |
| 7 | Token Rotation | Низкий | Реализовать Refresh Token Rotation (каждый refresh выдаёт новый refresh-токен — уже реализовано) |

---

## Заключение

| Направление | Статус | Комментарий |
|---|---|---|
| Unit-тестирование | Выполнено (117 тестов: 38 backend + 79 mobile) | Покрыты backend-сервисы и shared-модуль мобилки |
| Функциональное тестирование | Описаны 7 сценариев | Готовы для выполнения с Compose Testing / XCUITest |
| Тестирование производительности | План подготовлен | Готов сценарий для k6, описаны метрики |
| Usability-тестирование | План подготовлен | 5 сценариев + чек-лист Нильсена |
| Тестирование безопасности | Анализ выполнен | OWASP Top 10, мобильная безопасность, рекомендации |
