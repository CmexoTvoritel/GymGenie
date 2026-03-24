package com.asc.gymgenie.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.theme.OnBackground
import com.asc.gymgenie.ui.theme.Primary

@Composable
fun SectionHeader(
    title: String,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OnBackground,
        )

        if (actionTitle != null && onAction != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = actionTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "\u2192",
                    fontSize = 12.sp,
                    color = Primary,
                )
            }
        }
    }
}
