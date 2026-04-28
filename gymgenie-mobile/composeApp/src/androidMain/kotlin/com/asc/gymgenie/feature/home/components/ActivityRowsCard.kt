package com.asc.gymgenie.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.theme.ActivityMeditation
import com.asc.gymgenie.ui.theme.ActivityStretching
import com.asc.gymgenie.ui.theme.ActivityWalking
import com.asc.gymgenie.ui.theme.ActivityWater
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.SoftCard

private data class ActivityRowUi(
    val emoji: String,
    val title: String,
    val progressLabel: String,
    val progress: Float,
    val color: Color,
)

/**
 * "Quick log" activity rows, grouped in a single soft card.
 *
 * TODO: replace hardcoded rows with the user's real activity feed once the
 * backend endpoint is exposed.
 */
@Composable
fun ActivityRowsCard() {
    val rows = defaultRows
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SoftCard)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        rows.forEach { row ->
            ActivityRowItem(row = row)
        }
    }
}

private val defaultRows: List<ActivityRowUi> = listOf(
    ActivityRowUi("💧", "Вода", "1.2 / 2.5 л", 0.48f, ActivityWater),
    ActivityRowUi("🚶", "Ходьба", "20 / 45 мин", 0.44f, ActivityWalking),
    ActivityRowUi("🤸", "Растяжка", "0 / 1 раз", 0f, ActivityStretching),
    ActivityRowUi("🧘", "Медитация", "5 / 10 мин", 0.5f, ActivityMeditation),
)

@Composable
private fun ActivityRowItem(row: ActivityRowUi) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(row.color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = row.emoji, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = row.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepInk,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = row.progressLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MutedText,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            ProgressBar(progress = row.progress, color = row.color)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(DeepInk)
                .clickable { /* TODO: open quick-log sheet */ },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun ProgressBar(progress: Float, color: Color) {
    val safe = progress.coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = safe)
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}
