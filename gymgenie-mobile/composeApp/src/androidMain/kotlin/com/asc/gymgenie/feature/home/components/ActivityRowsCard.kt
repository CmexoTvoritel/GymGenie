package com.asc.gymgenie.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.asc.gymgenie.activity.ActivityTodayResponse
import com.asc.gymgenie.feature.home.components.activities.ActivityCard
import com.asc.gymgenie.feature.home.components.activities.FILTER_ALL
import com.asc.gymgenie.feature.home.components.activities.FilterChipsRow
import com.asc.gymgenie.feature.home.components.activities.PresetBottomSheet

@Composable
fun ActivityRowsCard(
    activities: List<ActivityTodayResponse>,
    onCheckIn: (activityId: String, value: Int) -> Unit,
) {
    var activeFilter by rememberSaveable { mutableStateOf(FILTER_ALL) }
    var presetTarget by remember { mutableStateOf<ActivityTodayResponse?>(null) }

    val visible = remember(activities, activeFilter) {
        if (activeFilter == FILTER_ALL) activities
        else activities.filter { it.ring == activeFilter }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FilterChipsRow(
            active = activeFilter,
            onSelect = { activeFilter = it },
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            visible.forEach { activity ->
                ActivityCard(
                    activity = activity,
                    onCheckIn = onCheckIn,
                    onOpenPreset = { presetTarget = activity },
                )
            }
        }
    }

    PresetBottomSheet(
        activity = presetTarget,
        onDismiss = { presetTarget = null },
        onPick = { id, value ->
            onCheckIn(id, value)
            presetTarget = null
        },
    )
}
