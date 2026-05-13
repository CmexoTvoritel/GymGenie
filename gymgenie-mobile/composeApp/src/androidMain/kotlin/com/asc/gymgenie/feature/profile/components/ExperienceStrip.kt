package com.asc.gymgenie.feature.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
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
import com.asc.gymgenie.ui.theme.Coral

@Composable
fun ExperienceStrip(experience: String, frequency: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, ProfileBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ProfileCoralSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.FitnessCenter,
                contentDescription = null,
                tint = Coral,
                modifier = Modifier.size(24.dp),
            )
        }
        Column {
            Text(
                text = "ОПЫТ",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = ProfileMuted,
                letterSpacing = 0.6.sp,
            )
            Text(
                text = if (frequency.isBlank()) experience else "$experience · ${frequency.lowercase()}",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = ProfileBlack,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
