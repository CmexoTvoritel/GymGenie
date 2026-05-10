package com.asc.gymgenie.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText

/**
 * Vertical block of trainer courses for the Home screen:
 *  - one full-width featured card,
 *  - a 2-column grid of small cards,
 *  - a "see all" outline button at the bottom.
 *
 * Data is hardcoded for now until the backend exposes a courses endpoint.
 * Layout dimensions, colors and typography mirror the design spec exactly.
 */

private val CardBorder = Color(0xFFEDEDEF)
private val StarYellow = Color(0xFFD4A017)

private data class FeaturedCourse(
    val title: String,
    val tag: String,
    val category: String,
    val duration: String,
    val author: String,
    val rating: String,
    val gradientStart: Color,
    val gradientEnd: Color,
)

private data class SmallCourse(
    val title: String,
    val author: String,
    val duration: String,
    val rating: String,
    val badge: String,
    val gradientStart: Color,
    val gradientEnd: Color,
)

private val featuredCourse = FeaturedCourse(
    title = "Утреннее пробуждение",
    tag = "популярное",
    category = "Йога",
    duration = "25 мин",
    author = "Мария Л.",
    rating = "4.9",
    gradientStart = Color(0xFFFF8674),
    gradientEnd = Color(0xFFFF5A3C),
)

private val smallCourses = listOf(
    SmallCourse(
        title = "Кардио Хит",
        author = "Елена В.",
        duration = "20 мин",
        rating = "5.0",
        badge = "Интенсив",
        gradientStart = Color(0xFFFFAA8A),
        gradientEnd = Color(0xFFE94A2C),
    ),
    SmallCourse(
        title = "Сила и мощь",
        author = "Борис Л.",
        duration = "20 мин",
        rating = "5.0",
        badge = "Интенсив",
        gradientStart = Color(0xFF3D3D45),
        gradientEnd = Color(0xFF0A0A0A),
    ),
)

@Composable
fun CoursesBlock(
    onSeeAll: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FeaturedCourseCard(course = featuredCourse)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            smallCourses.forEach { course ->
                Box(modifier = Modifier.weight(1f)) {
                    SmallCourseCard(course = course)
                }
            }
        }

        SeeAllCoursesButton(onClick = onSeeAll)
    }
}

@Composable
private fun FeaturedCourseCard(course: FeaturedCourse) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(
                width = 1.5.dp,
                color = CardBorder,
                shape = RoundedCornerShape(20.dp),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(
                    Brush.linearGradient(
                        listOf(course.gradientStart, course.gradientEnd),
                    ),
                ),
        ) {
            // "популярное" frosted badge — top-left
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.85f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = course.tag,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                )
            }

            // Title + category overlay — bottom-left
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            ) {
                Text(
                    text = course.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${course.category} · ${course.duration}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }

        // Author row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFFAA8A), Color(0xFFFF7E5F)),
                        ),
                    ),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = course.author,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            RatingPill(rating = course.rating)
        }
    }
}

@Composable
private fun RatingPill(rating: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFFFF6D6))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "⭐",
            fontSize = 11.sp,
            color = StarYellow,
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = rating,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = DeepInk,
        )
    }
}

@Composable
private fun SmallCourseCard(course: SmallCourse) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(
                width = 1.5.dp,
                color = CardBorder,
                shape = RoundedCornerShape(18.dp),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(
                    Brush.linearGradient(
                        listOf(course.gradientStart, course.gradientEnd),
                    ),
                ),
        ) {
            // Duration badge — bottom-right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = course.duration,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = course.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = course.author,
                fontSize = 11.5.sp,
                color = MutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE1F1FF))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = course.badge,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A84FF),
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⭐", fontSize = 10.sp, color = StarYellow)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = course.rating,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepInk,
                    )
                }
            }
        }
    }
}

@Composable
private fun SeeAllCoursesButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = 1.5.dp,
                color = CardBorder,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Посмотреть все курсы",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DeepInk,
        )
    }
}
