package com.asc.gymgenie.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.activity.ActivityRing
import com.asc.gymgenie.activity.ActivityTodayResponse
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.RingLife
import com.asc.gymgenie.ui.theme.RingMind
import com.asc.gymgenie.ui.theme.RingMove
import com.asc.gymgenie.ui.theme.SoftCard
import com.asc.gymgenie.ui.theme.WarmOffWhite

/**
 * Modal bottom sheet with quick preset values for a PRESET activity.
 *
 * Renders nothing while [activity] is `null`; the parent owns the visibility
 * state. Picking a preset calls [onPick] with the activity id and the picked
 * value; the parent is expected to dismiss after handling the pick.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetBottomSheet(
    activity: ActivityTodayResponse?,
    onDismiss: () -> Unit,
    onPick: (activityId: String, value: Int) -> Unit,
) {
    if (activity == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ringColor = ringColorFor(activity.ring)
    val presets = activity.presets.orEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WarmOffWhite,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Header(activity = activity, ringColor = ringColor)

            Spacer(modifier = Modifier.height(20.dp))

            if (presets.isEmpty()) {
                // Defensive: server contract says PRESET activities always
                // ship a preset list, but if a malformed payload sneaks
                // through we still want to render something useful instead
                // of an empty sheet.
                Text(
                    text = "Нет вариантов для быстрого выбора",
                    fontSize = 14.sp,
                    color = MutedText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(presets) { preset ->
                        PresetTile(
                            value = preset,
                            unit = activity.unit.orEmpty(),
                            ringColor = ringColor,
                            onClick = { onPick(activity.activityId, preset) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CancelButton(onClick = onDismiss)

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Header(activity: ActivityTodayResponse, ringColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(ringColor.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = activity.name.take(1).uppercase(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ringColor,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = activity.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepInk,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Сколько сегодня?",
                fontSize = 13.sp,
                color = MutedText,
            )
        }
    }
}

@Composable
private fun PresetTile(
    value: Int,
    unit: String,
    ringColor: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ringColor.copy(alpha = 0.10f))
            .border(1.5.dp, ringColor.copy(alpha = 0.20f), shape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DeepInk,
        )
        if (unit.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = unit,
                fontSize = 12.sp,
                color = MutedText,
            )
        }
    }
}

@Composable
private fun CancelButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SoftCard)
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Отмена",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DeepInk,
        )
    }
}

private fun ringColorFor(ring: String): Color = when (ring) {
    ActivityRing.MOVE.name -> RingMove
    ActivityRing.MIND.name -> RingMind
    ActivityRing.LIFE.name -> RingLife
    else -> DeepInk
}
