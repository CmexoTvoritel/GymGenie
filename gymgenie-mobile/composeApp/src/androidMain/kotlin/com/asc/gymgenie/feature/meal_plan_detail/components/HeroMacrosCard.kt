package com.asc.gymgenie.feature.meal_plan_detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText

@Composable
fun HeroMacrosCard(
    totalKcal: Int,
    proteinG: Int,
    fatG: Int,
    carbsG: Int,
    modifier: Modifier = Modifier,
) {
    val macroKcalTotal = proteinG * 4 + fatG * 9 + carbsG * 4
    val proteinPct = if (macroKcalTotal > 0) (proteinG * 4 * 100) / macroKcalTotal else 0
    val fatPct = if (macroKcalTotal > 0) (fatG * 9 * 100) / macroKcalTotal else 0
    val carbsPct = if (macroKcalTotal > 0) 100 - proteinPct - fatPct else 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Coral.copy(alpha = 0.08f),
                spotColor = Coral.copy(alpha = 0.12f),
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, DeepInk.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            .drawBehind {
                val glowRadius = size.minDimension * 0.6f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Coral.copy(alpha = 0.07f), Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = glowRadius,
                    ),
                    radius = glowRadius,
                    center = Offset(size.width, 0f),
                )
            }
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MacroDonut(
                proteinG = proteinG,
                fatG = fatG,
                carbsG = carbsG,
                totalKcal = totalKcal,
            )

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(
                    text = "ВСЕГО ЗА ПРИЁМ",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedText,
                    letterSpacing = 1.sp,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = totalKcal.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepInk,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ккал",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MutedText,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        StackedMacroBar(
            proteinG = proteinG,
            fatG = fatG,
            carbsG = carbsG,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MacroMiniCard(
                label = "Белки",
                grams = proteinG,
                pct = proteinPct,
                dotColor = ProteinColor,
                modifier = Modifier.weight(1f),
            )
            MacroMiniCard(
                label = "Жиры",
                grams = fatG,
                pct = fatPct,
                dotColor = FatColor,
                modifier = Modifier.weight(1f),
            )
            MacroMiniCard(
                label = "Углеводы",
                grams = carbsG,
                pct = carbsPct,
                dotColor = CarbsColor,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StackedMacroBar(
    proteinG: Int,
    fatG: Int,
    carbsG: Int,
    modifier: Modifier = Modifier,
) {
    val total = (proteinG * 4 + fatG * 9 + carbsG * 4).toFloat()
    val pFrac = if (total > 0f) (proteinG * 4f) / total else 0f
    val fFrac = if (total > 0f) (fatG * 9f) / total else 0f
    val cFrac = if (total > 0f) (carbsG * 4f) / total else 0f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(6.dp)),
    ) {
        if (pFrac > 0f) {
            Box(
                modifier = Modifier
                    .weight(pFrac)
                    .height(8.dp)
                    .background(ProteinColor),
            )
        }
        if (fFrac > 0f) {
            Box(
                modifier = Modifier
                    .weight(fFrac)
                    .height(8.dp)
                    .background(FatColor),
            )
        }
        if (cFrac > 0f) {
            Box(
                modifier = Modifier
                    .weight(cFrac)
                    .height(8.dp)
                    .background(CarbsColor),
            )
        }
        if (total <= 0f) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .background(DeepInk.copy(alpha = 0.06f)),
            )
        }
    }
}

@Composable
private fun MacroMiniCard(
    label: String,
    grams: Int,
    pct: Int,
    dotColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DeepInk.copy(alpha = 0.05f))
            .padding(horizontal = 10.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MutedText,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${grams}г",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${pct}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MutedText,
            )
        }
    }
}
