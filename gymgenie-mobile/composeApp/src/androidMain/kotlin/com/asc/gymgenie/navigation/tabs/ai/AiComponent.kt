package com.asc.gymgenie.navigation.tabs.ai

import com.arkivanov.decompose.ComponentContext

interface AiComponent

class DefaultAiComponent(
    componentContext: ComponentContext,
) : AiComponent, ComponentContext by componentContext
