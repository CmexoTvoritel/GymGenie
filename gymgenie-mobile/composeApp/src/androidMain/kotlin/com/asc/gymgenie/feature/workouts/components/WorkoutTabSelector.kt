package com.asc.gymgenie.feature.workouts.components

import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.asc.gymgenie.presentation.WorkoutsTab
import com.asc.gymgenie.ui.theme.OnSurfaceVariant
import com.asc.gymgenie.ui.theme.Primary

@Composable
fun WorkoutTabSelector(
    selectedTab: WorkoutsTab,
    onTabSelected: (WorkoutsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    TabRow(
        selectedTabIndex = if (selectedTab == WorkoutsTab.WORKOUTS) 0 else 1,
        containerColor = Color.Transparent,
        indicator = { tabPositions ->
            SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(
                    tabPositions[if (selectedTab == WorkoutsTab.WORKOUTS) 0 else 1],
                ),
                color = Primary,
            )
        },
        modifier = modifier,
    ) {
        Tab(
            selected = selectedTab == WorkoutsTab.WORKOUTS,
            onClick = { onTabSelected(WorkoutsTab.WORKOUTS) },
            text = {
                Text(
                    text = "Тренировки",
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedTab == WorkoutsTab.WORKOUTS) Primary else OnSurfaceVariant,
                )
            },
        )
        Tab(
            selected = selectedTab == WorkoutsTab.EXERCISES,
            onClick = { onTabSelected(WorkoutsTab.EXERCISES) },
            text = {
                Text(
                    text = "Упражнения",
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedTab == WorkoutsTab.EXERCISES) Primary else OnSurfaceVariant,
                )
            },
        )
    }
}
