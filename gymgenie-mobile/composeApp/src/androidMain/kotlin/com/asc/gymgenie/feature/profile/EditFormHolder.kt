package com.asc.gymgenie.feature.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.essenty.instancekeeper.InstanceKeeper

class EditFormHolder : InstanceKeeper.Instance {
    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var weightKg by mutableIntStateOf(70)
    var heightCm by mutableIntStateOf(175)
    var ageYears by mutableIntStateOf(25)
    var experience by mutableStateOf("Недавно")
    var frequency by mutableStateOf("Редко")
    var hasHealthIssues by mutableStateOf(false)
    var healthIssues by mutableStateOf("")
    var initialized by mutableStateOf(false)

    override fun onDestroy() = Unit
}
