package com.asc.gymgenie.feature.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.essenty.instancekeeper.InstanceKeeper

/**
 * Shared mutable form state for the EditProfile flow.
 *
 * Lives in Decompose's [InstanceKeeper] so it survives configuration changes and is shared between
 * [EditProfileScreen] and the dedicated sub-screens ([EditMetricsScreen], [EditExperienceScreen],
 * [EditHealthScreen]). The sub-screens read/write the same fields, and EditProfileScreen reflects
 * the latest values when the user returns via Decompose back navigation.
 *
 * [initialized] is used by EditProfileScreen to seed the form from the loaded profile exactly once
 * and avoid clobbering in-flight user edits on recomposition.
 */
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
