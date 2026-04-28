package com.asc.gymgenie.feature.activities

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.ActivityMeditation
import com.asc.gymgenie.ui.theme.ActivityStretching
import com.asc.gymgenie.ui.theme.ActivityWalking
import com.asc.gymgenie.ui.theme.ActivityWater
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.RingMovement
import com.asc.gymgenie.ui.theme.SoftCard
import com.asc.gymgenie.ui.theme.WarmOffWhite

private enum class ActivityTab { TODAY, HISTORY }

private data class ActivityListRowUi(
    val emoji: String,
    val title: String,
    val unit: String,
    val current: Double,
    val total: Double,
    val color: Color,
) {
    val progress: Float
        get() = if (total <= 0.0) 0f else (current / total).toFloat().coerceIn(0f, 1f)

    val percent: Int
        get() = (progress * 100f).toInt()

    val formattedProgress: String
        get() = "${format(current)} / ${format(total)} $unit"

    private fun format(value: Double): String {
        val isInt = value % 1.0 == 0.0
        return if (isInt) {
            // Insert thin-space thousand separators, e.g. 10 000
            val longValue = value.toLong()
            val raw = longValue.toString()
            buildString {
                raw.reversed().forEachIndexed { idx, c ->
                    if (idx != 0 && idx % 3 == 0) insert(0, ' ')
                    insert(0, c)
                }
            }
        } else {
            String.format("%.1f", value)
        }
    }
}

// TODO: hardcoded mock rows until the backend exposes an activity feed endpoint.
private val todayRows = listOf(
    ActivityListRowUi("🚶", "Шаги", "шагов", 6_420.0, 10_000.0, ActivityWalking),
    ActivityListRowUi("💧", "Вода", "стаканов", 5.0, 8.0, ActivityWater),
    ActivityListRowUi("🔥", "Калории", "ккал", 1_200.0, 2_000.0, RingMovement),
    ActivityListRowUi("😴", "Сон", "часов", 7.0, 8.0, ActivityMeditation),
    ActivityListRowUi("🧘", "Медитация", "минут", 0.0, 15.0, ActivityStretching),
)

@Composable
fun ActivitiesScreen(
    onBack: () -> Unit,
    onOpenCatalog: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(ActivityTab.TODAY) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .statusBarsPadding(),
    ) {
        TopBar(onBack = onBack, onAdd = onOpenCatalog)
        TabBar(selected = selectedTab, onSelect = { selectedTab = it })

        when (selectedTab) {
            ActivityTab.TODAY -> TodayList()
            ActivityTab.HISTORY -> HistoryPlaceholder()
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SoftCard)
                .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "‹", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DeepInk)
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Активности",
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DeepInk,
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AccentOrange)
                .clickable { onAdd() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun TabBar(selected: ActivityTab, onSelect: (ActivityTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        TabButton(title = "Сегодня", isSelected = selected == ActivityTab.TODAY) {
            onSelect(ActivityTab.TODAY)
        }
        TabButton(title = "История", isSelected = selected == ActivityTab.HISTORY) {
            onSelect(ActivityTab.HISTORY)
        }
    }
}

@Composable
private fun TabButton(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) DeepInk else MutedText,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (isSelected) AccentOrange else Color.Transparent),
        )
    }
}

@Composable
private fun TodayList() {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(todayRows) { row ->
            RowCard(row = row)
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun RowCard(row: ActivityListRowUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularIndicator(row = row)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = row.formattedProgress,
                fontSize = 13.sp,
                color = MutedText,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(AccentOrange)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                text = "${row.percent}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun CircularIndicator(row: ActivityListRowUi) {
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(48.dp)) {
            val stroke = 3.dp.toPx()
            val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            val arcSize = Size(size.width - stroke, size.height - stroke)

            drawArc(
                color = row.color.copy(alpha = 0.3f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, pathEffect = dash),
            )
            drawArc(
                color = row.color,
                startAngle = -90f,
                sweepAngle = 360f * row.progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(text = row.emoji, fontSize = 20.sp)
    }
}

@Composable
private fun HistoryPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SoftCard)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "⌛", fontSize = 32.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "История ещё пуста",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepInk,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Отмечай активности — они появятся здесь",
                fontSize = 13.sp,
                color = MutedText,
            )
        }
    }
}
