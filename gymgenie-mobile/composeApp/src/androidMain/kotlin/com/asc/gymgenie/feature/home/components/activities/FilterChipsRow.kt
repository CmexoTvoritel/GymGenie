package com.asc.gymgenie.feature.home.components.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
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
import com.asc.gymgenie.activity.ActivityRing
import com.asc.gymgenie.ui.theme.ActivityCardBorder
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.RingLife
import com.asc.gymgenie.ui.theme.RingMind
import com.asc.gymgenie.ui.theme.RingMove

internal const val FILTER_ALL = "ALL"

@Composable
internal fun FilterChipsRow(
    active: String,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) {
        item {
            FilterChip(
                label = "Все",
                isActive = active == FILTER_ALL,
                activeColor = DeepInk,
                onClick = { onSelect(FILTER_ALL) },
            )
        }
        item {
            FilterChip(
                label = "Движение",
                isActive = active == ActivityRing.MOVE.name,
                activeColor = RingMove,
                onClick = { onSelect(ActivityRing.MOVE.name) },
            )
        }
        item {
            FilterChip(
                label = "Разум",
                isActive = active == ActivityRing.MIND.name,
                activeColor = RingMind,
                onClick = { onSelect(ActivityRing.MIND.name) },
            )
        }
        item {
            FilterChip(
                label = "Режим",
                isActive = active == ActivityRing.LIFE.name,
                activeColor = RingLife,
                onClick = { onSelect(ActivityRing.LIFE.name) },
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val background = if (isActive) activeColor else Color.White
    val textColor = if (isActive) Color.White else DeepInk

    Row(
        modifier = Modifier
            .clip(shape)
            .background(background)
            .let { if (!isActive) it.border(1.5.dp, ActivityCardBorder, shape) else it }
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
    }
}
