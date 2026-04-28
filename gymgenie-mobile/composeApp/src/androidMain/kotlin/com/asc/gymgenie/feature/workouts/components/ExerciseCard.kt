package com.asc.gymgenie.feature.workouts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.asc.gymgenie.exercise.ExerciseShortResponse
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.SoftCard

@Composable
fun ExerciseCard(
    exercise: ExerciseShortResponse,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SoftCard),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = muscleGroupEmoji(exercise.muscleGroup),
                fontSize = 36.sp,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = exercise.nameRu,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = DeepInk,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 17.sp,
        )

        if (exercise.difficultyLevel.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            DifficultyBadge(level = exercise.difficultyLevel)
        }
    }
}

@Composable
private fun DifficultyBadge(level: String) {
    val (label, color) = when (level.uppercase()) {
        "BEGINNER" -> "Легко" to Color(0xFF4CAF50)
        "INTERMEDIATE" -> "Средн." to AccentOrange
        "ADVANCED" -> "Сложн." to Color(0xFFE53935)
        else -> level to MutedText
    }
    Text(
        text = label,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

internal fun muscleGroupEmoji(muscleGroup: String): String = when (muscleGroup.uppercase()) {
    "CHEST" -> "🤸"
    "BACK" -> "🏋"
    "SHOULDERS" -> "💪"
    "BICEPS", "TRICEPS", "FOREARMS" -> "💪"
    "ABS" -> "⚡"
    "QUADRICEPS", "HAMSTRINGS", "CALVES" -> "🏃"
    "GLUTES" -> "🔥"
    "CARDIO" -> "❤️"
    "FULL_BODY" -> "⭐"
    else -> "🏋"
}
