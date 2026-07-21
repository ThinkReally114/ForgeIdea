package com.forgeidea.ui.theme

import com.forgeidea.domain.model.PresetTheme
import kotlinx.coroutines.flow.MutableStateFlow

class ThemeState(initial: PresetTheme) {
    val theme: MutableStateFlow<PresetTheme> = MutableStateFlow(initial)
}
