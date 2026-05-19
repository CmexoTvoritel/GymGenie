package com.asc.gymgenie.feature.workout_history

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.gymgenie.presentation.WorkoutHistoryViewModel
import org.koin.core.context.GlobalContext

class WorkoutHistoryViewModelHolder : ViewModel() {
    val shared: WorkoutHistoryViewModel = GlobalContext.get().get()

    override fun onCleared() {
        shared.onCleared()
        super.onCleared()
    }
}

@Composable
fun rememberWorkoutHistoryViewModel(): WorkoutHistoryViewModel {
    val holder: WorkoutHistoryViewModelHolder = viewModel()
    return holder.shared
}
