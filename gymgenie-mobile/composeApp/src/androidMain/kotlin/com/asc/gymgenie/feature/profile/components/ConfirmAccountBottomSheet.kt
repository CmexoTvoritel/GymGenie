package com.asc.gymgenie.feature.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.theme.Coral

@Composable
fun ConfirmAccountSheetContent(
    isDelete: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = 4.dp, bottom = 8.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isDelete) ProfileDangerSoft else ProfileCoralSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isDelete) Icons.Filled.Delete else Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = if (isDelete) ProfileDangerRed else Coral,
                modifier = Modifier.size(26.dp),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (isDelete) "Удалить аккаунт?" else "Выйти из аккаунта?",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = ProfileBlack,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isDelete) {
                "Все ваши тренировки и прогресс будут безвозвратно удалены."
            } else {
                "Чтобы продолжить, нужно будет войти заново."
            },
            fontSize = 16.sp,
            color = ProfileMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDelete) ProfileDangerRed else Coral,
                contentColor = Color.White,
            ),
        ) {
            Text(
                text = if (isDelete) "Удалить навсегда" else "Выйти",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Отмена",
                color = ProfileMuted,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
