package com.asc.gymgenie.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.auth.AuthApi
import com.asc.gymgenie.common.createAuthenticatedClient
import com.asc.gymgenie.feature.home.components.ActivityRingsCard
import com.asc.gymgenie.feature.home.components.ActivityRowsCard
import com.asc.gymgenie.feature.home.components.AiTipCard
import com.asc.gymgenie.feature.home.components.CourseCard
import com.asc.gymgenie.feature.home.components.HomeHeaderSection
import com.asc.gymgenie.feature.home.components.WorkoutOfTheDayCard
import com.asc.gymgenie.presentation.HomeViewModel
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.SoftCard
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.user.UserApi
import com.asc.gymgenie.workout.ActiveWorkoutSession
import com.asc.gymgenie.workout.WorkoutApi
import com.asc.gymgenie.workout.WorkoutPlanShortResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home screen — premium redesign.
 *
 * Keeps the existing KMM [HomeViewModel] contract intact and only changes
 * the UI layer. Navigation to Activities / ActivityCatalog is hoisted to
 * the parent ([com.asc.gymgenie.feature.main.MainScreen]) via callbacks, so
 * this screen stays agnostic of the surrounding navigation container.
 */
@Composable
fun HomeScreen(
    tokenStorage: TokenStorage,
    onLogout: () -> Unit,
    onOpenActivities: () -> Unit,
    onOpenCatalog: () -> Unit,
    onSessionReady: (ActiveWorkoutSession) -> Unit,
) {
    val viewModel = remember {
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
        onDispose { viewModel.onCleared() }
    }

    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        if (state.userProfile == null && !state.isLoading) {
            viewModel.load()
        }
    }

    LaunchedEffect(state.pendingSession) {
        state.pendingSession?.let { session ->
            onSessionReady(session)
            viewModel.clearPendingSession()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite)
            .statusBarsPadding(),
    ) {
        when {
            state.isLoading && state.userProfile == null -> LoadingState()

            state.errorMessage != null && state.userProfile == null ->
                ErrorState(
                    message = state.errorMessage.orEmpty(),
                    onRetry = viewModel::retry,
                )

            else -> HomeContent(
                username = state.username.ifBlank { "друг" },
                streakDays = state.streakDays,
                activePlans = state.activeWorkoutPlans,
                onStartPlan = { plan -> viewModel.startWorkout(plan.id, plan.name) },
                onOpenActivities = onOpenActivities,
                onOpenCatalog = onOpenCatalog,
                isLoadingSession = state.isLoadingSession,
                sessionError = state.sessionError,
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = AccentOrange)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Загрузка...", fontSize = 14.sp, color = MutedText)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "⚠️", fontSize = 40.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = MutedText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Повторить", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HomeContent(
    username: String,
    streakDays: Int,
    activePlans: List<WorkoutPlanShortResponse>,
    onStartPlan: (WorkoutPlanShortResponse) -> Unit,
    onOpenActivities: () -> Unit,
    onOpenCatalog: () -> Unit,
    isLoadingSession: Boolean,
    sessionError: String?,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            HomeHeaderSection(
                username = username,
                streakDays = streakDays,
                date = remember { currentDateLabel() },
            )
        }

        item {
            if (activePlans.isEmpty()) {
                EmptyWorkoutCard()
            } else {
                val plan = activePlans.first()
                WorkoutOfTheDayCard(plan = plan, onStart = { onStartPlan(plan) })
            }
        }

        if (isLoadingSession || sessionError != null) {
            item {
                SessionStatusBanner(
                    isLoading = isLoadingSession,
                    errorMessage = sessionError,
                )
            }
        }

        item {
            SectionHeaderPremium(
                title = "Активности",
                subtitle = "Отметить вручную — без умных часов",
                actionTitle = "Ещё",
                onAction = onOpenActivities,
            )
        }

        item { ActivityRingsCard() }

        item { ActivityRowsCard() }

        item {
            AddActivityButton(onClick = onOpenCatalog)
        }

        item { AiTipCard() }

        item {
            SectionHeaderPremium(
                title = "Курсы тренеров",
                subtitle = "От профи под твои цели",
                actionTitle = "Все",
                onAction = { /* TODO: hook up courses screen once backend exposes the list */ },
            )
        }

        item { CoursesRow() }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun SessionStatusBanner(isLoading: Boolean, errorMessage: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SoftCard)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = AccentOrange,
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Загружаем тренировку...",
                fontSize = 14.sp,
                color = DeepInk,
            )
        } else if (errorMessage != null) {
            Text(text = "⚠️", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = errorMessage,
                fontSize = 13.sp,
                color = MutedText,
            )
        }
    }
}

@Composable
private fun EmptyWorkoutCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SoftCard)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "🏋", fontSize = 28.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Нет активных тренировок",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepInk,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Создай или выбери план, чтобы начать",
            fontSize = 13.sp,
            color = MutedText,
        )
    }
}

@Composable
fun SectionHeaderPremium(
    title: String,
    subtitle: String? = null,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.widthIn(max = 260.dp)) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepInk,
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (actionTitle != null && onAction != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onAction() }
                    .padding(6.dp),
            ) {
                Text(
                    text = actionTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentOrange,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "→",
                    fontSize = 14.sp,
                    color = AccentOrange,
                )
            }
        }
    }
}

@Composable
private fun AddActivityButton(onClick: () -> Unit) {
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .drawBehind { drawDashedBorder(dashEffect) }
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(text = "+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepInk)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Добавить активность",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepInk,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDashedBorder(effect: PathEffect) {
    val stroke = Stroke(width = 3f, pathEffect = effect)
    drawRoundRectBorder(
        color = DeepInk.copy(alpha = 0.35f),
        size = Size(size.width, size.height),
        stroke = stroke,
        cornerPx = 16.dp.toPx(),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundRectBorder(
    color: Color,
    size: Size,
    stroke: Stroke,
    cornerPx: Float,
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(0f, 0f),
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerPx, cornerPx),
        style = stroke,
    )
}

@Composable
private fun CoursesRow() {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // TODO: wire up to backend when the courses endpoint is exposed.
        CourseCard(
            badge = "НОВИЧОК",
            title = "Силовая база",
            trainer = "Елена В.",
            ctaTitle = "Бесплатно",
            isPro = false,
        )
        CourseCard(
            badge = "ИНТЕНСИВ",
            title = "HIIT каждый день",
            trainer = "Борис Л.",
            ctaTitle = "PRO",
            isPro = true,
        )
    }
}

private fun currentDateLabel(): String {
    val formatter = SimpleDateFormat("EEEE, d MMMM", Locale("ru", "RU"))
    return formatter.format(Date())
}
