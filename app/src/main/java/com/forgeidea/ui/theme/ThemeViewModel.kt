package com.forgeidea.ui.theme

import androidx.lifecycle.ViewModel
import com.forgeidea.data.datastore.ApiKeyStore
import com.forgeidea.domain.model.PresetTheme
import kotlinx.coroutines.flow.StateFlow

class ThemeViewModel(
    private val apiKeyStore: ApiKeyStore,
    private val themeState: ThemeState
) : ViewModel() {
    val theme: StateFlow<PresetTheme> = themeState.theme

    fun setTheme(theme: PresetTheme) {
        apiKeyStore.setTheme(theme)
        themeState.theme.value = theme
    }
}
