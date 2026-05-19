package com.asc.gymgenie.feature.workout_session.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.presentation.ExerciseDetailViewModel
import com.asc.gymgenie.ui.theme.Coral
import com.asc.gymgenie.ui.theme.WarmOffWhite
import com.asc.gymgenie.ui.theme.OnBackground
import com.asc.gymgenie.ui.theme.OnSurfaceVariant
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
                    color = OnSurfaceVariant,
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
                    color = OnBackground,
                )
                if (ex.nameEn.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ex.nameEn,
                        fontSize = 14.sp,
                        color = OnSurfaceVariant,
                    )
                }

                ex.description?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Описание",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnBackground,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = it, fontSize = 14.sp, color = OnBackground, lineHeight = 20.sp)
                }

                if (ex.equipment.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Оборудование",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnBackground,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = ex.equipment.joinToString(", "),
                        fontSize = 14.sp,
                        color = OnBackground,
                    )
                }

                if (ex.instructions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Техника выполнения",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnBackground,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ex.instructions.forEachIndexed { index, step ->
                        Text(
                            text = "${index + 1}. $step",
                            fontSize = 14.sp,
                            color = OnBackground,
                            lineHeight = 20.sp,
                        )
                        if (index < ex.instructions.lastIndex) {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}
