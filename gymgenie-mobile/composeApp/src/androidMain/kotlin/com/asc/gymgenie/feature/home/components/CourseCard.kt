package com.asc.gymgenie.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.WarmOffWhite

@Composable
fun CourseCard(
    badge: String,
    title: String,
    trainer: String,
    ctaTitle: String,
    isPro: Boolean,
) {
    val bg = if (isPro) DeepInk else WarmOffWhite
    val primaryText = if (isPro) Color.White else DeepInk
    val secondaryText = if (isPro) Color.White.copy(alpha = 0.7f) else MutedText
    val badgeBg = if (isPro) Color.White.copy(alpha = 0.18f) else AccentOrange.copy(alpha = 0.18f)
    val ctaBg = if (isPro) AccentOrange else DeepInk
    val ctaText = Color.White

    Column(
        modifier = Modifier
            .size(width = 240.dp, height = 200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .then(
                if (!isPro) Modifier.border(
                    width = 1.dp,
                    color = DeepInk.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(24.dp),
                ) else Modifier,
            )
            .padding(20.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(badgeBg)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = badge,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = primaryText,
                letterSpacing = 0.4.sp,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = primaryText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = trainer,
            fontSize = 13.sp,
            color = secondaryText,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(ctaBg)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = ctaTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ctaText,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "›", color = ctaText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
