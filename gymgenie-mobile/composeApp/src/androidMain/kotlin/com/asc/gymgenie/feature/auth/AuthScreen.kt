package com.asc.gymgenie.feature.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.asc.gymgenie.R
import com.asc.gymgenie.presentation.AuthViewModel
import com.asc.gymgenie.ui.components.GymGenieButton
import com.asc.gymgenie.ui.components.GymGenieTextField
import com.asc.gymgenie.ui.theme.Coral

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    initialIsLogin: Boolean = true,
    onAuthSuccess: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var isLoginMode by remember { mutableStateOf(initialIsLogin) }

    LaunchedEffect(state.loginSuccess) {
        if (state.loginSuccess) {
            viewModel.consumeLoginSuccess()
            onAuthSuccess()
        }
    }

    LaunchedEffect(state.registerSuccess) {
        if (state.registerSuccess) {
            viewModel.consumeRegisterSuccess()
            onAuthSuccess()
        }
    }

    val loginAlpha by animateFloatAsState(
        targetValue = if (isLoginMode) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "loginAlpha",
    )
    val registerAlpha by animateFloatAsState(
        targetValue = if (isLoginMode) 0f else 1f,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "registerAlpha",
    )

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val loginPasswordFocus = remember { FocusRequester() }
    val registerEmailFocus = remember { FocusRequester() }
    val registerPasswordFocus = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .imePadding()
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
        ) {
            val loginPainter = painterResource(id = R.drawable.ic_login)
            val registerPainter = painterResource(id = R.drawable.ic_registration)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Coral.copy(alpha = 0.20f)),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Image(
                    painter = loginPainter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                        .alpha(loginAlpha)
                        .zIndex(if (isLoginMode) 1f else 0f),
                )

                Image(
                    painter = registerPainter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                        .alpha(registerAlpha)
                        .zIndex(if (!isLoginMode) 1f else 0f),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-32).dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "С возвращением",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .alpha(loginAlpha)
                            .zIndex(if (isLoginMode) 1f else 0f),
                    )
                    Text(
                        text = "Добро пожаловать",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .alpha(registerAlpha)
                            .zIndex(if (!isLoginMode) 1f else 0f),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(registerAlpha)
                            .zIndex(if (!isLoginMode) 1f else 0f),
                    ) {
                        GymGenieTextField(
                            value = state.name,
                            onValueChange = viewModel::onNameChanged,
                            label = "Имя",
                            leadingIcon = { Image(painter = painterResource(R.drawable.ic_name), contentDescription = null, modifier = Modifier.size(20.dp)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { registerEmailFocus.requestFocus() }),
                            isError = state.errorMessage != null,
                            enabled = !isLoginMode,
                            accentColor = Coral,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        GymGenieTextField(
                            value = state.email,
                            onValueChange = viewModel::onEmailChanged,
                            label = "Email",
                            leadingIcon = { Image(painter = painterResource(R.drawable.ic_mail), contentDescription = null, modifier = Modifier.size(20.dp)) },
                            modifier = Modifier.focusRequester(registerEmailFocus),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { registerPasswordFocus.requestFocus() }),
                            isError = state.errorMessage != null,
                            enabled = !isLoginMode,
                            accentColor = Coral,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        GymGenieTextField(
                            value = state.password,
                            onValueChange = viewModel::onPasswordChanged,
                            label = "Пароль",
                            leadingIcon = { Image(painter = painterResource(R.drawable.ic_password), contentDescription = null, modifier = Modifier.size(20.dp)) },
                            modifier = Modifier.focusRequester(registerPasswordFocus),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); keyboardController?.hide() }),
                            isError = state.errorMessage != null,
                            enabled = !isLoginMode,
                            accentColor = Coral,
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(loginAlpha)
                            .zIndex(if (isLoginMode) 1f else 0f),
                    ) {
                        GymGenieTextField(
                            value = state.email,
                            onValueChange = viewModel::onEmailChanged,
                            label = "Email",
                            leadingIcon = { Image(painter = painterResource(R.drawable.ic_mail), contentDescription = null, modifier = Modifier.size(20.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { loginPasswordFocus.requestFocus() }),
                            isError = state.errorMessage != null,
                            enabled = isLoginMode,
                            accentColor = Coral,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        GymGenieTextField(
                            value = state.password,
                            onValueChange = viewModel::onPasswordChanged,
                            label = "Пароль",
                            leadingIcon = { Image(painter = painterResource(R.drawable.ic_password), contentDescription = null, modifier = Modifier.size(20.dp)) },
                            modifier = Modifier.focusRequester(loginPasswordFocus),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); keyboardController?.hide() }),
                            isError = state.errorMessage != null,
                            enabled = isLoginMode,
                            accentColor = Coral,
                        )
                    }
                }

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

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    GymGenieButton(
                        text = "Войти",
                        onClick = viewModel::login,
                        isLoading = state.isLoading,
                        enabled = isLoginMode,
                        containerColor = Coral,
                        textStyle = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp),
                        modifier = Modifier
                            .alpha(loginAlpha)
                            .zIndex(if (isLoginMode) 1f else 0f),
                    )
                    GymGenieButton(
                        text = "Зарегистрироваться",
                        onClick = viewModel::register,
                        isLoading = state.isLoading,
                        enabled = !isLoginMode,
                        containerColor = Coral,
                        textStyle = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp),
                        modifier = Modifier
                            .alpha(registerAlpha)
                            .zIndex(if (!isLoginMode) 1f else 0f),
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "  Войдите через  ",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 17.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                SocialButtonsRow()

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                append("Еще нет аккаунта? ")
                            }
                            withStyle(SpanStyle(color = Coral)) {
                                append("Зарегистрируйтесь")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 17.sp),
                        modifier = Modifier
                            .alpha(loginAlpha)
                            .zIndex(if (isLoginMode) 1f else 0f)
                            .clickable(
                                enabled = isLoginMode,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                viewModel.resetState()
                                isLoginMode = false
                            },
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                append("Уже есть аккаунт? ")
                            }
                            withStyle(SpanStyle(color = Coral)) {
                                append("Войти")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 17.sp),
                        modifier = Modifier
                            .alpha(registerAlpha)
                            .zIndex(if (!isLoginMode) 1f else 0f)
                            .clickable(
                                enabled = !isLoginMode,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                viewModel.resetState()
                                isLoginMode = true
                            },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SocialButtonsRow() {
    data class SocialIcon(val drawableRes: Int, val contentDescription: String, val bgColor: Color)

    val socialIcons = listOf(
        SocialIcon(R.drawable.ic_google, "Google", Coral),
        SocialIcon(R.drawable.ic_apple, "Apple", Coral),
        SocialIcon(R.drawable.ic_vk, "VK", Coral),
        SocialIcon(R.drawable.ic_telegram, "Telegram", Coral),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        socialIcons.forEach { social ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(social.bgColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(social.drawableRes),
                    contentDescription = social.contentDescription,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
