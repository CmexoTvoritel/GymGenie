package com.asc.gymgenie.feature.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText

private val CardBorder = Color(0xFFEDEDEF)

/**
 * Empty-day placeholder used when the user has neither a recurring workout
 * for today nor any one-off plans queued up.
 */
@Composable
fun NoWorkoutPlaceholder(
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(
                border = BorderStroke(1.5.dp, CardBorder),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(vertical = 28.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF4F4F6)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "🏋", fontSize = 24.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Сегодня выходной",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DeepInk,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "На этот день тренировки не запланированы",
            fontSize = 13.sp,
            color = MutedText,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Coral)
                .clickable { onCreate() }
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Создать тренировку",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}
