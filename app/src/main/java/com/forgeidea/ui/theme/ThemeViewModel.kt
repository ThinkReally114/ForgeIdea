package com.forgeidea.ui.theme

import androidx.lifecycle.ViewModel
import com.forgeidea.data.datastore.ApiKeyStore
import com.forgeidea.domain.model.PresetTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeViewModel(
    private val apiKeyStore: ApiKeyStore
) : ViewModel() {
    private val _theme = MutableStateFlow(apiKeyStore.getTheme())
    val theme: StateFlow<PresetTheme> = _theme.asStateFlow()

    fun setTheme(theme: PresetTheme) {
        apiKeyStore.setTheme(theme)
        _theme.value = theme
    }
}
