package com.asc.gymgenie.feature.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.feature.activities.GoalCategory
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.PastelBlue
import com.asc.gymgenie.ui.theme.PastelCoral
import com.asc.gymgenie.ui.theme.PastelGreen
import com.asc.gymgenie.ui.theme.PastelLavender
import com.asc.gymgenie.ui.theme.PastelYellow
import com.asc.gymgenie.ui.theme.SoftCard
import com.asc.gymgenie.ui.theme.WarmOffWhite

private data class ActivityCategory(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val background: Color,
    val fullWidth: Boolean = false,
    val unit: String = "раз",
    val defaultValue: Int = 1,
    val step: Int = 1,
)

// TODO: replace with backend-driven catalog when the endpoint is exposed.
private val categories = listOf(
    ActivityCategory("🏃", "Физическая активность", "Шаги, кардио, бег", PastelCoral, unit = "шагов", defaultValue = 10000, step = 500),
    ActivityCategory("💧", "Питьевой режим", "Вода, напитки", PastelBlue, unit = "стаканов", defaultValue = 8, step = 1),
    ActivityCategory("😴", "Сон", "Отслеживание сна", PastelLavender, unit = "часов", defaultValue = 8, step = 1),
    ActivityCategory("🧘", "Медитация", "Осознанность, дыхание", PastelGreen, unit = "минут", defaultValue = 15, step = 5),
    ActivityCategory("⭐", "Кастомная цель", "Создай свою активность", PastelYellow, fullWidth = true, unit = "раз", defaultValue = 1, step = 1),
)

@Composable
fun ActivityCatalogScreen(
    onBack: () -> Unit,
    onCategorySelected: (GoalCategory) -> Unit = {},
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(query) {
        if (query.isBlank()) categories
        else categories.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.subtitle.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .statusBarsPadding(),
    ) {
        TopBar(onBack = onBack)
        SearchBar(value = query, onValueChange = { query = it })

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                items = filtered,
                span = { item ->
                    if (item.fullWidth) GridItemSpan(2) else GridItemSpan(1)
                },
            ) { category ->
                CategoryCard(
                    category = category,
                    onClick = {
                        onCategorySelected(
                            GoalCategory(
                                emoji = category.emoji,
                                title = category.title,
                                unit = category.unit,
                                defaultValue = category.defaultValue,
                                step = category.step,
                            )
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SoftCard)
                .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "‹", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DeepInk)
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Каталог активностей",
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DeepInk,
        )

        Spacer(modifier = Modifier.weight(1f))

        Spacer(modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun SearchBar(value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SoftCard)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "🔍", fontSize = 14.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = "Найти активность...",
                    fontSize = 15.sp,
                    color = MutedText,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                cursorBrush = SolidColor(DeepInk),
                textStyle = TextStyle(
                    color = DeepInk,
                    fontSize = 15.sp,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CategoryCard(category: ActivityCategory, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (category.fullWidth) 120.dp else 150.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(category.background)
            .clickable { onClick() }
            .padding(20.dp),
    ) {
        Text(text = category.emoji, fontSize = if (category.fullWidth) 44.sp else 40.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = category.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DeepInk,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.subtitle,
            fontSize = 12.sp,
            color = MutedText,
        )
    }
}
