package com.asc.gymgenie.feature.workout_history.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.R

private val InkBlack = Color(0xFF0A0A0A)
private val MutedGray = Color(0xFF8B8B92)

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_weekend_day),
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "День отдыха",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = InkBlack,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "В этот день тренировок не было",
            fontSize = 18.sp,
            color = MutedGray,
        )
    }
}
