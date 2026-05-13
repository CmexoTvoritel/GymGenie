package com.asc.gymgenie.feature.profile.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        color = ProfileMuted,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp),
    )
}
