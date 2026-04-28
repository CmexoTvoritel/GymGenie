package com.asc.gymgenie.feature.workouts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.SoftCard

private val MuscleGroups: List<Pair<String?, String>> = listOf(
    null to "Все",
    "CHEST" to "Грудь",
    "BACK" to "Спина",
    "SHOULDERS" to "Плечи",
    "QUADRICEPS" to "Ноги",
    "ABS" to "Пресс",
    "BICEPS" to "Руки",
    "CARDIO" to "Кардио",
    "FULL_BODY" to "Всё тело",
)

@Composable
fun MuscleGroupFilterChips(
    selected: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MuscleGroups.forEach { (value, label) ->
            val isSelected = selected == value
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Color.White else DeepInk,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) AccentOrange else SoftCard)
                    .clickable { onSelected(value) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}
