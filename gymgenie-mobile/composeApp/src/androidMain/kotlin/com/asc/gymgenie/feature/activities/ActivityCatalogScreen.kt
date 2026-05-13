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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.asc.gymgenie.activity.ActivityCatalogResponse
import com.asc.gymgenie.activity.ActivityKind
import com.asc.gymgenie.activity.ActivityRing
import com.asc.gymgenie.presentation.ActivityCatalogViewModel
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.RingLife
import com.asc.gymgenie.ui.theme.RingMind
import com.asc.gymgenie.ui.theme.RingMove
import com.asc.gymgenie.ui.theme.SoftCard
import com.asc.gymgenie.ui.theme.WarmOffWhite
import org.koin.core.context.GlobalContext

/**
 * Activity catalog: full list of activities the user can add to their daily
 * plan. Items are grouped by ring (Movement / Mind / Life) and the per-row
 * toggle reflects the current plan membership exposed by
 * [ActivityCatalogViewModel].
 */
@Composable
fun ActivityCatalogScreen(onBack: () -> Unit) {
    val koin = remember { GlobalContext.get() }
    val viewModel = remember { koin.get<ActivityCatalogViewModel>() }
    val state by viewModel.state.collectAsState()
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.load() }

    DisposableEffect(viewModel) {
        onDispose { viewModel.onCleared() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .statusBarsPadding(),
    ) {
        TopBar(onBack = onBack)
        SearchBar(value = query, onValueChange = { query = it })

        when {
            state.isLoading -> CenteredSpinner()
            state.error != null && state.catalog.isEmpty() ->
                ErrorState(message = state.error.orEmpty(), onRetry = viewModel::load)
            else -> CatalogList(
                catalog = state.catalog,
                planIds = state.planIds,
                query = query,
                onTogglePlan = viewModel::togglePlan,
            )
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
                textStyle = TextStyle(color = DeepInk, fontSize = 15.sp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CatalogList(
    catalog: List<ActivityCatalogResponse>,
    planIds: Set<String>,
    query: String,
    onTogglePlan: (String) -> Unit,
) {
    val filtered = remember(catalog, query) {
        if (query.isBlank()) catalog
        else catalog.filter { it.name.contains(query, ignoreCase = true) }
    }

    if (filtered.isEmpty()) {
        EmptyState(
            icon = "🔍",
            message = "Ничего не найдено",
            hint = "Попробуй изменить запрос",
        )
        return
    }

    val grouped = remember(filtered) { filtered.groupBy { it.ring } }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActivityRing.entries.forEach { ring ->
            val items = grouped[ring.name].orEmpty()
            if (items.isEmpty()) return@forEach

            item(key = "header-${ring.name}") {
                Text(
                    text = ringLabel(ring),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedText,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }
            items(items, key = { it.id }) { activity ->
                CatalogActivityCard(
                    activity = activity,
                    isInPlan = activity.id in planIds,
                    onToggle = { onTogglePlan(activity.id) },
                )
            }
        }
    }
}

@Composable
private fun CatalogActivityCard(
    activity: ActivityCatalogResponse,
    isInPlan: Boolean,
    onToggle: () -> Unit,
) {
    val ringColor = ringColorFor(activity.ring)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(ringColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = activity.name.take(1).uppercase(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ringColor,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepInk,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = kindLabel(activity.kind),
                fontSize = 12.sp,
                color = MutedText,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isInPlan) ringColor else SoftCard)
                .clickable { onToggle() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isInPlan) "✓" else "+",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isInPlan) Color.White else ringColor,
            )
        }
    }
}

@Composable
private fun CenteredSpinner() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AccentOrange)
    }
}

@Composable
private fun EmptyState(icon: String, message: String, hint: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SoftCard)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepInk,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = hint, fontSize = 13.sp, color = MutedText)
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SoftCard)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "⚠️", fontSize = 32.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Не удалось загрузить",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepInk,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = message, fontSize = 13.sp, color = MutedText)
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentOrange)
                    .clickable { onRetry() }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "Повторить",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

private fun ringColorFor(ring: String): Color = when (ring) {
    ActivityRing.MOVE.name -> RingMove
    ActivityRing.MIND.name -> RingMind
    ActivityRing.LIFE.name -> RingLife
    else -> AccentOrange
}

private fun ringLabel(ring: ActivityRing): String = when (ring) {
    ActivityRing.MOVE -> "Движение"
    ActivityRing.MIND -> "Разум"
    ActivityRing.LIFE -> "Режим"
}

private fun kindLabel(kind: String): String =
    when (runCatching { ActivityKind.valueOf(kind) }.getOrNull()) {
        ActivityKind.BINARY -> "Да/Нет"
        ActivityKind.COUNTER -> "Счётчик"
        ActivityKind.PRESET -> "Пресеты"
        null -> kind
    }
