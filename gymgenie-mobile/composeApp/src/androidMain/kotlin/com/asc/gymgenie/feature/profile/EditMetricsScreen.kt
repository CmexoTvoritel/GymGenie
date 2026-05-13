package com.asc.gymgenie.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.asc.gymgenie.ui.components.GymGenieToolbar
import com.asc.gymgenie.ui.theme.WarmOffWhite

@Composable
fun EditMetricsScreen(
    form: EditFormHolder,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        GymGenieToolbar(
            title = "Параметры",
            showBackNavigation = true,
            onBackClick = onBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            SliderField(
                label = "Возраст",
                value = form.ageYears,
                min = 10,
                max = 80,
                onChange = { form.ageYears = it },
            )
            SliderField(
                label = "Рост (см)",
                value = form.heightCm,
                min = 100,
                max = 220,
                onChange = { form.heightCm = it },
            )
            SliderField(
                label = "Вес (кг)",
                value = form.weightKg,
                min = 30,
                max = 150,
                onChange = { form.weightKg = it },
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
        StepBottomButton(text = "Готово", onClick = onBack)
    }
}
