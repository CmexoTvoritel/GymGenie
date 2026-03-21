package com.asc.gymgenie.feature.paywall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.components.GymGenieButton
import com.asc.gymgenie.ui.theme.AccentGreen
import com.asc.gymgenie.ui.theme.OnBackground

@Composable
fun PurchaseSuccessScreen(
    onContinue: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AccentGreen.copy(alpha = 0.3f),
                        AccentGreen.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Illustration placeholder
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(AccentGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\uD83C\uDFCB\uFE0F",
                    fontSize = 56.sp,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Успешно!",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = OnBackground,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Вы разблокировали все функции",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnBackground,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Теперь давайте начнём с вашего ИИ-плана и изучим все возможности приложения",
                fontSize = 15.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            GymGenieButton(
                text = "Начать с планом от ИИ",
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
