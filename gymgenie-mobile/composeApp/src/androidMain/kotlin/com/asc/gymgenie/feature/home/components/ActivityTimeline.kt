package com.asc.gymgenie.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.asc.gymgenie.ui.theme.AccentGreen
import com.asc.gymgenie.ui.theme.OnBackground
import com.asc.gymgenie.ui.theme.Primary

@Composable
fun ActivityTimeline() {
    val activities = listOf(
        Triple("Бег", "\uD83C\uDFC3", Color(0xFFFF9800)),
        Triple("Йога", "\uD83E\uDDD8", Color(0xFF9C27B0)),
        Triple("Силовая", "\uD83C\uDFCB", Primary),
        Triple("Кардио", "\u2764\uFE0F", Color(0xFFF44336)),
        Triple("Растяжка", "\uD83E\uDD38", AccentGreen),
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(activities) { (title, emoji, color) ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(color.copy(alpha = 0.1f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = emoji, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnBackground,
                )
            }
        }
    }
}
