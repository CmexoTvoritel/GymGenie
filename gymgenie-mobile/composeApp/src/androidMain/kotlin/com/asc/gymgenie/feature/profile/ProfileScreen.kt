package com.asc.gymgenie.feature.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.feature.paywall.PaywallScreen
import com.asc.gymgenie.presentation.ProfileViewModel
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.user.UserApi
import com.asc.gymgenie.user.UserProfileStore
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

private val CoralSoft = Color(0xFFFFE8E2)
private val Black = Color(0xFF0A0A0A)
private val Border = Color(0xFFEDEDEF)
private val Muted = Color(0xFF8B8B92)
private val Soft = Color(0xFFF4F4F6)
private val DangerRed = Color(0xFFE5484D)

@Composable
fun ProfileScreen(
    tokenStorage: TokenStorage,
    userProfileStore: UserProfileStore,
    onLogout: () -> Unit,
) {
    val koin = remember { GlobalContext.get() }
    val userApi = remember { koin.get<UserApi>() }
    val profileViewModel = remember {
        ProfileViewModel(userApi = userApi, userProfileStore = userProfileStore)
    }
    DisposableEffect(Unit) { onDispose { profileViewModel.onCleared() } }

    val profile by userProfileStore.profile.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showEdit by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }
    var confirmDialog by remember { mutableStateOf<String?>(null) }

    val hasPro = profile?.subscriptionType == "PREMIUM"
    val displayName = buildString {
        profile?.firstName?.takeIf { it.isNotBlank() }?.let { append(it) }
        profile?.lastName?.takeIf { it.isNotBlank() }?.let {
            if (isNotEmpty()) append(" ")
            append(it)
        }
        if (isEmpty()) profile?.username?.let { append(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmOffWhite)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Профиль",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Black,
                    letterSpacing = (-0.5).sp,
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Soft)
                        .clickable { showEdit = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = Black,
                        modifier = Modifier.size(14.dp),
                    )
                    Text("Ред.", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Black)
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                HeroCard(
                    displayName = displayName,
                    email = profile?.email ?: "",
                    hasPro = hasPro,
                    weightKg = profile?.weightKg,
                    heightCm = profile?.heightCm,
                    ageYears = profile?.ageYears,
                )

                val exp = profile?.experience
                val freq = profile?.frequency
                if (!exp.isNullOrBlank() || !freq.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    ExperienceStrip(
                        experience = exp ?: "—",
                        frequency = freq ?: "—",
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                SubscriptionCard(
                    hasPro = hasPro,
                    onClick = { if (!hasPro) showPaywall = true },
                )

                SectionLabel("Общее")
                SettingsGroupCard {
                    SettingsRow(label = "Язык", icon = Icons.Filled.Language, value = "Русский", onClick = {})
                    HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(label = "Тема", icon = Icons.Filled.Palette, value = "Системная", onClick = {})
                    HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(label = "Уведомления", icon = Icons.Filled.Notifications, onClick = {})
                }

                SectionLabel("Активность")
                SettingsGroupCard {
                    SettingsRow(label = "Статистика", icon = Icons.Filled.BarChart, onClick = {})
                }

                SectionLabel("Поддержка")
                SettingsGroupCard {
                    SettingsRow(label = "Помощь и FAQ", icon = Icons.AutoMirrored.Filled.Help, onClick = {})
                    HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(label = "Отправить отзыв", icon = Icons.Filled.Email, onClick = {})
                }

                SectionLabel("Аккаунт")
                SettingsGroupCard {
                    SettingsRow(
                        label = "Выйти из аккаунта",
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        onClick = { confirmDialog = "logout" },
                    )
                    HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        label = "Удалить аккаунт",
                        icon = Icons.Filled.Delete,
                        labelColor = DangerRed,
                        iconColor = DangerRed,
                        onClick = { confirmDialog = "delete" },
                    )
                }

                Text(
                    text = "Версия 1.0.0",
                    fontSize = 12.sp,
                    color = Color(0xFFB5B5BD),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 22.dp, bottom = 16.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = showEdit,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.fillMaxSize(),
        ) {
            EditProfileScreen(
                userProfileStore = userProfileStore,
                profileViewModel = profileViewModel,
                onBack = { showEdit = false },
            )
        }

        AnimatedVisibility(
            visible = showPaywall,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize(),
        ) {
            PaywallScreen(
                userApi = userApi,
                userProfileStore = userProfileStore,
                onPurchaseSuccess = { showPaywall = false },
                onSkip = { showPaywall = false },
            )
        }

        if (confirmDialog != null) {
            ConfirmBottomSheet(
                isDelete = confirmDialog == "delete",
                onDismiss = { confirmDialog = null },
                onConfirm = {
                    when (confirmDialog) {
                        "logout" -> coroutineScope.launch {
                            confirmDialog = null
                            tokenStorage.clearTokens()
                            userProfileStore.clear()
                            onLogout()
                        }
                        "delete" -> confirmDialog = null
                    }
                },
            )
        }
    }
}

@Composable
private fun ProfileAvatar(name: String, size: Int) {
    val initials = name.trim()
        .split("\\s+".toRegex())
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Coral, Color(0xFFFF8A6E)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = (size * 0.36f).sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        )
    }
}

@Composable
private fun HeroCard(
    displayName: String,
    email: String,
    hasPro: Boolean,
    weightKg: Double?,
    heightCm: Double?,
    ageYears: Int?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, Border, RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProfileAvatar(name = displayName.ifEmpty { "?" }, size = 76)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = displayName.ifEmpty { "Пользователь" },
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = Black,
        )

        if (email.isNotBlank()) {
            Text(
                text = email,
                fontSize = 13.sp,
                color = Muted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (hasPro) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Black)
                    .padding(horizontal = 11.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    text = "PRO MEMBER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.4.sp,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Soft)
                    .padding(horizontal = 11.dp, vertical = 5.dp),
            ) {
                Text(
                    text = "FREE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Muted,
                    letterSpacing = 0.4.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF8F8FA))
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatItem(value = weightKg?.toInt()?.toString() ?: "—", unit = "кг", label = "Вес")
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp)
                    .background(Border),
            )
            StatItem(value = heightCm?.toInt()?.toString() ?: "—", unit = "см", label = "Рост")
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp)
                    .background(Border),
            )
            StatItem(value = ageYears?.toString() ?: "—", unit = "лет", label = "Возраст")
        }
    }
}

@Composable
private fun StatItem(value: String, unit: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Black)
            if (value != "—") {
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    color = Muted,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(start = 2.dp, bottom = 1.dp),
                )
            }
        }
        Text(text = label, fontSize = 11.sp, color = Muted, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun ExperienceStrip(experience: String, frequency: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Border, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CoralSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.FitnessCenter,
                contentDescription = null,
                tint = Coral,
                modifier = Modifier.size(20.dp),
            )
        }
        Column {
            Text(
                text = "ОПЫТ",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Muted,
                letterSpacing = 0.6.sp,
            )
            Text(
                text = "$experience · ${frequency.lowercase()}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Black,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun SubscriptionCard(hasPro: Boolean, onClick: () -> Unit) {
    if (hasPro) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.5.dp, Coral, RoundedCornerShape(20.dp))
                .clickable(onClick = onClick)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CoralSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = Coral,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Premium Plan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Black)
                Text(text = "Активна", fontSize = 13.sp, color = Muted, modifier = Modifier.padding(top = 2.dp))
            }
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFFC8C8CE), modifier = Modifier.size(20.dp))
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Black, Color(0xFF1F1F22))))
                .clickable(onClick = onClick)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x2DFF5A3C)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = Coral,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Открой Premium", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "AI-планы, статистика, без рекламы", fontSize = 13.sp, color = Color(0xFFA8A8A8), modifier = Modifier.padding(top = 2.dp))
            }
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = Muted,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp),
    )
}

@Composable
private fun SettingsGroupCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Border, RoundedCornerShape(18.dp)),
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(
    label: String,
    icon: ImageVector? = null,
    value: String? = null,
    labelColor: Color = Black,
    iconColor: Color = Black,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (iconColor == DangerRed) Color(0xFFFEE7E9) else Soft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = labelColor,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(value, fontSize = 14.sp, color = Muted, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFC8C8CE),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ConfirmBottomSheet(
    isDelete: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color.White)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { }
                .padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE0E0E5)),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isDelete) Color(0xFFFEE7E9) else CoralSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isDelete) Icons.Filled.Delete else Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = if (isDelete) DangerRed else Coral,
                    modifier = Modifier.size(26.dp),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isDelete) "Удалить аккаунт?" else "Выйти из аккаунта?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Black,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isDelete) {
                    "Все ваши тренировки и прогресс будут безвозвратно удалены."
                } else {
                    "Чтобы продолжить, нужно будет войти заново."
                },
                fontSize = 14.sp,
                color = Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isDelete) DangerRed else Coral),
            ) {
                Text(
                    text = if (isDelete) "Удалить навсегда" else "Выйти",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Отмена", color = Muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
