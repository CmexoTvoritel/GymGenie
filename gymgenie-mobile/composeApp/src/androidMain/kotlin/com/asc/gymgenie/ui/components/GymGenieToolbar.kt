package com.asc.gymgenie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import com.asc.gymgenie.ui.theme.WarmOffWhite

private val ToolbarTitleColor = Color(0xFF0A0A0A)
private val ToolbarActionBackground = Color(0xFFF4F4F6)

data class ToolbarAction(
    val content: @Composable () -> Unit,
    val onClick: () -> Unit,
)

@Composable
fun GymGenieToolbar(
    title: String,
    showBackNavigation: Boolean = false,
    showCloseIcon: Boolean = false,
    onBackClick: () -> Unit = {},
    actions: List<ToolbarAction> = emptyList(),
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WarmOffWhite)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showBackNavigation) {
                Row(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ToolbarActionBackground)
                        .clickable(onClick = onBackClick),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (showCloseIcon) Icons.Filled.Close
                            else Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (showCloseIcon) "Закрыть" else "Назад",
                        tint = ToolbarTitleColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = ToolbarTitleColor,
            )
        }
        if (actions.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                actions.forEach { action ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(ToolbarActionBackground)
                            .clickable(onClick = action.onClick)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        action.content()
                    }
                }
            }
        }
    }
}
