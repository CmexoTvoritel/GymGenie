package com.asc.gymgenie.feature.meal_plan_detail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.nutrition.MealPlanDetailDish
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText

private val BorderColor = Color(0xFFEDEDEF)
private val SoftBg = Color(0xFFF4F4F6)
private val ProteinBg = Color(0xFFE1F1FF)
private val FatBg = Color(0xFFFFF4DC)
private val CarbsBg = Color(0xFFE8F7E8)

@Composable
fun ProductCard(
    dish: MealPlanDetailDish,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val emoji = deriveEmoji(dish.name)
    val grams = parseGrams(dish.portionDescription)
    val kcal = dish.calories?.toInt() ?: 0
    val protein = dish.proteinG?.toInt() ?: 0
    val fat = dish.fatG?.toInt() ?: 0
    val carbs = dish.carbsG?.toInt() ?: 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.5.dp, BorderColor, RoundedCornerShape(18.dp))
            .clickable { expanded = !expanded }
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SoftBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dish.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = grams,
                    fontSize = 16.sp,
                    color = DeepInk,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = kcal.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepInk,
                )
                Text(
                    text = "ККАЛ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MutedText,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MacroChip(
                label = "Б",
                value = "${protein}г",
                bg = ProteinBg,
                modifier = Modifier.weight(1f),
            )
            MacroChip(
                label = "Ж",
                value = "${fat}г",
                bg = FatBg,
                modifier = Modifier.weight(1f),
            )
            MacroChip(
                label = "У",
                value = "${carbs}г",
                bg = CarbsBg,
                modifier = Modifier.weight(1f),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = "На 100 г — справочно",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedText,
                    letterSpacing = 0.3.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))

                val gramsValue = parseGramsNumber(dish.portionDescription)
                val factor = if (gramsValue > 0) 100.0 / gramsValue else 0.0
                val kcalPer100 = (kcal * factor).toInt()
                val proteinPer100 = (protein * factor).toInt()
                val fatPer100 = (fat * factor).toInt()
                val carbsPer100 = (carbs * factor).toInt()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Per100Cell(label = "Ккал", value = kcalPer100.toString(), modifier = Modifier.weight(1f))
                    Per100Cell(label = "Б", value = "${proteinPer100}г", modifier = Modifier.weight(1f))
                    Per100Cell(label = "Ж", value = "${fatPer100}г", modifier = Modifier.weight(1f))
                    Per100Cell(label = "У", value = "${carbsPer100}г", modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MacroChip(
    label: String,
    value: String,
    bg: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DeepInk,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepInk,
        )
    }
}

@Composable
private fun Per100Cell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SoftBg)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MutedText,
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DeepInk,
        )
    }
}

private fun deriveEmoji(dishName: String): String {
    val lower = dishName.lowercase()
    return when {
        lower.contains("курица") || lower.contains("куриц") || lower.contains("грудка") -> "🍗"
        lower.contains("рыб") || lower.contains("лосось") || lower.contains("тунец") || lower.contains("форель") -> "🐟"
        lower.contains("яйц") || lower.contains("омлет") -> "🥚"
        lower.contains("салат") || lower.contains("овощ") -> "🥗"
        lower.contains("каш") || lower.contains("овсян") || lower.contains("рис") || lower.contains("гречк") -> "🌾"
        lower.contains("хлеб") || lower.contains("тост") -> "🍞"
        lower.contains("молок") || lower.contains("творог") || lower.contains("йогурт") || lower.contains("кефир") -> "🥛"
        lower.contains("фрукт") || lower.contains("яблок") || lower.contains("банан") -> "🍎"
        lower.contains("орех") -> "🥜"
        lower.contains("суп") || lower.contains("бульон") -> "🍲"
        lower.contains("мяс") || lower.contains("говядин") || lower.contains("свинин") || lower.contains("стейк") -> "🥩"
        lower.contains("паст") || lower.contains("макарон") || lower.contains("спагетт") -> "🍝"
        lower.contains("чай") || lower.contains("кофе") -> "☕"
        lower.contains("сок") || lower.contains("смузи") -> "🥤"
        else -> "🍽️"
    }
}

private fun parseGrams(portionDescription: String?): String {
    if (portionDescription.isNullOrBlank()) return "Порция"
    return portionDescription
}

private fun parseGramsNumber(portionDescription: String?): Double {
    if (portionDescription.isNullOrBlank()) return 0.0
    val regex = Regex("(\\d+(?:\\.\\d+)?)")
    val match = regex.find(portionDescription)
    return match?.value?.toDoubleOrNull() ?: 0.0
}
