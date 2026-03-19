package com.asc.gymgenie.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.asc.gymgenie.ui.components.IconEmail
import com.asc.gymgenie.ui.components.IconLock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.components.GymGenieButton
import com.asc.gymgenie.ui.components.GymGenieTextField
import com.asc.gymgenie.ui.theme.IllustrationBackground

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IllustrationBackground),
    ) {
        // Top illustration area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Illustration",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Bottom card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.65f)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "С возвращением",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(24.dp))

            GymGenieTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChanged,
                label = "Email",
                leadingIcon = IconEmail,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = state.errorMessage != null,
            )

            Spacer(modifier = Modifier.height(16.dp))

            GymGenieTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChanged,
                label = "Пароль",
                leadingIcon = IconLock,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = state.errorMessage != null,
            )

            // Error message
            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.errorMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            GymGenieButton(
                text = "Войти",
                onClick = viewModel::login,
                isLoading = state.isLoading,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Divider with text
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "  Войдите через  ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Social buttons placeholder
            SocialButtonsRow()

            Spacer(modifier = Modifier.height(24.dp))

            // Register link
            val annotatedText = buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    append("Еще нет аккаунта? ")
                }
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append("Зарегистрируйтесь")
                }
            }
            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { onNavigateToRegister() },
            )
        }
    }
}

@Composable
fun SocialButtonsRow() {
    val socialLabels = listOf("G", "A", "VK", "TG")
    val socialColors = listOf(
        Color(0xFFDB4437),
        Color(0xFF000000),
        Color(0xFF4680C2),
        Color(0xFF0088CC),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        socialLabels.forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(socialColors[index].copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = socialColors[index],
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
