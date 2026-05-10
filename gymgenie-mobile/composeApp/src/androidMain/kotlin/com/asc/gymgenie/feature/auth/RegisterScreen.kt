package com.asc.gymgenie.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.asc.gymgenie.ui.components.IconEmail
import com.asc.gymgenie.ui.components.IconLock
import com.asc.gymgenie.ui.components.IconPerson
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.asc.gymgenie.presentation.AuthViewModel
import com.asc.gymgenie.ui.components.GymGenieButton
import com.asc.gymgenie.ui.components.GymGenieTextField
import com.asc.gymgenie.ui.theme.IllustrationBackground

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.registerSuccess) {
        if (state.registerSuccess) {
            viewModel.consumeRegisterSuccess()
            onRegisterSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IllustrationBackground)
            .statusBarsPadding()
            // Tap on any non-interactive area of the screen (illustration,
            // paddings, divider gaps) clears focus from the form fields.
            // Buttons and text fields consume their own taps so this only
            // fires for empty space.
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            },
    ) {
        // Top illustration area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.30f),
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
                .weight(0.70f)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Добро пожаловать",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(24.dp))

            GymGenieTextField(
                value = state.username,
                onValueChange = viewModel::onUsernameChanged,
                label = "Имя",
                leadingIcon = IconPerson,
                isError = state.errorMessage != null,
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                text = "Зарегистрироваться",
                onClick = viewModel::register,
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

            SocialButtonsRow()

            Spacer(modifier = Modifier.height(24.dp))

            // Login link
            val annotatedText = buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    append("Уже есть аккаунт? ")
                }
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append("Войти")
                }
            }
            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable {
                    // Navigating away should also drop focus so the keyboard
                    // doesn't carry over to the next screen.
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onNavigateToLogin()
                },
            )
        }
    }
}
