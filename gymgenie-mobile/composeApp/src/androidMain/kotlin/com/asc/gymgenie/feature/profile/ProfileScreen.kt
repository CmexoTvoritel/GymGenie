package com.asc.gymgenie.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.auth.AuthApi
import com.asc.gymgenie.common.createAuthenticatedClient
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.ui.theme.*
import com.asc.gymgenie.presentation.HomeViewModel
import com.asc.gymgenie.user.UserApi
import com.asc.gymgenie.workout.WorkoutApi
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember

@Composable
fun ProfileScreen(tokenStorage: TokenStorage, onLogout: () -> Unit = {}) {
    val homeViewModel = remember {
        val authApi = AuthApi()
        val client = createAuthenticatedClient(tokenStorage, authApi)
        HomeViewModel(
            userApi = UserApi(client),
            workoutApi = WorkoutApi(client),
            tokenStorage = tokenStorage,
            onLogout = onLogout,
        )
    }

    DisposableEffect(Unit) {
        onDispose { homeViewModel.onCleared() }
    }

    val state by homeViewModel.state.collectAsState()

    var darkMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { homeViewModel.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Title
        Text(
            text = "Профиль",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = OnBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )

        // User card
        UserCard(
            username = state.username,
            email = state.userProfile?.email,
            weightKg = state.userProfile?.weightKg,
            heightCm = state.userProfile?.heightCm,
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp),
        )

        // Level card
        LevelCard(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp))

        // Subscription card
        SubscriptionCard(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp))

        // General settings
        SettingsSectionHeader("ОБЩЕЕ")
        SettingsCard(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            SettingsRow(icon = "🌐", label = "Язык", trailing = { Text("Русский", color = OnSurfaceVariant, fontSize = 14.sp) })
            Divider(color = Color.Gray.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))
            SettingsRow(icon = "🌙", label = "Тёмная тема", trailing = {
                Switch(checked = darkMode, onCheckedChange = { darkMode = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentGreen))
            })
            Divider(color = Color.Gray.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))
            SettingsRow(icon = "🔔", label = "Уведомления")
        }

        // Support settings
        SettingsSectionHeader("ПОДДЕРЖКА")
        SettingsCard(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            SettingsRow(icon = "❓", label = "Помощь / FAQ")
            Divider(color = Color.Gray.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))
            SettingsRow(icon = "✉️", label = "Отправить отзыв")
        }

        // Sign out
        TextButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        ) {
            Text("Выйти из аккаунта", color = Color.Red.copy(alpha = 0.8f), fontSize = 15.sp)
        }

        Text(
            text = "Версия 1.0.0",
            fontSize = 12.sp,
            color = OnSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun UserCard(
    username: String,
    email: String?,
    weightKg: Double?,
    heightCm: Double?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(AccentGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(36.dp),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(username.ifEmpty { "Пользователь" }, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OnBackground)
            if (email != null) {
                Text(email, fontSize = 13.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AccentGreen)
                    .padding(horizontal = 14.dp, vertical = 4.dp),
            ) {
                Text("PRO MEMBER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.Gray.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatCell(value = weightKg?.let { "${it.toInt()} кг" } ?: "—", label = "Вес")
                VerticalDivider()
                StatCell(value = heightCm?.let { "${it.toInt()} см" } ?: "—", label = "Рост")
                VerticalDivider()
                StatCell(value = "—", label = "Возраст")
            }
        }
    }
}

@Composable
private fun StatCell(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = OnBackground)
        Text(label, fontSize = 12.sp, color = OnSurfaceVariant)
    }
}

@Composable
private fun VerticalDivider() {
    Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color.Gray.copy(alpha = 0.15f)))
}

@Composable
private fun LevelCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.2f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("ТЕКУЩИЙ УРОВЕНЬ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Продвинутый", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                Spacer(modifier = Modifier.weight(1f))
                Text("Начало: Новичок", fontSize = 12.sp, color = OnSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { 0.7f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = AccentGreen,
                trackColor = Color.Gray.copy(alpha = 0.15f),
            )
        }
    }
}

@Composable
private fun SubscriptionCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF4196EB).copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Premium Plan", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF4196EB))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("подтверждён", fontSize = 12.sp, color = OnSurfaceVariant)
                }
                Text("Управление подпиской и оплатой", fontSize = 13.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
            Text("›", fontSize = 20.sp, color = OnSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = OnSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(icon: String, label: String, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 18.sp, modifier = Modifier.width(28.dp))
        Text(label, fontSize = 15.sp, color = OnBackground, modifier = Modifier.weight(1f))
        if (trailing != null) {
            trailing()
        } else {
            Text("›", fontSize = 18.sp, color = OnSurfaceVariant)
        }
    }
}
