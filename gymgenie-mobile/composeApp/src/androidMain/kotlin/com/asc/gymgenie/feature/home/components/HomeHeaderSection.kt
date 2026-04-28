package com.asc.gymgenie.feature.home.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.SoftCard

/**
 * Premium home header: avatar, date + greeting, streak pill, notifications button.
 */
@Composable
fun HomeHeaderSection(
    username: String,
    streakDays: Int,
    date: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(username = username)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = date,
                fontSize = 12.sp,
                color = MutedText,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Привет, $username!",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(AccentOrange)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(text = "🔥", fontSize = 13.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = streakDays.toString(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = {},
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SoftCard),
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Уведомления",
                tint = DeepInk,
            )
        }
    }
}

@Composable
private fun Avatar(username: String) {
    val initial = username.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(SoftCard),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = DeepInk,
        )
    }
}

