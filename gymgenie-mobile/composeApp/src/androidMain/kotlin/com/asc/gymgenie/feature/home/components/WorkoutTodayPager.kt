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
import androidx.compose.foundation.layout.width
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
import com.asc.gymgenie.workout.WorkoutPlanShortResponse

private val InactiveDot = androidx.compose.ui.graphics.Color(0xFFD5D5DB)

/**
 * Horizontally pageable list of workout cards for the day.
 *
 * Renders one [WorkoutPlanCard] per plan and shows a pill/dot indicator when
 * more than one plan is available. Single-plan callers get a clean card with
 * no indicator overhead, so the component is safe to use in both the
 * recurring and recommendation slots.
 *
 * [isRecommended] is kept on the public surface so the today-slot resolution
 * stays untouched; the home pager uses [WorkoutPlanCard]'s "no view" footer
 * variant, which already styles itself correctly for both slot types.
 */
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

/**
 * Builds the today/recommendation slot from the active plan list.
 *
 * The selection rules live here (and not in the ViewModel) by design — the
 * filtering depends on the device's day-of-week clock, which is a UI concern
 * and would force the shared layer to take a system-clock dependency just for
 * this view. The contract is documented on the `today*` parameters so it can
 * be unit-tested if it grows further.
 */
data class TodaySlot(
    val title: String,
    val subtitle: String,
    val showAll: Boolean,
    val plans: List<WorkoutPlanShortResponse>,
    val isRecommended: Boolean,
)

private val DayNamesRu = mapOf(
    "MONDAY" to "понедельник",
    "TUESDAY" to "вторник",
    "WEDNESDAY" to "среда",
    "THURSDAY" to "четверг",
    "FRIDAY" to "пятница",
    "SATURDAY" to "суббота",
    "SUNDAY" to "воскресенье",
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
    val dayRu = DayNamesRu[todayUpper] ?: todayUpper.lowercase()

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
