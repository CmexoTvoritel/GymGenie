package com.asc.gymgenie.feature.workouts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.theme.OnBackground
import com.asc.gymgenie.ui.theme.OnSurfaceVariant
import com.asc.gymgenie.ui.theme.Primary
import com.asc.gymgenie.workout.WorkoutPlanShortResponse

@Composable
fun WorkoutCardSmall(plan: WorkoutPlanShortResponse) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(12.dp),
    ) {
        Text(
            text = "План",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Primary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Primary.copy(alpha = 0.1f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = plan.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = OnBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${plan.daysCount} дн.",
            fontSize = 12.sp,
            color = OnSurfaceVariant,
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Детали",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary,
            )

            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text("Начать", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
