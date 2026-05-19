package com.asc.gymgenie.feature.activities.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.PillBg

enum class ActivityTab { PLAN, HISTORY }

private val Tabs: List<Pair<ActivityTab, String>> = listOf(
    ActivityTab.PLAN to "План активностей",
    ActivityTab.HISTORY to "История",
)

@Composable
fun ActivityTabSelector(
    selectedTab: ActivityTab,
    onTabSelected: (ActivityTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = Tabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0)
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        label = "tabPillIndicator",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(PillBg)
            .padding(3.dp),
    ) {
        val density = LocalDensity.current
        val tabWidthDp = with(density) { (constraints.maxWidth.toFloat() / Tabs.size).toDp() }

        Box(
            modifier = Modifier
                .width(tabWidthDp)
                .fillMaxHeight()
                .offset(x = tabWidthDp * animatedIndex)
                .clip(RoundedCornerShape(50))
                .background(DeepInk),
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tabs.forEachIndexed { index, (tab, label) ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .clickable { onTabSelected(tab) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) Color.White else MutedText,
                    )
                }
            }
        }
    }
}
