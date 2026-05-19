package com.asc.gymgenie.feature.home.components.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.activity.ActivityTodayResponse
import com.asc.gymgenie.ui.theme.ActivityCardBorder
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.SoftCard
import com.asc.gymgenie.ui.theme.WarmOffWhite
import kotlin.math.roundToInt

private const val DEFAULT_MAX = 120

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PresetBottomSheet(
    activity: ActivityTodayResponse?,
    onDismiss: () -> Unit,
    onPick: (activityId: String, value: Int) -> Unit,
) {
    if (activity == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ringColor = ringColorFor(activity.ring)
    val maxValue = if ((activity.goal ?: 0) > 0) activity.goal!! else DEFAULT_MAX
    val unit = activity.unit.orEmpty()

    val sliderValue = remember { mutableFloatStateOf(activity.logValue.toFloat()) }

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

            Spacer(modifier = Modifier.height(24.dp))

            ValueLabel(
                value = sliderValue.floatValue.roundToInt(),
                unit = unit,
            )

            Slider(
                value = sliderValue.floatValue,
                onValueChange = { sliderValue.floatValue = it },
                valueRange = 0f..maxValue.toFloat(),
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = ringColor,
                    activeTrackColor = ringColor,
                    inactiveTrackColor = ringColor.copy(alpha = 0.15f),
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            QuickPickChips(
                maxValue = maxValue,
                unit = unit,
                selectedValue = sliderValue.floatValue.roundToInt(),
                ringColor = ringColor,
                onSelect = { sliderValue.floatValue = it.toFloat() },
            )

            Spacer(modifier = Modifier.height(24.dp))

            SaveButton(
                ringColor = ringColor,
                onClick = {
                    onPick(activity.activityId, sliderValue.floatValue.roundToInt())
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

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
private fun ValueLabel(value: Int, unit: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value.toString(),
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepInk,
            )
            if (unit.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = unit,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MutedText,
                    modifier = Modifier.padding(bottom = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun QuickPickChips(
    maxValue: Int,
    unit: String,
    selectedValue: Int,
    ringColor: Color,
    onSelect: (Int) -> Unit,
) {
    val chips = listOf(
        maxValue / 4,
        maxValue / 2,
        maxValue * 3 / 4,
        maxValue,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chipValue ->
            QuickPickChip(
                value = chipValue,
                unit = unit,
                isSelected = selectedValue == chipValue,
                ringColor = ringColor,
                onClick = { onSelect(chipValue) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickPickChip(
    value: Int,
    unit: String,
    isSelected: Boolean,
    ringColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    val background = if (isSelected) ringColor else Color.White
    val textColor = if (isSelected) Color.White else DeepInk

    val label = if (unit.isNotBlank()) "$value $unit" else value.toString()

    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .let { if (!isSelected) it.border(1.5.dp, ActivityCardBorder, shape) else it }
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun SaveButton(ringColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ringColor)
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Сохранить",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
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
