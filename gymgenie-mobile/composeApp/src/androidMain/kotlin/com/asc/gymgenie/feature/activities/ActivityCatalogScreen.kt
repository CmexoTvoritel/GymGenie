package com.asc.gymgenie.feature.activities

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.activity.ActivityCatalogResponse
import com.asc.gymgenie.activity.ActivityRing
import com.asc.gymgenie.feature.activities.components.CatalogActivityCard
import com.asc.gymgenie.feature.activities.components.ringLabel
import com.asc.gymgenie.presentation.ActivityCatalogViewModel
import com.asc.gymgenie.ui.components.GymGenieToolbar
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.SoftCard
import com.asc.gymgenie.ui.theme.WarmOffWhite
import org.koin.core.context.GlobalContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ActivityCatalogScreen(onBack: () -> Unit) {
    val koin = remember { GlobalContext.get() }
    val viewModel = remember { koin.get<ActivityCatalogViewModel>() }
    val state by viewModel.state.collectAsState()
    var query by remember { mutableStateOf("") }
    var scheduleTarget by remember { mutableStateOf<ActivityCatalogResponse?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }

    DisposableEffect(viewModel) {
        onDispose { viewModel.onCleared() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        GymGenieToolbar(
            title = "Добавить активность",
            showBackNavigation = true,
            onBackClick = onBack,
        )
        SearchBar(value = query, onValueChange = { query = it })

        when {
            state.isLoading -> CenteredSpinner()
            state.error != null && state.catalog.isEmpty() ->
                ErrorState(message = state.error.orEmpty(), onRetry = viewModel::load)
            else -> CatalogList(
                catalog = state.catalog,
                planIds = state.planIds,
                query = query,
                onToggle = { activity ->
                    if (activity.id in state.planIds) {
                        viewModel.togglePlan(activity.id)
                    } else {
                        scheduleTarget = activity
                    }
                },
            )
        }
    }

    SchedulePickerBottomSheet(
        activity = scheduleTarget,
        onDismiss = { scheduleTarget = null },
        onConfirm = { activityId, scheduleType, scheduleDays, oneOffDate ->
            viewModel.addToPlanWithSchedule(
                activityId = activityId,
                scheduleType = scheduleType,
                scheduleDays = scheduleDays,
                oneOffDate = oneOffDate,
                goal = null,
            )
            scheduleTarget = null
        },
    )
}

@Composable
private fun SearchBar(value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SoftCard)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MutedText,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = "Найти активность...",
                    fontSize = 15.sp,
                    color = MutedText,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                cursorBrush = SolidColor(DeepInk),
                textStyle = TextStyle(color = DeepInk, fontSize = 15.sp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MutedText.copy(alpha = 0.3f))
                    .clickable { onValueChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = DeepInk,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun CatalogList(
    catalog: List<ActivityCatalogResponse>,
    planIds: Set<String>,
    query: String,
    onToggle: (ActivityCatalogResponse) -> Unit,
) {
    val filtered = remember(catalog, query) {
        if (query.isBlank()) catalog
        else catalog.filter { it.name.contains(query, ignoreCase = true) }
    }

    if (filtered.isEmpty()) {
        EmptyState(
            icon = "🔍",
            message = "Ничего не найдено",
            hint = "Попробуй изменить запрос",
        )
        return
    }

    val grouped = remember(filtered) { filtered.groupBy { it.ring } }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActivityRing.entries.forEach { ring ->
            val items = grouped[ring.name].orEmpty()
            if (items.isEmpty()) return@forEach

            item(key = "header-${ring.name}") {
                Text(
                    text = ringLabel(ring),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = DeepInk,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }
            items(items, key = { it.id }) { activity ->
                CatalogActivityCard(
                    activity = activity,
                    isInPlan = activity.id in planIds,
                    onToggle = { onToggle(activity) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Schedule picker bottom sheet — shown when adding an activity to the plan
// ---------------------------------------------------------------------------

private val catalogDayLabelToBackend = linkedMapOf(
    "Пн" to "MONDAY",
    "Вт" to "TUESDAY",
    "Ср" to "WEDNESDAY",
    "Чт" to "THURSDAY",
    "Пт" to "FRIDAY",
    "Сб" to "SATURDAY",
    "Вс" to "SUNDAY",
)

private val catalogDaysOfWeek = catalogDayLabelToBackend.keys.toList()

private enum class CatalogScheduleMode { EVERY_DAY, RECURRING, ONE_TIME }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchedulePickerBottomSheet(
    activity: ActivityCatalogResponse?,
    onDismiss: () -> Unit,
    onConfirm: (activityId: String, scheduleType: String?, scheduleDays: List<String>?, oneOffDate: String?) -> Unit,
) {
    if (activity == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var scheduleMode by rememberSaveable { mutableStateOf(CatalogScheduleMode.EVERY_DAY) }
    var selectedDays by rememberSaveable { mutableStateOf(catalogDaysOfWeek.toSet()) }
    var oneOffDate by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WarmOffWhite,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            // --- Header ---
            Text(
                text = activity.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Выберите расписание",
                fontSize = 14.sp,
                color = MutedText,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- Schedule mode selector ---
            CatalogScheduleMode.entries.forEach { mode ->
                val label = when (mode) {
                    CatalogScheduleMode.EVERY_DAY -> "Каждый день"
                    CatalogScheduleMode.RECURRING -> "По дням недели"
                    CatalogScheduleMode.ONE_TIME -> "На конкретную дату"
                }
                val isSelected = mode == scheduleMode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { scheduleMode = mode }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .then(
                                if (isSelected) Modifier.background(AccentOrange)
                                else Modifier.border(1.5.dp, MutedText.copy(alpha = 0.4f), CircleShape),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = label,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) DeepInk else MutedText,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Day chips (RECURRING mode) ---
            if (scheduleMode == CatalogScheduleMode.RECURRING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    catalogDaysOfWeek.forEach { day ->
                        val isSelected = day in selectedDays
                        val baseModifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                        val styledModifier = if (isSelected) {
                            baseModifier.background(AccentOrange)
                        } else {
                            baseModifier.border(1.dp, MutedText.copy(alpha = 0.35f), CircleShape)
                        }
                        Box(
                            modifier = styledModifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    selectedDays = if (day in selectedDays && selectedDays.size > 1) {
                                        selectedDays - day
                                    } else {
                                        selectedDays + day
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = day,
                                color = if (isSelected) Color.White else DeepInk,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // --- Date picker (ONE_TIME mode) ---
            if (scheduleMode == CatalogScheduleMode.ONE_TIME) {
                val displayDate = remember(oneOffDate) {
                    if (oneOffDate.isBlank()) "Выбрать дату"
                    else {
                        runCatching {
                            val ld = LocalDate.parse(oneOffDate)
                            val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))
                            ld.format(fmt)
                        }.getOrDefault(oneOffDate)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MutedText.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .clickable {
                            val today = LocalDate.now()
                            val initial = if (oneOffDate.isNotBlank()) {
                                runCatching { LocalDate.parse(oneOffDate) }.getOrDefault(today)
                            } else today

                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val picked = LocalDate.of(year, month + 1, day)
                                    oneOffDate = picked.toString()
                                },
                                initial.year,
                                initial.monthValue - 1,
                                initial.dayOfMonth,
                            )
                                .apply { datePicker.minDate = System.currentTimeMillis() }
                                .show()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = displayDate,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (oneOffDate.isBlank()) MutedText else DeepInk,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Confirm button ---
            val isFormValid = when (scheduleMode) {
                CatalogScheduleMode.EVERY_DAY -> true
                CatalogScheduleMode.RECURRING -> selectedDays.isNotEmpty()
                CatalogScheduleMode.ONE_TIME -> oneOffDate.isNotBlank()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isFormValid) AccentOrange else AccentOrange.copy(alpha = 0.4f))
                    .then(
                        if (isFormValid) Modifier.clickable {
                            val (type, days, date) = resolveCatalogScheduleParams(
                                scheduleMode, selectedDays, oneOffDate,
                            )
                            onConfirm(activity.id, type, days, date)
                        } else Modifier,
                    )
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Добавить",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Cancel button ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SoftCard)
                    .clickable { onDismiss() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Отмена",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepInk,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun resolveCatalogScheduleParams(
    mode: CatalogScheduleMode,
    selectedDays: Set<String>,
    oneOffDate: String,
): Triple<String?, List<String>?, String?> = when (mode) {
    CatalogScheduleMode.EVERY_DAY -> Triple(null, null, null)
    CatalogScheduleMode.RECURRING -> {
        val backendDays = selectedDays.mapNotNull { catalogDayLabelToBackend[it] }
        Triple("RECURRING", backendDays, null)
    }
    CatalogScheduleMode.ONE_TIME -> Triple("ONE_TIME", null, oneOffDate)
}

// ---------------------------------------------------------------------------

@Composable
private fun CenteredSpinner() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AccentOrange)
    }
}

@Composable
private fun EmptyState(icon: String, message: String, hint: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SoftCard)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepInk,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = hint, fontSize = 13.sp, color = MutedText)
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SoftCard)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "⚠️", fontSize = 32.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Не удалось загрузить",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepInk,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = message, fontSize = 13.sp, color = MutedText)
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentOrange)
                    .clickable { onRetry() }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "Повторить",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}
