package com.asc.gymgenie.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.common.SessionManager
import com.asc.gymgenie.feature.profile.components.ConfirmAccountSheetContent
import com.asc.gymgenie.feature.profile.components.ExperienceStrip
import com.asc.gymgenie.feature.profile.components.HeroCard
import com.asc.gymgenie.feature.profile.components.ProfileSectionLabel
import com.asc.gymgenie.feature.profile.components.SettingsGroupCard
import com.asc.gymgenie.feature.profile.components.SettingsRow
import com.asc.gymgenie.feature.profile.components.SubscriptionCard
import com.asc.gymgenie.ui.components.GymGenieToolbar
import com.asc.gymgenie.ui.components.ToolbarAction
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.user.UserProfileStore
import org.koin.core.context.GlobalContext

private val ProfileBorderDivider = Color(0xFFEDEDEF)
private val ProfileDangerRow = Color(0xFFE5484D)
private val ProfileVersionTextColor = Color(0xFFB5B5BD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onOpenEditProfile: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenHistory: () -> Unit = {},
) {
    val koin = remember { GlobalContext.get() }
    val userProfileStore = remember { koin.get<UserProfileStore>() }
    val sessionManager = remember { koin.get<SessionManager>() }

    val profile by userProfileStore.profile.collectAsState()
    val context = LocalContext.current

    var confirmDialog by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val versionName = remember {
        runCatching {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName
        }.getOrNull() ?: "—"
    }

    val hasPro = profile?.subscriptionType == "PREMIUM"
    val displayName = buildString {
        profile?.firstName?.takeIf { it.isNotBlank() }?.let { append(it) }
        profile?.lastName?.takeIf { it.isNotBlank() }?.let {
            if (isNotEmpty()) append(" ")
            append(it)
        }
        if (isEmpty()) profile?.username?.let { append(it) }
    }

    Box(modifier = Modifier.fillMaxSize().background(WarmOffWhite)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            GymGenieToolbar(
                title = "Профиль",
                actions = listOf(
                    ToolbarAction(
                        content = {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Ред.",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Normal,
                            )
                        },
                        onClick = onOpenEditProfile,
                    ),
                ),
            )

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
                        experience = exp ?: "",
                        frequency = freq ?: "",
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                SubscriptionCard(hasPro = hasPro, onClick = { if (!hasPro) onOpenPaywall() })

                ProfileSectionLabel("Общее")
                SettingsGroupCard {
                    SettingsRow(
                        label = "Язык",
                        icon = Icons.Filled.Language,
                        value = "Русский",
                        onClick = {},
                    )
                    HorizontalDivider(color = ProfileBorderDivider, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        label = "Тема",
                        icon = Icons.Filled.Palette,
                        value = "Системная",
                        onClick = {},
                    )
                    HorizontalDivider(color = ProfileBorderDivider, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        label = "Уведомления",
                        icon = Icons.Filled.Notifications,
                        onClick = {},
                    )
                }

                ProfileSectionLabel("Активность")
                SettingsGroupCard {
                    SettingsRow(
                        label = "Статистика",
                        icon = Icons.Filled.BarChart,
                        onClick = onOpenHistory,
                    )
                }

                ProfileSectionLabel("Поддержка")
                SettingsGroupCard {
                    SettingsRow(
                        label = "Помощь и FAQ",
                        icon = Icons.AutoMirrored.Filled.Help,
                        onClick = {},
                    )
                    HorizontalDivider(color = ProfileBorderDivider, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        label = "Отправить отзыв",
                        icon = Icons.Filled.Email,
                        onClick = {},
                    )
                }

                ProfileSectionLabel("Аккаунт")
                SettingsGroupCard {
                    SettingsRow(
                        label = "Выйти из аккаунта",
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        onClick = { confirmDialog = "logout" },
                    )
                    HorizontalDivider(color = ProfileBorderDivider, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        label = "Удалить аккаунт",
                        icon = Icons.Filled.Delete,
                        labelColor = ProfileDangerRow,
                        iconColor = ProfileDangerRow,
                        onClick = { confirmDialog = "delete" },
                    )
                }

                Text(
                    text = "Версия $versionName",
                    fontSize = 12.sp,
                    color = ProfileVersionTextColor,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 22.dp, bottom = 16.dp),
                )
            }
        }

        confirmDialog?.let { dialogKey ->
            ModalBottomSheet(
                onDismissRequest = { confirmDialog = null },
                sheetState = sheetState,
                containerColor = Color.White,
            ) {
                ConfirmAccountSheetContent(
                    isDelete = dialogKey == "delete",
                    onConfirm = {
                        when (dialogKey) {
                            "logout" -> {
                                confirmDialog = null
                                // Token clearance and profile-store reset
                                // happen in `App.kt`'s SessionManager listener;
                                // ProfileScreen only signals the intent.
                                sessionManager.triggerLogout()
                            }
                            "delete" -> confirmDialog = null
                        }
                    },
                    onDismiss = { confirmDialog = null },
                )
            }
        }
    }
}

