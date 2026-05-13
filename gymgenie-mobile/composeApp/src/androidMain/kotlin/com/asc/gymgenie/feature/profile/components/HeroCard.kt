package com.asc.gymgenie.feature.profile.components

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
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HeroCard(
    displayName: String,
    email: String,
    hasPro: Boolean,
    weightKg: Double?,
    heightCm: Double?,
    ageYears: Int?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, ProfileBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProfileAvatar(name = displayName.ifEmpty { "?" }, size = 76)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = displayName.ifEmpty { "Пользователь" },
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = ProfileBlack,
        )

        if (email.isNotBlank()) {
            Text(
                text = email,
                fontSize = 15.sp,
                color = ProfileMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (hasPro) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ProfileBlack)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = "PRO MEMBER",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.4.sp,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ProfileSoft)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "FREE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ProfileMuted,
                    letterSpacing = 0.4.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ProfileStatBackground)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                StatItem(
                    value = weightKg?.toInt()?.toString() ?: "—",
                    unit = "кг",
                    label = "Вес",
                )
            }
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp)
                    .background(ProfileBorder),
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                StatItem(
                    value = heightCm?.toInt()?.toString() ?: "—",
                    unit = "см",
                    label = "Рост",
                )
            }
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp)
                    .background(ProfileBorder),
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                StatItem(
                    value = ageYears?.toString() ?: "—",
                    unit = "лет",
                    label = "Возраст",
                )
            }
        }
    }
}

@Composable
private fun StatItem(value: String, unit: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ProfileBlack,
            )
            if (value != "—") {
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    color = ProfileMuted,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(start = 2.dp, bottom = 1.dp),
                )
            }
        }
        Text(
            text = label,
            fontSize = 15.sp,
            color = ProfileMuted,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
