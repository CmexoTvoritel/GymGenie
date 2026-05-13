package com.asc.gymgenie.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.components.GymGenieToolbar
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.WarmOffWhite

private val EditBlack = Color(0xFF0A0A0A)
private val EditBorder = Color(0xFFEDEDEF)
private val EditMuted = Color(0xFF8B8B92)

@Composable
fun EditHealthScreen(
    form: EditFormHolder,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        GymGenieToolbar(
            title = "Здоровье",
            showBackNavigation = true,
            onBackClick = onBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            EditFieldLabel("ОГРАНИЧЕНИЯ ПО ЗДОРОВЬЮ?")
            PickerGroup(
                options = listOf("Да", "Нет"),
                selected = if (form.hasHealthIssues) "Да" else "Нет",
                onSelect = { selected ->
                    val enabled = selected == "Да"
                    form.hasHealthIssues = enabled
                    if (!enabled) form.healthIssues = ""
                },
            )
            if (form.hasHealthIssues) {
                Spacer(modifier = Modifier.height(24.dp))
                EditFieldLabel("ОПИШИТЕ ПОДРОБНЕЕ")
                OutlinedTextField(
                    value = form.healthIssues,
                    onValueChange = { form.healthIssues = it },
                    placeholder = { Text("Например: грыжа позвоночника", color = EditMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Coral,
                        unfocusedBorderColor = EditBorder,
                    ),
                )
            }
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
