package com.asc.gymgenie.feature.workouts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.components.GymGenieButton
import com.asc.gymgenie.ui.theme.AccentOrange

private val SectionTitleColor = Color(0xFF0A0A0A)
private val SecondaryText = Color(0xFF4C4C53)
private val ChipNeutralBackground = Color(0xFFF5F5F5)
private val ChipNeutralBorder = Color(0xFFE0E0E0)
private val ChipNeutralText = Color(0xFF666666)

private val BeginnerColor = Color(0xFF22A06B)
private val IntermediateColor = Color(0xFFE89B12)
private val AdvancedColor = Color(0xFFD14343)

private const val SORT_DESC = "DESC"
private const val SORT_ASC = "ASC"

private data class DifficultyOption(
    val value: String,
    val label: String,
    val color: Color,
)

private val DifficultyOptions: List<DifficultyOption> = listOf(
    DifficultyOption("BEGINNER", "Легко", BeginnerColor),
    DifficultyOption("INTERMEDIATE", "Средне", IntermediateColor),
    DifficultyOption("ADVANCED", "Сложно", AdvancedColor),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseFilterBottomSheet(
    currentDifficulties: List<String>,
    currentRequiresEquipment: Boolean?,
    currentSortByDifficulty: String?,
    currentSortByCalories: String?,
    onApply: (
        difficulties: List<String>,
        requiresEquipment: Boolean?,
        sortByDifficulty: String?,
        sortByCalories: String?,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Local pending state — the user can dismiss without applying. Keyed on
    // incoming props so a re-open with different current values resets the
    // pending selection.
    var pendingDifficulties by remember(currentDifficulties) {
        mutableStateOf(currentDifficulties.toSet())
    }
    var pendingRequiresEquipment by remember(currentRequiresEquipment) {
        mutableStateOf(currentRequiresEquipment)
    }
    var pendingSortByDifficulty by remember(currentSortByDifficulty) {
        mutableStateOf(currentSortByDifficulty)
    }
    var pendingSortByCalories by remember(currentSortByCalories) {
        mutableStateOf(currentSortByCalories)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = "Фильтры",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = SectionTitleColor,
            )

            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle(text = "Уровень сложности")

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DifficultyOptions.forEach { option ->
                    val isSelected = option.value in pendingDifficulties
                    DifficultyOptionChip(
                        option = option,
                        isSelected = isSelected,
                        onClick = {
                            pendingDifficulties = if (isSelected) {
                                pendingDifficulties - option.value
                            } else {
                                pendingDifficulties + option.value
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle(text = "Сортировка по сложности")

            Spacer(modifier = Modifier.height(12.dp))

            SortChipsRow(
                selected = pendingSortByDifficulty,
                onSelected = { pendingSortByDifficulty = it },
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle(text = "Сортировка по калориям")

            Spacer(modifier = Modifier.height(12.dp))

            SortChipsRow(
                selected = pendingSortByCalories,
                onSelected = { pendingSortByCalories = it },
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle(text = "Оборудование")

            Spacer(modifier = Modifier.height(8.dp))

            ToggleRow(
                label = "Требуется оборудование",
                checked = pendingRequiresEquipment == true,
                onCheckedChange = { isChecked ->
                    pendingRequiresEquipment = if (isChecked) true else null
                },
            )

            Spacer(modifier = Modifier.height(28.dp))

            GymGenieButton(
                text = "Применить",
                textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                containerColor = AccentOrange,
                onClick = {
                    onApply(
                        pendingDifficulties.toList(),
                        pendingRequiresEquipment,
                        pendingSortByDifficulty,
                        pendingSortByCalories,
                    )
                    onDismiss()
                },
            )

            Spacer(modifier = Modifier.height(4.dp))

            TextButton(
                onClick = {
                    onApply(emptyList(), null, null, null)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Сбросить",
                    color = SecondaryText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = SectionTitleColor,
    )
}

@Composable
private fun DifficultyOptionChip(
    option: DifficultyOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (isSelected) option.color.copy(alpha = 0.15f) else ChipNeutralBackground
    val borderColor = if (isSelected) option.color else ChipNeutralBorder
    val textColor = if (isSelected) option.color else ChipNeutralText
    Text(
        text = option.label,
        fontSize = 16.sp,
        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
        color = textColor,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .border(1.5.dp, borderColor, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        textAlign = TextAlign.Center,
    )
}

/**
 * Row with two mutually-exclusive sort direction chips (DESC / ASC).
 *
 * Tapping the active chip clears the selection (passes `null`) so the user
 * does not need a separate "no sort" option. Used by both the difficulty
 * and the calories sort sections — keeps the chip style and interaction
 * model identical across the sheet.
 */
@Composable
private fun SortChipsRow(
    selected: String?,
    onSelected: (String?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SortChip(
            label = "Убывание",
            isSelected = selected == SORT_DESC,
            onClick = { onSelected(if (selected == SORT_DESC) null else SORT_DESC) },
            modifier = Modifier.weight(1f),
        )
        SortChip(
            label = "Возрастание",
            isSelected = selected == SORT_ASC,
            onClick = { onSelected(if (selected == SORT_ASC) null else SORT_ASC) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SortChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (isSelected) {
        Color(0xFF0A0A0A).copy(alpha = 0.08f)
    } else {
        ChipNeutralBackground
    }
    val borderColor = if (isSelected) Color(0xFF0A0A0A) else ChipNeutralBorder
    val textColor = if (isSelected) Color(0xFF0A0A0A) else ChipNeutralText
    Text(
        text = label,
        fontSize = 16.sp,
        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
        color = textColor,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 17.sp,
            color = SectionTitleColor,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentOrange,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFD7D7D9),
                uncheckedBorderColor = Color(0xFFD7D7D9),
            ),
        )
    }
}
