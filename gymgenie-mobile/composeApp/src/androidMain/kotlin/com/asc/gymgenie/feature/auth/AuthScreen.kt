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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import com.asc.gymgenie.ui.components.IconEmail
import com.asc.gymgenie.ui.components.IconLock
import com.asc.gymgenie.ui.components.IconPerson
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

    // Synchronized cross-fade between login and register states.
    // We drive every mode-dependent element from the same alpha pair
    // so they animate in lockstep without any layout reflow.
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

    // Focus requesters for IME field navigation
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
                .verticalScroll(rememberScrollState()),
        ) {
            // Top illustration area — coral tinted background, images bottom-aligned.
            // Height is computed from the image's intrinsic aspect ratio so that
            // the coral area tightly wraps: 16dp top padding + rendered image + 32dp
            // bottom padding (the card overlaps those 32dp with its negative offset).
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
            ) {
                val loginPainter = painterResource(id = R.drawable.ic_login)
                val loginIntrinsicSize = loginPainter.intrinsicSize
                val loginAspectRatio = if (loginIntrinsicSize.height > 0f) {
                    loginIntrinsicSize.width / loginIntrinsicSize.height
                } else {
                    1f
                }

                val registerPainter = painterResource(id = R.drawable.ic_registration)
                val registerIntrinsicSize = registerPainter.intrinsicSize
                val registerAspectRatio = if (registerIntrinsicSize.height > 0f) {
                    registerIntrinsicSize.width / registerIntrinsicSize.height
                } else {
                    1f
                }

                val imageWidth = maxWidth - 40.dp
                val loginImageHeight = imageWidth / loginAspectRatio
                val registerImageHeight = imageWidth / registerAspectRatio
                val maxImageHeight = maxOf(loginImageHeight, registerImageHeight)
                val coralHeight = 16.dp + maxImageHeight + 32.dp

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(coralHeight)
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
                            .aspectRatio(loginAspectRatio)
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
                            .aspectRatio(registerAspectRatio)
                            .alpha(registerAlpha)
                            .zIndex(if (!isLoginMode) 1f else 0f),
                    )
                }
            }

            // White card with rounded top corners. Overlaps the coral area by 32dp
            // via negative offset so the rounded corners visually sit on top of the
            // illustration. Everything scrolls as one unit — when the keyboard opens,
            // the illustration goes off-screen and the fields + button remain visible.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-32).dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color.White)
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Title — both variants stacked, alpha-cross-faded.
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

                // Fields box — register column always reserves the height (3 fields),
                // login column is centered inside the same space (2 fields). No fixed
                // height: the box naturally grows to the taller (register) child.
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    // Register fields — define box height. Always present in layout.
                    // Visible Column lives above the hidden one via zIndex, so the
                    // hidden Column never receives touches.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(registerAlpha)
                            .zIndex(if (!isLoginMode) 1f else 0f),
                    ) {
                        GymGenieTextField(
                            value = state.username,
                            onValueChange = viewModel::onUsernameChanged,
                            label = "Имя",
                            leadingIcon = IconPerson,
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
                            leadingIcon = IconEmail,
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
                            leadingIcon = IconLock,
                            modifier = Modifier.focusRequester(registerPasswordFocus),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); keyboardController?.hide() }),
                            isError = state.errorMessage != null,
                            enabled = !isLoginMode,
                            accentColor = Coral,
                        )
                    }

                    // Login fields — vertically centered by the parent Box.
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
                            leadingIcon = IconEmail,
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
                            leadingIcon = IconLock,
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

                // Primary action button — both variants stacked, only the visible one
                // is interactive. Coral background per design.
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

                // Static section — divider + social row never transitions between modes.
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

                // Mode-switch link — both labels stacked, only the visible one captures clicks.
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

                // Safe-area bottom padding
                Spacer(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .height(16.dp),
                )
            }
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
