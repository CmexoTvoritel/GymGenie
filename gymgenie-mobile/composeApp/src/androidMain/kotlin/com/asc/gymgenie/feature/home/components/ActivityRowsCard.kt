package com.asc.gymgenie.feature.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.R
import com.asc.gymgenie.activity.ActivityTodayResponse
import com.asc.gymgenie.feature.home.components.activities.ActivityCard
import com.asc.gymgenie.feature.home.components.activities.FILTER_ALL
import com.asc.gymgenie.feature.home.components.activities.FilterChipsRow
import com.asc.gymgenie.feature.home.components.activities.PresetBottomSheet
import com.asc.gymgenie.ui.theme.DeepInk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityRowsCard(
    activities: List<ActivityTodayResponse>,
    onCheckIn: (activityId: String, value: Int) -> Unit,
    onRemoveFromPlan: ((String) -> Unit)? = null,
    onOpenScheduleSettings: ((ActivityTodayResponse) -> Unit)? = null,
) {
    var activeFilter by rememberSaveable { mutableStateOf(FILTER_ALL) }
    var presetTarget by remember { mutableStateOf<ActivityTodayResponse?>(null) }
    var deleteTarget by remember { mutableStateOf<ActivityTodayResponse?>(null) }

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
                    onLongPress = if (onRemoveFromPlan != null) {
                        { deleteTarget = activity }
                    } else null,
                    onOpenScheduleSettings = if (onOpenScheduleSettings != null) {
                        { onOpenScheduleSettings(activity) }
                    } else null,
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

    if (deleteTarget != null) {
        ModalBottomSheet(
            onDismissRequest = { deleteTarget = null },
            containerColor = Color.White,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    text = deleteTarget!!.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepInk,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFFF0EE))
                        .clickable {
                            onRemoveFromPlan?.invoke(deleteTarget!!.activityId)
                            deleteTarget = null
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color(0xFFD94444)),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Удалить из плана",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFD94444),
                    )
                }
            }
        }
    }
}
