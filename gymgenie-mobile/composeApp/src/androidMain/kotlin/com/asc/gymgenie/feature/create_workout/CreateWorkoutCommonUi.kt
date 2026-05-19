package com.asc.gymgenie.feature.create_workout

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText

/**
 * Back row used at the top of every step of the create-workout flow.
 *
 * Kept as a small building block instead of a full top app bar because the
 * existing design language uses a subtle title/back combination rather than
 * a solid Material app bar.
 */
@Composable
internal fun CreateWorkoutTopBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    showCloseIcon: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (showCloseIcon) Icons.Filled.Close
                    else Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = if (showCloseIcon) "Закрыть" else "Назад",
                tint = DeepInk,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Step indicator shown under the toolbar on every numbered step of the
 * "add exercise" sub-flow.
 *
 * Renders the canonical "Шаг X из Y" label plus a coral progress bar whose
 * fill mirrors `currentStep / totalSteps`. The component is intentionally
 * non-scrollable: callers place it between the toolbar and the scrollable
 * content so the indicator stays pinned regardless of list size.
 */
@Composable
internal fun WorkoutFlowStepHeader(
    currentStep: Int,
    totalSteps: Int = 3,
    modifier: Modifier = Modifier,
) {
    val safeTotal = totalSteps.coerceAtLeast(1)
    val safeCurrent = currentStep.coerceIn(1, safeTotal)
    val fraction = safeCurrent.toFloat() / safeTotal.toFloat()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 8.dp),
    ) {
        Text(
            text = "Шаг $safeCurrent из $safeTotal",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(StepHeaderTrackColor),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Coral),
            )
        }
    }
}

private val StepHeaderTrackColor = Color(0xFFE0E0E0)

/**
 * Small stepper button used in the exercise config and rest time steppers.
 *
 * Disabled state uses a muted gray so "already at limit" is visually obvious.
 */
@Composable
internal fun StepperCircleButton(
    symbol: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (enabled) AccentOrange else MutedText.copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
