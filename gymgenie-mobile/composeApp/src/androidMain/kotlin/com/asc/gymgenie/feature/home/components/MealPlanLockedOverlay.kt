package com.asc.gymgenie.feature.home.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.components.GymGenieButton
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.WarmOffWhite

private val MealCardBorder = Color(0xFFEDEDEF)
private val ShimmerLight = Color(0xFFF3F2EF)
private val ShimmerDark = Color(0xFFEAE9E6)

@Composable
fun MealPlanLockedOverlay(onUnlock: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = 0.75f },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LockedSkeletonMealCard()
            LockedSkeletonMealCard()
            LockedSkeletonMealCard()
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(WarmOffWhite.copy(alpha = 0.85f)),
        )

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MutedText,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Доступно в Premium",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepInk,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            GymGenieButton(
                text = "Разблокировать",
                onClick = onUnlock,
                containerColor = Coral,
                modifier = Modifier.fillMaxWidth(0.65f),
            )
        }
    }
}

@Composable
private fun LockedSkeletonMealCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.5.dp, MealCardBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ShimmerLight),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box {
                Text(
                    text = "Завтрак",
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Transparent,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .matchParentSize()
                        .clip(RoundedCornerShape(4.dp))
                        .background(ShimmerLight),
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Box {
                Text(
                    text = "Блюдо · Блюдо · Блюдо",
                    fontSize = 14.sp,
                    color = Color.Transparent,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .matchParentSize()
                        .clip(RoundedCornerShape(4.dp))
                        .background(ShimmerDark),
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Box {
                Text(
                    text = "500",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Transparent,
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(4.dp))
                        .background(ShimmerLight),
                )
            }
            Box {
                Text(
                    text = "ККАЛ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Transparent,
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(4.dp))
                        .background(ShimmerDark),
                )
            }
        }
    }
}
