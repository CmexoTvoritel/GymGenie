package com.asc.gymgenie.feature.workouts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText

@Composable
fun ExercisesEmptyState(
    hasFilter: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "📦", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasFilter) "Ничего не найдено" else "Каталог пуст",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepInk,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (hasFilter) "Попробуйте изменить запрос или фильтр" else "Упражнения скоро появятся",
            fontSize = 13.sp,
            color = MutedText,
            textAlign = TextAlign.Center,
        )
    }
}
