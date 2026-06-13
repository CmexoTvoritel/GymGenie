package com.asc.gymgenie.feature.meal_plan_detail.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.R
import com.asc.gymgenie.ui.theme.Coral

private val DeleteBg = Color(0xFFFFF0EC)
private val DeleteIcon = Color(0xFFE94A2C)
private val BorderColor = Color(0xFFEDEDEF)

@Composable
fun BottomActionBar(
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    showEdit: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = BorderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1f,
                )
            }
            .background(Color.White)
            .padding(top = 12.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .then(if (showEdit) Modifier.size(52.dp) else Modifier.fillMaxWidth().height(52.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(DeleteBg)
                .clickable { onDelete() },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = "Удалить",
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(DeleteIcon),
            )
        }

        if (showEdit) {
            Spacer(modifier = Modifier.width(12.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Coral)
                    .clickable { onEdit() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = "Редактировать",
                    modifier = Modifier.size(18.dp),
                    colorFilter = ColorFilter.tint(Color.White),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Редактировать",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}
