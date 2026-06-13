package com.asc.gymgenie.feature.main

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.Neutrals400
import com.asc.gymgenie.ui.theme.Neutrals700

@Composable
fun BottomNavBar(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(items.isNotEmpty()) { "BottomNavBar requires at least one item" }

    val itemWidth = ItemWidth
    val itemHeight = ItemHeight
    val barHeight = BarHeight
    val edgePadding = EdgePadding
    val barWidth = edgePadding * 2 + itemWidth * items.size

    val targetIndicatorX = edgePadding + itemWidth * selectedIndex
    val indicatorX by animateDpAsState(
        targetValue = targetIndicatorX,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "BottomNavIndicatorX",
    )

    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            .padding(bottom = bottomInset + 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(barHeight)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(BarCornerRadius),
                    ambientColor = Color.Black.copy(alpha = 0.10f),
                    spotColor = Color.Black.copy(alpha = 0.10f),
                )
                .clip(RoundedCornerShape(BarCornerRadius)),
        ) {
            GlassBackdrop(modifier = Modifier.fillMaxSize())
            HairlineBorder(modifier = Modifier.fillMaxSize())
            TopSheen(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f),
            )

            Indicator(
                modifier = Modifier
                    .offset(x = indicatorX, y = (barHeight - itemHeight) / 2)
                    .size(width = itemWidth, height = itemHeight),
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = edgePadding),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    BottomNavItemView(
                        item = item,
                        itemWidth = itemWidth,
                        isSelected = index == selectedIndex,
                        onClick = { onItemSelected(index) },
                    )
                }
            }
        }
    }
}

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
)

@Composable
private fun GlassBackdrop(modifier: Modifier) {

    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.graphicsLayer {
            renderEffect = BlurEffect(20f, 20f, TileMode.Clamp)
        }
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .then(blurModifier)
            .background(Neutrals400.copy(alpha = 0.35f)),
    )
}

@Composable
private fun HairlineBorder(modifier: Modifier) {
    Box(
        modifier = modifier.drawBehind {

            val strokeWidth = 0.8.dp.toPx()
            val radius = size.height / 2f
            drawRoundedStroke(
                color = Color.White.copy(alpha = 0.45f),
                strokeWidth = strokeWidth,
                cornerRadius = radius,
            )
        },
    )
}

@Composable
private fun TopSheen(modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    topStart = BarCornerRadius,
                    topEnd = BarCornerRadius,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp,
                ),
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0f),
                    ),
                ),
            ),
    )
}

@Composable
private fun Indicator(modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(BarCornerRadius))
            .background(Coral),
    )
}

@Composable
private fun BottomNavItemView(
    item: BottomNavItem,
    itemWidth: Dp,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(itemWidth)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        ItemContent(
            item = item,
            color = if (isSelected) Color.White else Neutrals700,
            useSelectedIcon = isSelected,
        )
    }
}

@Composable
private fun ItemContent(
    item: BottomNavItem,
    color: Color,
    useSelectedIcon: Boolean,
) {
    Column(
        modifier = Modifier.wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (useSelectedIcon) item.selectedIcon else item.icon,
            contentDescription = item.title,
            tint = color,
            modifier = Modifier.size(IconSize),
        )
        Spacer(Modifier.height(IconLabelGap))
        Text(
            text = item.title,
            color = color,
            fontSize = LabelFontSize,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundedStroke(
    color: Color,
    strokeWidth: Float,
    cornerRadius: Float,
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
        size = androidx.compose.ui.geometry.Size(
            width = size.width - strokeWidth,
            height = size.height - strokeWidth,
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth),
    )
}

private val BarHeight: Dp = 60.dp
private val ItemWidth: Dp = 91.dp
private val ItemHeight: Dp = 54.dp
private val EdgePadding: Dp = 6.dp
private val IconSize: Dp = 24.dp
private val IconLabelGap: Dp = 2.dp
private val BarCornerRadius: Dp = 100.dp
private val LabelFontSize = 13.sp
