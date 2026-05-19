package com.asc.gymgenie.feature.workout_history.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val InkBlack = Color(0xFF0A0A0A)
private val MutedGray = Color(0xFF8B8B92)

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "🌙", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "День отдыха",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = InkBlack,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "В этот день тренировок не было",
            fontSize = 18.sp,
            color = MutedGray,
        )
    }
}
