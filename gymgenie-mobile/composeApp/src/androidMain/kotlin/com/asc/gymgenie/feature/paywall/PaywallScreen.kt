package com.asc.gymgenie.feature.paywall

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.feature.paywall.components.FeatureListItem
import com.asc.gymgenie.feature.paywall.components.PlanCard
import com.asc.gymgenie.presentation.PaywallViewModel
import com.asc.gymgenie.presentation.PlanType
import com.asc.gymgenie.ui.components.GymGenieButton
import com.asc.gymgenie.ui.theme.OnBackground

@Composable
fun PaywallScreen(
    onPurchaseSuccess: () -> Unit,
    onSkip: () -> Unit,
) {
    val viewModel = remember { PaywallViewModel() }

    DisposableEffect(Unit) {
        onDispose { viewModel.onCleared() }
    }

    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.purchaseSuccess) {
        if (state.purchaseSuccess) {
            onPurchaseSuccess()
            viewModel.consumePurchaseSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top bar
        TopBar(onSkip = onSkip)

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
            isSelected = state.selectedPlan == PlanType.MONTHLY,
            title = "1 Месяц",
            price = "$7.99 / МО",
            onClick = { viewModel.selectPlan(PlanType.MONTHLY) },
        )

        Spacer(modifier = Modifier.height(12.dp))

        PlanCard(
            isSelected = state.selectedPlan == PlanType.YEARLY,
            title = "1 Год",
            price = "$59.99",
            originalPrice = "$95.88",
            badge = "ПОПУЛЯРНО",
            subtitle = "$4.99 / МО",
            onClick = { viewModel.selectPlan(PlanType.YEARLY) },
        )

        Spacer(modifier = Modifier.height(32.dp))

        GymGenieButton(
            text = "Получить полный доступ",
            onClick = { viewModel.purchase() },
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
private fun TopBar(onSkip: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Восстановить",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.clickable {
                // TODO:GymGenie - Replace with real restore logic
            },
        )
    }
}

@Composable
private fun FeaturesList() {
    val features = listOf(
        "Функция ИИ",
        "Функция \u00ABПлан\u00BB",
        "Функция \u00ABСледующий\u00BB",
        "Функция \u00ABЕщё\u00BB",
    )

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        features.forEachIndexed { index, feature ->
            FeatureListItem(
                text = feature,
                isFirst = index == 0,
                isLast = index == features.size - 1,
            )
        }
    }
}
