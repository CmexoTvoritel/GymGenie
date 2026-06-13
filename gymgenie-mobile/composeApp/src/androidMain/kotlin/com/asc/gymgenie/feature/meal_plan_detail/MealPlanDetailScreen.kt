package com.asc.gymgenie.feature.meal_plan_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.feature.meal_plan_detail.components.BottomActionBar
import com.asc.gymgenie.feature.meal_plan_detail.components.HeroMacrosCard
import com.asc.gymgenie.feature.meal_plan_detail.components.ProductCard
import com.asc.gymgenie.feature.meal_plan_detail.components.ScheduleChip
import com.asc.gymgenie.nutrition.AiMealType
import com.asc.gymgenie.nutrition.MealPlanDetail
import com.asc.gymgenie.nutrition.MealPlanDetailDish
import com.asc.gymgenie.nutrition.MealPlanDetailViewModel
import com.asc.gymgenie.ui.components.GymGenieToolbar
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.WarmOffWhite
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

@Composable
fun MealPlanDetailScreen(
    planId: String,
    isPastDate: Boolean = false,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val koin = remember { GlobalContext.get() }
    val viewModel = remember { koin.get<MealPlanDetailViewModel> { parametersOf(planId) } }
    DisposableEffect(Unit) { onDispose { viewModel.onCleared() } }

    LaunchedEffect(Unit) { viewModel.load() }

    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onDeleted()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val plan = state.plan
            val titleText = if (plan != null) {
                val firstMealType = plan.meals.firstOrNull()?.mealType
                AiMealType.fromWireValue(firstMealType)?.displayName ?: plan.name
            } else {
                ""
            }

            GymGenieToolbar(
                title = titleText,
                showBackNavigation = true,
                onBackClick = onBack,
            )

            when {
                state.isLoading && plan == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Coral)
                    }
                }

                state.errorMessage != null && plan == null -> {
                    ErrorState(
                        message = state.errorMessage ?: "",
                        onRetry = viewModel::retry,
                    )
                }

                plan != null -> {
                    DetailContent(
                        plan = plan,
                        onDelete = { showDeleteDialog = true },
                        onEdit = onEdit,
                        showBottomBar = !isPastDate,
                        modifier = Modifier.weight(1f),
                    )
                }

                else -> {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }

        if (showDeleteDialog) {
            DeleteConfirmDialog(
                isDeleting = state.isDeleting,
                onConfirm = { viewModel.delete() },
                onCancel = { showDeleteDialog = false },
            )
        }
    }
}

@Composable
private fun DetailContent(
    plan: MealPlanDetail,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    showBottomBar: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val allDishes = plan.meals.flatMap { it.dishes }
    val totalKcal = allDishes.sumOf { it.calories?.toInt() ?: 0 }
    val totalProtein = allDishes.sumOf { it.proteinG?.toInt() ?: 0 }
    val totalFat = allDishes.sumOf { it.fatG?.toInt() ?: 0 }
    val totalCarbs = allDishes.sumOf { it.carbsG?.toInt() ?: 0 }

    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ScheduleChip(plan = plan)
            }

            item {
                HeroMacrosCard(
                    totalKcal = totalKcal,
                    proteinG = totalProtein,
                    fatG = totalFat,
                    carbsG = totalCarbs,
                )
            }

            item {
                Text(
                    text = "ПРОДУКТЫ: ${allDishes.size} ${productDeclension(allDishes.size)}",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepInk,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            items(items = allDishes, key = { it.id }) { dish ->
                ProductCard(dish = dish)
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (showBottomBar) {
            BottomActionBar(
                onDelete = onDelete,
                onEdit = onEdit,
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Warning,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = AccentOrange,
        )
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
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Повторить", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onCancel() },
        title = {
            Text(
                text = "Удалить план питания?",
                color = DeepInk,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = "План питания и все добавленные продукты будут удалены. Действие нельзя отменить.",
                color = MutedText,
                fontSize = 15.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isDeleting) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        color = Color(0xFFE94A2C),
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text(
                    text = "Удалить",
                    color = Color(0xFFE94A2C),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !isDeleting) {
                Text(
                    text = "Отмена",
                    color = DeepInk,
                    fontSize = 15.sp,
                )
            }
        },
        containerColor = Color.White,
    )
}

private fun productDeclension(count: Int): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..19 -> "продуктов"
        mod10 == 1 -> "продукт"
        mod10 in 2..4 -> "продукта"
        else -> "продуктов"
    }
}
