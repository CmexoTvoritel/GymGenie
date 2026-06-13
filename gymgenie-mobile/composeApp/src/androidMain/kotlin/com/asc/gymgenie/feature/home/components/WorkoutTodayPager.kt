package com.asc.gymgenie.feature.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.asc.gymgenie.ui.components.WorkoutPlanCard
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.utils.weekdayNameRu
import com.asc.gymgenie.workout.WorkoutPlanShortResponse

private val InactiveDot = androidx.compose.ui.graphics.Color(0xFFD5D5DB)

@Composable
fun WorkoutTodayPager(
    plans: List<WorkoutPlanShortResponse>,
    @Suppress("UNUSED_PARAMETER") isRecommended: Boolean,
    onView: (WorkoutPlanShortResponse) -> Unit,
    onStart: (WorkoutPlanShortResponse) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (plans.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { plans.size })

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            key = { plans[it].id },
            contentPadding = PaddingValues(horizontal = 0.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val plan = plans[page]
            WorkoutPlanCard(
                plan = plan,
                onView = { onView(plan) },
                onStart = { onStart(plan) },
            )
        }

        if (plans.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            DotIndicators(
                count = plans.size,
                selected = pagerState.currentPage,
            )
        }
    }
}

@Composable
private fun DotIndicators(count: Int, selected: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val isActive = index == selected
            val width by animateDpAsState(
                targetValue = if (isActive) 22.dp else 6.dp,
                label = "dotWidth",
            )
            val color by animateColorAsState(
                targetValue = if (isActive) Coral else InactiveDot,
                label = "dotColor",
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(width = width, height = 6.dp)
                    .clip(if (isActive) RoundedCornerShape(3.dp) else CircleShape)
                    .background(color),
            )
        }
    }
}

data class TodaySlot(
    val title: String,
    val subtitle: String,
    val showAll: Boolean,
    val plans: List<WorkoutPlanShortResponse>,
    val isRecommended: Boolean,
)

fun resolveTodaySlot(
    activePlans: List<WorkoutPlanShortResponse>,
    today: String,
): TodaySlot {
    val todayUpper = today.uppercase()
    val recurringToday = activePlans.filter { plan ->
        plan.scheduleType.equals("RECURRING", ignoreCase = true) &&
            plan.scheduleDays.any { it.equals(todayUpper, ignoreCase = true) }
    }
    val oneOff = activePlans.filter {
        it.scheduleType.equals("ONE_TIME", ignoreCase = true)
    }
    val dayRu = weekdayNameRu(todayUpper)

    return when {
        recurringToday.isNotEmpty() -> TodaySlot(
            title = "Тренировка на сегодня",
            subtitle = "${recurringToday.size} запланировано · $dayRu",
            showAll = true,
            plans = recurringToday,
            isRecommended = false,
        )
        oneOff.isNotEmpty() -> TodaySlot(
            title = "Рекомендуем сегодня",
            subtitle = "Подобрали разовую под твой день",
            showAll = false,
            plans = listOf(oneOff.first()),
            isRecommended = true,
        )
        else -> TodaySlot(
            title = "Рекомендуем сегодня",
            subtitle = "На этот день тренировки не запланированы",
            showAll = false,
            plans = emptyList(),
            isRecommended = true,
        )
    }
}
