package com.asc.gymgenie.feature.workout_session.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.OnBackground

@Composable
internal fun ElapsedTimer(
    elapsedSeconds: Int,
    paused: Boolean,
    onTogglePause: () -> Unit,
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimerBox(text = String.format("%02d", minutes), modifier = Modifier.weight(1f))
            Text(
                ":",
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = Coral,
            )
            TimerBox(text = String.format("%02d", seconds), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(elevation = 4.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(Color.White)
                .border(width = 1.5.dp, color = Coral, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = onTogglePause,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
            ) {
                Icon(
                    imageVector = if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (paused) "Возобновить" else "Пауза",
                    tint = Coral,
                )
            }
        }
    }
}
