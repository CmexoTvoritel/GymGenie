package com.asc.gymgenie.feature.nutrition

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.nutrition.AddedMealItem
import com.asc.gymgenie.nutrition.CreateMealPlanStep
import com.asc.gymgenie.nutrition.CreateMealPlanViewModel
import com.asc.gymgenie.nutrition.FoodCategory
import com.asc.gymgenie.nutrition.FoodPortionMacros
import com.asc.gymgenie.nutrition.FoodProduct
import com.asc.gymgenie.nutrition.ManualMealKind
import com.asc.gymgenie.nutrition.ManualScheduleMode
import com.asc.gymgenie.nutrition.macrosForGrams
import com.asc.gymgenie.ui.components.GymGenieToolbar
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.SoftCard
import com.asc.gymgenie.ui.theme.WarmOffWhite
import org.koin.core.context.GlobalContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val CardBorder = Color(0xFFEDEDEF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMealPlanFlowScreen(
    initialMealType: String? = null,
    initialDate: String? = null,
    editPlanId: String? = null,
    onDismiss: () -> Unit,
    onDiscardToHome: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val koin = remember { GlobalContext.get() }
    val viewModel = remember { koin.get<CreateMealPlanViewModel>() }
    DisposableEffect(Unit) { onDispose { viewModel.onCleared() } }

    val state by viewModel.state.collectAsState()

    var didApplyInitial by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (editPlanId != null) {
            didApplyInitial = true
            viewModel.loadForEditing(editPlanId)
            return@LaunchedEffect
        }
    }

    LaunchedEffect(state.isInitializing) {
        if (didApplyInitial) return@LaunchedEffect
        if (initialMealType != null && initialDate != null) {
            if (!state.isInitializing) {
                didApplyInitial = true
                viewModel.initWithMealTypeAndDate(initialMealType, initialDate)
            }
        } else {
            didApplyInitial = true
            val kind = ManualMealKind.fromWireValue(initialMealType)
            if (kind != null) viewModel.setMealKind(kind)
        }
    }
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedPlan?.id) {
        if (state.savedPlan != null) onSaved()
    }

    val hasPrefilledInit = editPlanId != null || (initialMealType != null && initialDate != null)

    BackHandler {
        when (state.step) {
            CreateMealPlanStep.SETUP -> onDismiss()
            CreateMealPlanStep.EDIT -> showDiscardDialog = true
            else -> viewModel.goBack()
        }
    }

    if (state.isInitializing || (hasPrefilledInit && state.step == CreateMealPlanStep.SETUP)) {
        Box(
            modifier = modifier.fillMaxSize().background(WarmOffWhite),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.CircularProgressIndicator(color = Coral)
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    val forward = targetState.index >= initialState.index
                    val direction = if (forward) 1 else -1
                    (slideInHorizontally(
                        animationSpec = tween(220),
                        initialOffsetX = { it * direction },
                    ) + fadeIn(tween(180))) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(220),
                            targetOffsetX = { -it * direction },
                        ) + fadeOut(tween(180)))
                },
                modifier = Modifier.fillMaxSize(),
                label = "CreateMealPlanStep",
            ) { step ->
                when (step) {
                    CreateMealPlanStep.SETUP -> SetupStep(
                        state = state,
                        onClose = onDismiss,
                        onMealKind = viewModel::setMealKind,
                        onScheduleMode = viewModel::setScheduleMode,
                        onSelectDate = viewModel::selectDate,
                        onToggleWeekday = viewModel::toggleWeekday,
                        onNext = viewModel::goToEdit,
                    )
                    CreateMealPlanStep.EDIT -> EditStep(
                        state = state,
                        onBack = viewModel::goBack,
                        onRequestDiscard = { showDiscardDialog = true },
                        onPlanName = viewModel::setPlanName,
                        onAddProduct = viewModel::openPicker,
                        onRemove = viewModel::removeItem,
                        onEditGrams = viewModel::openEditGrams,
                        onSave = viewModel::save,
                    )
                    CreateMealPlanStep.PICKER -> PickerStep(
                        state = state,
                        onBack = viewModel::goBack,
                        onSearch = viewModel::setSearchQuery,
                        onCategory = viewModel::setCategory,
                        onSelect = viewModel::openInfo,
                    )
                    CreateMealPlanStep.INFO -> InfoStep(
                        state = state,
                        onBack = viewModel::goBack,
                        onChooseGrams = viewModel::openGramsSheet,
                    )
                }
            }
        }

        if (state.gramsFor != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = viewModel::closeGramsSheet,
                sheetState = sheetState,
                containerColor = WarmOffWhite,
            ) {
                GramsSheet(
                    product = state.gramsFor!!,
                    onAdd = { grams ->
                        viewModel.addItem(state.gramsFor!!, grams)
                    },
                )
            }
        }

        if (state.editingItem != null) {
            val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = viewModel::closeEditGrams,
                sheetState = editSheetState,
                containerColor = WarmOffWhite,
            ) {
                GramsSheet(
                    product = state.editingItem!!.product,
                    initialGrams = state.editingItem!!.grams,
                    buttonTitle = "Сохранить",
                    onAdd = { grams -> viewModel.updateItemGrams(state.editingItem!!.uid, grams) },
                )
            }
        }

        AnimatedVisibility(
            visible = state.isSaving,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            SavingOverlay()
        }

        AnimatedVisibility(
            visible = state.errorMessage != null && !state.isSaving,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ErrorBanner(
                message = state.errorMessage ?: "",
                onDismiss = viewModel::clearError,
            )
        }

        if (showDiscardDialog) {
            DismissCreateMealPlanDialog(
                onConfirm = {
                    showDiscardDialog = false
                    onDiscardToHome()
                },
                onCancel = { showDiscardDialog = false },
            )
        }
    }
}


@Composable
private fun SetupStep(
    state: com.asc.gymgenie.nutrition.CreateMealPlanUiState,
    onClose: () -> Unit,
    onMealKind: (ManualMealKind) -> Unit,
    onScheduleMode: (ManualScheduleMode) -> Unit,
    onSelectDate: (String) -> Unit,
    onToggleWeekday: (String) -> Unit,
    onNext: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        GymGenieToolbar(
            title = "Создать приём пищи",
            showBackNavigation = true,
            onBackClick = onClose,
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Text(
                    text = "Что планируем?",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepInk,
                )
            }
            items(items = listOf(ManualMealKind.BREAKFAST, ManualMealKind.LUNCH, ManualMealKind.DINNER)) { kind ->
                MealKindCard(
                    kind = kind,
                    isSelected = state.mealKind == kind,
                    onTap = { onMealKind(kind) },
                )
            }
            if (state.mealKind != null) {
                item {
                    Text(
                        text = "Когда?",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepInk,
                    )
                }
                item {
                    ScheduleModeToggle(
                        mode = state.scheduleMode,
                        onSelect = onScheduleMode,
                    )
                }
                item {
                    if (state.scheduleMode == ManualScheduleMode.ONE_OFF) {
                        OneOffDateStrip(
                            bookedDates = state.bookedOneOffDates,
                            selectedDate = state.selectedDate,
                            onSelect = onSelectDate,
                        )
                    } else {
                        WeekdayChipsRow(
                            bookedDays = state.bookedRecurringDays,
                            selectedDays = state.selectedWeekdays,
                            onToggle = onToggleWeekday,
                        )
                    }
                }
            }
        }

        BottomCtaBar {
            PrimaryButton(
                title = "Далее",
                enabled = state.canContinueFromSetup,
                onClick = onNext,
            )
        }
    }
}

@Composable
private fun MealKindCard(
    kind: ManualMealKind,
    isSelected: Boolean,
    onTap: () -> Unit,
) {
    val emoji = when (kind) {
        ManualMealKind.BREAKFAST -> "🌅"
        ManualMealKind.LUNCH -> "☀️"
        ManualMealKind.DINNER -> "🌙"
    }
    val border = if (isSelected) Coral else CardBorder
    val borderWidth = if (isSelected) 2.dp else 1.5.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(borderWidth, border, RoundedCornerShape(18.dp))
            .clickable { onTap() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isSelected) Coral.copy(alpha = 0.18f) else SoftCard),
            contentAlignment = Alignment.Center,
        ) { Text(text = emoji, fontSize = 26.sp) }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = kind.displayName,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepInk,
            )
            Text(text = kind.kcalHintRu, fontSize = 16.sp, color = DeepInk)
        }

        Box(
            modifier = Modifier.size(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(
                        1.5.dp,
                        if (isSelected) Coral else Color(0xFFD5D5DB),
                        CircleShape,
                    ),
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Coral),
                )
            }
        }
    }
}

@Composable
private fun ScheduleModeToggle(
    mode: ManualScheduleMode,
    onSelect: (ManualScheduleMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SoftCard)
            .padding(4.dp),
    ) {
        ToggleSegment(
            label = "Разово",
            isSelected = mode == ManualScheduleMode.ONE_OFF,
            onClick = { onSelect(ManualScheduleMode.ONE_OFF) },
            modifier = Modifier.weight(1f),
        )
        ToggleSegment(
            label = "По дням",
            isSelected = mode == ManualScheduleMode.RECURRING,
            onClick = { onSelect(ManualScheduleMode.RECURRING) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ToggleSegment(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (isSelected) Coral else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else DeepInk,
        )
    }
}

private data class DateStripEntry(
    val iso: String,
    val day: Int,
    val weekdayShort: String,
)

@Composable
private fun OneOffDateStrip(
    bookedDates: List<String>,
    selectedDate: String?,
    onSelect: (String) -> Unit,
) {
    val dates = remember { upcomingDateStrip(daysAhead = 14) }
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        dates.forEach { entry ->
            val isBooked = bookedDates.contains(entry.iso)
            val isSelected = selectedDate == entry.iso
            DateCell(
                weekdayShort = entry.weekdayShort,
                day = entry.day,
                isSelected = isSelected,
                isBooked = isBooked,
                onTap = { if (!isBooked) onSelect(entry.iso) },
            )
        }
    }
}

private fun upcomingDateStrip(daysAhead: Int): List<DateStripEntry> {
    val isoFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    return List(daysAhead) {
        val iso = isoFmt.format(cal.time)
        val entry = DateStripEntry(
            iso = iso,
            day = cal.get(Calendar.DAY_OF_MONTH),
            weekdayShort = weekdayShortRu(cal.get(Calendar.DAY_OF_WEEK)),
        )
        cal.add(Calendar.DAY_OF_MONTH, 1)
        entry
    }
}

@Composable
private fun DateCell(
    weekdayShort: String,
    day: Int,
    isSelected: Boolean,
    isBooked: Boolean,
    onTap: () -> Unit,
) {
    val bg = when {
        isSelected -> Coral
        isBooked -> SoftCard
        else -> Color.White
    }
    val border = if (isSelected) Coral else CardBorder
    val textColor = when {
        isSelected -> Color.White
        isBooked -> MutedText
        else -> DeepInk
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(width = 56.dp, height = 70.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(14.dp))
            .clickable(enabled = !isBooked) { onTap() },
    ) {
        Text(
            text = weekdayShort,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color.White else MutedText,
        )
        Text(
            text = day.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
        )
        if (isBooked) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Занято",
                tint = MutedText,
                modifier = Modifier.size(10.dp),
            )
        }
    }
}

@Composable
private fun WeekdayChipsRow(
    bookedDays: List<String>,
    selectedDays: List<String>,
    onToggle: (String) -> Unit,
) {
    val days = listOf(
        "MONDAY" to "Пн",
        "TUESDAY" to "Вт",
        "WEDNESDAY" to "Ср",
        "THURSDAY" to "Чт",
        "FRIDAY" to "Пт",
        "SATURDAY" to "Сб",
        "SUNDAY" to "Вс",
    )
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        days.forEach { (wire, label) ->
            val isBooked = bookedDays.contains(wire)
            val isSelected = selectedDays.contains(wire)
            WeekdayChip(
                label = label,
                isSelected = isSelected,
                isBooked = isBooked,
                onTap = { if (!isBooked) onToggle(wire) },
            )
        }
    }
}

@Composable
private fun WeekdayChip(
    label: String,
    isSelected: Boolean,
    isBooked: Boolean,
    onTap: () -> Unit,
) {
    val bg = when {
        isSelected -> Coral
        isBooked -> SoftCard
        else -> Color.White
    }
    val borderColor = if (isSelected) Coral else CardBorder
    val textColor = when {
        isSelected -> Color.White
        isBooked -> MutedText
        else -> DeepInk
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.5.dp, borderColor, RoundedCornerShape(50))
            .clickable(enabled = !isBooked) { onTap() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
        if (isBooked) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Занято",
                tint = MutedText,
                modifier = Modifier.size(10.dp),
            )
        }
    }
}


@Composable
private fun EditStep(
    state: com.asc.gymgenie.nutrition.CreateMealPlanUiState,
    onBack: () -> Unit,
    onRequestDiscard: () -> Unit,
    onPlanName: (String) -> Unit,
    onAddProduct: () -> Unit,
    onRemove: (Long) -> Unit,
    onEditGrams: (AddedMealItem) -> Unit,
    onSave: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
    ) {
        GymGenieToolbar(
            title = state.mealKind?.displayName ?: "Приём пищи",
            showBackNavigation = true,
            showCloseIcon = true,
            onBackClick = onRequestDiscard,
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                MacrosSummaryCard(
                    kcal = state.totalCalories,
                    proteinG = state.totalProteinG,
                    fatG = state.totalFatG,
                    carbsG = state.totalCarbsG,
                )
            }
            item {
                PlanNameField(value = state.planName, onChange = onPlanName)
            }

            if (state.addedItems.isEmpty()) {
                item { EmptyEditState(onAdd = onAddProduct) }
            } else {
                item {
                    Text(
                        text = "ДОБАВЛЕНО",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedText,
                        letterSpacing = 0.5.sp,
                    )
                }
                items(items = state.addedItems, key = { it.uid }) { item ->
                    AddedItemRow(
                        item = item,
                        onEdit = { onEditGrams(item) },
                        onDelete = { onRemove(item.uid) },
                    )
                }
                item { AddProductButton(onClick = onAddProduct) }
            }
        }

        BottomCtaBar {
            PrimaryButton(
                title = if (state.isSaving) "Сохраняем..." else "Сохранить",
                enabled = state.canSave,
                onClick = onSave,
            )
        }
    }
}

@Composable
private fun MacrosSummaryCard(
    kcal: Int,
    proteinG: Double,
    fatG: Double,
    carbsG: Double,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(DeepInk)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ВСЕГО КАЛОРИЙ",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.65f),
                    letterSpacing = 0.5.sp,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$kcal ккал",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
            }
            Text(text = "🔥", fontSize = 32.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MacroPill(label = "Б", grams = proteinG)
            MacroPill(label = "Ж", grams = fatG)
            MacroPill(label = "У", grams = carbsG)
        }
    }
}

@Composable
private fun MacroPill(label: String, grams: Double) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "${grams.roundToIntSafe()}г",
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
        )
    }
}

@Composable
private fun PlanNameField(value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "НАЗВАНИЕ",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = DeepInk,
            letterSpacing = 0.5.sp,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text("Завтрак 17 мая", fontSize = 18.sp, color = MutedText) },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Coral,
                unfocusedBorderColor = CardBorder,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Coral,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AddedItemRow(item: AddedMealItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.5.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SoftCard),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.product.emoji ?: defaultEmoji(item.product.category),
                fontSize = 20.sp,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.product.nameRu,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${item.grams.roundToIntSafe()}г, ${item.portion.calories.toInt()} ккал, Б${shortMacro(item.portion.proteinG)}, Ж${shortMacro(item.portion.fatG)}, У${shortMacro(item.portion.carbsG)}",
                fontSize = 14.sp,
                color = DeepInk,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(SoftCard)
                    .clickable { onEdit() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Изменить",
                    tint = DeepInk,
                    modifier = Modifier.size(16.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFE8E8))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Удалить",
                    tint = Color(0xFFE5392E),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun AddProductButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                1.5.dp,
                DeepInk.copy(alpha = 0.35f),
                RoundedCornerShape(14.dp),
            )
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepInk)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Добавить продукт",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DeepInk,
        )
    }
}

@Composable
private fun EmptyEditState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .border(1.5.dp, Color(0xFFDFDFE5), RoundedCornerShape(22.dp))
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = "🥗", fontSize = 40.sp)
        Text(
            text = "Пока пусто",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DeepInk,
        )
        Text(
            text = "Добавьте продукты, из которых будет состоять приём пищи",
            fontSize = 15.sp,
            color = MutedText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Coral)
                .clickable { onAdd() }
                .padding(horizontal = 22.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Добавить продукт",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}


@Composable
private fun PickerStep(
    state: com.asc.gymgenie.nutrition.CreateMealPlanUiState,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onCategory: (FoodCategory?) -> Unit,
    onSelect: (FoodProduct) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableFloatStateOf(with(density) { 120.dp.toPx() }) }
    var scrollOffsetPx by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f && scrollOffsetPx > 0f) {
                    val toExpand = minOf(available.y, scrollOffsetPx)
                    scrollOffsetPx -= toExpand
                    return Offset(0f, toExpand)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (consumed.y < 0f) {
                    scrollOffsetPx = (scrollOffsetPx - consumed.y).coerceIn(0f, headerHeightPx)
                }
                return Offset.Zero
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
    ) {
        GymGenieToolbar(
            title = "Выбор продукта",
            showBackNavigation = true,
            onBackClick = onBack,
        )

        when {
            state.isLoadingProducts && state.products.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Coral)
                }
            }
            state.productsError != null && state.products.isEmpty() -> {
                ErrorPlaceholder(message = state.productsError ?: "Ошибка")
            }
            state.filteredProducts.isEmpty() && state.searchQuery.isNotEmpty() && !state.isLoadingProducts -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 8.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SearchField(query = state.searchQuery, onChange = onSearch)
                        CategoryChipsRow(selected = state.selectedCategory, onSelect = onCategory)
                    }
                    EmptyPlaceholder()
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnection)) {
                    val visibleHeaderHeightDp = with(density) {
                        (headerHeightPx - scrollOffsetPx).coerceAtLeast(0f).toDp()
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(visibleHeaderHeightDp)
                            .clipToBounds(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                                .graphicsLayer { translationY = -scrollOffsetPx }
                                .onGloballyPositioned { headerHeightPx = it.size.height.toFloat() },
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SearchField(
                                query = state.searchQuery,
                                onChange = onSearch,
                                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 8.dp),
                            )
                            CategoryChipsRow(
                                selected = state.selectedCategory,
                                onSelect = onCategory,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    if (state.isLoadingProducts) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Coral, modifier = Modifier.size(32.dp))
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(items = state.filteredProducts, key = { it.id }) { product ->
                                PickerProductRow(product = product, onTap = { onSelect(product) })
                            }
                            item { Spacer(modifier = Modifier.height(20.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(Color.White),
    ) {
        TextField(
            value = query,
            onValueChange = onChange,
            placeholder = { Text("Поиск продукта", color = MutedText) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MutedText,
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Очистить",
                            tint = MutedText,
                        )
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                cursorColor = Coral,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CategoryChipsRow(
    selected: FoodCategory?,
    onSelect: (FoodCategory?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CategoryChip(label = "Все", isSelected = selected == null, onClick = { onSelect(null) })
        FoodCategory.entries.forEach { cat ->
            CategoryChip(
                label = cat.displayName(),
                isSelected = selected == cat,
                onClick = { onSelect(cat) },
            )
        }
    }
}

@Composable
private fun CategoryChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (isSelected) Color.White else DeepInk,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isSelected) AccentOrange else SoftCard)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@Composable
private fun PickerProductRow(product: FoodProduct, onTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.5.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable { onTap() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(categoryBg(product.category)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = product.emoji ?: defaultEmoji(product.category),
                fontSize = 20.sp,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.nameRu,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniMacroChip(label = "ккал", value = product.caloriesPer100g.toInt().toString())
                MiniMacroChip(label = "Б", value = shortMacro(product.proteinPer100g))
                MiniMacroChip(label = "Ж", value = shortMacro(product.fatPer100g))
                MiniMacroChip(label = "У", value = shortMacro(product.carbsPer100g))
            }
        }
    }
}

@Composable
private fun MiniMacroChip(label: String, value: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(SoftCard)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = DeepInk,
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = DeepInk,
        )
    }
}


@Composable
private fun InfoStep(
    state: com.asc.gymgenie.nutrition.CreateMealPlanUiState,
    onBack: () -> Unit,
    onChooseGrams: (FoodProduct) -> Unit,
) {
    val product = state.infoFor
    Column(modifier = Modifier.fillMaxSize()) {
        GymGenieToolbar(
            title = "Выбор продукта",
            showBackNavigation = true,
            onBackClick = onBack,
        )
        if (product != null) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(categoryBg(product.category)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = product.emoji ?: defaultEmoji(product.category),
                            fontSize = 64.sp,
                        )
                    }
                }
                item {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = product.nameRu,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepInk,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "БЖУ продукта на 100г",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = DeepInk,
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BigMacroCell(
                            label = "Калории",
                            value = "${product.caloriesPer100g.toInt()} ккал",
                            fg = Color(0xFFE07B00),
                            modifier = Modifier.weight(1f),
                        )
                        BigMacroCell(
                            label = "Белки",
                            value = "${shortMacro(product.proteinPer100g)} г",
                            fg = Color(0xFF0A84FF),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BigMacroCell(
                            label = "Жиры",
                            value = "${shortMacro(product.fatPer100g)} г",
                            fg = Color(0xFFFF6B6B),
                            modifier = Modifier.weight(1f),
                        )
                        BigMacroCell(
                            label = "Углеводы",
                            value = "${shortMacro(product.carbsPer100g)} г",
                            fg = Color(0xFF34C759),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            BottomCtaBar {
                PrimaryButton(
                    title = "Выбрать порцию",
                    enabled = true,
                    onClick = { onChooseGrams(product) },
                )
            }
        }
    }
}

@Composable
private fun BigMacroCell(
    label: String,
    value: String,
    fg: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .border(1.5.dp, fg, RoundedCornerShape(18.dp))
            .padding(vertical = 18.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = fg,
            letterSpacing = 0.4.sp,
        )
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DeepInk,
        )
    }
}


@Composable
private fun GramsSheet(
    product: FoodProduct,
    initialGrams: Double = 100.0,
    buttonTitle: String = "Добавить",
    onAdd: (Double) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var gramsText by remember { mutableStateOf(initialGrams.toInt().toString()) }
    val parsedGrams = gramsText.replace(',', '.').toDoubleOrNull()
        ?.coerceIn(1.0, 5000.0)
    val canAdd = parsedGrams != null && parsedGrams > 0.0
    val macros: FoodPortionMacros? = parsedGrams?.let { product.macrosForGrams(it) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(categoryBg(product.category)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = product.emoji ?: defaultEmoji(product.category),
                    fontSize = 24.sp,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.nameRu,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepInk,
                    maxLines = 2,
                )
                Text(
                    text = "${product.caloriesPer100g.toInt()} ккал, Б${shortMacro(product.proteinPer100g)}, Ж${shortMacro(product.fatPer100g)}, У${shortMacro(product.carbsPer100g)} / 100г",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MutedText,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StepperButton(symbol = "−", onClick = {
                val v = (gramsText.toDoubleOrNull() ?: 100.0) - 10.0
                gramsText = v.coerceAtLeast(1.0).toInt().toString()
            })
            OutlinedTextField(
                value = gramsText,
                onValueChange = { raw ->
                    val cleaned = raw.filter { it.isDigit() }
                    if (cleaned.length <= 4) gramsText = cleaned
                },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, color = DeepInk),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Coral,
                    unfocusedBorderColor = CardBorder,
                    focusedContainerColor = SoftCard,
                    unfocusedContainerColor = SoftCard,
                    cursorColor = Coral,
                ),
                modifier = Modifier.weight(1f),
            )
            StepperButton(symbol = "+", onClick = {
                val v = (gramsText.toDoubleOrNull() ?: 100.0) + 10.0
                gramsText = v.coerceAtMost(5000.0).toInt().toString()
            })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(50, 100, 150, 200).forEach { preset ->
                val isSelected = gramsText == preset.toString()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Coral else SoftCard)
                        .clickable { gramsText = preset.toString() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${preset}г",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else DeepInk,
                    )
                }
            }
        }

        if (macros != null) {
            LiveMacrosBar(macros = macros)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (canAdd) Coral else SoftCard)
                .clickable(enabled = canAdd) {
                    parsedGrams?.let(onAdd)
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = buttonTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (canAdd) Color.White else MutedText,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun StepperButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.5.dp, CardBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = symbol, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DeepInk)
    }
}

@Composable
private fun LiveMacrosBar(macros: FoodPortionMacros) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.5.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MacroBlock(label = "ккал", value = macros.calories.toInt().toString(), modifier = Modifier.weight(1f))
        VerticalDivider()
        MacroBlock(label = "Б", value = shortMacro(macros.proteinG), modifier = Modifier.weight(1f))
        VerticalDivider()
        MacroBlock(label = "Ж", value = shortMacro(macros.fatG), modifier = Modifier.weight(1f))
        VerticalDivider()
        MacroBlock(label = "У", value = shortMacro(macros.carbsG), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MacroBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = DeepInk,
        )
        Text(
            text = label,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = DeepInk,
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(CardBorder),
    )
}


@Composable
private fun DismissCreateMealPlanDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = "Завершить создание?",
                color = Color(0xFF0A0A0A),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = "Вы уверены, что хотите выйти? Прогресс создания плана питания будет потерян.",
                color = Color(0xFF8B8B92),
                fontSize = 16.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Да",
                    color = Color(0xFFE5484D),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = "Нет", color = Color(0xFF0A0A0A), fontSize = 16.sp)
            }
        },
        containerColor = Color.White,
    )
}


@Composable
private fun BottomCtaBar(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WarmOffWhite)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        content()
    }
}

@Composable
private fun PrimaryButton(
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) Coral else SoftCard)
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (enabled) Color.White else MutedText,
        )
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFE8E8))
            .border(1.5.dp, Color(0xFFE5392E).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable { onDismiss() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "⚠️", fontSize = 16.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            color = Color(0xFF7A1F1A),
            modifier = Modifier.weight(1f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SavingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .padding(24.dp),
        ) {
            CircularProgressIndicator(color = Coral)
            Text(
                text = "Сохраняем рацион...",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepInk,
            )
        }
    }
}

@Composable
private fun ErrorPlaceholder(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "⚠️", fontSize = 36.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            color = MutedText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "🔍", fontSize = 36.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Ничего не найдено", fontSize = 14.sp, color = MutedText)
    }
}


private fun shortMacro(value: Double): String {
    return if (value == value.toLong().toDouble()) value.toLong().toString()
    else "%.1f".format(value)
}

private fun Double.roundToIntSafe(): Int = (this + 0.5).toInt()

private fun weekdayShortRu(calendarDayOfWeek: Int): String = when (calendarDayOfWeek) {
    Calendar.MONDAY -> "Пн"
    Calendar.TUESDAY -> "Вт"
    Calendar.WEDNESDAY -> "Ср"
    Calendar.THURSDAY -> "Чт"
    Calendar.FRIDAY -> "Пт"
    Calendar.SATURDAY -> "Сб"
    Calendar.SUNDAY -> "Вс"
    else -> ""
}

private fun defaultEmoji(category: FoodCategory): String = when (category) {
    FoodCategory.MEAT -> "🍗"
    FoodCategory.FISH -> "🐟"
    FoodCategory.DAIRY -> "🥛"
    FoodCategory.EGGS -> "🥚"
    FoodCategory.GRAINS -> "🌾"
    FoodCategory.LEGUMES -> "🫘"
    FoodCategory.VEGETABLES -> "🥦"
    FoodCategory.FRUITS -> "🍎"
    FoodCategory.NUTS_SEEDS -> "🥜"
    FoodCategory.OILS -> "🫙"
    FoodCategory.OTHER -> "🍴"
}

private fun categoryBg(category: FoodCategory): Color = when (category) {
    FoodCategory.MEAT -> Color(0xFFFFE8E0)
    FoodCategory.FISH -> Color(0xFFE0F0FF)
    FoodCategory.DAIRY -> Color(0xFFFFF8E8)
    FoodCategory.EGGS -> Color(0xFFFFF4D6)
    FoodCategory.GRAINS -> Color(0xFFFFF0D6)
    FoodCategory.LEGUMES -> Color(0xFFE8F7E8)
    FoodCategory.VEGETABLES -> Color(0xFFE0F5E8)
    FoodCategory.FRUITS -> Color(0xFFFDE8F0)
    FoodCategory.NUTS_SEEDS -> Color(0xFFF5EDE0)
    FoodCategory.OILS -> Color(0xFFFFF9E0)
    FoodCategory.OTHER -> Color(0xFFF3F2EF)
}
