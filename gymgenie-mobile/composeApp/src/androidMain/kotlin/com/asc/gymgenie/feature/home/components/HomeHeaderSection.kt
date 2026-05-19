package com.asc.gymgenie.feature.home.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.SoftCard

@Composable
fun HomeHeaderSection(
    username: String,
    date: String,
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onProfileClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(username = username)

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = date,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = MutedText,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Привет, $username!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        IconButton(
            onClick = onNotificationsClick,
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
            .size(48.dp)
            .clip(CircleShape)
            .background(Coral),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}
