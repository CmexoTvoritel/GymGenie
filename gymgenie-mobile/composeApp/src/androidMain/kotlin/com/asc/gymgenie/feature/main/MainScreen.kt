package com.asc.gymgenie.feature.main

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.feature.home.HomeScreen
import com.asc.gymgenie.feature.workouts.WorkoutsScreen
import com.asc.gymgenie.ui.theme.OnBackground
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.ui.theme.OnSurfaceVariant
import com.asc.gymgenie.ui.theme.Primary

enum class MainTab(
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    HOME("Главная", Icons.Outlined.Home, Icons.Filled.Home),
    WORKOUTS("Тренировки", Icons.Outlined.Home, Icons.Filled.Home), // will use custom
    AI_COACH("ИИ", Icons.Outlined.Home, Icons.Filled.Home), // will use custom
    STATS("Статистика", Icons.Outlined.Home, Icons.Filled.Home), // will use custom
    PROFILE("Профиль", Icons.Outlined.Person, Icons.Filled.Person),
}

@Composable
fun MainScreen(tokenStorage: TokenStorage) {
    var selectedTab by remember { mutableStateOf(MainTab.HOME) }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
        ) {
            when (selectedTab) {
                MainTab.HOME -> HomeScreen(tokenStorage = tokenStorage)
                MainTab.WORKOUTS -> WorkoutsScreen(tokenStorage = tokenStorage)
                MainTab.AI_COACH -> PlaceholderScreen("ИИ Тренер", "Скоро будет доступен")
                MainTab.STATS -> PlaceholderScreen("Статистика", "Скоро будет доступна")
                MainTab.PROFILE -> PlaceholderScreen("Профиль", "Скоро будет доступен")
            }
        }

        // Custom floating bottom bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                )
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color.White)
                .padding(top = 12.dp, bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                // Home
                TabItem(
                    title = "Главная",
                    icon = if (selectedTab == MainTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                    isSelected = selectedTab == MainTab.HOME,
                    onClick = { selectedTab = MainTab.HOME },
                )

                // Workouts
                TabItem(
                    title = "Тренировки",
                    iconText = "\uD83C\uDFCB",
                    isSelected = selectedTab == MainTab.WORKOUTS,
                    onClick = { selectedTab = MainTab.WORKOUTS },
                )

                // AI Center button (elevated)
                Box(
                    modifier = Modifier
                        .offset(y = (-16).dp)
                        .size(56.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = Primary.copy(alpha = 0.4f),
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Primary, Primary.copy(alpha = 0.8f)),
                            ),
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { selectedTab = MainTab.AI_COACH },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "\u2728",
                        fontSize = 22.sp,
                    )
                }

                // Stats
                TabItem(
                    title = "Статистика",
                    iconText = "\uD83D\uDCCA",
                    isSelected = selectedTab == MainTab.STATS,
                    onClick = { selectedTab = MainTab.STATS },
                )

                // Profile
                TabItem(
                    title = "Профиль",
                    icon = if (selectedTab == MainTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                    isSelected = selectedTab == MainTab.PROFILE,
                    onClick = { selectedTab = MainTab.PROFILE },
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    title: String,
    icon: ImageVector? = null,
    iconText: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) Primary else OnSurfaceVariant,
            )
        } else if (iconText != null) {
            Text(
                text = iconText,
                fontSize = 20.sp,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Primary else OnSurfaceVariant,
        )
    }
}

@Composable
fun PlaceholderScreen(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = OnBackground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
        )
    }
}
