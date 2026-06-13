package com.asc.gymgenie.feature.activities.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.R
import com.asc.gymgenie.activity.ActivityCatalogResponse
import com.asc.gymgenie.activity.ActivityKind
import com.asc.gymgenie.activity.ActivityRing
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.RingLife
import com.asc.gymgenie.ui.theme.RingMind
import com.asc.gymgenie.ui.theme.RingMove
import com.asc.gymgenie.ui.theme.SoftCard

@Composable
fun CatalogActivityCard(
    activity: ActivityCatalogResponse,
    isInPlan: Boolean,
    onToggle: () -> Unit,
) {
    val ringColor = ringColorFor(activity.ring)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(ringColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(
                    when (activity.ring) {
                        ActivityRing.MOVE.name -> R.drawable.ic_activity_run
                        ActivityRing.MIND.name -> R.drawable.ic_activity_mind
                        else -> R.drawable.ic_activity_schedule
                    }
                ),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = DeepInk,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = kindLabel(activity.kind),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = MutedText,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isInPlan) ringColor else SoftCard)
                .clickable { onToggle() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isInPlan) "✓" else "+",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isInPlan) Color.White else ringColor,
            )
        }
    }
}

internal fun ringColorFor(ring: String): Color = when (ring) {
    ActivityRing.MOVE.name -> RingMove
    ActivityRing.MIND.name -> RingMind
    ActivityRing.LIFE.name -> RingLife
    else -> AccentOrange
}

internal fun ringLabel(ring: ActivityRing): String = when (ring) {
    ActivityRing.MOVE -> "Движение"
    ActivityRing.MIND -> "Разум"
    ActivityRing.LIFE -> "Режим"
}

internal fun kindLabel(kind: String): String =
    when (runCatching { ActivityKind.valueOf(kind) }.getOrNull()) {
        ActivityKind.BINARY -> "Да/Нет"
        ActivityKind.COUNTER -> "Счётчик"
        ActivityKind.PRESET -> "Пресеты"
        null -> kind
    }
