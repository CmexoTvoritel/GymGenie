package com.asc.gymgenie.feature.nutrition

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.nutrition.MealGoal
import com.asc.gymgenie.nutrition.MealPlanShortInfo
import com.asc.gymgenie.nutrition.MealPlansApi
import com.asc.gymgenie.nutrition.MealPlansListUiState
import com.asc.gymgenie.nutrition.MealPlansListViewModel
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.user.UserProfileStore
import org.koin.core.context.GlobalContext

private val NutritionCardBorder = Color(0xFFEDEDEF)
private val NutritionChipBg = Color(0xFFF4F4F6)
private val NutritionChipText = Color(0xFF3A3A40)
private val NutritionDeleteRed = Color(0xFFE53935)

/**
 * Saved AI-generated meal plans list.
 *
 * Mirrors [com.asc.gymgenie.feature.workouts.WorkoutsScreen] in spirit: a
 * scrollable list with header, pull-to-refresh, an empty state, and a
 * primary "create plan" entry point that launches the AI meal coach flow.
 *
 * The AI coach is hosted inline as a slide-up overlay so the surface owns
 * its own navigation: the parent only needs to host this single screen.
 *
 * Wires the shared [MealPlansListViewModel] directly — same convention used
 * by [com.asc.gymgenie.feature.workouts.WorkoutsScreen] and
 * [com.asc.gymgenie.feature.ai.AiFlowScreen]. Lifetime is tied to the
 * surrounding Compose tree via `remember { } / DisposableEffect`.
 */
@Composable
fun NutritionScreen(
    tokenStorage: TokenStorage,
    userProfileStore: UserProfileStore,
    onBack: () -> Unit,
) {
    val koin = remember { GlobalContext.get() }
    val viewModel = remember {
        MealPlansListViewModel(mealPlansApi = koin.get<MealPlansApi>())
    }
    DisposableEffect(Unit) { onDispose { viewModel.onCleared() } }

    val state by viewModel.state.collectAsState()

    var planPendingDelete by remember { mutableStateOf<MealPlanShortInfo?>(null) }
    var showAiCoach by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!state.plansLoaded) viewModel.load()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HeaderSection(onBack = onBack)

            when {
                state.isLoading && state.plans.isEmpty() -> LoadingContent()
                state.errorMessage != null && state.plans.isEmpty() && !isExpectedEmptyState(state) ->
                    ErrorContent(
                        message = state.errorMessage ?: "",
                        onRetry = viewModel::retry,
                    )
                else -> ListContent(
                    plans = state.plans,
                    isRefreshing = state.isRefreshing,
                    isLoadingMore = state.isLoadingMore,
                    deletingPlanId = state.deletingPlanId,
                    hasMore = state.hasMore,
                    onRefresh = viewModel::refresh,
                    onLoadMore = viewModel::loadMore,
                    onCreatePlan = { showAiCoach = true },
                    onRequestDelete = { planPendingDelete = it },
                )
            }
        }

        CreatePlanFab(
            onClick = { showAiCoach = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
        )

        AnimatedVisibility(
            visible = showAiCoach,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            AiMealFlowScreen(
                userProfileStore = userProfileStore,
                onDismiss = {
                    showAiCoach = false
                    // Refresh on dismiss so a freshly-saved plan shows up
                    // without needing a manual pull-to-refresh.
                    viewModel.refresh()
                },
            )
        }
    }

    val pendingDelete = planPendingDelete
    if (pendingDelete != null) {
        ConfirmDeleteDialog(
            planName = pendingDelete.name,
            onCancel = { planPendingDelete = null },
            onConfirm = {
                viewModel.deletePlan(pendingDelete.id)
                planPendingDelete = null
            },
        )
    }
}

/**
 * "Successful empty result" must render the empty state, not the generic
 * error fallback. Mirrors [com.asc.gymgenie.feature.workouts.WorkoutsScreen]
 * to avoid the bug where a stale [errorMessage] hides a valid empty list.
 */
private fun isExpectedEmptyState(state: MealPlansListUiState): Boolean =
    state.plansLoaded && state.plans.isEmpty()

@Composable
private fun HeaderSection(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 4.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = DeepInk,
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Рацион",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Сохранённые планы питания",
                fontSize = 13.sp,
                color = MutedText,
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Coral)
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "⚠️", fontSize = 36.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = MutedText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Coral),
            shape = RoundedCornerShape(50),
        ) {
            Text("Повторить", color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListContent(
    plans: List<MealPlanShortInfo>,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    deletingPlanId: String?,
    hasMore: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onCreatePlan: () -> Unit,
    onRequestDelete: (MealPlanShortInfo) -> Unit,
) {
    val refreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    // Infinite-scroll trigger: when the user reveals the last card we ask the
    // VM for the next page. The VM itself guards against duplicate / racing
    // requests via its in-flight job + `hasMore` flag.
    LaunchedEffect(listState, hasMore) {
        snapshotFlow {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            total > 0 && last >= total - 2
        }.collect { nearEnd ->
            if (nearEnd && hasMore) onLoadMore()
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = refreshState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = refreshState,
                isRefreshing = isRefreshing,
                containerColor = Color.White,
                color = Coral,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
    ) {
        if (plans.isEmpty()) {
            // Empty state must remain pull-to-refresh-able; LazyColumn fills
            // the viewport so the gesture has a target on every device.
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { EmptyState(onCreate = onCreatePlan) }
            }
            return@PullToRefreshBox
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(plans, key = { it.id }) { plan ->
                MealPlanCard(
                    plan = plan,
                    isDeleting = deletingPlanId == plan.id,
                    onDelete = { onRequestDelete(plan) },
                )
            }
            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = Coral,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
            // Reserve space at the bottom so the FAB does not cover the last card.
            item { Spacer(modifier = Modifier.height(96.dp)) }
        }
    }
}

@Composable
private fun EmptyState(onCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "🥗", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Нет сохранённых рационов",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepInk,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Создайте первый рацион с ИИ-нутрициологом",
            fontSize = 13.sp,
            color = MutedText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onCreate,
            colors = ButtonDefaults.buttonColors(
                containerColor = Coral,
                contentColor = Color.White,
            ),
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Создать рацион",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CreatePlanFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(elevation = 8.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(Coral)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = "Создать рацион",
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun MealPlanCard(
    plan: MealPlanShortInfo,
    isDeleting: Boolean,
    onDelete: () -> Unit,
) {
    val goalDisplay = remember(plan.goal) { MealGoal.fromWireValue(plan.goal)?.displayName }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.5.dp, NutritionCardBorder, RoundedCornerShape(20.dp))
            .alpha(if (isDeleting) 0.5f else 1f)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Coral.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "🥗", fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plan.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!plan.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = plan.description!!,
                        fontSize = 12.sp,
                        color = MutedText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            CardActionsMenu(enabled = !isDeleting, onDelete = onDelete)
        }

        // Chips row: only render the chips that have meaningful content. The
        // composable handles its own internal spacing so an absent chip does
        // not leave a phantom gap.
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (goalDisplay != null) {
                Chip(label = goalDisplay)
            }
            val kcal = plan.totalCalories ?: 0
            if (kcal > 0) {
                Chip(label = "$kcal ккал")
            }
            Chip(label = "${plan.mealsCount} ${pluralizeMeals(plan.mealsCount)}")
        }
    }
}

@Composable
private fun CardActionsMenu(enabled: Boolean, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            enabled = enabled,
        ) {
            Icon(
                imageVector = Icons.Filled.MoreHoriz,
                contentDescription = "Действия",
                tint = MutedText,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Удалить", color = NutritionDeleteRed) },
                onClick = {
                    expanded = false
                    onDelete()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = NutritionDeleteRed,
                    )
                },
            )
        }
    }
}

@Composable
private fun Chip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(NutritionChipBg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = NutritionChipText,
        )
    }
}

@Composable
private fun ConfirmDeleteDialog(
    planName: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = "Удалить рацион?") },
        text = {
            Text(
                text = "Это действие нельзя отменить. \"$planName\" будет удалён.",
                color = MutedText,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Удалить", color = NutritionDeleteRed, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = "Отмена", color = AccentOrange)
            }
        },
        containerColor = Color.White,
    )
}

/**
 * Russian pluralization for "приём пищи" / "приёма пищи" / "приёмов пищи".
 *
 * Russian uses three plural forms keyed on the trailing digits, with the
 * teens (11–14) treated as the "many" form regardless of the last digit.
 */
private fun pluralizeMeals(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod10 == 1 && mod100 != 11 -> "приём пищи"
        mod10 in 2..4 && mod100 !in 12..14 -> "приёма пищи"
        else -> "приёмов пищи"
    }
}
