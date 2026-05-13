package com.asc.gymgenie.feature.workouts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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

private val CardBorder = Color(0xFFEDEDEF)
private val ImageBackground = Color(0xFFF8F8FA)
private val PrimaryText = Color(0xFF0A0A0A)
private val MetaText = Color(0xFF4C4C53)

private val BeginnerColor = Color(0xFF22A06B)
private val IntermediateColor = Color(0xFFE89B12)
private val AdvancedColor = Color(0xFFD14343)

@Composable
fun ExerciseCard(
    exercise: ExerciseShortResponse,
    onClick: () -> Unit = {},
) {
    // Note: we intentionally do NOT call fillMaxHeight() here. LazyVerticalGrid
    // does not provide a bounded height to its cells, so fillMaxHeight() would
    // crash with infinite constraints. Equal-height cards are achieved
    // structurally: the image area is a square of identical width across both
    // columns, and the title reserves two lines (minLines = 2). Cards in the
    // same row therefore align naturally.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.5.dp, CardBorder, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(ImageBackground),
        ) {
            Text(
                text = muscleGroupEmoji(exercise.muscleGroup),
                fontSize = 54.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = exercise.nameRu,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = PrimaryText,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp,
        )

        if (exercise.difficultyLevel.isNotEmpty()) {
            DifficultyChip(
                level = exercise.difficultyLevel,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MuscleGroupChip(
                muscleGroup = exercise.muscleGroup,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

@Composable
private fun MuscleGroupChip(
    muscleGroup: String,
    modifier: Modifier = Modifier,
) {
    val color = muscleGroupColor(muscleGroup)
    val label = muscleGroupLabel(muscleGroup)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MetaText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DifficultyChip(
    level: String,
    modifier: Modifier = Modifier,
) {
    val (label, color) = when (level.uppercase()) {
        "BEGINNER" -> "Легко" to BeginnerColor
        "INTERMEDIATE" -> "Средне" to IntermediateColor
        "ADVANCED" -> "Сложно" to AdvancedColor
        else -> level to MetaText
    }
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

internal fun muscleGroupLabel(muscleGroup: String): String = when (muscleGroup.uppercase()) {
    "CHEST" -> "Грудь"
    "BACK" -> "Спина"
    "SHOULDERS" -> "Плечи"
    "BICEPS" -> "Бицепс"
    "TRICEPS" -> "Трицепс"
    "FOREARMS" -> "Предплечья"
    "ABS" -> "Пресс"
    "QUADRICEPS" -> "Квадрицепс"
    "HAMSTRINGS" -> "Бицепс бедра"
    "GLUTES" -> "Ягодицы"
    "CALVES" -> "Икры"
    "FULL_BODY" -> "Всё тело"
    "CARDIO" -> "Кардио"
    else -> muscleGroup
}

internal fun muscleGroupColor(muscleGroup: String): Color = when (muscleGroup.uppercase()) {
    "CHEST" -> Color(0xFFE94A2C)
    "BACK" -> Color(0xFF3B5BDB)
    "SHOULDERS" -> Color(0xFFE89B12)
    "BICEPS", "TRICEPS", "FOREARMS" -> Color(0xFFB8860B)
    "ABS" -> Color(0xFF6741D9)
    "QUADRICEPS", "HAMSTRINGS", "GLUTES", "CALVES" -> Color(0xFF2F9E44)
    "FULL_BODY" -> Color(0xFFFF5A3C)
    "CARDIO" -> Color(0xFFC2255C)
    else -> Color(0xFF76726A)
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
