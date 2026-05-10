package com.asc.gymgenie.feature.paywall

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.R
import com.asc.gymgenie.ui.components.GymGenieButton
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.OnBackground

@Composable
fun PurchaseSuccessScreen(
    onContinue: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        // Close button — absolutely positioned, does not participate in layout flow.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 20.dp, top = 8.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.15f))
                .clickable { onContinue() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✕",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnBackground,
            )
        }

        // Main column — image+texts block is vertically centered between the
        // X button area at the top and the action buttons at the bottom by two
        // weighted spacers, so the visual content stays balanced regardless of
        // device height. Buttons live inside the same column to keep the
        // layout flow predictable (no overlapping BottomCenter overlay).
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            // Reserved space for the absolutely-placed X button so the
            // content block does not shift under it.
            Spacer(modifier = Modifier.height(56.dp))

            Spacer(modifier = Modifier.weight(1f))

            // Centered content block — image + both texts as one cohesive group.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_paywall_success),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit,
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Успешно!\nВы разблокировали все функции",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 35.sp,
                    color = OnBackground,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Теперь давайте начнём с вашего ИИ-плана и изучим все возможности приложения",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom buttons — part of the same column so weighted spacers
            // distribute the vertical free space symmetrically around the
            // content block above.
            GymGenieButton(
                text = "Начать с планом от ИИ",
                onClick = onContinue,
                containerColor = Coral,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable { onContinue() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Продолжить без плана",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
