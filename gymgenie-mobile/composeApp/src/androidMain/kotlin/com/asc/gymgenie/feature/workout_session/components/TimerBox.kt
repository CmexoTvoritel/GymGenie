package com.asc.gymgenie.feature.workout_session.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.theme.CoralLight
import com.asc.gymgenie.ui.theme.OnBackground

@Composable
internal fun TimerBox(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(88.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CoralLight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
            color = OnBackground,
        )
    }
}
