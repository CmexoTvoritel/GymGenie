package com.asc.gymgenie.feature.paywall

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.gymgenie.ui.components.GymGenieButton
import com.asc.gymgenie.ui.theme.AccentGreen
import com.asc.gymgenie.ui.theme.OnBackground

@Composable
fun PaywallScreen(
    onPurchaseSuccess: () -> Unit,
    onSkip: () -> Unit,
    viewModel: PaywallViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Close button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.15f))
                    .clickable { onSkip() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\u2715",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnBackground,
                )
            }

            // Restore button
            Text(
                text = "Восстановить",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.clickable {
                    // TODO:GymGenie - Replace with real restore logic
                },
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = "Получи доступ ко всем функциям",
            style = MaterialTheme.typography.headlineMedium,
            color = OnBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Features
        FeaturesList()

        Spacer(modifier = Modifier.height(32.dp))

        // Plans
        PlanCard(
            isSelected = state.selectedPlan == PaywallPlan.MONTHLY,
            title = "1 Месяц",
            price = "$7.99 / МО",
            originalPrice = null,
            badge = null,
            subtitle = null,
            onClick = { viewModel.selectPlan(PaywallPlan.MONTHLY) },
        )

        Spacer(modifier = Modifier.height(12.dp))

        PlanCard(
            isSelected = state.selectedPlan == PaywallPlan.YEARLY,
            title = "1 Год",
            price = "$59.99",
            originalPrice = "$95.88",
            badge = "ПОПУЛЯРНО",
            subtitle = "$4.99 / МО",
            onClick = { viewModel.selectPlan(PaywallPlan.YEARLY) },
        )

        Spacer(modifier = Modifier.height(32.dp))

        GymGenieButton(
            text = "Получить полный доступ",
            onClick = { viewModel.purchase(onSuccess = onPurchaseSuccess) },
            isLoading = state.isPurchasing,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Автопродление. Отмена в любое время.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FeaturesList() {
    val features = listOf(
        "Функция ИИ",
        "Функция «План»",
        "Функция «Следующий»",
        "Функция «Ещё»",
    )

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        features.forEachIndexed { index, feature ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Dot and line
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(12.dp)
                                .background(AccentGreen.copy(alpha = 0.3f)),
                        )
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(AccentGreen),
                    )

                    if (index < features.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(12.dp)
                                .background(AccentGreen.copy(alpha = 0.3f)),
                        )
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = feature,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnBackground,
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    isSelected: Boolean,
    title: String,
    price: String,
    originalPrice: String?,
    badge: String?,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp),
                    )
                } else {
                    Modifier
                },
            )
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            if (badge != null) {
                Text(
                    text = badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AccentGreen)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = OnBackground,
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (originalPrice != null) {
                    Text(
                        text = originalPrice,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textDecoration = TextDecoration.LineThrough,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = price,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground,
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color.Gray,
                )
            }
        }
    }
}
