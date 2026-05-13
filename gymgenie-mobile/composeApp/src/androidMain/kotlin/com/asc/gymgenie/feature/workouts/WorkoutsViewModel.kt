package com.asc.gymgenie.feature.workouts

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.gymgenie.presentation.WorkoutsViewModel
import org.koin.core.context.GlobalContext

class WorkoutsViewModelHolder : ViewModel() {
    val shared: WorkoutsViewModel = GlobalContext.get().get()

    override fun onCleared() {
        shared.onCleared()
        super.onCleared()
    }
}

@Composable
fun rememberWorkoutsViewModel(): WorkoutsViewModel {
    val holder: WorkoutsViewModelHolder = viewModel()
    return holder.shared
}
