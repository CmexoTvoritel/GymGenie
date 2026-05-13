package com.asc.gymgenie.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.components.GymGenieToolbar
import com.asc.gymgenie.ui.theme.WarmOffWhite

private val EditBlack = Color(0xFF0A0A0A)

@Composable
fun EditExperienceScreen(
    form: EditFormHolder,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        GymGenieToolbar(
            title = "Опыт",
            showBackNavigation = true,
            onBackClick = onBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            EditFieldLabel("КАК ДАВНО ВЫ ЗАНИМАЕТЕСЬ")
            PickerGroup(
                options = listOf("Давно", "Недавно", "Не занимался"),
                selected = form.experience,
                onSelect = { form.experience = it },
            )
            Spacer(modifier = Modifier.height(24.dp))
            EditFieldLabel("КАК ЧАСТО ВЫ ЗАНИМАЕТЕСЬ")
            PickerGroup(
                options = listOf("Часто", "Редко", "Не занимался"),
                selected = form.frequency,
                onSelect = { form.frequency = it },
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
        StepBottomButton(text = "Готово", onClick = onBack)
    }
}

@Composable
private fun EditFieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = EditBlack,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}
