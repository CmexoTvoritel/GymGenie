package com.asc.gymgenie.feature.workout_session.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.utils.muscleGroupNameRu
import com.asc.gymgenie.presentation.ExerciseDetailViewModel
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.WarmOffWhite
import org.koin.core.context.GlobalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExerciseInfoSheet(
    exerciseId: String?,
    onDismiss: () -> Unit,
) {
    if (exerciseId == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = WarmOffWhite,
    ) {
        ExerciseDetailSheetContent(exerciseId = exerciseId)
    }
}

@Composable
internal fun ExerciseDetailSheetContent(exerciseId: String) {
    val koin = remember { GlobalContext.get() }
    val viewModel = remember(exerciseId) { koin.get<ExerciseDetailViewModel>() }
    DisposableEffect(exerciseId) { onDispose { viewModel.onCleared() } }

    LaunchedEffect(exerciseId) { viewModel.load(exerciseId) }

    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 24.dp),
    ) {
        when {
            state.isLoading && state.exercise == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Coral)
                }
            }

            state.errorMessage != null && state.exercise == null -> {
                Text(
                    text = state.errorMessage ?: "Ошибка загрузки",
                    color = MutedText,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(vertical = 32.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }

            state.exercise != null -> {
                val ex = state.exercise!!

                Text(
                    text = ex.nameRu,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepInk,
                )
                if (ex.nameEn.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ex.nameEn,
                        fontSize = 14.sp,
                        color = MutedText,
                    )
                }

                if (ex.muscleGroup.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = muscleGroupNameRu(ex.muscleGroup),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Coral)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }

                ex.description?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    SheetSectionTitle("Описание")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it, fontSize = 17.sp, color = DeepInk, lineHeight = 22.sp)
                }

                if (ex.instructions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SheetSectionTitle("Техника выполнения")
                    Spacer(modifier = Modifier.height(10.dp))
                    ex.instructions.forEachIndexed { index, step ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Coral),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = (index + 1).toString(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = step,
                                fontSize = 17.sp,
                                color = DeepInk,
                                lineHeight = 22.sp,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (index < ex.instructions.lastIndex) {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }

                ex.techniqueTip?.takeIf { it.isNotBlank() }?.let { tip ->
                    Spacer(modifier = Modifier.height(16.dp))
                    SheetSectionTitle("Совет по технике")
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Coral.copy(alpha = 0.08f))
                            .padding(14.dp),
                    ) {
                        Text(
                            text = tip,
                            fontSize = 17.sp,
                            color = DeepInk,
                            lineHeight = 22.sp,
                        )
                    }
                }

                if (ex.equipment.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SheetSectionTitle("Оборудование")
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ex.equipment.forEach { item ->
                            Text(
                                text = item,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepInk,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .border(1.dp, DeepInk.copy(alpha = 0.25f), RoundedCornerShape(50))
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = DeepInk,
    )
}
