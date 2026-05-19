package com.asc.gymgenie.nutrition

import com.asc.gymgenie.auth.NetworkException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random

/**
 * Steps inside the manual meal-plan creation flow.
 *
 * Modeled as an enum (with explicit `index` like [AiMealFlowStep]) so the
 * presenter exposes a single source of truth for navigation and the iOS view
 * layer can drive directional step transitions from the indexed value.
 *
 *  - SETUP   meal-type + schedule picker
 *  - EDIT    name/description + product list
 *  - PICKER  catalog search
 *  - INFO    product detail (push from picker)
 */
enum class CreateMealPlanStep(val index: Int) {
    SETUP(0),
    EDIT(1),
    PICKER(2),
    INFO(3),
}

/**
 * UI state for the manual meal-plan creation flow.
 *
 * Field grouping reflects which screen owns the data:
 *  - [step], [errorMessage] — flow-wide
 *  - [mealKind], [scheduleMode], [selectedDate], [selectedWeekdays],
 *    [bookedRecurringDays], [bookedOneOffDates] — setup
 *  - [planName], [planDescription], [addedItems] — editor
 *  - [products], [searchQuery], [selectedCategory], [isLoadingProducts],
 *    [productsError] — picker
 *  - [gramsFor], [infoFor] — modal slots layered on top of the picker /
 *    editor
 *  - [isSaving], [isSaved], [savedPlan] — terminal save phase
 *
 * The `selectedCategory == null` case represents "Все" (all categories) — the
 * picker UI maps a `null` category onto the rainbow-emoji "Все" chip.
 */
data class CreateMealPlanUiState(
    val step: CreateMealPlanStep = CreateMealPlanStep.SETUP,

    val isInitializing: Boolean = true,
    val editingPlanId: String? = null,
    val mealKind: ManualMealKind? = null,
    val scheduleMode: ManualScheduleMode = ManualScheduleMode.ONE_OFF,
    val selectedDate: String? = null,
    val selectedWeekdays: List<String> = emptyList(),
    val bookedRecurringDays: List<String> = emptyList(),
    val bookedOneOffDates: List<String> = emptyList(),
    /** Booked-days cache keyed by [ManualMealKind.wireValue], loaded once at init. */
    val allBookedDays: Map<String, BookedDaysResponse> = emptyMap(),

    // Editor ---------------------------------------------------------
    val planName: String = "",
    val planDescription: String = "",
    val addedItems: List<AddedMealItem> = emptyList(),
    val planNameTouched: Boolean = false,

    // Picker ---------------------------------------------------------
    val products: List<FoodProduct> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: FoodCategory? = null,
    val isLoadingProducts: Boolean = false,
    val productsError: String? = null,

    // Modal slots ----------------------------------------------------
    val gramsFor: FoodProduct? = null,
    val infoFor: FoodProduct? = null,
    val editingItem: AddedMealItem? = null,

    // Save -----------------------------------------------------------
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val savedPlan: MealPlanDetail? = null,

    val errorMessage: String? = null,
) {
    /**
     * Filtered catalog rendered in the picker. Recomputed from [products],
     * [searchQuery], and [selectedCategory] on every state read; kept as a
     * derived property (instead of a stored field) so we never observe a
     * transient state where the filter is out of sync with its inputs.
     */
    val filteredProducts: List<FoodProduct>
        get() {
            val trimmed = searchQuery.trim()
            return products.asSequence()
                .filter { selectedCategory == null || it.category == selectedCategory }
                .filter {
                    if (trimmed.isEmpty()) true
                    else it.nameRu.contains(trimmed, ignoreCase = true) ||
                        (it.nameEn?.contains(trimmed, ignoreCase = true) == true)
                }
                .toList()
        }

    /** Live total kcal across all added items (rounded to whole kcal). */
    val totalCalories: Int
        get() = addedItems.sumOf { it.portion.calories }.toInt()

    val totalProteinG: Double get() = addedItems.sumOf { it.portion.proteinG }
    val totalFatG: Double get() = addedItems.sumOf { it.portion.fatG }
    val totalCarbsG: Double get() = addedItems.sumOf { it.portion.carbsG }

    /**
     * Setup CTA gating. Requires a meal kind + at least one date / weekday
     * picked, depending on schedule mode.
     */
    val canContinueFromSetup: Boolean
        get() = mealKind != null && when (scheduleMode) {
            ManualScheduleMode.ONE_OFF -> selectedDate != null
            ManualScheduleMode.RECURRING -> selectedWeekdays.isNotEmpty()
        }

    /** Editor save CTA gating. */
    val canSave: Boolean
        get() = mealKind != null &&
            addedItems.isNotEmpty() &&
            planName.isNotBlank() &&
            !isSaving
}

/**
 * Presenter for the manual meal-plan creation flow.
 *
 * Owns:
 *  - step navigation between the 4 internal pages (+ 2 modal layers)
 *  - the booked-days lookup + setup-form inputs
 *  - the product catalog load + filter inputs
 *  - the editor's accumulated [AddedMealItem]s
 *  - the final save call to [ManualMealPlanApi.createManualMealPlan]
 *
 * The `addedItems` list is local-only state — the backend assigns durable ids
 * only after `save()`. Each row carries a client-side `uid` (random Long)
 * used as a stable LazyList / ForEach key, independent of position.
 *
 * Lifetime: callers must invoke [onCleared] when the surface is disposed so
 * in-flight coroutines are cancelled. Android does this from a
 * `DisposableEffect`; iOS does it from the wrapper's `deinit`.
 */
class CreateMealPlanViewModel(
    private val foodProductApi: FoodProductApi,
    private val manualMealPlanApi: ManualMealPlanApi,
    private val mealPlansApi: MealPlansApi,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(CreateMealPlanUiState())
    val state: StateFlow<CreateMealPlanUiState> = _state.asStateFlow()

    init {
        loadAllBookedDays()
    }

    /**
     * Currently-running debounced catalog reload, kept around so a fresh
     * keystroke / category tap can cancel the in-flight wait before issuing
     * a newer request. The first load (triggered by [openPicker]) bypasses
     * this and runs immediately.
     */
    private var searchJob: Job? = null

    // -----------------------------------------------------------------------
    // Setup screen
    // -----------------------------------------------------------------------

    fun setMealKind(kind: ManualMealKind) {
        val current = _state.value
        if (current.mealKind == kind) return
        val booked = current.allBookedDays[kind.wireValue]
        _state.update {
            it.copy(
                mealKind = kind,
                bookedRecurringDays = booked?.recurringDays ?: emptyList(),
                bookedOneOffDates = booked?.oneOffDates ?: emptyList(),
                selectedDate = null,
                selectedWeekdays = emptyList(),
                planName = if (it.planNameTouched) it.planName
                else generatedPlanName(kind, it.scheduleMode, null, emptyList()),
                errorMessage = null,
            )
        }
    }

    private fun loadAllBookedDays() {
        scope.launch {
            val kinds = ManualMealKind.entries.toList()
            val results = kinds.map { kind ->
                async { kind.wireValue to manualMealPlanApi.getBookedDays(kind.wireValue) }
            }.awaitAll()
            val map = results.mapNotNull { (wireValue, result) ->
                result.getOrNull()?.let { wireValue to it }
            }.toMap()
            _state.update { it.copy(isInitializing = false, allBookedDays = map) }
        }
    }

    fun setScheduleMode(mode: ManualScheduleMode) {
        if (_state.value.scheduleMode == mode) return
        _state.update {
            it.copy(
                scheduleMode = mode,
                // Switching mode resets the conflicting selection so the
                // single-source `canContinueFromSetup` rule stays clean.
                selectedDate = null,
                selectedWeekdays = emptyList(),
                planName = if (it.planNameTouched) it.planName
                else generatedPlanName(it.mealKind, mode, null, emptyList()),
            )
        }
    }

    /**
     * Selects a one-off date. No-ops if the date is in the booked list — the
     * UI is responsible for showing the lock icon, the VM enforces the
     * invariant so a stale tap on a row that turned booked between renders
     * cannot push an invalid selection into state.
     */
    fun selectDate(dateKey: String) {
        val current = _state.value
        if (current.scheduleMode != ManualScheduleMode.ONE_OFF) return
        if (current.bookedOneOffDates.contains(dateKey)) return
        _state.update {
            it.copy(
                selectedDate = dateKey,
                planName = if (it.planNameTouched) it.planName
                else generatedPlanName(it.mealKind, it.scheduleMode, dateKey, it.selectedWeekdays),
            )
        }
    }

    /**
     * Toggles a weekday in the recurring selection. Same anti-stale-tap
     * guard as [selectDate].
     */
    fun toggleWeekday(day: String) {
        val current = _state.value
        if (current.scheduleMode != ManualScheduleMode.RECURRING) return
        if (current.bookedRecurringDays.contains(day)) return
        val next = if (current.selectedWeekdays.contains(day)) {
            current.selectedWeekdays - day
        } else {
            current.selectedWeekdays + day
        }
        _state.update {
            it.copy(
                selectedWeekdays = next,
                planName = if (it.planNameTouched) it.planName
                else generatedPlanName(it.mealKind, it.scheduleMode, it.selectedDate, next),
            )
        }
    }

    // -----------------------------------------------------------------------
    // Editor screen
    // -----------------------------------------------------------------------

    fun setPlanName(name: String) {
        _state.update {
            it.copy(planName = name, planNameTouched = true)
        }
    }

    fun setPlanDescription(desc: String) {
        _state.update { it.copy(planDescription = desc) }
    }

    fun removeItem(uid: Long) {
        _state.update { it.copy(addedItems = it.addedItems.filterNot { row -> row.uid == uid }) }
    }

    fun openEditGrams(item: AddedMealItem) {
        _state.update { it.copy(editingItem = item) }
    }

    fun closeEditGrams() {
        _state.update { it.copy(editingItem = null) }
    }

    fun updateItemGrams(uid: Long, newGrams: Double) {
        if (newGrams <= 0.0) return
        _state.update { st ->
            st.copy(
                addedItems = st.addedItems.map { if (it.uid == uid) it.copy(grams = newGrams) else it },
                editingItem = null,
            )
        }
    }

    // -----------------------------------------------------------------------
    // Picker screen
    // -----------------------------------------------------------------------

    fun openPicker() {
        _state.update { it.copy(step = CreateMealPlanStep.PICKER) }
        // First open of the picker: pull immediately (no debounce) using the
        // current search/category — usually empty/null on first entry.
        if (_state.value.products.isEmpty() || _state.value.productsError != null) {
            reloadProducts(immediate = true)
        }
    }

    fun setSearchQuery(q: String) {
        if (_state.value.searchQuery == q) return
        _state.update { it.copy(searchQuery = q) }
        // Debounced reload — backend may filter more accurately than the
        // client-side `filteredProducts` view (e.g. nameEn synonyms not
        // present locally). The 300ms wait avoids spamming the endpoint on
        // every keystroke while still feeling snappy.
        reloadProducts(immediate = false)
    }

    fun setCategory(category: FoodCategory?) {
        if (_state.value.selectedCategory == category) return
        _state.update { it.copy(selectedCategory = category) }
        // Category taps are discrete (no rapid changes) but reuse the same
        // pipeline so the loading indicator behaves consistently.
        reloadProducts(immediate = true)
    }

    /**
     * Backwards-compatible alias for the old caller name. Re-runs the catalog
     * fetch using the current search/category state immediately.
     */
    fun loadProducts() {
        reloadProducts(immediate = true)
    }

    /**
     * Cancels any in-flight debounced reload and queues a new one.
     *
     * @param immediate when true the request fires without waiting (used by
     *   the initial open and by category taps); when false the request waits
     *   [SEARCH_DEBOUNCE_MS] ms so consecutive keystrokes coalesce into a
     *   single backend call.
     */
    private fun reloadProducts(immediate: Boolean) {
        searchJob?.cancel()
        searchJob = scope.launch {
            if (!immediate) delay(SEARCH_DEBOUNCE_MS)
            val snapshot = _state.value
            _state.update { it.copy(isLoadingProducts = true, productsError = null) }
            foodProductApi.searchProducts(
                query = snapshot.searchQuery.trim().takeIf { it.isNotEmpty() },
                category = snapshot.selectedCategory,
            ).fold(
                onSuccess = { products ->
                    _state.update {
                        it.copy(
                            isLoadingProducts = false,
                            products = products,
                            productsError = null,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoadingProducts = false,
                            productsError = "Не удалось загрузить продукты: ${error.message}",
                        )
                    }
                },
            )
        }
    }

    // -----------------------------------------------------------------------
    // Modal layers
    // -----------------------------------------------------------------------

    fun openGramsSheet(product: FoodProduct) {
        _state.update { it.copy(gramsFor = product) }
    }

    fun closeGramsSheet() {
        _state.update { it.copy(gramsFor = null) }
    }

    fun openInfo(product: FoodProduct) {
        _state.update { it.copy(infoFor = product, step = CreateMealPlanStep.INFO) }
    }

    fun closeInfo() {
        // Returning from info pops back to the picker — same convention as
        // pushing/popping a destination on a stack.
        _state.update {
            it.copy(infoFor = null, step = CreateMealPlanStep.PICKER)
        }
    }

    /**
     * Adds a portion of [product] to the editor list and pops the picker
     * stack back to the editor. Idempotent at the row level — every call
     * appends a fresh `AddedMealItem` with a new `uid`, so the user can add
     * the same product twice (e.g. a side dish + a topping of the same
     * ingredient).
     */
    fun addItem(product: FoodProduct, grams: Double) {
        if (grams <= 0.0) return
        val newItem = AddedMealItem(
            uid = nextUid(),
            product = product,
            grams = grams,
        )
        _state.update {
            it.copy(
                addedItems = it.addedItems + newItem,
                gramsFor = null,
                infoFor = null,
                step = CreateMealPlanStep.EDIT,
            )
        }
    }

    // -----------------------------------------------------------------------
    // Step navigation
    // -----------------------------------------------------------------------

    fun initWithMealTypeAndDate(mealType: String, date: String) {
        val kind = ManualMealKind.fromWireValue(mealType) ?: return
        setMealKind(kind)
        setScheduleMode(ManualScheduleMode.ONE_OFF)
        selectDate(date)
        goToEdit()
    }

    fun loadForEditing(planId: String) {
        _state.update { it.copy(isInitializing = true) }
        scope.launch {
            mealPlansApi.getMealPlanById(planId).fold(
                onSuccess = { plan ->
                    val meal = plan.meals.firstOrNull()
                    val mealKind = ManualMealKind.fromWireValue(meal?.mealType)

                    val items = meal?.dishes?.mapNotNull { dish ->
                        val grams = dish.grams
                            ?: parseGramsFromDescription(dish.portionDescription)
                            ?: return@mapNotNull null
                        if (grams <= 0.0) return@mapNotNull null

                        val hasCatalog = dish.foodProductId != null
                        val productId = dish.foodProductId ?: dish.id

                        val product = FoodProduct(
                            id = productId,
                            nameRu = dish.name,
                            nameEn = null,
                            category = FoodCategory.OTHER,
                            emoji = null,
                            caloriesPer100g = (dish.calories?.toDouble() ?: 0.0) * 100.0 / grams,
                            proteinPer100g = (dish.proteinG?.toDouble() ?: 0.0) * 100.0 / grams,
                            fatPer100g = (dish.fatG?.toDouble() ?: 0.0) * 100.0 / grams,
                            carbsPer100g = (dish.carbsG?.toDouble() ?: 0.0) * 100.0 / grams,
                            fiberPer100g = null,
                            sugarPer100g = null,
                        )

                        AddedMealItem(
                            uid = nextUid(),
                            product = product,
                            grams = grams,
                            hasCatalogProduct = hasCatalog,
                        )
                    } ?: emptyList()

                    val scheduleMode = when (plan.scheduleType) {
                        "RECURRING" -> ManualScheduleMode.RECURRING
                        else -> ManualScheduleMode.ONE_OFF
                    }

                    _state.update {
                        it.copy(
                            isInitializing = false,
                            editingPlanId = planId,
                            mealKind = mealKind,
                            planName = plan.name,
                            planNameTouched = true,
                            addedItems = items,
                            scheduleMode = scheduleMode,
                            selectedDate = plan.oneOffDate,
                            selectedWeekdays = plan.scheduleDays,
                            step = CreateMealPlanStep.EDIT,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isInitializing = false,
                            errorMessage = when (error) {
                                is NetworkException -> error.message ?: "Ошибка сети"
                                else -> "Не удалось загрузить план"
                            },
                        )
                    }
                },
            )
        }
    }

    private fun parseGramsFromDescription(desc: String?): Double? {
        if (desc == null) return null
        val match = Regex("(\\d+)").find(desc) ?: return null
        return match.groupValues[1].toDoubleOrNull()
    }

    fun goToEdit() {
        val current = _state.value
        if (!current.canContinueFromSetup) return
        val name = if (current.planName.isNotBlank()) current.planName
        else generatedPlanName(
            kind = current.mealKind,
            mode = current.scheduleMode,
            selectedDate = current.selectedDate,
            selectedWeekdays = current.selectedWeekdays,
        )
        _state.update {
            it.copy(
                step = CreateMealPlanStep.EDIT,
                planName = name,
                errorMessage = null,
            )
        }
    }

    /**
     * Single back-button entry point.
     *
     *  - INFO   → PICKER
     *  - PICKER → EDIT
     *  - EDIT   → SETUP
     *  - SETUP  → no-op (caller handles dismiss)
     */
    fun goBack() {
        val current = _state.value
        when (current.step) {
            CreateMealPlanStep.INFO -> _state.update {
                it.copy(step = CreateMealPlanStep.PICKER, infoFor = null)
            }
            CreateMealPlanStep.PICKER -> _state.update {
                it.copy(
                    step = CreateMealPlanStep.EDIT,
                    searchQuery = "",
                    selectedCategory = null,
                    gramsFor = null,
                    infoFor = null,
                )
            }
            CreateMealPlanStep.EDIT -> _state.update {
                it.copy(step = CreateMealPlanStep.SETUP)
            }
            CreateMealPlanStep.SETUP -> Unit
        }
    }

    // -----------------------------------------------------------------------
    // Save
    // -----------------------------------------------------------------------

    /**
     * Posts the manual plan to the backend.
     *
     * Pre-conditions are gated by [CreateMealPlanUiState.canSave] (meal kind +
     * non-empty items + non-blank name + not already saving). The request is
     * built from the current state at call time — no defensive copy needed
     * because [_state] is the only writer.
     */
    fun save() {
        val current = _state.value
        if (!current.canSave) return
        val kind = current.mealKind ?: return
        _state.update { it.copy(isSaving = true, errorMessage = null) }

        val request = CreateManualMealPlanRequest(
            name = current.planName.trim(),
            description = current.planDescription.trim().takeIf { it.isNotBlank() },
            mealType = kind.wireValue,
            goal = null,
            scheduleType = current.scheduleMode.wireValue,
            scheduleDays = if (current.scheduleMode == ManualScheduleMode.RECURRING)
                current.selectedWeekdays else emptyList(),
            oneOffDate = if (current.scheduleMode == ManualScheduleMode.ONE_OFF)
                current.selectedDate else null,
            items = current.addedItems.map {
                    if (it.hasCatalogProduct) {
                        ManualMealItemRequest(
                            foodProductId = it.product.id,
                            grams = it.grams,
                        )
                    } else {
                        val macros = it.portion
                        ManualMealItemRequest(
                            grams = it.grams,
                            name = it.product.nameRu,
                            calories = macros.calories.toInt(),
                            proteinG = macros.proteinG.toInt(),
                            carbsG = macros.carbsG.toInt(),
                            fatG = macros.fatG.toInt(),
                        )
                    }
                },
        )

        scope.launch {
            manualMealPlanApi.createManualMealPlan(request).fold(
                onSuccess = { plan ->
                    val editId = _state.value.editingPlanId
                    if (editId != null) {
                        mealPlansApi.deleteMealPlan(editId)
                    }
                    _state.update {
                        it.copy(
                            isSaving = false,
                            isSaved = true,
                            savedPlan = plan,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = when (error) {
                                is NetworkException -> error.message ?: "Ошибка сети"
                                else -> error.message ?: "Не удалось сохранить рацион"
                            },
                        )
                    }
                },
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun onCleared() {
        scope.cancel()
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Builds a default plan name like "Завтрак 17 мая" or
     * "Обед Пн, Ср". Used only while the user has not edited the field.
     */
    private fun generatedPlanName(
        kind: ManualMealKind?,
        mode: ManualScheduleMode,
        selectedDate: String?,
        selectedWeekdays: List<String>,
    ): String {
        if (kind == null) return ""
        val mealLabel = kind.displayName
        val schedulePart: String? = when (mode) {
            ManualScheduleMode.ONE_OFF -> selectedDate?.let(::dateLabel)
            ManualScheduleMode.RECURRING -> selectedWeekdays
                .takeIf { it.isNotEmpty() }
                ?.joinToString(", ") { weekdayShort(it) }
        }
        return if (schedulePart == null) mealLabel else "$mealLabel $schedulePart"
    }

    private fun dateLabel(iso: String): String {
        val parsed = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return iso
        return "${parsed.dayOfMonth} ${monthShort(parsed.monthNumber)}"
    }

    private fun nextUid(): Long = Random.nextLong()

    companion object {
        /**
         * Number of dates rendered in the one-off date strip on the setup
         * screen. Two weeks ahead of today is generous enough that the user
         * is unlikely to scroll out of range yet keeps the strip lightweight.
         */
        const val DATE_STRIP_LENGTH = 14

        /**
         * Debounce window for keystroke-driven catalog reloads. 300ms is the
         * standard "feels responsive but coalesces typing" value used across
         * the app and matches the spec for this flow.
         */
        const val SEARCH_DEBOUNCE_MS = 300L

        /**
         * Generates the upcoming [DATE_STRIP_LENGTH]-day window starting from
         * "today" in the device's current timezone. Kept here (not in the
         * view layer) so iOS and Android render identical strips.
         */
        fun upcomingDates(start: LocalDate = today()): List<LocalDate> =
            (0 until DATE_STRIP_LENGTH).map { offset ->
                start.plus(DatePeriod(days = offset))
            }

        fun today(): LocalDate = Clock.System
            .now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date

        /**
         * Localized short weekday label for a wire `DayOfWeek` name.
         * Falls back to the raw input if the value is unknown — keeps the UI
         * rendering even after a backend addition.
         */
        fun weekdayShort(wireValue: String): String = when (wireValue) {
            "MONDAY" -> "Пн"
            "TUESDAY" -> "Вт"
            "WEDNESDAY" -> "Ср"
            "THURSDAY" -> "Чт"
            "FRIDAY" -> "Пт"
            "SATURDAY" -> "Сб"
            "SUNDAY" -> "Вс"
            else -> wireValue
        }

        fun weekdayLong(wireValue: String): String = when (wireValue) {
            "MONDAY" -> "Понедельник"
            "TUESDAY" -> "Вторник"
            "WEDNESDAY" -> "Среда"
            "THURSDAY" -> "Четверг"
            "FRIDAY" -> "Пятница"
            "SATURDAY" -> "Суббота"
            "SUNDAY" -> "Воскресенье"
            else -> wireValue
        }

        /**
         * Wire `DayOfWeek` name for a [kotlinx.datetime.LocalDate].
         */
        fun dayOfWeekWire(date: LocalDate): String = when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "MONDAY"
            DayOfWeek.TUESDAY -> "TUESDAY"
            DayOfWeek.WEDNESDAY -> "WEDNESDAY"
            DayOfWeek.THURSDAY -> "THURSDAY"
            DayOfWeek.FRIDAY -> "FRIDAY"
            DayOfWeek.SATURDAY -> "SATURDAY"
            DayOfWeek.SUNDAY -> "SUNDAY"
        }

        fun monthShort(monthNumber: Int): String = when (monthNumber) {
            1 -> "янв"
            2 -> "фев"
            3 -> "мар"
            4 -> "апр"
            5 -> "мая"
            6 -> "июн"
            7 -> "июл"
            8 -> "авг"
            9 -> "сен"
            10 -> "окт"
            11 -> "ноя"
            12 -> "дек"
            else -> ""
        }
    }
}
