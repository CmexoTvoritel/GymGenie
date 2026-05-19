package com.asc.gymgenie.navigation.tabs.ai

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value

interface AiComponent {
    val showBottomBar: Value<Boolean>
    fun setBottomBarVisible(visible: Boolean)
}

class DefaultAiComponent(
    componentContext: ComponentContext,
) : AiComponent, ComponentContext by componentContext {
    private val _showBottomBar = MutableValue(true)
    override val showBottomBar: Value<Boolean> = _showBottomBar
    override fun setBottomBarVisible(visible: Boolean) {
        _showBottomBar.value = visible
    }
}
