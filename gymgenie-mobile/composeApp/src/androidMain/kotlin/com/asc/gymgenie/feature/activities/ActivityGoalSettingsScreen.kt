package com.asc.gymgenie.feature.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.WarmOffWhite

/**
 * Configuration for a single activity goal.
 *
 * Kept as a UI-only model local to this feature because the screen is purely
 * local-state (no backend persistence yet).
 */
data class GoalCategory(
    val emoji: String,
    val title: String,
    val unit: String,
    val defaultValue: Int,
    val step: Int,
) {
    /**
     * Upper bound for the goal counter. Derived from the unit so the UI cannot
     * produce silly values (e.g. 100000 steps or 500 glasses of water).
     */
    val maxValue: Int
        get() = when (unit) {
            "шагов" -> 50_000
            "стаканов" -> 20
            "часов" -> 24
            "минут" -> 180
            else -> 999
        }
}

private val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
private val reminderIntervals = listOf(1, 2, 3, 4)

@Composable
fun ActivityGoalSettingsScreen(
    category: GoalCategory,
    onBack: () -> Unit,
    onConfirm: (GoalCategory) -> Unit = { onBack() },
) {
    var value by rememberSaveable(category.title) { mutableStateOf(category.defaultValue) }
    var remindersEnabled by rememberSaveable { mutableStateOf(true) }
    var intervalHours by rememberSaveable { mutableStateOf(2) }
    var selectedDays by rememberSaveable { mutableStateOf(daysOfWeek.toSet()) }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 24.dp),
        ) {
            TopBar(onBack = onBack)

            Spacer(modifier = Modifier.height(8.dp))

            HeroSection(emoji = category.emoji, title = category.title)

            Spacer(modifier = Modifier.height(24.dp))

            GoalCounterCard(
                value = value,
                unit = category.unit,
                onDecrement = {
                    val next = (value - category.step).coerceAtLeast(1)
                    value = next
                },
                onIncrement = {
                    val next = (value + category.step).coerceAtMost(category.maxValue)
                    value = next
                },
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            RemindersCard(
                remindersEnabled = remindersEnabled,
                onToggle = { remindersEnabled = it },
                intervalHours = intervalHours,
                onIntervalChange = { intervalHours = it },
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            DaysOfWeekCard(
                selectedDays = selectedDays,
                onToggleDay = { day ->
                    selectedDays = if (day in selectedDays) {
                        selectedDays - day
                    } else {
                        selectedDays + day
                    }
                },
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    onConfirm(
                        category.copy(defaultValue = value),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentOrange,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text = "Добавить активность",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Text(
                    text = "Отмена",
                    color = MutedText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Настройка цели",
            color = DeepInk,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "‹",
                color = DeepInk,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun HeroSection(emoji: String, title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = emoji,
            fontSize = 72.sp,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = title,
            color = DeepInk,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun GoalCounterCard(
    value: Int,
    unit: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WhiteCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Цель на день",
                color = MutedText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CircleStepButton(symbol = "−", onClick = onDecrement)

                Text(
                    text = value.toString(),
                    color = DeepInk,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                )

                CircleStepButton(symbol = "+", onClick = onIncrement)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = unit,
                color = MutedText,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun CircleStepButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(AccentOrange)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() },
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

@Composable
private fun RemindersCard(
    remindersEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    intervalHours: Int,
    onIntervalChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    WhiteCard(modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Включить напоминания",
                    color = DeepInk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )

                Switch(
                    checked = remindersEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccentOrange,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = MutedText.copy(alpha = 0.4f),
                    ),
                )
            }

            if (remindersEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Интервал",
                    color = MutedText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    reminderIntervals.forEach { hours ->
                        IntervalChip(
                            label = "${hours}ч",
                            isSelected = hours == intervalHours,
                            onClick = { onIntervalChange(hours) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "с 08:00",
                        color = DeepInk,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "до 22:00",
                        color = DeepInk,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun IntervalChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val backgroundModifier = if (isSelected) {
        Modifier
            .clip(shape)
            .background(AccentOrange)
    } else {
        Modifier
            .clip(shape)
            .border(1.dp, MutedText.copy(alpha = 0.35f), shape)
    }

    Box(
        modifier = backgroundModifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else DeepInk,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DaysOfWeekCard(
    selectedDays: Set<String>,
    onToggleDay: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    WhiteCard(modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Дни недели",
                color = DeepInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                daysOfWeek.forEach { day ->
                    DayChip(
                        label = day,
                        isSelected = day in selectedDays,
                        onClick = { onToggleDay(day) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val baseModifier = Modifier
        .size(36.dp)
        .clip(CircleShape)

    val styledModifier = if (isSelected) {
        baseModifier.background(AccentOrange)
    } else {
        baseModifier.border(1.dp, MutedText.copy(alpha = 0.35f), CircleShape)
    }

    Box(
        modifier = styledModifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else DeepInk,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun WhiteCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = DeepInk.copy(alpha = 0.06f),
                spotColor = DeepInk.copy(alpha = 0.08f),
            )
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White),
    ) {
        content()
    }
}
