package com.asc.gymgenie.feature.workouts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.SoftCard
import com.asc.gymgenie.workout.WorkoutPlanShortResponse

@Composable
fun FeaturedWorkoutCard(
    plan: WorkoutPlanShortResponse,
    onStart: () -> Unit = {},
) {
    val isAI = plan.createdBy.equals("AI", ignoreCase = true)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .then(
                if (plan.isActive) Modifier.border(
                    width = 2.dp,
                    color = AccentOrange,
                    shape = RoundedCornerShape(20.dp),
                ) else Modifier
            )
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = plan.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (isAI) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "+ AI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AccentOrange)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TagChip(text = "${plan.daysCount} дней/нед.")
            plan.description?.let { desc ->
                val trimmed = desc.trim()
                if (trimmed.isNotEmpty()) {
                    TagChip(text = trimmed.take(25))
                }
            }
        }

        if (plan.isActive) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentOrange),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "АКТИВНЫЙ ПЛАН",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange,
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentOrange,
                contentColor = Color.White,
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            Text(
                text = "Начать тренировку",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun TagChip(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = MutedText,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(SoftCard)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}
